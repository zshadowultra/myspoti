package com.music.spotui.ui.screens

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import com.music.spotui.data.api.Api
import com.music.spotui.data.entity.LibraryEntry
import com.music.spotui.data.local.LibraryOrder
import com.music.spotui.data.local.LocalPlaylists
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RootLibraryTest {
    private val labels = RootLibraryLabels("Liked Songs", "Local Files", "Downloaded", "On this phone", "local-cover")
    private fun entry(id: String, name: String) = LibraryEntry(id, name, "owner", "cover", true)
    private val entries = listOf(entry(Api.HomeCache.LIKED_SONGS_ID, "old label"), entry("b", "Beta"), entry("a", "Alpha"))

    @Before fun prepare() {
        LocalPlaylists.playlists.clear()
        LibraryOrder.choose(LibraryOrder.Sort.CUSTOM)
    }

    @After fun restore() {
        LocalPlaylists.playlists.clear()
        LibraryOrder.choose(LibraryOrder.Sort.CUSTOM)
    }

    @Test fun unchangedReadsReuseAssemblyWithoutLosingRows() {
        var assemblies = 0
        val root = derivedStateOf { assemblies++; rootLibrary(entries, labels, false) }
        val first = root.value
        repeat(20) { assertSame(first, root.value) }
        assertEquals(1, assemblies)
        assertEquals(listOf(Api.HomeCache.LIKED_SONGS_ID, Api.HomeCache.LOCAL_FILES_ID, "b", "a"), first.entries.map { it.spotifyId })
        assertEquals("Liked Songs", first.entries.first().name)
        assertEquals("", first.entries.first().subtitle)
        assertEquals(2, first.pinned)
    }

    @Test fun sortAndCustomOrderRevisionInvalidateDerivedRoot() {
        var assemblies = 0
        val root = derivedStateOf { assemblies++; rootLibrary(entries, labels, false) }
        root.value
        LibraryOrder.choose(LibraryOrder.Sort.ALPHABETICAL)
        assertEquals(listOf("a", "b"), root.value.entries.drop(root.value.pinned).map { it.spotifyId })
        val count = assemblies
        // Even if sort is already CUSTOM, a saved reorder bumps the revision.
        LibraryOrder.choose(LibraryOrder.Sort.CUSTOM)
        root.value
        LibraryOrder.saveCustom(LibraryOrder.ROOT, listOf("b", "a"))
        root.value
        assertTrue(assemblies > count + 1)
    }

    @Test fun localCreateRenameDeleteInvalidateWithoutNetworkRefresh() {
        val root = derivedStateOf { rootLibrary(entries, labels, false) }
        assertEquals(4, root.value.entries.size)
        val local = LocalPlaylists.create("Phone mix")
        assertEquals(LOCAL_PLAYLIST_PREFIX + local.id, root.value.entries[2].spotifyId)
        LocalPlaylists.rename(local.id, "Renamed")
        assertEquals("Renamed", root.value.entries[2].name)
        LocalPlaylists.delete(local.id)
        assertEquals(4, root.value.entries.size)
    }

    @Test fun downloadsAndMissingLikedKeepLocalEntriesPinned() {
        val downloads = mutableStateOf(false)
        val root = derivedStateOf { rootLibrary(emptyList(), labels, downloads.value) }
        assertEquals(listOf(Api.HomeCache.LOCAL_FILES_ID), root.value.entries.map { it.spotifyId })
        assertEquals(1, root.value.pinned)
        downloads.value = true
        assertEquals(listOf(Api.HomeCache.LOCAL_FILES_ID, Api.HomeCache.DOWNLOADS_ID), root.value.entries.map { it.spotifyId })
        assertEquals(2, root.value.pinned)
        downloads.value = false
        assertEquals(1, root.value.entries.size)
    }
}
