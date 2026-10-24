package com.music.spotui.ui.screens

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

/** Glide executes and caches this transformation off-main; the output is at most 64 x 64. */
internal class LyricsArtworkBlur : BitmapTransformation() {
    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        // Also bound unusual decoders that ignore the request's downsample strategy.
        val ratio = minOf(1f, 64f / maxOf(toTransform.width, toTransform.height))
        val width = (toTransform.width * ratio).toInt().coerceAtLeast(1)
        val height = (toTransform.height * ratio).toInt().coerceAtLeast(1)
        val small = if (width == toTransform.width && height == toTransform.height) toTransform
            else Bitmap.createScaledBitmap(toTransform, width, height, true)
        val pixels = IntArray(width * height)
        small.getPixels(pixels, 0, width, 0, 0, width, height)
        if (small !== toTransform) small.recycle()
        val blurred = blurLyricsArtwork(pixels, width, height)
        return pool.get(width, height, Bitmap.Config.ARGB_8888).apply {
            setHasAlpha(toTransform.hasAlpha())
            setPixels(blurred, 0, width, 0, 0, width, height)
        }
    }

    override fun equals(other: Any?): Boolean = other is LyricsArtworkBlur
    override fun hashCode(): Int = ID.hashCode()
    override fun updateDiskCacheKey(messageDigest: MessageDigest) { messageDigest.update(ID_BYTES) }

    private companion object {
        const val ID = "com.music.spotui.lyrics.artwork.blur.64.radius8.passes3.v1"
        val ID_BYTES = ID.toByteArray(Charsets.UTF_8)
    }
}
