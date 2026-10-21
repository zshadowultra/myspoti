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

class RefusedPauseTest {
    private class Host : BridgeHost {
        override val messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
        val methods = mutableListOf<String>()
        override suspend fun evaluate(script: String) {
            val m = Regex("""Myspoti\.call\((\d+), "([^"]+)",""").find(script) ?: return
            methods += m.groupValues[2]
            messages.emit("""{"type":"result","id":${m.groupValues[1]},"ok":true,"value":null}""")
        }
    }

    private fun refusedAfterPlay(pause: Boolean): List<String> = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val host = Host()
            val player = SpotifyPlayer(Bridge(host, scope), scope)
            player.play("spotify:playlist:p", trackUri = "spotify:track:t")
            if (pause) player.pause()
            host.methods.clear()
            // ControlRelay emits this for any player/command 402, including a refused pause.
            host.messages.emit("""{"type":"refused","status":402,"path":"player/command"}""")
            host.methods.toList()
        } finally {
            scope.cancel()
        }
    }

    @Test fun `a refusal arriving after pause cannot restart the playlist`() {
        assertEquals(emptyList<String>(), refusedAfterPlay(pause = true))
    }

    @Test fun `a refused play still retries its context when the user has not paused`() {
        assertEquals(listOf("player.play"), refusedAfterPlay(pause = false))
    }
}
