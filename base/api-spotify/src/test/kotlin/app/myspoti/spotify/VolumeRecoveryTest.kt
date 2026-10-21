package app.myspoti.spotify

import app.myspoti.spotify.bridge.Bridge
import app.myspoti.spotify.bridge.BridgeHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Regression for #43/#54: reconnecting must not change a Connect device's volume. */
class VolumeRecoveryTest {
    private class Host(private val media: String) : BridgeHost {
        override val messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
        val calls = mutableListOf<String>()

        override suspend fun evaluate(script: String) {
            val match = Regex("""Myspoti\.call\((\d+), "([^"]+)",""").find(script) ?: return
            val method = match.groupValues[2]
            calls += method
            val reply = if (method == "media.state") media else "null"
            messages.emit("""{"type":"result","id":${match.groupValues[1]},"ok":true,"value":$reply}""")
        }
    }

    private fun recover(media: String, mutedByUser: Boolean = false): List<String> = runBlocking {
        // All collectors start immediately, as on Android's main dispatcher. No clock waits.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val host = Host(media)
            val player = SpotifyPlayer(Bridge(host, scope), scope)
            if (mutedByUser) {
                player.setVolume(0.0)
                host.calls.clear() // Only automatic recovery is under test.
            }
            player.restoreLocalVolume()
            host.calls.toList()
        } finally {
            scope.cancel()
        }
    }

    @Test fun `a remote device is never unmuted by recovery`() {
        assertEquals(listOf("media.state"), recover("""{"active":false,"level":0,"own":1,"playerVolume":0}"""))
    }

    @Test fun `unknown device ownership is not permission to change the volume`() {
        assertEquals(listOf("media.state"), recover("""{"active":null,"level":0,"own":1,"playerVolume":0}"""))
        assertEquals(listOf("media.state"), recover("null"))
    }

    @Test fun `a deliberately muted local player stays muted`() {
        assertEquals(listOf("media.state"), recover("""{"active":true,"level":0,"own":0,"playerVolume":0}"""))
        assertEquals(listOf("media.state"), recover("""{"active":true,"level":0,"own":1}""", mutedByUser = true))
    }

    @Test fun `a lost local fade restores the element only, never Connect volume`() {
        assertEquals(listOf("media.state", "media.volume"), recover("""{"active":true,"level":0.0001,"own":0.7,"playerVolume":0.7}"""))
    }

    @Test fun `a healthy local element needs no repair`() {
        assertEquals(listOf("media.state"), recover("""{"active":true,"level":0.7,"own":0.7}"""))
    }
}
