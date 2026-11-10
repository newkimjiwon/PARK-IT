package com.nangmanroad.vehicle.ui.driving

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.ARRIVAL_COMFORT_PERCENT
import com.nangmanroad.core.data.ARRIVAL_LOW_PERCENT
import com.nangmanroad.core.data.CarCamping
import com.nangmanroad.core.data.SunMoment
import com.nangmanroad.core.data.arrivalLabel
import com.nangmanroad.core.data.arrivalPercent
import com.nangmanroad.core.data.arrivalTips
import com.nangmanroad.core.data.at
import com.nangmanroad.core.data.carCampingStale
import com.nangmanroad.core.data.formatClock
import com.nangmanroad.core.data.formatDuration
import com.nangmanroad.core.data.formatMeters
import com.nangmanroad.core.data.sunMoment
import com.nangmanroad.core.data.sunTimesToday
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.color
import com.nangmanroad.core.ui.label
import com.nangmanroad.core.ui.tabular
import com.nangmanroad.vehicle.AppUiState
import com.nangmanroad.vehicle.ListenPurpose
import com.nangmanroad.vehicle.VoiceUi
import com.nangmanroad.vehicle.pleos.DrivingState
import com.nangmanroad.vehicle.ui.components.ActionButton

/**
 * While the car can move: no photos, feeds or scrolling, just the destination and at most three
 * short tips, readable in a one-to-two-second glance.
 *
 * Laid out like a printed timetable rather than a stack of cards: the remaining distance is by far
 * the largest thing on screen, everything else is small labelled text on the plain ground, and
 * sections are split by 1px rules. Colour appears only where it carries a warning.
 */
@Composable
fun DrivingScreen(
    state: AppUiState,
    onExitPreview: () -> Unit,
    onToggleBriefing: () -> Unit,
    onListen: () -> Unit,
) {
    val destination = state.destination
    val routed = state.routedSpot
    val tips = routed?.arrivalTips().orEmpty()

    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 72.dp, vertical = 44.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 1240.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("주행 중", style = MaterialTheme.typography.labelLarge, color = Palette.TextSecondary)
                Spacer(Modifier.weight(1f))
                VoiceControls(state.voice, onToggleBriefing, onListen)
            }
            Spacer(Modifier.height(16.dp))
            Rule(Palette.TextPrimary, thickness = 2)
            HeardLine(state.voice)
            Spacer(Modifier.height(28.dp))

            if (destination != null || routed != null) {
                Text(
                    destination?.name ?: routed?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Palette.TextSecondary,
                )
                if (destination != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            formatMeters(destination.remainMeters),
                            style = MaterialTheme.typography.displayLarge.tabular(),
                            color = Palette.TextPrimary,
                        )
                        Spacer(Modifier.width(56.dp))
                        Figure("남은 시간", formatDuration(destination.remainSeconds), Modifier.padding(bottom = 18.dp))
                        Spacer(Modifier.width(48.dp))
                        Figure("도착 예정", arrivalTime(destination.remainSeconds), Modifier.padding(bottom = 18.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                        // What the navigation's road distance leaves in the pack: the number an EV driver watches.
                        state.battery?.let { battery ->
                            val left = arrivalPercent(battery.percent, battery.capacityKwh, destination.remainMeters / 1000.0)
                            val tone = when {
                                left < ARRIVAL_LOW_PERCENT -> Palette.Alert
                                left < ARRIVAL_COMFORT_PERCENT -> Palette.Warning
                                else -> Palette.TextPrimary
                            }
                            Text(arrivalLabel(left), style = MaterialTheme.typography.titleMedium.tabular(), color = tone)
                        }
                        // For a sunset or sunrise place, the one question is whether we make it in time.
                        val moment = routed?.category?.sunMoment
                        val sun = if (moment != null && routed != null) sunTimesToday(routed.location) else null
                        if (moment != null && sun != null) {
                            if (state.battery != null) VerticalRule(Modifier.padding(horizontal = 24.dp))
                            Text(
                                sunVerdict(moment, sun.at(moment), destination.remainSeconds),
                                style = MaterialTheme.typography.titleMedium.tabular(),
                                color = Palette.TextPrimary,
                            )
                        }
                    }
                } else {
                    Text("내비게이션 안내를 따라가세요", style = MaterialTheme.typography.headlineLarge, color = Palette.TextPrimary)
                }
            } else {
                Text("정차하면 지도와 사진을 다시 볼 수 있어요", style = MaterialTheme.typography.headlineLarge, color = Palette.TextPrimary)
            }

            // Whether the night can be spent there is the one thing to know before arriving.
            if (routed != null && routed.carCamping != CarCamping.CHECK) {
                Spacer(Modifier.height(36.dp))
                val tone = if (routed.carCampingStale) Palette.TextSecondary else routed.carCamping.color()
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(Modifier.width(4.dp).fillMaxHeight().background(tone))
                    Column(Modifier.padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (routed.carCampingStale) "${routed.carCamping.label} · 오래된 정보" else routed.carCamping.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = tone,
                        )
                        if (routed.carCampingNote.isNotBlank()) {
                            Text(routed.carCampingNote, style = MaterialTheme.typography.bodyLarge, color = Palette.TextPrimary)
                        }
                    }
                }
            }

            if (tips.isNotEmpty()) {
                Spacer(Modifier.height(40.dp))
                Text("도착 전에 알아두세요", style = MaterialTheme.typography.labelLarge, color = Palette.TextSecondary)
                Spacer(Modifier.height(12.dp))
                Rule(Palette.Sage)
                // Three numbered columns split by rules, like a timetable; only the status word is coloured.
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    tips.forEachIndexed { index, tip ->
                        if (index > 0) VerticalRule()
                        Column(
                            Modifier.weight(1f).padding(start = if (index > 0) 28.dp else 0.dp, end = 28.dp, top = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("${index + 1}", style = MaterialTheme.typography.labelLarge.tabular(), color = Palette.TextTertiary)
                                Text(tip.kind.label, style = MaterialTheme.typography.labelLarge, color = Palette.TextSecondary)
                                Text(tip.status.label(), style = MaterialTheme.typography.labelLarge, color = tip.status.color())
                            }
                            Text(tip.summary, style = MaterialTheme.typography.headlineSmall, color = Palette.TextPrimary)
                        }
                    }
                }
            }

            if (state.drivingPreview && state.drivingState != DrivingState.DRIVING) {
                Spacer(Modifier.height(40.dp))
                ActionButton("미리보기 끝내기", null, onExitPreview)
            }
        }
    }
}

