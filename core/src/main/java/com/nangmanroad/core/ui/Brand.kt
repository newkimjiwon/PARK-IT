package com.nangmanroad.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nangmanroad.core.R

/** The app's names: 파킷 on screen, PARK-IT on the contest form; the code keeps its 낭만로드 working name. */
const val BRAND_NAME = "PARK-IT"
const val BRAND_NAME_KO = "파킷"
const val BRAND_TAGLINE = "당신의 경험이 누군가의 목적지가 되는 곳"

/** The crescent P and background used by the launcher icon. */
@Composable
fun ParkItMark(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(colorResource(R.color.parkit_brand)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(R.drawable.ic_parkit_glyph),
            contentDescription = BRAND_NAME_KO,
            modifier = Modifier.size(size * 0.7f),
        )
    }
}

/**
 * Mark plus wordmark for screen headers. [caption] sits under the name when given, e.g. the
 * Korean name or the tagline on the sign-in screen.
 */
@Composable
fun ParkItWordmark(
    modifier: Modifier = Modifier,
    markSize: Dp = 36.dp,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    caption: String? = null,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(markSize / 3)) {
        ParkItMark(markSize)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                BRAND_NAME_KO,
                style = textStyle.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                color = Palette.TextPrimary,
            )
            if (caption != null) Text(caption, style = MaterialTheme.typography.labelLarge, color = Palette.TextSecondary)
        }
    }
}
