package com.nangmanroad.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
