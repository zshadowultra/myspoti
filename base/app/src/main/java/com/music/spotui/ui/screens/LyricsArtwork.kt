package com.music.spotui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy

/** Cached image treatment on every API level, with extra platform blur and flat darkening. */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun LyricsArtwork(artworkUri: String?) {
    if (artworkUri.isNullOrBlank()) return
    val blur = remember { LyricsArtworkBlur() }
    // Recreate the image on track changes: never retain the previous cover on load failure.
    key(artworkUri) {
        GlideImage(
            model = artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(48.dp),
            requestBuilderTransform = {
                it.override(64, 64)
                    .downsample(DownsampleStrategy.AT_MOST)
                    .disallowHardwareConfig()
                    .dontAnimate()
                    .transform(blur)
            },
        )
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.68f)))
}
