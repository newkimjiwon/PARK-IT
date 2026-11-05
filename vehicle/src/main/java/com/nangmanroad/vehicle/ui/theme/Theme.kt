package com.nangmanroad.vehicle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Design tokens from the Figma Make prompt: charcoal surfaces with a sunset coral accent. */
object Palette {
    val Background = Color(0xFF111318)
    val BackgroundRaised = Color(0xFF1C2028)
    val Surface = Color(0xFF282D36)
    val SurfaceHigh = Color(0xFF323844)
    val TextPrimary = Color(0xFFF9FAFB)
    val TextSecondary = Color(0xFFA3A9B5)
    val TextTertiary = Color(0xFF6E7582)
    val Coral = Color(0xFFFF957C)
    val Peach = Color(0xFFFFC5AC)
    val Success = Color(0xFF54B899)
    val Warning = Color(0xFFF0B46B)
    val Scrim = Color(0xCC111318)

    val MapSea = Color(0xFF14171D)
    val MapLand = Color(0xFF1F232B)
    val MapRoad = Color(0xFF353B47)
    val MapLabel = Color(0xFF7A818E)
}

/** Sizes for a head unit viewed from arm's length (Pleos minimum: 18px text, large targets). */
object Dimens {
    val TouchTarget = 64.dp
    val Gutter = 24.dp
    val CardRadius = 20.dp
    val ChipRadius = 32.dp
    val RailWidth = 104.dp
}

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 44.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 38.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 32.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 23.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 22.sp, lineHeight = 32.sp),
    bodyMedium = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
    bodySmall = TextStyle(fontSize = 18.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp),
)

@Composable
fun NangmanRoadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Coral,
            onPrimary = Palette.Background,
            secondary = Palette.Peach,
            background = Palette.Background,
            onBackground = Palette.TextPrimary,
            surface = Palette.BackgroundRaised,
            onSurface = Palette.TextPrimary,
            surfaceVariant = Palette.Surface,
            onSurfaceVariant = Palette.TextSecondary,
            outline = Palette.SurfaceHigh,
        ),
        typography = AppTypography,
        content = content,
    )
}
