package com.music.spotui.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A gesture owns its preview and captures duration once; cancellation never seeks. */
internal class SeekPreview {
    var fraction by mutableStateOf<Float?>(null)
        private set
    private var durationMs = 0L

    fun drag(value: Float, duration: Long) {
        if (!value.isFinite()) return
        if (fraction == null) durationMs = duration.coerceAtLeast(0)
        fraction = value.coerceIn(0f, 1f)
    }

    fun release(): Long? {
        val value = fraction ?: return null
        cancel()
        return (value.toDouble() * durationMs).toLong().coerceIn(0, durationMs)
    }

    fun cancel() { fraction = null }
}
