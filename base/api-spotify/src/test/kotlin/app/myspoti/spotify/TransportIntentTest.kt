package app.myspoti.spotify

import app.myspoti.api.ItemKind
import app.myspoti.api.NowPlaying
import app.myspoti.spotify.bridge.Bridge
import app.myspoti.spotify.bridge.BridgeHost
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Real bridge calls, including an in-flight failure, not a mock of the intent policy. */
class TransportIntentTest {
    private class Host(private val failMethod: String? = null) : BridgeHost {
        override val messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
        val failed = CompletableDeferred<Unit>()
        val methods = mutableListOf<String>()
        override suspend fun evaluate(script: String) {
            val call = Regex("""Myspoti\.call\((\d+), "([^"]+)",""").find(script) ?: return
            val method = call.groupValues[2]
            methods += method
            val fail = method == failMethod && !failed.isCompleted
            messages.emit(if (fail)
                """{"type":"result","id":${call.groupValues[1]},"ok":false,"error":"No such device"}"""
            else
                """{"type":"result","id":${call.groupValues[1]},"ok":true,"value":null}""")
            if (fail) failed.complete(Unit)
        }
    }

    private fun supersededRetry(first: String, newer: String) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val host = Host(first)
            val player = SpotifyPlayer(Bridge(host, scope), scope)
            host.messages.emit("""{"type":"ready"}""")
            val old = scope.launch {
                when (first) {
                    "player.pause" -> player.pause()
                    "player.next" -> player.next()
                    "player.previous" -> player.previous()
                    "queue.skipTo" -> player.skipTo(item)
                    else -> player.play("spotify:track:a")
                }
            }
            withTimeout(2_000) { host.failed.await() }
            if (newer == "player.pause") player.pause() else player.resume()
            withTimeout(2_000) { old.join() }
            assertTrue(old.isCancelled, "superseded retry must unwind")
            assertEquals(listOf(first, newer), host.methods.filter { it.startsWith("player.") || it == "queue.skipTo" })
        } finally { scope.cancel() }
    }

    @Test fun `a newer pause cancels a suspended play retry`() = supersededRetry("player.play", "player.pause")
    @Test fun `a newer resume cancels a suspended pause retry`() = supersededRetry("player.pause", "player.resume")

    @Test fun `a newer pause cancels a suspended next retry`() = supersededRetry("player.next", "player.pause")
    @Test fun `a newer pause cancels a suspended previous retry`() = supersededRetry("player.previous", "player.pause")
    @Test fun `a newer pause cancels a suspended queue jump retry`() = supersededRetry("queue.skipTo", "player.pause")

    @Test fun `a newer resume supersedes delayed page recovery`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val host = Host()
            val bridge = Bridge(host, scope)
            val player = SpotifyPlayer(bridge, scope)
            host.messages.emit("""{"type":"ready"}""")
            host.messages.emit("""{"type":"state","state":{"track":{"uri":"spotify:track:a","name":"A"},"isPaused":false,"timestamp":100}}""")
            bridge.pageGone()
            host.messages.emit("""{"type":"ready"}""") // recovery now waits for a new state
            player.resume()
            host.messages.emit("""{"type":"state","state":{"track":{"uri":"spotify:track:a","name":"A"},"isPaused":true,"timestamp":200}}""")
            delay(250)
            assertEquals(listOf("player.resume"), host.methods.filter { it.startsWith("player.") })
        } finally { scope.cancel() }
    }

    private val item = NowPlaying(kind = ItemKind.TRACK, uri = "spotify:track:b", uid = null,
        name = "B", artists = emptyList(), album = null, imageUrl = null, durationMs = 200_000)

    @Test fun `a pause discards a held skip before the page becomes ready`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val host = Host()
            val player = SpotifyPlayer(Bridge(host, scope), scope)
            player.skipTo(item)
            player.pause()
            host.messages.emit("""{"type":"ready"}""")
            delay(SpotifyPlayer.RESTORE_GAP_MS + 100)
            assertEquals(listOf("player.pause"), host.methods.filter { it.startsWith("player.") || it == "queue.skipTo" })
        } finally { scope.cancel() }
    }
}
