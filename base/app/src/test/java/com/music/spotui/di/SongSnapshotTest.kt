package com.music.spotui.di

import org.junit.Assert.*
import org.junit.Test

class SongSnapshotTest {
    @Test fun metadataPublishesAsOneImmutableValue() {
        val state = CurrentSongState()
        state.updateSongState("art-A", "A", "artist-A", false, 1, 0, "album-A")
        val old = state.snapshot.value
        state.updateSongState("art-B", "B", "artist-B", false, 2, 1, "album-B")
        assertEquals(SongSnapshot("B", "album-B", "artist-B", "art-B", false, 1, 2), state.snapshot.value)
        assertEquals("A", old.title)
        assertEquals("art-A", old.coverUri)
        assertEquals("B", state.title.value)
        assertEquals("art-B", state.coverUri.value)
        val beforePause = state.snapshot.value
        state.updatePlayingState(true)
        assertEquals(beforePause.copy(playing = true), state.snapshot.value)
        assertEquals("artist-B", state.singer.value)
    }
}
