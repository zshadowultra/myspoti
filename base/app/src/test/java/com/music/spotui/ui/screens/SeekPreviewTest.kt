package com.music.spotui.ui.screens

import org.junit.Assert.*
import org.junit.Test

class SeekPreviewTest {
    @Test fun manyMovesCommitOnceWithOriginalDuration() {
        val preview = SeekPreview()
        preview.drag(0f, 100_000)
        repeat(120) { preview.drag(it / 120f, 200_000) }
        preview.drag(0.75f, 200_000)
        assertEquals(75_000L, preview.release())
        assertNull(preview.fraction)
        assertNull(preview.release())
    }

    @Test fun cancellationDiscardsTargetAndNextGestureCapturesFreshDuration() {
        val preview = SeekPreview()
        preview.drag(0.9f, 100_000)
        preview.cancel()
        assertNull(preview.fraction)
        assertNull(preview.release())
        preview.drag(0.5f, 200_000)
        assertEquals(100_000L, preview.release())
    }

    @Test fun invalidInputAndUnknownDurationNeverProduceAnInvalidSeek() {
        val preview = SeekPreview()
        preview.drag(Float.NaN, 100_000)
        assertNull(preview.release())
        preview.drag(-1f, -1)
        assertEquals(0L, preview.release())
        preview.drag(2f, 100_000)
        assertEquals(100_000L, preview.release())
    }
}
