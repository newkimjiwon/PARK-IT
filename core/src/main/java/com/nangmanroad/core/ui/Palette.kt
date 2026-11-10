package com.nangmanroad.core.ui

import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Colours shared by the car and phone apps, built on the "Sage Moon" palette
 * (#F3F6F1 #D8E2D2 #B2C5B1 #7F9B8E #3F5C55).
 *
 * Two sets follow the system's dark mode. Dark is the main look: a deep green-black night with
 * mist-white text and sage light, like moonlight on leaves. Light is the same palette by day:
 * misty sage ground, near-black green text. Each MainActivity sets [dark] from its configuration,
 * and every colour below is read through it, so the screens recompose when the mode changes.
 *
 * The accent stays small: the single main action on a screen (route guidance in the car, sending
 * to the car on the phone) and the spot picked on the map. Selection elsewhere is text with an
 * underline, so the accent keeps its meaning.
 */
object Palette {
    // Sage Moon, lightest to darkest; fixed in both modes.
    val Mist = Color(0xFFF3F6F1)
    val Leaf = Color(0xFFD8E2D2)
    val Sage = Color(0xFFB2C5B1)
    val Fern = Color(0xFF7F9B8E)
    val Pine = Color(0xFF3F5C55)

    /** True while the system is in dark mode; set by each MainActivity from its configuration. */
    var dark by mutableStateOf(true)

    /** Call from onCreate and onConfigurationChanged so the colours follow the system setting. */
    fun followNightMode(configuration: Configuration) {
        dark = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun pick(night: Long, day: Long) = Color(if (dark) night else day)

    val Background: Color get() = pick(0xFF0E1513, 0xFFF3F6F1)
    /** Floating cards and side panels sit a step off the ground. */
    val BackgroundRaised: Color get() = pick(0xFF141D1A, 0xFFFAFBF8)
    /** Buttons, fields and chips. */
    val Surface: Color get() = pick(0xFF1B2622, 0xFFE6ECE2)
    val SurfaceHigh: Color get() = pick(0xFF25322D, 0xFFD8E2D2)
    /** Row dividers and quiet outlines. */
    val Hairline: Color get() = pick(0xFF222E2A, 0xFFD8E2D2)
    /** 1px rules that frame floating cards, chips and the columns of the driving screen. */
    val Rule: Color get() = pick(0xFF364740, 0xFFB2C5B1)
    /** The empty part of a bar, such as the battery level. */
    val Track: Color get() = pick(0xFF25322D, 0xFFD8E2D2)

    val TextPrimary: Color get() = pick(0xFFEEF3EC, 0xFF1F2B27)
    val TextSecondary: Color get() = pick(0xFFA9BBAD, 0xFF51665E)
    /** Hints and captions, kept around 3.5:1 or better on the ground. */
    val TextTertiary: Color get() = pick(0xFF7F9B8E, 0xFF6B8579)

    /** Sage glowing on the night ground; deep pine on the day ground. */
    val Accent: Color get() = pick(0xFFB2C5B1, 0xFF3F5C55)
    /** Text and icons on an [Accent] fill. */
    val OnAccent: Color get() = pick(0xFF12201B, 0xFFF3F6F1)

    // Status colours, softened for the night and darkened for the day so both read on their ground.
    val Success: Color get() = pick(0xFF86C7A2, 0xFF3E8463)
    val Warning: Color get() = pick(0xFFE2B66E, 0xFF9C6A17)
    /** Conflicting or unverified information. */
    val Alert: Color get() = pick(0xFFEE7D74, 0xFFC0473F)
    val Info: Color get() = pick(0xFF86AEE9, 0xFF3F74B8)
    /** A veil behind labels laid on photos, in the ground's colour. */
    val Scrim: Color get() = pick(0xD90E1513, 0xD9F3F6F1)

    val MapSea: Color get() = pick(0xFF0B1110, 0xFFDCE6DC)
    val MapLand: Color get() = pick(0xFF16201C, 0xFFF3F6F1)
    val MapRoad: Color get() = pick(0xFF2A3833, 0xFFB2C5B1)
    val MapLabel: Color get() = pick(0xFF7F9B8E, 0xFF7F9B8E)
}
