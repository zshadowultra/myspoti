package com.music.spotui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

/** Static image treatment: tiny filtered artwork + platform blur + flat black darkening. */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun LyricsArtwork(artworkUri: String?) {
    if (artworkUri.isNullOrBlank()) return
    GlideImage(
        model = artworkUri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().blur(48.dp),
        // Small decoding bounds memory and also supplies a soft image on Android 10/11,
        // where Compose blur is unavailable. Request/texture are static, not frame work.
        requestBuilderTransform = { it.override(16, 16).dontTransform() },
    )
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.68f)))
}
