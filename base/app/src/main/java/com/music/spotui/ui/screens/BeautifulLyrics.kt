package com.music.spotui.ui.screens

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.ui.theme.AppSans
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos

private data class SweepRegion(
    val span: LyricsTiming.Span,
    val bounds: Rect,
    val precedingWidth: Float,
    val totalWidth: Float,
)

/** Cache glyph geometry, including words/syllables wrapping onto several visual rows. */
private fun sweepRegions(line: LyricsTiming.Line, layout: TextLayoutResult): List<SweepRegion> = buildList {
    for (span in line.spans) {
        val rows = mutableListOf<Rect>()
        var row: Rect? = null
        var visualLine = -1
        for (offset in span.from until span.until.coerceAtMost(layout.layoutInput.text.length)) {
            val bounds = layout.getBoundingBox(offset)
            val currentLine = layout.getLineForOffset(offset)
            if (currentLine != visualLine) {
                row?.let(rows::add)
                row = bounds
                visualLine = currentLine
            } else {
                val previous = row!!
                row = Rect(minOf(previous.left, bounds.left), minOf(previous.top, bounds.top),
                    maxOf(previous.right, bounds.right), maxOf(previous.bottom, bounds.bottom))
            }
        }
        row?.let(rows::add)
        val totalWidth = rows.sumOf { it.width.toDouble() }.toFloat()
        var preceding = 0f
        for (bounds in rows) {
            add(SweepRegion(span, bounds, preceding, totalWidth))
            preceding += bounds.width
        }
    }
}

/** Only draw reads position. Timing spans and clip geometry are not allocated on frames. */
@Composable
internal fun SweepingLyricLine(
    line: LyricsTiming.Line,
    current: Boolean,
    past: Boolean,
    interlude: Boolean,
    opacity: Float,
    position: () -> Long,
    translation: String?,
    modifier: Modifier = Modifier,
) {
    var layout by remember(line) { mutableStateOf<TextLayoutResult?>(null) }
    val scale by animateFloatAsState(if (current || interlude) 1.035f else 1f,
        spring(dampingRatio = 0.82f, stiffness = 240f), label = "lyricScale")
    val alpha by animateFloatAsState(opacity,
        spring(dampingRatio = 1f, stiffness = 260f), label = "lyricDistance")
    Column(modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
        transformOrigin = TransformOrigin(0f, 0.5f)
    }) {
        Text(
            text = line.text,
            fontFamily = AppSans,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            lineHeight = 39.sp,
            color = Color.White.copy(alpha = when {
                past || (current && line.spans.isEmpty()) -> 1f
                current -> 0.30f
                else -> 0.65f
            }),
            onTextLayout = { if (layout != it) layout = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).drawWithCache {
                val textLayout = layout
                val regions = textLayout?.let { sweepRegions(line, it) }.orEmpty()
                val dotRadius = 4.dp.toPx()
                val dotSpacing = 20.dp.toPx()
                onDrawWithContent {
                    if (interlude) {
                        // Beautiful Lyrics' 2.25 s breath, with a small travelling phase.
                        val phase = position().toDouble() / 2_250.0
                        for (dot in 0..2) {
                            val breath = ((1.0 - cos((phase - dot * 0.16) * 2.0 * PI)) * 0.5).toFloat()
                            drawCircle(Color.White.copy(alpha = 0.35f + 0.65f * breath),
                                radius = dotRadius * (0.75f + 0.30f * breath),
                                center = Offset(dotRadius + dot * dotSpacing, size.height / 2f - breath * dotRadius))
                        }
                    } else {
                        drawContent()
                        if (current && textLayout != null && regions.isNotEmpty()) {
                            val now = position()
                            for (index in regions.indices) {
                                val region = regions[index]
                                val p = lyricSweep(now, region.span.startMs, region.span.endMs)
                                val width = (p * region.totalWidth - region.precedingWidth).coerceIn(0f, region.bounds.width)
                                if (width > 0f) clipRect(region.bounds.left, region.bounds.top,
                                    region.bounds.left + width, region.bounds.bottom) {
                                    drawText(textLayout, color = Color.White)
                                }
                            }
                        }
                    }
                }
            },
        )
        if (!translation.isNullOrBlank() && !interlude) Text(
            text = translation,
            color = Color.White.copy(alpha = 0.65f),
            fontFamily = AppSans,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

/** Spring scroll without LazyColumn's distant-item teleport. Correct once geometry arrives. */
@Composable
internal fun SpringLyricsFollow(list: LazyListState, target: Int, enabled: Boolean) {
    var userScrollAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(list) {
        list.interactionSource.interactions.collect {
            if (it is DragInteraction.Start || it is DragInteraction.Stop)
                userScrollAt = android.os.SystemClock.elapsedRealtime()
        }
    }
    LaunchedEffect(list, target, enabled, userScrollAt) {
        if (!enabled || target < 0) return@LaunchedEffect
        val wait = 3_000L - (android.os.SystemClock.elapsedRealtime() - userScrollAt)
        if (wait > 0) delay(wait)
        repeat(4) {
            val info = list.layoutInfo
            val visible = info.visibleItemsInfo
            if (visible.isEmpty()) return@LaunchedEffect
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val item = visible.firstOrNull { it.index == target }
            val reference = visible.first()
            val average = visible.sumOf { it.size }.toFloat() / visible.size + info.mainAxisItemSpacing
            val distance = if (item != null) item.offset + item.size / 2f - center
                else reference.offset + (target - reference.index) * average + average / 2f - center
            if (kotlin.math.abs(distance) < 1f) return@LaunchedEffect
            list.scroll {
                var previous = 0f
                animate(0f, distance, animationSpec = spring(dampingRatio = 0.9f, stiffness = 180f)) { value, _ ->
                    scrollBy(value - previous)
                    previous = value
                }
            }
        }
    }
}
