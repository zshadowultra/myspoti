package com.music.spotui.ui.screens

/** Three separable box passes approximate a Gaussian, once per artwork request, not per frame. */
internal fun blurLyricsArtwork(pixels: IntArray, width: Int, height: Int, radius: Int = 8): IntArray {
    require(width > 0 && height > 0 && width <= 64 && height <= 64)
    require(pixels.size == width * height && radius in 1..64)
    val result = pixels.copyOf()
    val scratch = IntArray(pixels.size)
    repeat(3) {
        blurArtworkPass(result, scratch, width, height, radius, horizontal = true)
        blurArtworkPass(scratch, result, width, height, radius, horizontal = false)
    }
    return result
}

private fun blurArtworkPass(
    source: IntArray,
    target: IntArray,
    width: Int,
    height: Int,
    radius: Int,
    horizontal: Boolean,
) {
    val length = if (horizontal) width else height
    val lines = if (horizontal) height else width
    val stride = if (horizontal) 1 else width
    val window = radius * 2 + 1
    for (line in 0 until lines) {
        val base = if (horizontal) line * width else line
        var alpha = 0
        var red = 0
        var green = 0
        var blue = 0
        for (sample in -radius..radius) {
            val pixel = source[base + sample.coerceIn(0, length - 1) * stride]
            alpha += pixel ushr 24
            red += (pixel ushr 16) and 255
            green += (pixel ushr 8) and 255
            blue += pixel and 255
        }
        for (offset in 0 until length) {
            target[base + offset * stride] = ((alpha / window) shl 24) or
                ((red / window) shl 16) or ((green / window) shl 8) or (blue / window)
            // Clamp samples to the border, avoiding transparent/black halos at the crop edges.
            val leaving = source[base + (offset - radius).coerceIn(0, length - 1) * stride]
            val entering = source[base + (offset + radius + 1).coerceIn(0, length - 1) * stride]
            alpha += (entering ushr 24) - (leaving ushr 24)
            red += ((entering ushr 16) and 255) - ((leaving ushr 16) and 255)
            green += ((entering ushr 8) and 255) - ((leaving ushr 8) and 255)
            blue += (entering and 255) - (leaving and 255)
        }
    }
}
