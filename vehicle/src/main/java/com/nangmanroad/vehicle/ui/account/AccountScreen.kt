package com.nangmanroad.vehicle.ui.account

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
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nangmanroad.core.ui.Avatar
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.tabular
import com.nangmanroad.vehicle.AppUiState
import com.nangmanroad.vehicle.AppViewModel
import com.nangmanroad.vehicle.ui.components.ActionButton
import kotlinx.coroutines.delay

/** Links the head unit to the driver's phone account with a short-lived 6-digit code. */
@Composable
fun AccountScreen(state: AppUiState, actions: AppViewModel) {
    Box(Modifier.fillMaxSize().padding(48.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 980.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            val session = state.session
            if (session != null) {
                Text("휴대폰과 연결되어 있어요", style = MaterialTheme.typography.headlineLarge, color = Palette.TextPrimary)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Palette.BackgroundRaised).padding(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Avatar(session.user.name, size = 72.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(session.user.name, style = MaterialTheme.typography.headlineSmall, color = Palette.TextPrimary)
                        Text(
                            "저장, 좋아요, 팔로우가 휴대폰과 같아요. 휴대폰에서 '차량으로 보내기'한 장소는 왼쪽 '휴대폰' 메뉴에 모여요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.TextSecondary,
                        )
                        Text(
                            if (state.online == false) "지금은 서버에 연결할 수 없어요 · ${state.serverUrl}" else "동기화 서버 · ${state.serverUrl}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.online == false) Palette.Warning else Palette.TextTertiary,
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Palette.BackgroundRaised).padding(horizontal = 28.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("파킷 위치를 시·군 중심으로만 보내기", style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary)
                        Text(
                            "집 앞이나 회사 주차장에서 파킷해도 정확한 자리가 남지 않아요. 휴대폰에서 공유할 때도 '대략적인 위치'로 올라가요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.TextSecondary,
                        )
                    }
                    ActionButton(
                        text = if (state.approximateParkit) "켜짐" else "꺼짐",
                        icon = if (state.approximateParkit) Icons.Outlined.LocationOff else Icons.Outlined.MyLocation,
                        onClick = { actions.setApproximateParkit(!state.approximateParkit) },
                        active = state.approximateParkit,
                    )
                }
                VoiceGuidanceRow(state, actions)
                ActionButton("연결 해제", Icons.Outlined.LinkOff, actions::unlinkAccount)
            } else {
                Text("휴대폰 계정과 연결하기", style = MaterialTheme.typography.headlineLarge, color = Palette.TextPrimary)
                Text(
                    "휴대폰 파킷 앱의 차량 탭 > 차량 연결에 이 코드를 입력하세요.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Palette.TextSecondary,
                )
                PairingCodeCard(state, actions)
                VoiceGuidanceRow(state, actions)
            }
        }
    }
}

/** Spoken guidance works without an account, so it sits under both the linked and the pairing view. */
@Composable
private fun VoiceGuidanceRow(state: AppUiState, actions: AppViewModel) {
    val voice = state.voice
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Palette.BackgroundRaised).padding(horizontal = 28.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("음성 안내", style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary)
            Text(
                "출발할 때 남은 거리, 도착 시 배터리, 일몰, 차박 상태와 꿀팁을 읽어 줘요. 1km 남으면 꿀팁을 한 번 더, 휴대폰에서 장소가 오면 알려 줘요.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextSecondary,
            )
            Text(
                when {
                    voice.canSpeak && voice.canListen -> "Pleos 음성 합성·인식 연결됨 · 주행 화면의 '말하기'로 물어볼 수 있어요"
                    voice.problem != null -> voice.problem
                    voice.canSpeak -> "Pleos 음성 합성 연결됨 · 음성 인식은 쓸 수 없어요"
                    else -> "이 차량에서는 Pleos 음성 서비스에 연결할 수 없어요"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (voice.canSpeak) Palette.TextTertiary else Palette.Warning,
            )
        }
        ActionButton(
            text = if (voice.guidance) "켜짐" else "꺼짐",
            icon = if (voice.guidance) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
            onClick = { actions.setVoiceGuidance(!voice.guidance) },
            active = voice.guidance,
        )
    }
}

@Composable
private fun PairingCodeCard(state: AppUiState, actions: AppViewModel) {
    val pairing = state.pairing
    val code = pairing?.code
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Palette.BackgroundRaised).padding(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        if (pairing?.error != null) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = Palette.Warning, modifier = Modifier.size(44.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                pairing?.error != null -> {
                    Text(pairing.error, style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary)
                    Text(
                        "서버 주소: ${state.serverUrl}\n개발용 Mac에서 sh gradlew :server:run 으로 서버를 켜 주세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.TextSecondary,
                    )
                }
                code == null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(Modifier.size(36.dp), color = Palette.TextSecondary)
                    Text("코드를 받는 중이에요", style = MaterialTheme.typography.titleLarge, color = Palette.TextSecondary)
                }
                else -> {
                    Text(
                        code.code.chunked(3).joinToString("  "),
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 72.sp, lineHeight = 84.sp, letterSpacing = 4.sp).tabular(),
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.TextPrimary,
                    )
                    RemainingTime(code.expiresAt)
                }
            }
        }
        if (pairing?.error != null || code != null) {
            ActionButton(if (pairing?.error != null) "다시 시도" else "새 코드", Icons.Outlined.Refresh, actions::startPairing)
        }
    }
}

@Composable
private fun RemainingTime(expiresAt: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(expiresAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val seconds = ((expiresAt - now) / 1000).coerceAtLeast(0)
    Text(
        "휴대폰에서 입력하기를 기다리는 중 · ${seconds / 60}분 ${seconds % 60}초 남음",
        style = MaterialTheme.typography.bodyMedium.tabular(),
        color = Palette.TextSecondary,
    )
}
