package com.music.spotui.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import app.myspoti.engine.R
import com.music.spotui.ui.components.DockMiniPlayer
import com.music.spotui.ui.theme.AppSans
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal val DockSurface = Color(0xFF313131)
private val DockSelected = Color(0xFF646464)

/**
 * What re-tapping the tab you are already on does in the official app: Home and Library
 * scroll their list back to the top without reloading anything, Search focuses its field.
 * The bar only sees the tap; the list state and the text field live inside the screens,
 * so the tap is published here and each screen collects its own event.
 */
object TabReselect {
    // replay = 0 + a one-slot buffer: a tap is never held back, and a tap that lands while
    // the screen is not composed is simply dropped rather than replayed on the next visit.
    private val _home = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(
        extraBufferCapacity = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    private val _library = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(
        extraBufferCapacity = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    private val _search = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(
        extraBufferCapacity = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )

    val home: kotlinx.coroutines.flow.SharedFlow<Unit> = _home
    val library: kotlinx.coroutines.flow.SharedFlow<Unit> = _library
    val search: kotlinx.coroutines.flow.SharedFlow<Unit> = _search

    fun emit(route: String) {
        when (route) {
            Routes.Home.route -> _home.tryEmit(Unit)
            Routes.Library.route -> _library.tryEmit(Unit)
            Routes.Search.route -> _search.tryEmit(Unit)
        }
    }
}

class NoRippleInteractionSource : MutableInteractionSource {

    override val interactions: Flow<Interaction> = emptyFlow()

    override suspend fun emit(interaction: Interaction) {}

    override fun tryEmit(interaction: Interaction) = true
}

@Composable
fun MainBottomNavigation(
    navController: NavHostController,
    bottomBarState: MutableState<Boolean>,
    bottomBarPlayerState: MutableState<Boolean>,
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val fullScreen = currentRoute in listOf(Routes.Player.route, Routes.Queue.route, Routes.Spotiflac.route)
    if (fullScreen || PlayerSheet.open.value) return
    val tabs = listOf(Routes.Home, Routes.Library, Routes.Settings)
    var selectedTab by rememberSaveable { mutableStateOf(Routes.Home.route) }
    LaunchedEffect(currentRoute) {
        if (currentRoute in tabs.map { it.route } + Routes.Search.route) selectedTab = currentRoute!!
    }
    fun navigate(item: Routes) {
        if (item.route == selectedTab) {
            if (currentRoute == item.route) TabReselect.emit(item.route)
            else if (!navController.popBackStack(item.route, inclusive = false)) {
                navController.navigate(item.route) { launchSingleTop = true }
            }
        } else {
            navController.navigate(item.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = item.route != Routes.Home.route
            }
        }
    }
    val collapsed = LocalDockScroll.current?.collapsed == true
    val progress = animateFloatAsState(if (collapsed) 1f else 0f,
        tween(280, easing = CubicBezierEasing(0.6f, 0f, 0.1f, 1f)), label = "dockMorph")
    val hasSong = com.music.spotui.di.Graph.currentSongState.snapshot.value.title.isNotBlank() ||
        com.music.spotui.di.SongPlayer.adPlaying
    // Both geometries remain allocated. Only GPU layer transforms/opacity animate;
    // Compose's animation clock honors the system animator duration scale (including zero).
    Box(Modifier.fillMaxWidth().navigationBarsPadding()
        .padding(start = 15.dp, end = 15.dp, bottom = 12.dp)
        .height(if (hasSong) 124.dp else 64.dp)) {
        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .then(if (collapsed) Modifier.clearAndSetSemantics { } else Modifier)
                .graphicsLayer {
                    alpha = 1f - progress.value
                    translationY = 10.dp.toPx() * progress.value
                    scaleX = 1f - 0.04f * progress.value
                    scaleY = scaleX
                },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (hasSong) DockMiniPlayer(enabled = !collapsed)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(
                    Modifier.weight(1f).height(64.dp).clip(CircleShape).background(DockSurface).padding(4.dp),
                ) {
                    tabs.forEach { item ->
                        val icon = when (item) {
                            Routes.Home -> R.drawable.ic_dock_home
                            Routes.Library -> R.drawable.ic_dock_library
                            else -> R.drawable.ic_dock_settings
                        }
                        val label = stringResource(if (item == Routes.Settings) R.string.settings_title else item.labelRes)
                        val active = selectedTab == item.route
                        val tint = if (active) Color.White else Color(0xFFB3B3B3)
                        Column(
                            Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                                .background(if (active) DockSelected else DockSurface)
                                .semantics { selected = active }
                                .clickable(enabled = !collapsed, role = Role.Tab, onClickLabel = label) { navigate(item) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(painterResource(icon), null, Modifier.size(24.dp), tint = tint)
                            Spacer(Modifier.height(3.dp))
                            Text(label, color = tint, fontFamily = AppSans, fontWeight = FontWeight.Medium,
                                fontSize = 11.sp, lineHeight = 12.sp, maxLines = 1)
                        }
                    }
                }
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(DockSurface)
                        .semantics { selected = selectedTab == Routes.Search.route }
                        .clickable(enabled = !collapsed, role = Role.Tab) { navigate(Routes.Search) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_dock_search), stringResource(Routes.Search.labelRes),
                        Modifier.size(26.dp), tint = Color.White)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .then(if (!collapsed) Modifier.clearAndSetSemantics { } else Modifier)
                .graphicsLayer {
                    alpha = progress.value
                    translationY = 10.dp.toPx() * (1f - progress.value)
                    scaleX = 0.96f + 0.04f * progress.value
                    scaleY = scaleX
                },
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockCircle(R.drawable.ic_dock_home, stringResource(Routes.Home.labelRes),
                selectedTab == Routes.Home.route, collapsed) { navigate(Routes.Home) }
            Box(Modifier.weight(1f).height(54.dp)) {
                if (hasSong) DockMiniPlayer(compact = true, enabled = collapsed)
            }
            DockCircle(R.drawable.ic_dock_search, stringResource(Routes.Search.labelRes),
                selectedTab == Routes.Search.route, collapsed, search = true) { navigate(Routes.Search) }
        }
    }
}

@Composable
private fun DockCircle(icon: Int, label: String, active: Boolean, enabled: Boolean,
    search: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(54.dp).clip(CircleShape).background(DockSurface)
        .semantics { selected = active }
        .clickable(enabled = enabled, role = Role.Tab, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), label, Modifier.size(26.dp),
            tint = if (active || search) Color.White else Color(0xFFB3B3B3))
    }
}
