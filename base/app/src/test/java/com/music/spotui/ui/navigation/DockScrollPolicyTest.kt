package com.music.spotui.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockScrollPolicyTest {
    @Test fun startsExpandedAndCollapsesAfterEightDp() {
        val policy = DockScrollPolicy()
        assertFalse(policy.collapsed)
        assertFalse(policy.onScroll(3f, false, 0))
        assertFalse(policy.onScroll(4f, false, 10))
        assertTrue(policy.onScroll(1f, false, 20))
    }

    @Test fun alternatingJitterDoesNotCollapseOrExpand() {
        val policy = DockScrollPolicy()
        repeat(60) { i ->
            assertFalse(policy.onScroll(2f, false, i * 10L))
            assertFalse(policy.onScroll(-2f, false, i * 10L + 1))
        }
        assertTrue(policy.onScroll(8f, false, 700))
        repeat(60) { i ->
            assertTrue(policy.onScroll(-1f, false, 701 + i * 10L))
            assertTrue(policy.onScroll(1f, false, 702 + i * 10L))
        }
    }

    @Test fun onlySettlesAfterLastScrollPlus120Ms() {
        val policy = DockScrollPolicy()
        policy.onScroll(8f, false, 10)
        assertTrue(policy.settle(129))
        policy.onScroll(1f, false, 129)
        assertTrue(policy.settle(248))
        assertFalse(policy.settle(249))
    }

    @Test fun topAndNavigationResetImmediately() {
        val policy = DockScrollPolicy()
        policy.onScroll(8f, false, 0)
        assertFalse(policy.onScroll(0f, true, 10))
        policy.onScroll(8f, false, 20)
        policy.reset()
        assertFalse(policy.collapsed)
        assertFalse(policy.onScroll(1f, false, 30))
    }

    @Test fun zeroNetFrameStillRearmsActivitySettle() {
        val policy = DockScrollPolicy()
        policy.onScroll(8f, false, 0)
        policy.onActivity(100)
        assertTrue(policy.settle(219))
        assertFalse(policy.settle(220))
    }

    @Test fun horizontalOrZeroDeltasDoNotRearmSettle() {
        val policy = DockScrollPolicy()
        policy.onScroll(8f, false, 0)
        assertTrue(policy.onScroll(0f, false, 100))
        assertFalse(policy.settle(120))
    }
}
