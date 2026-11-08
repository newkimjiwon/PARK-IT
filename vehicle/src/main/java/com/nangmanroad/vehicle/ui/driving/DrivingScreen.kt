package com.nangmanroad.vehicle.ui.driving

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.CarCamping
import com.nangmanroad.core.data.carCampingStale
import com.nangmanroad.core.data.TipStatus
import com.nangmanroad.core.data.formatDuration
import com.nangmanroad.core.data.formatMeters
import com.nangmanroad.core.data.SunMoment
import com.nangmanroad.core.data.at
import com.nangmanroad.core.data.formatClock
import com.nangmanroad.core.data.sunMoment
import com.nangmanroad.core.data.sunTimesToday
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.byImportance
import com.nangmanroad.core.ui.color
import com.nangmanroad.core.ui.icon
import com.nangmanroad.core.ui.label
import com.nangmanroad.core.ui.tabular
import com.nangmanroad.vehicle.AppUiState
import com.nangmanroad.vehicle.pleos.DrivingState
import com.nangmanroad.vehicle.ui.components.ActionButton

/**
 * While the car can move: no photos, feeds or scrolling, just the destination and at most three
 * short tips, readable in a one-to-two-second glance. The numbers are the biggest thing on screen.
 */
@Composable
fun DrivingScreen(state: AppUiState, onExitPreview: () -> Unit) {
    val destination = state.destination
    // After a restart only the navigation app remembers the route, so match it back by name.
    val routed = state.spot(state.routedSpotId) ?: state.spots.firstOrNull { it.title == destination?.name }
    val tips = routed?.tips?.filter { it.status != TipStatus.GOOD }?.byImportance()?.take(3).orEmpty()

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.Background, Palette.BackgroundRaised)))
            .padding(horizontal = 56.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 1160.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(Palette.Coral))
                Text("주행 중", style = MaterialTheme.typography.titleMedium, color = Palette.TextSecondary)
            }
            if (destination != null || routed != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.Surface.copy(alpha = 0.6f))
                        .padding(horizontal = 40.dp, vertical = 36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    Box(
                        Modifier.size(96.dp).clip(CircleShape).background(Palette.Coral.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Flag, contentDescription = null, tint = Palette.Coral, modifier = Modifier.size(52.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            destination?.name ?: routed?.title.orEmpty(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Palette.TextSecondary,
                        )
                        if (destination != null) {
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                Text(
                                    formatMeters(destination.remainMeters),
                                    style = MaterialTheme.typography.displaySmall.tabular(),
                                    color = Palette.TextPrimary,
                                )
                                Text(
                                    "${formatDuration(destination.remainSeconds)} 남음",
                                    style = MaterialTheme.typography.headlineMedium.tabular(),
                                    color = Palette.TextPrimary,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                                Spacer(Modifier.weight(1f))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("도착 예정", style = MaterialTheme.typography.labelLarge, color = Palette.TextSecondary)
                                    Text(
                                        arrivalTime(destination.remainSeconds),
                                        style = MaterialTheme.typography.headlineMedium.tabular(),
                                        color = Palette.TextPrimary,
                                    )
                                }
                            }
                            // For a sunset or sunrise place, the one question is whether we make it in time.
                            val moment = routed?.category?.sunMoment
                            val sun = if (moment != null && routed != null) sunTimesToday(routed.location) else null
                            if (moment != null && sun != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(
                                        if (moment == SunMoment.SUNSET) Icons.Outlined.WbTwilight else Icons.Outlined.WbSunny,
                                        contentDescription = null,
                                        tint = Palette.Warning,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Text(
                                        sunVerdict(moment, sun.at(moment), destination.remainSeconds),
                                        style = MaterialTheme.typography.titleMedium.tabular(),
                                        color = Palette.TextPrimary,
                                    )
                                }
                            }
                        } else {
                            Text("내비게이션 안내를 따라가세요", style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Icon(Icons.Outlined.PauseCircle, contentDescription = null, tint = Palette.TextSecondary, modifier = Modifier.size(64.dp))
                    Text("정차하면 지도와 사진을 다시 볼 수 있어요", style = MaterialTheme.typography.headlineLarge, color = Palette.TextPrimary)
                }
            }
            // Whether the night can be spent there is the one thing to know before arriving.
            if (routed != null && routed.carCamping != CarCamping.CHECK) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(routed.carCamping.color().copy(alpha = 0.14f))
                        .padding(horizontal = 28.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Icon(routed.carCamping.icon(), contentDescription = null, tint = routed.carCamping.color(), modifier = Modifier.size(44.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (routed.carCampingStale) "${routed.carCamping.label} · 오래된 정보" else routed.carCamping.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (routed.carCampingStale) Palette.TextSecondary else routed.carCamping.color(),
                        )
                        if (routed.carCampingNote.isNotBlank()) {
                            Text(routed.carCampingNote, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
                        }
                    }
                }
            }
            if (tips.isNotEmpty()) {
                Text("도착 전에 알아두세요", style = MaterialTheme.typography.headlineSmall, color = Palette.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    tips.forEach { tip ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Palette.Surface.copy(alpha = 0.6f)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // A thin colour edge says "fine / prepare / check" before the words do.
                            Box(Modifier.width(8.dp).height(128.dp).background(tip.status.color()))
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                            ) {
                                Icon(tip.kind.icon(), contentDescription = null, tint = tip.status.color(), modifier = Modifier.size(44.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "${tip.kind.label} · ${tip.status.label()}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Palette.TextSecondary,
                                    )
                                    Text(tip.summary, style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary)
                                }
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

/** "일몰 오후 5:23 · 40분 전 도착", or that the sun will already be gone (or up) when we get there. */
private fun sunVerdict(moment: SunMoment, sunAt: Long, remainSeconds: Int): String {
    val arrival = System.currentTimeMillis() + remainSeconds * 1000L
    val minutesToSpare = ((sunAt - arrival) / 60_000L).toInt()
    val when_ = "${moment.label} ${formatClock(sunAt)}"
    return when {
        minutesToSpare >= 1 -> "$when_ · ${formatDuration(minutesToSpare * 60)} 전 도착"
        moment == SunMoment.SUNSET -> "$when_ · 해 진 뒤 도착해요"
        else -> "$when_ · 해 뜬 뒤 도착해요"
    }
}

/** Clock time [secondsFromNow] ahead, e.g. "오후 9:40", on the same Korean clock as the sun times. */
private fun arrivalTime(secondsFromNow: Int): String = formatClock(System.currentTimeMillis() + secondsFromNow * 1000L)
