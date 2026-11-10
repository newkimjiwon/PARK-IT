package com.nangmanroad.vehicle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.tabular
import com.nangmanroad.vehicle.ui.theme.Dimens

/**
 * A text tab: white with an underline when selected, grey otherwise. Used for filters, sort orders
 * and section tabs so the pine accent stays free for route guidance.
 */
@Composable
fun TextTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .heightIn(min = Dimens.TouchTarget)
            .clip(RoundedCornerShape(Dimens.ControlRadius))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Palette.TextPrimary else Palette.TextSecondary,
            )
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium.tabular(),
                    color = if (selected) Palette.TextSecondary else Palette.TextTertiary,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(if (selected) Palette.TextPrimary else Color.Transparent),
        )
    }
}

/**
 * Large, clearly labelled button. Only the screen's main action is [primary] (the pine accent). [active]
 * marks a toggled state such as saved or liked with a lighter fill; the icon carries the state.
 */
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
        primary -> Palette.Accent
        active -> Palette.SurfaceHigh
        else -> Palette.Surface
    }
    val content = if (primary) Palette.OnAccent else Palette.TextPrimary
    Row(
        modifier = modifier
            .heightIn(min = Dimens.TouchTarget)
            .clip(RoundedCornerShape(Dimens.ControlRadius))
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(28.dp))
        Text(text, style = MaterialTheme.typography.labelLarge.tabular(), color = content)
    }
}

/** A coloured dot and a label, e.g. a tip's status. */
@Composable
fun StatusLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
    }
}

/** A small outlined marker for facts such as "광고·제휴" or "질문". */
@Composable
fun Marker(text: String, modifier: Modifier = Modifier, color: Color = Palette.TextSecondary) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .border(1.dp, Palette.SurfaceHigh, RoundedCornerShape(3.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** A full-width 1dp divider between rows. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Palette.Hairline))
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 48.dp, horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Palette.TextTertiary, modifier = Modifier.size(40.dp))
        Box(Modifier.width(1.dp).height(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
    }
}
