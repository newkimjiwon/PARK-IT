package com.nangmanroad.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Wc
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.FacilityKinds
import com.nangmanroad.core.data.HoneyTip
import com.nangmanroad.core.data.facilityStatus
import com.nangmanroad.core.data.TipKind
import com.nangmanroad.core.data.TipStatus

fun TipKind.icon(): ImageVector = when (this) {
    TipKind.TOILET -> Icons.Outlined.Wc
    TipKind.PARKING -> Icons.Outlined.LocalParking
    TipKind.POWER -> Icons.Outlined.ElectricalServices
    TipKind.SIGNAL -> Icons.Outlined.SignalCellularAlt
    TipKind.NIGHT -> Icons.Outlined.NightsStay
    TipKind.STORE -> Icons.Outlined.Storefront
    TipKind.ACCESS -> Icons.Outlined.DirectionsCar
    TipKind.CAUTION -> Icons.Outlined.ReportProblem
}

fun TipStatus.color(): Color = when (this) {
    TipStatus.GOOD -> Palette.Success
    TipStatus.PREPARE -> Palette.Warning
    TipStatus.NEEDS_CHECK -> Palette.Alert
}

fun TipStatus.label(): String = when (this) {
    TipStatus.GOOD -> "괜찮아요"
    TipStatus.PREPARE -> "준비하세요"
    TipStatus.NEEDS_CHECK -> "확인 필요"
}

/** Problems first, then things to prepare, then the reassuring ones. */
fun List<HoneyTip>.byImportance(): List<HoneyTip> = sortedWith(
    compareBy<HoneyTip> { it.status != TipStatus.NEEDS_CHECK }
        .thenBy { it.status != TipStatus.PREPARE }
        .thenByDescending { it.helpfulCount },
)

/**
 * Facility icons in a row, coloured by what visitors reported: green fine, amber prepare, red
 * check, and dim when nothing is known. Camping apps answer "is there a toilet?" on the list
 * itself; this does the same without opening the place, which matters most on the head unit.
 */
@Composable
fun FacilityStrip(tips: List<HoneyTip>, modifier: Modifier = Modifier, iconSize: Dp = 22.dp, gap: Dp = 8.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        FacilityKinds.forEach { kind ->
            val status = tips.facilityStatus(kind)
            val tint = status?.color() ?: Palette.TextTertiary.copy(alpha = 0.55f)
            val description = "${kind.label} ${status?.label() ?: "정보 없음"}"
            Icon(
                kind.icon(),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize).semantics { contentDescription = description },
            )
        }
    }
}

/**
 * Toggle chips for "only places that have …": one per facility, selected ones drawn light on dark
 * so they read as a pressed key. Camping apps filter on toilets and power at the list; here the
 * answer comes from visitors' tips, so a chip only keeps places someone actually confirmed.
 * [height] sets the touch target: 56.dp for the head unit, 36.dp for the phone.
 */
@Composable
fun FacilityFilterRow(
    selected: Set<TipKind>,
    onToggle: (TipKind) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    iconSize: Dp = 24.dp,
    gap: Dp = 8.dp,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FacilityKinds.forEach { kind ->
            val on = kind in selected
            val textStyle = if (height >= 48.dp) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge
            Row(
                modifier = Modifier
                    .heightIn(min = height)
                    .clip(RoundedCornerShape(height / 2))
                    .background(if (on) Palette.TextPrimary else Palette.Surface)
                    .clickable(role = Role.Checkbox, onClickLabel = "${kind.label} 있는 곳만") { onToggle(kind) }
                    .padding(horizontal = height / 3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                Icon(
                    kind.icon(),
                    contentDescription = null,
                    tint = if (on) Palette.Background else Palette.TextSecondary,
                    modifier = Modifier.size(iconSize),
                )
                Text(kind.label, style = textStyle, color = if (on) Palette.Background else Palette.TextSecondary)
            }
        }
    }
}
