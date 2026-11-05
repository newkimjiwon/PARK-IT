package com.nangmanroad.vehicle.ui.driving

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nangmanroad.vehicle.AppUiState
import com.nangmanroad.core.data.TipStatus
import com.nangmanroad.core.data.formatDuration
import com.nangmanroad.core.data.formatMeters
import com.nangmanroad.vehicle.pleos.DrivingState
import com.nangmanroad.vehicle.ui.components.ActionButton
import com.nangmanroad.core.ui.byImportance
import com.nangmanroad.core.ui.color
import com.nangmanroad.core.ui.icon
import com.nangmanroad.core.ui.Palette

/**
 * While the car can move: no photos, feeds or scrolling, just the destination and at most three
 * short tips, readable in a one-to-two-second glance.
 */
@Composable
fun DrivingScreen(state: AppUiState, onExitPreview: () -> Unit) {
    val destination = state.destination
    // After a restart only the navigation app remembers the route, so match it back by name.
    val routed = state.spot(state.routedSpotId) ?: state.spots.firstOrNull { it.title == destination?.name }
    val tips = routed?.tips?.filter { it.status != TipStatus.GOOD }?.byImportance()?.take(3).orEmpty()

    Box(Modifier.fillMaxSize().background(Palette.Background).padding(48.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 1100.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Text("주행 중에는 간단한 정보만 보여드려요", style = MaterialTheme.typography.titleLarge, color = Palette.TextSecondary)
            if (destination != null || routed != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Palette.BackgroundRaised)
                        .padding(36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    Icon(Icons.Outlined.Flag, contentDescription = null, tint = Palette.Coral, modifier = Modifier.size(64.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(destination?.name ?: routed?.title.orEmpty(), style = MaterialTheme.typography.displaySmall, color = Palette.TextPrimary)
                        Text(
                            if (destination != null) "${formatMeters(destination.remainMeters)} · ${formatDuration(destination.remainSeconds)} 남음" else "내비게이션 안내를 따라가세요",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Palette.Peach,
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Icon(Icons.Outlined.PauseCircle, contentDescription = null, tint = Palette.Coral, modifier = Modifier.size(64.dp))
                    Text("정차하면 지도와 사진을 다시 볼 수 있어요", style = MaterialTheme.typography.headlineLarge, color = Palette.TextPrimary)
                }
            }
            if (tips.isNotEmpty()) {
                Text("도착 전에 알아두세요", style = MaterialTheme.typography.headlineSmall, color = Palette.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    tips.forEach { tip ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Palette.Surface)
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(tip.kind.icon(), contentDescription = null, tint = tip.status.color(), modifier = Modifier.size(44.dp))
                            Column {
                                Text(tip.kind.label, style = MaterialTheme.typography.titleSmall, color = Palette.TextSecondary)
                                Text(tip.summary, style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary)
                            }
                        }
                    }
                }
            }
            if (state.drivingPreview && state.drivingState != DrivingState.DRIVING) {
                ActionButton("미리보기 끝내기", null, onExitPreview)
            }
        }
    }
}
