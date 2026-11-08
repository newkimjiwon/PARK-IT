package com.nangmanroad.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.NightShelter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.CarCamping

fun CarCamping.color(): Color = when (this) {
    CarCamping.ALLOWED -> Palette.Success
    CarCamping.CHECK -> Palette.Warning
    CarCamping.BANNED -> Palette.Alert
}

fun CarCamping.icon(): ImageVector = when (this) {
    CarCamping.ALLOWED -> Icons.Outlined.NightShelter
    CarCamping.CHECK -> Icons.Outlined.HelpOutline
    CarCamping.BANNED -> Icons.Outlined.Block
}

/**
 * "차박 가능 / 현장 확인 / 차박 금지" as a tinted pill. The answer to the first question a 차박
 * visitor has, so it sits next to the category on every card and above the route button.
 */
@Composable
fun CarCampingBadge(status: CarCamping, modifier: Modifier = Modifier, iconSize: Dp = 18.dp, filled: Boolean = true, stale: Boolean = false) {
    // A stale answer loses its colour: it is still the last word, but not one to lean on.
    val color = if (stale) Palette.TextTertiary else status.color()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (filled) color.copy(alpha = 0.18f) else Color.Transparent)
            .padding(horizontal = if (filled) 8.dp else 0.dp, vertical = if (filled) 4.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(status.icon(), contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
        Text(status.label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
