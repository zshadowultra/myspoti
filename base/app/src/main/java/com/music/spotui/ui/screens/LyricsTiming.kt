package com.music.spotui.ui.screens

import com.music.spotui.data.entity.Lyrics
import kotlin.math.abs

/** Immutable presentation timing, assembled once per lyrics result (never on a frame). */
internal class LyricsTiming(lyrics: Lyrics, durationMs: Long) {
    data class Span(val from: Int, val until: Int, val startMs: Long, val endMs: Long)
    data class Line(val text: String, val spans: List<Span>, val startMs: Long, val endMs: Long?, val silent: Boolean)

    val synced = lyrics.synced
    val lines = lyrics.lines.mapIndexed { index, line ->
        val nextStart = lyrics.lines.getOrNull(index + 1)?.timeMs ?: durationMs
        val words = line.words.orEmpty()
        var offset = 0
        val spans = words.mapIndexed { wordIndex, word ->
            val nextWord = words.getOrNull(wordIndex + 1)?.startMs
            val end = when {
                word.endMs > word.startMs -> word.endMs
                nextWord != null && nextWord > word.startMs -> nextWord
                line.endMs != null && line.endMs > word.startMs -> line.endMs
                nextStart > word.startMs -> nextStart
                else -> word.startMs + 1_000L
            }
            Span(offset, offset + word.text.length, word.startMs, end).also { offset = it.until }
        }
        Line(
            text = if (words.isEmpty()) line.text else words.joinToString("") { it.text },
            spans = spans,
            startMs = line.timeMs,
            // Do not invent instrumental gaps from line-start-only data.
            endMs = line.endMs?.takeIf { it > line.timeMs }
                ?: words.lastOrNull()?.let { it.endMs.takeIf { end -> end > it.startMs } },
            silent = line.text.isBlank() || line.text.trim() == "♪" || line.text.trim() == "♫",
        )
    }

    fun activeIndex(positionMs: Long): Int {
        if (!synced) return -1
        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (lines[middle].startMs <= positionMs) {
                result = middle
                low = middle + 1
            } else high = middle - 1
        }
        return result
    }

    /** Dots occupy the next vocal's slot; explicit instrumental rows keep their own slot. */
    fun interludeIndex(positionMs: Long): Int {
        if (!synced || lines.isEmpty()) return -1
        val active = activeIndex(positionMs)
        if (active < 0) return if (lines.first().startMs >= GAP_MS) 0 else -1
        val line = lines[active]
        if (line.silent) return active
        val next = lines.getOrNull(active + 1) ?: return -1
        val end = line.endMs ?: return -1
        return if (next.startMs - end >= GAP_MS && positionMs >= end) active + 1 else -1
    }

    companion object { const val GAP_MS = 4_000L }
}

/** Smoothstep: exact endpoints, monotonic, zero velocity at either edge. */
internal fun lyricSweep(positionMs: Long, startMs: Long, endMs: Long): Float {
    val p = ((positionMs - startMs).toFloat() / (endMs - startMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
    return p * p * (3f - 2f * p)
}

internal fun lyricOpacity(index: Int, active: Int): Float =
    if (active < 0) 1f else when (abs(index - active)) {
        0 -> 1f
        1 -> 0.64f
        2 -> 0.44f
        else -> 0.28f
    }
