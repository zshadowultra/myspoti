package com.music.spotui.ui.screens

import com.music.spotui.data.api.Api
import com.music.spotui.data.entity.LibraryEntry
import com.music.spotui.data.local.LibraryOrder
import com.music.spotui.data.local.LocalPlaylists

internal data class RootLibraryLabels(
    val liked: String,
    val local: String,
    val downloads: String,
    val localSubtitle: String,
    val localCover: String,
)

internal data class RootLibrary(val entries: List<LibraryEntry>, val pinned: Int)

/** Read snapshot-backed local lists and order inside the caller's derivedStateOf. */
internal fun rootLibrary(
    entries: List<LibraryEntry>,
    labels: RootLibraryLabels,
    hasDownloads: Boolean,
): RootLibrary {
    val likedIndex = entries.indexOfFirst { it.spotifyId == Api.HomeCache.LIKED_SONGS_ID }
    val at = likedIndex + 1
    val pinned = at + 1 + if (hasDownloads) 1 else 0
    val renamed = entries.map {
        if (it.spotifyId == Api.HomeCache.LIKED_SONGS_ID) it.copy(name = labels.liked, subtitle = "") else it
    }
    val local = LibraryEntry(
        spotifyId = Api.HomeCache.LOCAL_FILES_ID, name = labels.local, subtitle = "",
        coverUri = labels.localCover, isPlaylist = true,
    )
    val downloads = if (hasDownloads) listOf(LibraryEntry(
        spotifyId = Api.HomeCache.DOWNLOADS_ID, name = labels.downloads, subtitle = "",
        coverUri = "", isPlaylist = true,
    )) else emptyList()
    val localLists = LocalPlaylists.list().map {
        LibraryEntry(
            spotifyId = LOCAL_PLAYLIST_PREFIX + it.id, name = it.name,
            subtitle = labels.localSubtitle, coverUri = "", isPlaylist = true,
        )
    }
    val combined = renamed.take(at) + local + downloads + localLists + renamed.drop(at)
    // Custom order lives in preferences, so observe its explicit revision too.
    LibraryOrder.version
    return RootLibrary(
        LibraryOrder.arrange(combined, LibraryOrder.sort, LibraryOrder.custom(LibraryOrder.ROOT), pinned),
        pinned,
    )
}
