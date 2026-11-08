package com.nangmanroad.core.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.nangmanroad.core.R

/**
 * Pretendard 1.3.9 (SIL OFL 1.1, see core/licenses/Pretendard-OFL.txt), bundled so the car head unit
 * and the phone render the same letterforms instead of whatever CJK font each system ships.
 */
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

/** Fixed-width digits, so distances, counts and times don't jitter as they change. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
