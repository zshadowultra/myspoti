package com.music.spotui.ui.navigation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockScrollControllerTest {
    @Test fun burstHasOneFrameAndOneTopRead() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val frame = CompletableDeferred<Unit>()
            var frameRequests = 0
            var topReads = 0
            val controller = DockScrollController(scope, nowMs = { 10L }, awaitFrame = {
                frameRequests++
                frame.await()
            })
            repeat(120) { controller.scroll(1f) { topReads++; false } }
            assertEquals(1, frameRequests)
            assertEquals(0, topReads)
            assertFalse(controller.collapsed)
            frame.complete(Unit)
            assertEquals(1, topReads)
            assertTrue(controller.collapsed)
        } finally { scope.cancel() }
    }

    @Test fun zeroAndHorizontalMovementScheduleNothing() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var frames = 0
            val controller = DockScrollController(scope, awaitFrame = { frames++ })
            repeat(120) { controller.scroll(0f) { error("Must not read top") } }
            assertEquals(0, frames)
            assertFalse(controller.collapsed)
        } finally { scope.cancel() }
    }

    @Test fun routeResetCancelsOldFrameWithoutReadingOldView() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val frame = CompletableDeferred<Unit>()
            val controller = DockScrollController(scope, nowMs = { 10L }, awaitFrame = { frame.await() })
            controller.scroll(20f) { error("Stale view must not be read") }
            controller.reset()
            frame.complete(Unit)
            assertFalse(controller.collapsed)
        } finally { scope.cancel() }
    }

    @Test fun topReadExpandsEvenDuringDownwardBurst() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = DockScrollController(scope, nowMs = { 10L }, awaitFrame = { })
            controller.scroll(8f) { false }
            assertTrue(controller.collapsed)
            controller.scroll(-8f) { true }
            assertFalse(controller.collapsed)
        } finally { scope.cancel() }
    }
}
