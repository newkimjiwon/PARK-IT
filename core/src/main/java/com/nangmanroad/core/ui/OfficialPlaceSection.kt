package com.nangmanroad.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.OfficialPlaceInfo

@Composable
fun OfficialPlaceSection(info: OfficialPlaceInfo, modifier: Modifier = Modifier, showSourceLink: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("공식 장소 정보", style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary)
        Text("주차 · ${info.parking}", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        Text("야간 이용 · ${info.nightUse}", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        Text("한국관광공사 고캠핑 · ${info.checkedOn} 조회", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
        info.photoCredit?.let {
            Text("사진 · $it", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
        }
        if (showSourceLink) {
            val uriHandler = LocalUriHandler.current
            TextButton(onClick = { uriHandler.openUri(info.sourceUrl) }) {
                Text("고캠핑 원문", color = Palette.TextPrimary)
            }
        }
    }
}
