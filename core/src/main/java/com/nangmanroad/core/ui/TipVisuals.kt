package com.nangmanroad.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.HoneyTip
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

/** The facilities a card answers at a glance, in the order a 차박 visitor asks about them. */
val FacilityKinds: List<TipKind> = listOf(TipKind.TOILET, TipKind.POWER, TipKind.SIGNAL, TipKind.STORE, TipKind.PARKING)

/**
 * The one status that sums up every tip of a kind: a disagreement wins over a warning, a warning
 * over a reassurance. Null when nobody has reported on that facility yet.
 */
fun List<HoneyTip>.facilityStatus(kind: TipKind): TipStatus? =
    filter { it.kind == kind }.map { it.status }.minByOrNull { status ->
        when (status) {
            TipStatus.NEEDS_CHECK -> 0
            TipStatus.PREPARE -> 1
            TipStatus.GOOD -> 2
        }
    }

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
