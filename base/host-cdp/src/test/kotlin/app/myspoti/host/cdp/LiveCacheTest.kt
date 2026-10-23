package app.myspoti.host.cdp

import app.myspoti.spotify.SpotifyApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

/** Read-only proof on the same API wiring as Android; does not alter playback or library. */
@EnabledIfSystemProperty(named = "myspoti.cdp", matches = ".+")
class LiveCacheTest {
    @Test fun `repeat search and catalog reads do not dispatch another GraphQL request`() = runBlocking {
        withTimeout(90_000) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val host = CdpHost.connect(System.getProperty("myspoti.cdp"))
            try {
                val api = SpotifyApi(host, scope)
                api.onPageLoaded()
                api.awaitReady()
                fun dispatched() = api.httpGql.direct.get() + api.httpGql.fellBack.get()
                val started = System.nanoTime()
                val first = api.search.tracks("papaoutai", limit = 1)
                val coldMs = (System.nanoTime() - started) / 1_000_000
                assertTrue(first.items.isNotEmpty())
                val afterFirst = dispatched()
                val warmStart = System.nanoTime()
                repeat(10) { assertEquals(first, api.search.tracks("papaoutai", limit = 1)) }
                val warmMs = (System.nanoTime() - warmStart) / 1_000_000
                assertEquals(afterFirst, dispatched(), "fresh search must use the API cache")
                val track = api.catalog.track(first.items.single().uri)
                val afterTrack = dispatched()
                repeat(10) { assertEquals(track, api.catalog.track(track.uri)) }
                assertEquals(afterTrack, dispatched(), "fresh catalog must use the API cache")
                println("read-only cache: first search=$coldMs ms; ten repeats=$warmMs ms; extra dispatches=0")
            } finally {
                host.close()
                scope.cancel()
            }
        }
    }
}
