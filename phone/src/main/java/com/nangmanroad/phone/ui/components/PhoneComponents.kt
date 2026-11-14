package com.nangmanroad.phone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.VisitCondition
import com.nangmanroad.core.data.distanceKm
import com.nangmanroad.core.data.formatKm
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.ToggleChip
import com.nangmanroad.core.ui.tabular

/** A choice in a form: outlined, white when chosen. */
@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Palette.TextPrimary else Palette.SurfaceHigh, shape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val tint = if (selected) Palette.TextPrimary else Palette.TextSecondary
        if (icon != null) Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}

/** A text tab: white with an underline when selected. Used for feeds, sections and filters. */
@Composable
fun TextTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, count: Int? = null) {
    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .heightIn(min = 44.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.padding(top = 11.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(text, style = MaterialTheme.typography.titleSmall, color = if (selected) Palette.TextPrimary else Palette.TextSecondary)
            if (count != null) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.labelLarge.tabular(),
                    color = if (selected) Palette.TextSecondary else Palette.TextTertiary,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(if (selected) Palette.TextPrimary else Color.Transparent))
    }
}

/** A row of [TextTab]s over a full-width hairline. */
@Composable
fun TabRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Column(modifier) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp), content = content)
        Hairline()
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Palette.Hairline))
}

/** The screen's one main action, in the pine accent. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) Palette.Accent else Palette.Surface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        val tint = if (enabled) Palette.OnAccent else Palette.TextTertiary
        if (icon != null) Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

/** Everything that is not the main action. [active] marks a toggled state (saved, following). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    container: Color = Palette.Surface,
) {
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Palette.SurfaceHigh else container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        val tint = if (enabled) Palette.TextPrimary else Palette.TextTertiary
        if (icon != null) Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge.tabular(), color = tint)
    }
}

/** A small outlined label for facts such as "광고·제휴", "질문" or "데모". */
@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .border(1.dp, Palette.SurfaceHigh, RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** A coloured dot and a label, e.g. a tip's status. */
@Composable
fun StatusLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** A plain icon button; the optional dot marks something new. */
@Composable
fun IconCircle(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, badge: Boolean = false) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Palette.TextPrimary, modifier = Modifier.size(24.dp))
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 9.dp, end = 10.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Palette.Accent),
            )
        }
    }
}

@Composable
fun VisitConditionChips(
    selected: Set<VisitCondition>,
    onToggle: (VisitCondition) -> Unit,
) {
    VisitCondition.entries.forEach { condition ->
        ToggleChip(
            icon = Icons.Outlined.CheckCircle,
            label = condition.label,
            on = condition in selected,
            onToggle = { onToggle(condition) },
            height = 36.dp,
            iconSize = 18.dp,
            gap = 6.dp,
        )
    }
}

/**
 * Shown under the filter chips while any is on: how many conditions, how many places are left, and
 * a way out. Also says what a facility chip means, since "no report" drops a place just as "none" does.
 */
@Composable
fun FilterSummary(conditions: Int, matches: Int, onClear: () -> Unit, modifier: Modifier = Modifier) {
    if (conditions == 0) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "조건 ${conditions}개 적용 · ${matches}곳",
                style = MaterialTheme.typography.titleSmall.tabular(),
                color = Palette.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                "조건 지우기",
                style = MaterialTheme.typography.labelLarge,
                color = Palette.TextPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .border(1.dp, Palette.SurfaceHigh, RoundedCornerShape(3.dp))
                    .clickable(role = Role.Button, onClick = onClear)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        Text(
            if (matches == 0) "조건을 모두 확인한 장소가 없어요. 조건을 줄여 보세요." else "방문자가 '괜찮아요'로 확인한 곳만 보여요. 정보가 없거나 없다고 한 곳은 빠져요.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary,
        )
    }
}

fun distanceFromHere(here: LatLng, spot: Spot, usesDeviceLocation: Boolean): String {
    val basis = if (usesDeviceLocation) "내 위치 기준" else "서울 기준"
    return "$basis ${formatKm(distanceKm(here, spot.location))}"
}
