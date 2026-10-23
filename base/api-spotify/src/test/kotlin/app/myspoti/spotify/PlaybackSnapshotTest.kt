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

/** Exercises host message -> bridge event -> mapper -> ordered, immutable player state. */
class PlaybackSnapshotTest {
    private class Host : BridgeHost {
        override val messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
        override suspend fun evaluate(script: String) = Unit
        suspend fun state(name: String, timestamp: Long) = messages.emit("""
            {"type":"state","state":{"track":{"uri":"spotify:track:$name","name":"$name",
            "artists":[{"uri":"spotify:artist:$name","name":"artist-$name"}],
            "album":{"uri":"spotify:album:$name","name":"album-$name"},"image":"art-$name"},
            "isPaused":true,"timestamp":$timestamp}}
        """.trimIndent())
    }

    @Test fun `interleaved old metadata cannot roll back a complete new track`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val host = Host()
            val player = SpotifyPlayer(Bridge(host, scope), scope)
            host.state("A", 100)
            val old = player.state.value!!.item!!
            host.state("B", 200)
            host.state("A", 150)
            val current = player.state.value!!.item!!
            assertEquals("spotify:track:B", current.uri)
            assertEquals("B", current.name)
            assertEquals("artist-B", current.artists.single().name)
            assertEquals("album-B", current.album!!.name)
            assertEquals("art-B", current.imageUrl)
            assertEquals("A", old.name)
            assertEquals("art-A", old.imageUrl)
        } finally { scope.cancel() }
    }
}
