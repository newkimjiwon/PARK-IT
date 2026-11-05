package com.nangmanroad.vehicle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.nangmanroad.vehicle.ui.theme.Dimens
import com.nangmanroad.vehicle.ui.theme.Palette

/** A landscape photo with a quiet placeholder for slow or offline networks. */
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
            .background(Brush.verticalGradient(listOf(Palette.SurfaceHigh, Palette.Surface))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Landscape, contentDescription = null, tint = Palette.TextTertiary)
    }
}

@Composable
fun Avatar(name: String, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Palette.Peach, Palette.Coral))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1),
            color = Palette.Background,
            style = if (size >= 56.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
        )
    }
}

/** Rounded selectable pill used for filters and tabs. */
@Composable
fun Pill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(Dimens.ChipRadius))
            .background(if (selected) Palette.Coral else Palette.Surface)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Palette.Background else Palette.TextPrimary,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Palette.Background.copy(alpha = 0.7f) else Palette.TextSecondary,
            )
        }
    }
}

/** Large, clearly labelled button; primary ones use the coral accent. */
@Composable
fun ActionButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    active: Boolean = false,
) {
    val container = when {
        primary -> Palette.Coral
        active -> Palette.Coral.copy(alpha = 0.18f)
        else -> Palette.Surface
    }
    val content = when {
        primary -> Palette.Background
        active -> Palette.Coral
        else -> Palette.TextPrimary
    }
    Row(
        modifier = modifier
            .heightIn(min = Dimens.TouchTarget)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(28.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
fun Badge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Palette.TextTertiary,
        modifier = modifier,
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Palette.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Palette.Coral, modifier = Modifier.size(40.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
    }
}

fun Modifier.outlineIf(selected: Boolean, color: Color, width: Dp, radius: Dp): Modifier =
    if (selected) border(width, color, RoundedCornerShape(radius)) else this
