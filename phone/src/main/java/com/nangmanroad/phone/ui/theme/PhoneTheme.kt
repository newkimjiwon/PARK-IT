package com.nangmanroad.phone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.Pretendard

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) = TextStyle(
    fontFamily = Pretendard,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

private val PhoneTypography = Typography(
    headlineLarge = style(28, 36, FontWeight.SemiBold, -0.6),
    headlineMedium = style(24, 32, FontWeight.SemiBold, -0.4),
    headlineSmall = style(21, 28, FontWeight.SemiBold, -0.3),
    titleLarge = style(19, 26, FontWeight.SemiBold, -0.2),
    titleMedium = style(17, 24, FontWeight.SemiBold),
    titleSmall = style(15, 21, FontWeight.Medium),
    bodyLarge = style(16, 24),
    bodyMedium = style(14, 21),
    bodySmall = style(13, 18),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(12, 16, FontWeight.Medium),
    labelSmall = style(11, 14, FontWeight.Medium),
)

@Composable
fun PhoneTheme(content: @Composable () -> Unit) {
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
            surfaceContainerHigh = Palette.Surface,
            outline = Palette.Hairline,
        ),
        typography = PhoneTypography,
        content = content,
    )
}
