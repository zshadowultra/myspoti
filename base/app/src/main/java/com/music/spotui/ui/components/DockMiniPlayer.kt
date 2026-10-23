package com.music.spotui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.myspoti.engine.R
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.di.Graph
import com.music.spotui.di.Palette
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.navigation.DockSurface
import com.music.spotui.ui.navigation.PlayerSheet
import com.music.spotui.ui.theme.AppSans
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Both dock layouts consume one immutable metadata/transport snapshot. */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun DockMiniPlayer(compact: Boolean = false, enabled: Boolean = true) {
    val song by Graph.currentSongState.snapshot
    val adPlaying = SongPlayer.adPlaying
    // Only one layer owns palette/Canvas work; no duplicate prefetch during the morph.
    if (!compact) {
        val context = LocalContext.current
        val np by SongPlayer.nowPlaying
        val energy by app.myspoti.power.EnergySettings.state.collectAsStateWithLifecycle()
        // Keep the existing full-player palette and Canvas prefetch, without animating
        // the dock surface or ticking progress in the always-visible navigation layer.
        LaunchedEffect(song.coverUri) {
            if (song.coverUri.isNotBlank()) Palette().extractSecondColorFromCoverUrl(context, song.coverUri) { }
        }
        LaunchedEffect(np.uri, energy.policy.allowCanvas, com.music.spotui.data.DataSaver.active) {
            if (!np.uri.startsWith("spotify:track:") || !energy.policy.allowCanvas ||
                com.music.spotui.data.DataSaver.active) return@LaunchedEffect
            val url = withContext(Dispatchers.IO) {
                runCatching { Graph.api.getCanvasUrl(np.uri.removePrefix("spotify:track:")) }.getOrNull()
            } ?: return@LaunchedEffect
            CanvasPlayer.preload(context, url)
        }
    }
    Row(
        Modifier.fillMaxWidth().height(54.dp).clip(CircleShape).background(DockSurface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).fillMaxHeight().clickable(enabled = enabled, role = Role.Button) { PlayerSheet.show() }
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlideImage(
                model = song.coverUri,
                contentDescription = if (compact) song.title else null,
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(7.dp)),
                contentScale = ContentScale.Crop,
                loading = placeholder(R.drawable.placeholder),
                failure = placeholder(R.drawable.placeholder),
            )
            if (!compact) Column(Modifier.padding(start = 10.dp)) {
                Text(if (adPlaying) stringResource(R.string.ad_playing) else song.title,
                    color = Color.White, fontFamily = AppSans, fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!adPlaying) Text(song.singer, color = Color(0xFFB3B3B3), fontFamily = AppSans,
                    fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = { if (song.playing) SongPlayer.pause() else SongPlayer.play() },
            enabled = enabled, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(if (song.playing) R.drawable.ic_dock_pause else R.drawable.ic_dock_play),
                stringResource(if (song.playing) R.string.content_description_pause_button else R.string.content_description_play_button),
                Modifier.size(24.dp), tint = Color.White)
        }
        if (!compact) IconButton(onClick = { SongPlayer.next() }, enabled = enabled, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_dock_next), stringResource(R.string.notif_action_next),
                Modifier.size(26.dp), tint = Color.White)
        }
        Spacer(Modifier.width(3.dp))
    }
}
