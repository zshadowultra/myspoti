package app.myspoti.spotify.gql

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class CachedGqlTest {
    private val vars = JsonObject(emptyMap())
    private val answer = json("""{"data":{"track":"A"}}""")
    private val refreshed = json("""{"data":{"track":"B"}}""")

    private fun json(text: String): JsonElement = Json.parseToJsonElement(text)
    private fun delegate(block: suspend (String, JsonObject) -> JsonElement) = object : Gql {
        override suspend fun query(name: String, variables: JsonObject) = block(name, variables)
    }

    // Unconfined + explicit gates makes ordering deterministic without sleeps or a device.
    // A plain owner Job also detects failures escaping the cache's supervisor boundary.
    private fun withScope(block: suspend (CoroutineScope) -> Unit) = runBlocking {
        withTimeout(5_000) {
            val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
            try { block(scope) } finally { scope.cancel() }
        }
    }

    @Test fun `ten catalog reads use one request and stale readers share a refresh`() = withScope { scope ->
        val calls = AtomicInteger()
        val now = AtomicLong()
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val cache = CachedGql(delegate { _, _ ->
            when (calls.incrementAndGet()) {
                1 -> answer
                else -> { started.complete(Unit); finish.await(); refreshed }
            }
        }, scope, now::get)

        repeat(10) { assertEquals(answer, cache.query("getTrack", vars)) }
        assertEquals(1, calls.get()) // uncached baseline: ten delegate calls
        now.set(300_000) // Exact fresh TTL boundary is stale, not a cache miss.
        repeat(20) { assertEquals(answer, cache.query("getTrack", vars)) }
        started.await()
        assertEquals(2, calls.get())
        finish.complete(Unit)
        assertEquals(refreshed, cache.query("getTrack", vars))
        assertEquals(2, calls.get())
        cache.clear()
        assertEquals(refreshed, cache.query("getTrack", vars))
        assertEquals(3, calls.get())
    }

    @Test fun `search TTL is shorter and ten TTLs require a foreground answer`() = withScope { scope ->
        val now = AtomicLong()
        val calls = AtomicInteger()
        val finish = CompletableDeferred<Unit>()
        val cache = CachedGql(delegate { _, _ ->
            if (calls.incrementAndGet() == 1) answer else { finish.await(); refreshed }
        }, scope, now::get)
        cache.query("searchTracks", vars)
        now.set(29_999)
        assertEquals(answer, cache.query("searchTracks", vars))
        assertEquals(1, calls.get())
        now.set(30_000)
        assertEquals(answer, cache.query("searchTracks", vars))
        assertEquals(2, calls.get())
        now.set(300_000)
        val expired = scope.async(start = CoroutineStart.UNDISPATCHED) { cache.query("searchTracks", vars) }
        assertFalse(expired.isCompleted)
        finish.complete(Unit)
        assertEquals(refreshed, expired.await())
        assertEquals(2, calls.get()) // Hard expiry joins the already-running refresh.
    }

    @Test fun `concurrent misses are coalesced and cancelling a waiter keeps the request`() = withScope { scope ->
        val calls = AtomicInteger()
        val finish = CompletableDeferred<Unit>()
        val cache = CachedGql(delegate { _, _ -> calls.incrementAndGet(); finish.await(); answer }, scope)
        val readers = List(20) { scope.async(start = CoroutineStart.UNDISPATCHED) { cache.query("getAlbum", vars) } }
        assertEquals(1, calls.get())
        readers.first().cancel()
        finish.complete(Unit)
        assertTrue(readers.first().isCancelled)
        assertEquals(List(19) { answer }, readers.drop(1).awaitAll())
        cache.query("getAlbum", vars)
        assertEquals(1, calls.get())
    }

    @Test fun `variable object ordering does not change keys but pages and operations do`() = withScope { scope ->
        val calls = AtomicInteger()
        val cache = CachedGql(delegate { _, _ -> calls.incrementAndGet(); answer }, scope)
        val first = json("""{"uri":"A","page":{"offset":0,"limit":10}}""") as JsonObject
        val reordered = json("""{"page":{"limit":10,"offset":0},"uri":"A"}""") as JsonObject
        cache.query("getAlbum", first)
        cache.query("getAlbum", reordered)
        assertEquals(1, calls.get())
        cache.query("getAlbum", buildJsonObject { put("uri", "B") })
        cache.query("getTrack", first)
        assertEquals(3, calls.get())
    }

    @Test fun `allowlist excludes writes library playback private and unknown reads`() = withScope { scope ->
        val calls = AtomicInteger()
        val cache = CachedGql(delegate { _, _ -> calls.incrementAndGet(); answer }, scope)
        val bypass = listOf("addToPlaylist", "removeFromPlaylist", "libraryV3", "fetchLibraryTracks",
            "fetchPlaylist", "fetchPlaylistContents", "home", "recents", "recentSearches", "canvas",
            "lyrics", "isFollowingUsers", "getBasePermission", "playerState", "unknownOperation")
        for (name in bypass) repeat(2) { cache.query(name, vars) }
        assertEquals(bypass.size * 2, calls.get())
    }

    @Test fun `all allowlisted operations retain successful responses`() = withScope { scope ->
        val calls = AtomicInteger()
        val cache = CachedGql(delegate { _, _ -> calls.incrementAndGet(); answer }, scope)
        val names = listOf("getTrack", "getAlbum", "queryArtistOverview", "queryArtistDiscographyAll",
            "queryArtistRelated", "queryArtistAppearsOn", "searchTracks", "searchArtists", "searchAlbums",
            "searchPlaylists", "searchPodcasts", "searchFullEpisodes", "searchAudiobooks", "searchUsers",
            "searchGenres", "searchSuggestions")
        for (name in names) repeat(2) { cache.query(name, vars) }
        assertEquals(names.size, calls.get())
    }

    @Test fun `entry eviction is access ordered`() = withScope { scope ->
        val calls = AtomicInteger()
        val cache = CachedGql(delegate { _, _ -> calls.incrementAndGet(); answer }, scope, maxEntries = 2)
        cache.query("getTrack", vars)
        cache.query("getAlbum", vars)
        cache.query("getTrack", vars) // Track becomes most recently used.
        cache.query("queryArtistOverview", vars)
        cache.query("getTrack", vars)
        assertEquals(3, calls.get())
        cache.query("getAlbum", vars)
        assertEquals(4, calls.get())
    }

    @Test fun `byte budget evicts and oversized or zero budget responses are not retained`() = withScope { scope ->
        val calls = AtomicInteger()
        val source = delegate { _, _ -> calls.incrementAndGet(); answer }
        val size = answer.toString().length * 2
        val cache = CachedGql(source, scope, maxBytes = size)
        cache.query("getTrack", vars)
        cache.query("getAlbum", vars)
        cache.query("getTrack", vars)
        assertEquals(3, calls.get())
        for (limited in listOf(CachedGql(source, scope, maxBytes = size - 1),
            CachedGql(source, scope, maxBytes = 0), CachedGql(source, scope, maxEntries = 0))) {
            repeat(2) { limited.query("getTrack", vars) }
        }
        assertEquals(9, calls.get())
        assertThrows(IllegalArgumentException::class.java) { CachedGql(source, scope, maxEntries = -1) }
        assertThrows(IllegalArgumentException::class.java) { CachedGql(source, scope, maxBytes = -1) }
    }

    @Test fun `partial errors and missing data are never retained`() = withScope { scope ->
        for (text in listOf("{}", "[]", "null", """{"data":null}""", """{"data":[]}""",
            """{"data":{},"errors":[{"message":"partial"}]}""", """{"data":{},"errors":"malformed"}""")) {
            val response = json(text)
            val calls = AtomicInteger()
            val cache = CachedGql(delegate { _, _ -> calls.incrementAndGet(); response }, scope)
            repeat(2) { assertEquals(response, cache.query("getTrack", vars)) }
            assertEquals(2, calls.get(), text)
        }
        for (text in listOf("""{"data":{}}""", """{"data":{},"errors":null}""", """{"data":{},"errors":[]}""")) {
            val calls = AtomicInteger()
            val cache = CachedGql(delegate { _, _ -> calls.incrementAndGet(); json(text) }, scope)
            repeat(2) { cache.query("getTrack", vars) }
            assertEquals(1, calls.get(), text)
        }
    }

    @Test fun `failed requests propagate to callers without cancelling owner and can retry`() = withScope { scope ->
        for (failure in listOf(IOException("offline"), GqlException("getTrack", "failed"))) {
            val calls = AtomicInteger()
            val cache = CachedGql(delegate { _, _ -> if (calls.incrementAndGet() == 1) throw failure else answer }, scope)
            assertSame(failure, runCatching { cache.query("getTrack", vars) }.exceptionOrNull())
            assertTrue(scope.coroutineContext[Job]!!.isActive)
            assertEquals(answer, cache.query("getTrack", vars))
            assertEquals(2, calls.get())
        }
        val errors = CachedGql(delegate { _, _ -> json("""{"errors":[{"message":"boom"}],"data":null}""") }, scope)
        val failure = runCatching { errors.query("getTrack", vars) }.exceptionOrNull()
        assertInstanceOf(GqlException::class.java, failure)
        assertEquals("getTrack: boom", failure?.message)
        assertTrue(scope.coroutineContext[Job]!!.isActive)
    }

    @Test fun `failed background refresh backs off while hard expiry still requires a request`() = withScope { scope ->
        val now = AtomicLong()
        val calls = AtomicInteger()
        val cache = CachedGql(delegate { _, _ -> if (calls.incrementAndGet() == 1) answer else throw IOException("offline") }, scope, now::get)
        cache.query("getTrack", vars)
        now.set(300_000)
        repeat(20) { assertEquals(answer, cache.query("getTrack", vars)) }
        assertEquals(2, calls.get())
        now.set(304_999)
        assertEquals(answer, cache.query("getTrack", vars))
        assertEquals(2, calls.get())
        now.set(305_000)
        assertEquals(answer, cache.query("getTrack", vars))
        assertEquals(3, calls.get())
        assertTrue(scope.coroutineContext[Job]!!.isActive)
        now.set(3_000_000)
        assertInstanceOf(IOException::class.java, runCatching { cache.query("getTrack", vars) }.exceptionOrNull())
        assertEquals(4, calls.get())
    }

    @Test fun `partial background response retains clean stale data without a request storm`() = withScope { scope ->
        val now = AtomicLong()
        val calls = AtomicInteger()
        val partial = json("""{"data":{"track":null},"errors":[{"message":"offline"}]}""")
        val cache = CachedGql(delegate { _, _ -> if (calls.incrementAndGet() == 1) answer else partial }, scope, now::get)
        cache.query("getTrack", vars)
        now.set(300_000)
        repeat(20) { assertEquals(answer, cache.query("getTrack", vars)) }
        assertEquals(2, calls.get())
        now.set(3_000_000)
        assertEquals(partial, cache.query("getTrack", vars)) // Hard expiry never hides delegate results.
        assertEquals(3, calls.get())
    }

    @Test fun `clear cancels multiple requests and old uncooperative completions cannot repopulate`() = withScope { scope ->
        val calls = AtomicInteger()
        val finishOld = CompletableDeferred<Unit>()
        val oldFinished = CompletableDeferred<Unit>()
        val cache = CachedGql(delegate { name, _ ->
            if (calls.incrementAndGet() <= 2) {
                withContext(NonCancellable) { finishOld.await() }
                if (name == "getTrack") oldFinished.complete(Unit)
                answer
            } else refreshed
        }, scope)
        val oldTrack = scope.async { cache.query("getTrack", vars) }
        val oldAlbum = scope.async { cache.query("getAlbum", vars) }
        assertEquals(2, calls.get())
        cache.clear()
        assertEquals(refreshed, cache.query("getTrack", vars))
        finishOld.complete(Unit)
        oldFinished.await()
        assertInstanceOf(CancellationException::class.java, runCatching { oldTrack.await() }.exceptionOrNull())
        assertInstanceOf(CancellationException::class.java, runCatching { oldAlbum.await() }.exceptionOrNull())
        assertEquals(refreshed, cache.query("getTrack", vars))
        assertEquals(refreshed, cache.query("getAlbum", vars))
        assertEquals(4, calls.get())
    }

    @Test fun `owning scope cancellation stops shared requests`() = withScope { scope ->
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val cache = CachedGql(delegate { _, _ ->
            started.complete(Unit)
            try { CompletableDeferred<Unit>().await(); answer } finally { cancelled.complete(Unit) }
        }, scope)
        val reader = scope.async { cache.query("getTrack", vars) }
        started.await()
        scope.cancel()
        cancelled.await()
        assertInstanceOf(CancellationException::class.java, runCatching { reader.await() }.exceptionOrNull())
    }
}
