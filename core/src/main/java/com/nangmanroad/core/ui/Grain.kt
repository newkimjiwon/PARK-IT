package com.nangmanroad.core.ui

import android.graphics.Bitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Random

/** One tile of grey noise, repeated across the screen; seeded so every launch looks the same. */
private val grainTile: ImageBitmap by lazy {
    val side = 160
    val random = Random(7)
    val pixels = IntArray(side * side) {
        val v = random.nextInt(256)
        (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/**
 * A faint paper grain over the flat sage ground, drawn behind the content. Large light areas get
 * the texture of printed paper instead of a flat digital fill; panels with their own background
 * cover it, which keeps the layers reading tone on tone.
 */
fun Modifier.paperGrain(alpha: Float = 0.05f): Modifier = drawWithCache {
    val brush = ShaderBrush(ImageShader(grainTile, TileMode.Repeated, TileMode.Repeated))
    onDrawBehind { drawRect(brush, alpha = alpha) }
}
