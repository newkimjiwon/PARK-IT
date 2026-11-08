package com.nangmanroad.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

/** A landscape photo (URL or local file path) with a quiet placeholder for slow or offline networks. */
@Composable
fun SpotPhoto(url: String, contentDescription: String?, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { PhotoPlaceholder() },
        error = { PhotoPlaceholder() },
    )
}

@Composable
private fun PhotoPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Landscape, contentDescription = null, tint = Palette.TextTertiary)
    }
}

/** The author's first letter on a plain circle; there are no profile photos yet. */
@Composable
fun Avatar(name: String, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Palette.SurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1),
            color = Palette.TextSecondary,
            style = if (size >= 56.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
        )
    }
}
