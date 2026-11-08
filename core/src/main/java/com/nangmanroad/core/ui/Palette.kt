package com.nangmanroad.core.ui

import androidx.compose.ui.graphics.Color

/**
 * Colours shared by the car and phone apps.
 *
 * The neutrals follow the Pleos Connect system UI: near-black with a faint cool cast, white text and
 * line icons. Coral is the one accent and is kept for the single main action on a screen (route
 * guidance in the car) and the spot picked on the map. Selection elsewhere is white text with an
 * underline, so the accent keeps its meaning.
 */
object Palette {
    val Background = Color(0xFF0E0F11)
    val BackgroundRaised = Color(0xFF16171A)
    val Surface = Color(0xFF1F2024)
    val SurfaceHigh = Color(0xFF2A2B30)
    /** Row dividers and quiet outlines. */
    val Hairline = Color(0xFF2A2B30)

    val TextPrimary = Color(0xFFF2F2F4)
    val TextSecondary = Color(0xFFA2A4AB)
    val TextTertiary = Color(0xFF6C6E76)

    val Coral = Color(0xFFFF957C)
    /** Text and icons on a coral fill. */
    val OnCoral = Color(0xFF2A120B)

    val Success = Color(0xFF5DBB98)
    val Warning = Color(0xFFE9B46A)
    /** Conflicting or unverified information. */
    val Alert = Color(0xFFEF6F6A)
    val Info = Color(0xFF6FA2EE)
    val Scrim = Color(0xCC0E0F11)

    val MapSea = Color(0xFF121316)
    val MapLand = Color(0xFF1C1D21)
    val MapRoad = Color(0xFF34363C)
    val MapLabel = Color(0xFF7C7E86)
}
