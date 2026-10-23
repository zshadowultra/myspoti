package app.myspoti.spotify

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

/** Exercise the real watcher and bridge, including intent changes while a poll is suspended. */
class WatchdogRecoveryTest {
    private class Host(var active: String, val holdPing: Boolean = false, val failNext: Boolean = false) : BridgeHost {
        override val messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
        var level = 1.0
        val failedNext = CompletableDeferred<Unit>()
        val pingStarted = CompletableDeferred<Unit>()
        val releasePing = CompletableDeferred<Unit>()
        val polls = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val calls = mutableListOf<String>()
        override suspend fun evaluate(script: String) {
            val call = Regex("""Myspoti\.call\((\d+), "([^"]+)",""").find(script) ?: return
            val method = call.groupValues[2]
            calls += method
            if (method == "bridge.version" && holdPing) {
                pingStarted.complete(Unit)
                releasePing.await()
            }
            val value = if (method == "media.state")
                """{"active":$active,"paused":false,"level":$level,"own":1,"time":20}"""
            else "null"
            if (method == "player.next" && failNext && !failedNext.isCompleted) {
                messages.emit("""{"type":"result","id":${call.groupValues[1]},"ok":false,"error":"No such device"}""")
                failedNext.complete(Unit)
            } else {
                messages.emit("""{"type":"result","id":${call.groupValues[1]},"ok":true,"value":$value}""")
            }
            if (method == "media.state") polls.send(Unit)
        }
        suspend fun state(paused: Boolean = false) {
            messages.emit("""{"type":"state","state":{"track":{"uri":"spotify:track:a","name":"A","durationMs":200000},"next":{"uri":"spotify:track:b","name":"B"},"isPaused":$paused,"positionMs":210000,"timestamp":${System.currentTimeMillis()}}}""")
        }
        fun transports() = calls.filter { it in setOf("player.play", "player.pause", "player.resume", "player.next", "player.previous", "player.seek", "queue.skipTo") }
    }

    private fun ownershipBlocksRecovery(active: String) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val host = Host(active)
            SpotifyPlayer(Bridge(host, scope), scope)
            host.messages.emit("""{"type":"ready"}""")
            host.state()
            withTimeout(10_000) { repeat(2) { host.polls.receive() } }
            assertTrue(host.transports().isEmpty(), "unknown/remote ownership must not self-advance")
        } finally { scope.cancel() }
    }

    @Test fun `remote ownership cannot repair an end stall`() = ownershipBlocksRecovery("false")
    @Test fun `unknown ownership cannot repair an end stall`() = ownershipBlocksRecovery("null")

    @Test fun `pause then resume during a watchdog ping supersedes the old end stall`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val host = Host("true", holdPing = true)
            val player = SpotifyPlayer(Bridge(host, scope), scope)
            host.messages.emit("""{"type":"ready"}""")
            host.state()
            withTimeout(10_000) { host.pingStarted.await() }
            player.pause()
            host.state(paused = true)
            player.resume()
            host.releasePing.complete(Unit)
            delay(150)
            assertEquals(listOf("player.pause", "player.resume"), host.transports())
        } finally { scope.cancel() }
    }

    @Test fun `a locally owned end stall still advances`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val host = Host("true")
            SpotifyPlayer(Bridge(host, scope), scope)
            host.messages.emit("""{"type":"ready"}""")
            host.state()
            withTimeout(10_000) { while ("player.next" !in host.transports()) delay(20) }
            assertEquals(listOf("player.next"), host.transports())
        } finally { scope.cancel() }
    }

    @Test fun `an unknown answer cannot reuse previously local ownership for volume repair`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val host = Host("true")
            val player = SpotifyPlayer(Bridge(host, scope), scope)
            host.messages.emit("""{"type":"ready"}""")
            host.state()
            withTimeout(6_000) { host.polls.receive() }
            host.active = "null"
            host.level = 0.0001
            host.calls.clear()
            player.restoreLocalVolume()
            assertEquals(listOf("media.state"), host.calls)
        } finally { scope.cancel() }
    }

    @Test fun `a Connect transfer during a failed automatic skip blocks its retry`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val host = Host("true", failNext = true)
            SpotifyPlayer(Bridge(host, scope), scope)
            host.messages.emit("""{"type":"ready"}""")
            host.state()
            withTimeout(10_000) { host.failedNext.await() }
            host.active = "false"
            delay(SpotifyPlayer.COMMAND_RETRY_MS + 200)
            assertEquals(listOf("player.next"), host.transports())
        } finally { scope.cancel() }
    }

    @Test fun `a reload ownership read cannot adopt a newer resume revision`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val host = object : BridgeHost {
                override val messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
                val transports = mutableListOf<String>()
                var hold = false
                override suspend fun evaluate(script: String) {
                    val call = Regex("""Myspoti\.call\((\d+), "([^"]+)",""").find(script) ?: return
                    val method = call.groupValues[2]
                    if (method.startsWith("player.")) transports += method
                    if (method == "media.state" && hold) { started.complete(Unit); release.await() }
                    val value = if (method == "media.state") """{"active":true,"level":1,"own":1}""" else "null"
                    messages.emit("""{"type":"result","id":${call.groupValues[1]},"ok":true,"value":$value}""")
                }
            }
            val bridge = Bridge(host, scope)
            val player = SpotifyPlayer(bridge, scope)
            host.messages.emit("""{"type":"ready"}""")
            host.messages.emit("""{"type":"state","state":{"track":{"uri":"spotify:track:a","name":"A"},"isPaused":false,"timestamp":100}}""")
            bridge.pageGone()
            host.hold = true
            val ready = scope.launch { host.messages.emit("""{"type":"ready"}""") }
            withTimeout(2_000) { started.await() }
            player.resume()
            release.complete(Unit)
            ready.join()
            host.messages.emit("""{"type":"state","state":{"track":{"uri":"spotify:track:a","name":"A"},"isPaused":true,"timestamp":200}}""")
            delay(200)
            assertEquals(listOf("player.resume"), host.transports)
        } finally { scope.cancel() }
    }
}
