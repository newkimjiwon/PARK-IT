package com.nangmanroad.vehicle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.Pretendard

/** Sizes for a head unit viewed from arm's length (Pleos minimum: 18px text, large targets). */
object Dimens {
    val TouchTarget = 64.dp
    val Gutter = 24.dp
    /** Buttons, fields and panels. Kept small to sit with the square-ish Pleos system UI. */
    val ControlRadius = 10.dp
    val PhotoRadius = 8.dp
    val RailWidth = 104.dp
}

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) = TextStyle(
    fontFamily = Pretendard,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

private val AppTypography = Typography(
    displaySmall = style(44, 54, FontWeight.SemiBold, -0.9),
    headlineLarge = style(38, 48, FontWeight.SemiBold, -0.7),
    headlineMedium = style(32, 42, FontWeight.SemiBold, -0.5),
    headlineSmall = style(28, 36, FontWeight.SemiBold, -0.4),
    titleLarge = style(26, 34, FontWeight.SemiBold, -0.3),
    titleMedium = style(23, 30, FontWeight.SemiBold, -0.2),
    titleSmall = style(20, 26, FontWeight.Medium),
    bodyLarge = style(22, 32),
    bodyMedium = style(20, 28),
    bodySmall = style(18, 24),
    labelLarge = style(20, 26, FontWeight.Medium),
    labelMedium = style(18, 24, FontWeight.Medium),
    labelSmall = style(16, 20, FontWeight.Medium),
)

@Composable
fun NangmanRoadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Coral,
            onPrimary = Palette.OnCoral,
            secondary = Palette.TextPrimary,
            background = Palette.Background,
            onBackground = Palette.TextPrimary,
            surface = Palette.BackgroundRaised,
            onSurface = Palette.TextPrimary,
            surfaceVariant = Palette.Surface,
            onSurfaceVariant = Palette.TextSecondary,
            outline = Palette.Hairline,
        ),
        typography = AppTypography,
        content = content,
    )
}
