package app.myspoti.spotify.gql

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Session-only LRU for allowlisted catalog/search reads, never playback or writes.
 * Fresh entries avoid requests; stale entries return immediately while one shared request
 * refreshes them. Results older than ten TTLs require a successful foreground request.
 * Refreshes populate the cache for the next reader, not an already-visible screen's state.
 * The byte budget estimates serialized UTF-16 size, not the full JSON object's heap cost.
 */
class CachedGql(
    private val delegate: Gql,
    scope: CoroutineScope,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 },
    private val maxEntries: Int = 32,
    private val maxBytes: Int = 4 * 1024 * 1024,
) : Gql {
    private data class Key(val name: String, val variables: JsonObject)
    private data class Entry(val value: JsonElement, val at: Long, val bytes: Int, val retryAfter: Long = 0)
    // Shared requests outlive an individual waiter, but not the owning session. A failed
    // background refresh must never cancel playback or the other requests in that session.
    private val requests = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
    private val lock = Any()
    private val entries = LinkedHashMap<Key, Entry>(16, 0.75f, true)
    private val inflight = mutableMapOf<Key, Deferred<JsonElement>>()
    private var generation = 0L
    private var bytes = 0L

    init {
        require(maxEntries >= 0 && maxBytes >= 0) { "cache budgets must be non-negative" }
    }

    fun clear() {
        val pending = synchronized(lock) {
            generation++
            entries.clear()
            bytes = 0
            inflight.values.toList().also { inflight.clear() }
        }
        // Completion callbacks remove requests too: detach before cancelling, outside the lock.
        pending.forEach { it.cancel() }
    }

    override suspend fun query(name: String, variables: JsonObject): JsonElement {
        val ttl = ttlMs(name) ?: return delegate.query(name, variables)
        val key = Key(name, variables)
        val (cached, pending) = synchronized(lock) {
            val entry = entries[key]
            val now = clock()
            val age = entry?.let { now - it.at }
            if (entry != null && age != null && age in 0 until ttl * 10) {
                entry.value to if (age >= ttl && now >= entry.retryAfter) request(key) else null
            } else {
                null to request(key)
            }
        }
        // Register under the lock, start outside it. Stale readers share the Deferred directly:
        // no queued launch per reader, no IO on a WebView/UI callback under the cache lock.
        pending?.start()
        return cached ?: pending!!.await()
    }

    /** Called only while holding [lock]. */
    private fun request(key: Key): Deferred<JsonElement> {
        inflight[key]?.let { return it }
        val epoch = generation
        val job = requests.async(start = CoroutineStart.LAZY) {
            val result = delegate.query(key.name, key.variables)
            checkErrors(key.name, result)
            val envelope = result as? JsonObject
            val errors = envelope?.get("errors")
            val clean = errors == null || errors is JsonNull || (errors is JsonArray && errors.isEmpty())
            val size = result.toString().length.toLong() * 2
            // Partial/error envelopes and oversized pages can be returned, but never retained.
            if (envelope?.get("data") is JsonObject && clean && maxEntries > 0 && size <= maxBytes) {
                synchronized(lock) {
                    if (epoch == generation) {
                        entries.remove(key)?.let { bytes -= it.bytes }
                        entries[key] = Entry(result, clock(), size.toInt())
                        bytes += size
                        while (entries.size > maxEntries || bytes > maxBytes) {
                            val oldest = entries.entries.iterator()
                            bytes -= oldest.next().value.bytes
                            oldest.remove()
                        }
                    }
                }
            } else {
                deferRefresh(key, epoch)
            }
            result
        }
        inflight[key] = job
        job.invokeOnCompletion { cause ->
            synchronized(lock) {
                if (inflight[key] === job) inflight.remove(key)
                if (cause != null && cause !is CancellationException) deferRefresh(key, epoch)
            }
        }
        return job
    }

    /** Retry failures on a bounded cadence, not once per stale reader during an outage. */
    private fun deferRefresh(key: Key, epoch: Long) = synchronized(lock) {
        if (epoch == generation) {
            entries[key]?.let { entries[key] = it.copy(retryAfter = clock() + 5_000) }
        }
    }

    private fun ttlMs(name: String): Long? = when (name) {
        "getTrack", "getAlbum", "queryArtistOverview", "queryArtistDiscographyAll", "queryArtistRelated", "queryArtistAppearsOn" -> 5 * 60_000L
        "searchTracks", "searchArtists", "searchAlbums", "searchPlaylists", "searchPodcasts", "searchFullEpisodes",
        "searchAudiobooks", "searchUsers", "searchGenres", "searchSuggestions" -> 30_000L
        else -> null // Library, lyrics, Canvas, permissions, unknown operations and mutations stay fresh.
    }
}
