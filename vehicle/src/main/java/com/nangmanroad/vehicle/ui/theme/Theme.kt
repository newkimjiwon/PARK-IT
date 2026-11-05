package com.nangmanroad.vehicle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.nangmanroad.core.ui.Palette
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
