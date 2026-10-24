package com.music.spotui.ui.screens

import com.music.spotui.data.entity.LyricLine
import com.music.spotui.data.entity.LyricWord
import com.music.spotui.data.entity.Lyrics
import org.junit.Assert.*
import org.junit.Test

class LyricsTimingTest {
    @Test fun sweepClampsAndEasesContinuously() {
        assertEquals(0f, lyricSweep(900, 1000, 2000), 0f)
        assertEquals(0.15625f, lyricSweep(1250, 1000, 2000), 0.00001f)
        assertEquals(0.5f, lyricSweep(1500, 1000, 2000), 0f)
        assertEquals(1f, lyricSweep(2100, 1000, 2000), 0f)
        assertEquals(1f, lyricSweep(1001, 1000, 1000), 0f)
        var previous = 0f
        for (position in 1000L..2000L) {
            val progress = lyricSweep(position, 1000, 2000)
            assertTrue(progress >= previous)
            previous = progress
        }
    }

    @Test fun spansPreserveSyllablesSpacesAndInferMissingEndsOnce() {
        val timing = LyricsTiming(Lyrics(listOf(
            LyricLine(1000, "Hello world", listOf(
                LyricWord(1000, 0, "Hel"), LyricWord(1300, 1600, "lo "), LyricWord(1600, 0, "world")), endMs = 2400),
            LyricLine(7000, "Next"),
        ), synced = true), 9000)
        val line = timing.lines.first()
        assertEquals("Hello world", line.text)
        assertEquals(listOf(1300L, 1600L, 2400L), line.spans.map { it.endMs })
        assertEquals(listOf(0, 3, 6), line.spans.map { it.from })
        assertEquals(11, line.spans.last().until)
        assertEquals(0, timing.activeIndex(1000))
        assertEquals(1, timing.interludeIndex(2400))
        assertEquals(-1, timing.interludeIndex(7000))
    }

    @Test fun gapsRequireEvidenceAndDoNotStartWordsEarly() {
        val timing = LyricsTiming(Lyrics(listOf(LyricLine(5000, "First"), LyricLine(20000, "Second")), true), 30000)
        assertEquals(-1, timing.activeIndex(4999))
        assertEquals(0, timing.interludeIndex(0))
        assertEquals(0, timing.activeIndex(5000))
        assertEquals(-1, timing.interludeIndex(10000)) // no invented vocal end
        assertEquals(1, timing.activeIndex(25000))
        assertEquals(0, timing.activeIndex(6000)) // backward seek
    }

    @Test fun explicitSilenceAndUnsyncedFallback() {
        val synced = LyricsTiming(Lyrics(listOf(LyricLine(0, "♪"), LyricLine(6000, "Sing")), true), 8000)
        assertEquals(0, synced.interludeIndex(1000))
        val plain = LyricsTiming(Lyrics(listOf(LyricLine(0, "Plain")), false), 8000)
        assertEquals(-1, plain.activeIndex(1000))
        assertEquals(-1, plain.interludeIndex(1000))
        assertEquals(1f, lyricOpacity(0, -1), 0f)
        assertTrue(lyricOpacity(0, 0) > lyricOpacity(1, 0))
        assertEquals(lyricOpacity(1, 0), lyricOpacity(0, 1), 0f)
    }
}
