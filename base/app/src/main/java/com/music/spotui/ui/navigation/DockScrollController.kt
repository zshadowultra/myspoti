package com.music.spotui.ui.navigation

import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Owned by the app composition; route changes cancel both frame and settle work. */
internal class DockScrollController(
    private val scope: CoroutineScope,
    private val nowMs: () -> Long = { SystemClock.uptimeMillis() },
    private val awaitFrame: suspend () -> Unit = { withFrameNanos { } },
) {
    private val policy = DockScrollPolicy()
    var collapsed by mutableStateOf(false)
        private set
    private var frame: Job? = null
    private var settle: Job? = null
    private var pendingDp = 0f
    private var lastEventMs = 0L

    fun reset() {
        frame?.cancel()
        settle?.cancel()
        frame = null
        pendingDp = 0f
        policy.reset()
        collapsed = false
    }

    fun scroll(deltaDp: Float, atTop: () -> Boolean) {
        if (deltaDp == 0f) return
        pendingDp += deltaDp
        lastEventMs = nowMs()
        settle?.cancel()
        if (frame?.isActive == true) return
        frame = scope.launch {
            awaitFrame()
            val delta = pendingDp
            pendingDp = 0f
            collapsed = policy.onScroll(delta, atTop(), lastEventMs)
            policy.onActivity(lastEventMs)
            settle = scope.launch {
                delay((DockScrollPolicy.SETTLE_MS - (nowMs() - lastEventMs)).coerceAtLeast(0L))
                collapsed = policy.settle(nowMs())
            }
        }
    }
}

internal val LocalDockScroll = staticCompositionLocalOf<DockScrollController?> { null }

@Composable
internal fun Modifier.dockScroll(state: LazyListState): Modifier {
    val topPx = with(LocalDensity.current) { 4.dp.roundToPx() }
    return dockScroll { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset <= topPx }
}

@Composable
internal fun Modifier.dockScroll(state: LazyGridState): Modifier {
    val topPx = with(LocalDensity.current) { 4.dp.roundToPx() }
    return dockScroll { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset <= topPx }
}

@Composable
internal fun Modifier.dockScroll(state: ScrollState): Modifier {
    val topPx = with(LocalDensity.current) { 4.dp.roundToPx() }
    return dockScroll { state.value <= topPx }
}

/** Attach only to an active view's vertical list, never horizontal shelves or sheets.
 * The single offset/top read happens in the frame decision, not for every scroll event.
 */
@Composable
internal fun Modifier.dockScroll(atTop: () -> Boolean): Modifier {
    val controller = LocalDockScroll.current ?: return this
    val density = LocalDensity.current.density
    val top by rememberUpdatedState(atTop)
    val connection = remember(controller, density) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Compose's consumed Y is negative when the viewport moves down.
                val vertical = consumed.y
                controller.scroll(-vertical / density) { top() }
                return Offset.Zero // Observe; never intercept content scrolling.
            }
        }
    }
    return nestedScroll(connection)
}