/** A small label over a number, for the figures beside the remaining distance. */
@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Palette.TextSecondary)
        Text(value, style = MaterialTheme.typography.headlineMedium.tabular(), color = Palette.TextPrimary)
    }
}

@Composable
private fun Rule(color: Color, thickness: Int = 1) {
    Box(Modifier.fillMaxWidth().height(thickness.dp).background(color))
}

@Composable
private fun VerticalRule(modifier: Modifier = Modifier) {
    Box(modifier.width(1.dp).fillMaxHeight().background(Palette.Sage))
}

/**
 * Two big buttons instead of anything to read: hear the briefing again (or stop it), and ask a
 * question out loud. Each shows only when the car's speech service answered.
 */
@Composable
private fun VoiceControls(voice: VoiceUi, onToggleBriefing: () -> Unit, onListen: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (voice.canSpeak) {
            ActionButton(
                text = if (voice.speaking) "그만 듣기" else "다시 듣기",
                icon = if (voice.speaking) Icons.Outlined.StopCircle else Icons.AutoMirrored.Outlined.VolumeUp,
                onClick = onToggleBriefing,
                active = voice.speaking,
            )
        }
        if (voice.canListen) {
            val listening = voice.listening == ListenPurpose.COMMAND
            ActionButton(
                text = if (listening) "듣는 중" else "말하기",
                icon = Icons.Outlined.Mic,
                onClick = onListen,
                active = listening,
            )
        }
    }
}

/** The words the microphone caught, so the driver can tell a misheard question from a wrong answer. */
@Composable
private fun HeardLine(voice: VoiceUi) {
    if (voice.heardFor != ListenPurpose.COMMAND) return
    val listening = voice.listening == ListenPurpose.COMMAND
    val text = when {
        listening && voice.heard.isBlank() -> "듣고 있어요. '꿀팁 알려 줘', '배터리 얼마나 남았어'처럼 말해 보세요"
        voice.heard.isNotBlank() -> "“${voice.heard}”"
        else -> return
    }
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = if (listening) Palette.TextPrimary else Palette.TextSecondary,
        modifier = Modifier.padding(top = 12.dp),
    )
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
