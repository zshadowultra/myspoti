package app.myspoti.spotify

import app.myspoti.spotify.bridge.Bridge
import app.myspoti.spotify.bridge.BridgeHost
import app.myspoti.spotify.gql.GqlOps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/** Exercises the actual SpotifyApi wrapper and its session invalidation, without credentials. */
class SpotifyCacheLifecycleTest {
    private class Host : BridgeHost {
        override val messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
        val reads = AtomicInteger()
        override suspend fun evaluate(script: String) {
            val call = Regex("""Myspoti\.call\((\d+), "([^"]+)",""").find(script) ?: return
            val id = call.groupValues[1]
            val response = when (call.groupValues[2]) {
                "token.get" -> """{"type":"result","id":$id,"ok":false,"error":"no account in unit test"}"""
                "gql.query" -> """{"type":"result","id":$id,"ok":true,"value":{"data":{"read":${reads.incrementAndGet()}}}}"""
                else -> """{"type":"result","id":$id,"ok":true,"value":null}"""
            }
            messages.emit(response)
        }
    }

    @Test fun `API cache is shared and cleared by operations page loss and ready`() = runBlocking {
        withTimeout(5_000) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val host = Host()
                val ops = GqlOps(mapOf("getTrack" to "test-hash"))
                val api = SpotifyApi(host, scope, ops = ops)
                val variables = JsonObject(emptyMap())
                suspend fun repeatRead(expected: Int) {
                    val first = api.raw.gql("getTrack", variables)
                    repeat(10) { assertEquals(first, api.raw.gql("getTrack", variables)) }
                    assertEquals(expected, host.reads.get())
                }
                host.messages.emit("""{"type":"ready","version":1}""")
                repeatRead(1)
                api.updateOps(ops)
                repeatRead(2)
                api.onPageStarting()
                assertFalse(api.ready.value)
                host.messages.emit("""{"type":"ready","version":1}""")
                repeatRead(3)
            } finally {
                scope.cancel()
            }
        }
    }
}
