package app.myspoti.spotify

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StateOrderTest {
    @Test fun `late snapshots never roll state backward`() {
        val order = StateOrder()
        assertTrue(order.accept(200))
        assertFalse(order.accept(100))
        assertTrue(order.accept(200)) // same instant can carry pause/restriction changes
        assertTrue(order.accept(300))
        assertFalse(order.accept(250))
        order.reset()
        assertTrue(order.accept(100)) // a new page/session is a new clock domain
    }
}
