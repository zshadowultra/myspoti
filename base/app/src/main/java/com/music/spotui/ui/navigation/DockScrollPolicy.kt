package com.music.spotui.ui.navigation

/** Scroll decisions only: no layout reads, timers or animation in this policy.
 * Positive deltas mean downward content movement; callers ignore horizontal shelves.
 */
internal class DockScrollPolicy {
    var collapsed = false
        private set
    private var downwardDp = 0f
    private var lastScrollMs: Long? = null

    fun reset() {
        collapsed = false
        downwardDp = 0f
        lastScrollMs = null
    }

    fun onScroll(deltaDp: Float, atTop: Boolean, nowMs: Long): Boolean {
        if (atTop) {
            reset()
            return collapsed
        }
        if (deltaDp == 0f) return collapsed
        lastScrollMs = nowMs
        // Reversal resets the accumulated downward movement, but does not expand
        // a collapsed dock. Only top/settle/navigation may expand it.
        downwardDp = if (deltaDp > 0f) downwardDp + deltaDp else 0f
        if (downwardDp >= COLLAPSE_DP) collapsed = true
        return collapsed
    }

    /** A coalesced frame can have zero net movement but still be an active scroll burst. */
    fun onActivity(nowMs: Long) {
        lastScrollMs = nowMs
    }

    fun settle(nowMs: Long): Boolean {
        val last = lastScrollMs ?: return collapsed
        if (nowMs - last >= SETTLE_MS) reset()
        return collapsed
    }

    companion object {
        const val COLLAPSE_DP = 8f
        const val SETTLE_MS = 120L
    }
}
