package com.music.spotui.ui.screens

import org.junit.Assert.*
import org.junit.Test

class LyricsArtworkPixelsTest {
    @Test fun uniformCoverPreservesColourAndOpaqueEdges() {
        val colour = 0xff247ac1.toInt()
        val input = IntArray(64 * 64) { colour }
        assertArrayEquals(input, blurLyricsArtwork(input, 64, 64))
    }

    @Test fun blurSpreadsDetailAcrossBothAxesWithoutMutatingSource() {
        val size = 33
        val input = IntArray(size * size) { 0xff000000.toInt() }
        val center = size / 2
        input[center * size + center] = 0xffffffff.toInt()
        val original = input.copyOf()
        val blurred = blurLyricsArtwork(input, size, size, radius = 2)
        assertArrayEquals(original, input)
        val peak = blurred[center * size + center] and 255
        assertTrue(peak in 1..254)
        assertTrue((blurred[center * size + center + 3] and 255) > 0)
        assertTrue((blurred[(center + 3) * size + center] and 255) > 0)
        assertEquals(blurred[center * size + center + 3], blurred[(center + 3) * size + center])
        assertTrue(blurred.all { (it ushr 24) == 255 })
    }

    @Test fun rectangularAndOnePixelCoversUseClampedBorders() {
        val colour = 0xffe67320.toInt()
        for ((width, height) in listOf(1 to 1, 1 to 64, 64 to 1, 32 to 64)) {
            val input = IntArray(width * height) { colour }
            assertArrayEquals(input, blurLyricsArtwork(input, width, height))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnboundedPixelBuffers() { blurLyricsArtwork(IntArray(65), 65, 1) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMismatchedDimensions() { blurLyricsArtwork(IntArray(4), 3, 3) }
}
