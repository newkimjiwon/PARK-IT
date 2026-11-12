package com.nangmanroad.core.ui

import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Shared colours for the car and phone apps. Dark mode uses charcoal and light mode uses
 * neutral gray. Coral marks primary actions and the selected place on the map.
 */
object Palette {
    val Mist = Color(0xFFF5F5F6)

    /** True while the system is in dark mode; set by each MainActivity from its configuration. */
    var dark by mutableStateOf(true)

    /** Call from onCreate and onConfigurationChanged so the colours follow the system setting. */
    fun followNightMode(configuration: Configuration) {
        dark = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun pick(night: Long, day: Long) = Color(if (dark) night else day)

    val Background: Color get() = pick(0xFF121316, 0xFFF5F5F6)
    /** Floating cards and side panels sit a step off the ground. */
    val BackgroundRaised: Color get() = pick(0xFF191A1E, 0xFFFFFFFF)
    /** Buttons, fields and chips. */
    val Surface: Color get() = pick(0xFF222328, 0xFFECECEF)
    val SurfaceHigh: Color get() = pick(0xFF2D2F35, 0xFFE1E1E6)
    /** Row dividers and quiet outlines. */
    val Hairline: Color get() = pick(0xFF292A30, 0xFFDFDFE3)
    /** 1px rules that frame floating cards, chips and the columns of the driving screen. */
    val Rule: Color get() = pick(0xFF41434B, 0xFFBCBDC4)
    /** The empty part of a bar, such as the battery level. */
    val Track: Color get() = pick(0xFF2D2F35, 0xFFE1E1E6)

    val TextPrimary: Color get() = pick(0xFFF5F5F6, 0xFF1D1E23)
    val TextSecondary: Color get() = pick(0xFFB9BAC2, 0xFF50515A)
    val TextTertiary: Color get() = pick(0xFF9597A2, 0xFF61636D)

    /** The same coral marks primary actions in both modes. */
    val Accent = Color(0xFFFF967E)
    /** Text and icons on an [Accent] fill. */
    val OnAccent = Color(0xFF291714)

    // Status colours, softened for the night and darkened for the day so both read on their ground.
    val Success: Color get() = pick(0xFF86C7A2, 0xFF3E8463)
    val Warning: Color get() = pick(0xFFE2B66E, 0xFF9C6A17)
    /** Conflicting or unverified information. */
    val Alert: Color get() = pick(0xFFEE7D74, 0xFFC0473F)
    val Info: Color get() = pick(0xFF86AEE9, 0xFF3F74B8)
    /** A veil behind labels laid on photos, in the ground's colour. */
    val Scrim: Color get() = pick(0xD9121316, 0xD9F5F5F6)

    val MapSea: Color get() = pick(0xFF121316, 0xFFE5E8EC)
    val MapLand: Color get() = pick(0xFF1D1E22, 0xFFF7F7F8)
    val MapRoad: Color get() = pick(0xFF383A43, 0xFFB7BCC4)
    val MapLabel: Color get() = pick(0xFF9597A2, 0xFF60636C)
}
