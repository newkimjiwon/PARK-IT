package com.nangmanroad.core.ui

import androidx.compose.ui.graphics.Color

/**
 * Colours shared by the car and phone apps, built on the "Sage Moon" palette
 * (#F3F6F1 #D8E2D2 #B2C5B1 #7F9B8E #3F5C55): misty light sage grounds, like moonlight on leaves.
 *
 * The darkest green, [Pine], stays a small accent: the single main action on a screen (route
 * guidance in the car, sending to the car on the phone), the spot picked on the map and the brand
 * mark. Text is a matte near-black with a green cast rather than pure black. Selection elsewhere is
 * dark text with an underline, so the accent keeps its meaning.
 */
object Palette {
    // Sage Moon, lightest to darkest.
    val Mist = Color(0xFFF3F6F1)
    val Leaf = Color(0xFFD8E2D2)
    val Sage = Color(0xFFB2C5B1)
    val Fern = Color(0xFF7F9B8E)
    val Pine = Color(0xFF3F5C55)

    val Background = Mist
    /** Floating cards and side panels sit a step lighter than the ground. */
    val BackgroundRaised = Color(0xFFFAFBF8)
    /** Buttons, fields and chips: between Mist and Leaf. */
    val Surface = Color(0xFFE6ECE2)
    val SurfaceHigh = Leaf
    /** Row dividers and quiet outlines. */
    val Hairline = Leaf

    val TextPrimary = Color(0xFF1F2B27)
    val TextSecondary = Color(0xFF51665E)
    /** Hints and captions; darker than [Fern] so small text keeps about 3.5:1 on [Mist]. */
    val TextTertiary = Color(0xFF6B8579)

    val Accent = Pine
    /** Text and icons on an [Accent] fill. */
    val OnAccent = Mist

    // Status colours darkened from their usual tones so they read on the light ground.
    val Success = Color(0xFF3E8463)
    val Warning = Color(0xFF9C6A17)
    /** Conflicting or unverified information. */
    val Alert = Color(0xFFC0473F)
    val Info = Color(0xFF3F74B8)
    /** A light veil behind labels laid on photos. */
    val Scrim = Color(0xD9F3F6F1)

    val MapSea = Color(0xFFDCE6DC)
    val MapLand = Mist
    val MapRoad = Sage
    val MapLabel = Fern
}
