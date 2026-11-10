package com.nangmanroad.phone.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Window
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.tabular
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.phone.PhoneUiState
import com.nangmanroad.phone.PhoneViewModel
import com.nangmanroad.phone.data.CarCommand
import com.nangmanroad.phone.data.CarDemoState
import com.nangmanroad.phone.ui.components.Hairline
import com.nangmanroad.phone.ui.components.SectionHeader
import com.nangmanroad.phone.ui.components.StatusLabel
import com.nangmanroad.phone.ui.components.Tag
import com.nangmanroad.phone.ui.components.PrimaryButton
import com.nangmanroad.phone.ui.components.SecondaryButton
import com.nangmanroad.phone.ui.detail.LabeledField

@Composable
fun CarScreen(state: PhoneUiState, actions: PhoneViewModel) {
    val car = state.car
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("내 차량", style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
                Tag("콘셉트", Palette.Warning)
            }
        }
        item { CarLinkCard(state, actions) }
        item { ConceptNotice() }
        item { CarSheet(car) }
        item { SectionHeader("원격 제어", Modifier.padding(top = 8.dp), trailing = { Tag("데모", Palette.Warning) }) }
        item {
            ControlRow(
                icon = if (car.doorsLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                title = if (car.doorsLocked) "도어 잠금 해제" else "도어 잠그기",
                subtitle = if (car.doorsLocked) "지금은 잠겨 있어요" else "지금은 열려 있어요",
                busy = car.processing == CarCommand.LOCK || car.processing == CarCommand.UNLOCK,
            ) { actions.requestCarCommand(if (car.doorsLocked) CarCommand.UNLOCK else CarCommand.LOCK) }
        }
        item {
            ClimateRow(car, actions)
        }
        item {
            ControlRow(
                icon = Icons.Outlined.Window,
                title = "창문 닫기",
                subtitle = if (car.driverWindowOpen) "운전석 창문이 열려 있어요" else "모든 창문이 닫혀 있어요",
                busy = car.processing == CarCommand.CLOSE_WINDOWS,
                enabled = car.driverWindowOpen,
            ) { actions.requestCarCommand(CarCommand.CLOSE_WINDOWS) }
        }
        item {
            ControlRow(
                icon = Icons.Outlined.Lightbulb,
                title = "실내 무드 조명",
                subtitle = "이 차량에서는 지원하지 않아요",
                busy = false,
                enabled = car.moodLightSupported,
            ) {}
        }
        item { SectionHeader("차량으로 보낸 장소", Modifier.padding(top = 8.dp)) }
        val queued = state.spots.filter { it.id in state.carQueue }
        if (queued.isEmpty()) {
            item { Text("장소 상세에서 '차량으로 보내기'를 누르면 여기에 모여요.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary) }
        }
        queued.forEach { spot ->
            item(key = "queued-${spot.id}") {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).clickable { actions.openSpot(spot.id) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SpotPhoto(spot.photos.first(), null, Modifier.size(56.dp).clip(RoundedCornerShape(3.dp)))
                    Column(Modifier.weight(1f)) {
                        Text(spot.title, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
                        Text(
                            "${spot.region} · " + when {
                                state.account.session == null -> "로그인하면 차량에 전송돼요"
                                state.user.linkedCars == 0 -> "차량을 연결하면 바로 보여요"
                                else -> "차량 화면의 '휴대폰에서 보냄'에 있어요"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.TextSecondary,
                        )
                    }
                    Icon(Icons.Outlined.Close, "목록에서 빼기", tint = Palette.TextSecondary, modifier = Modifier.size(36.dp).clip(CircleShape).clickable { actions.removeFromCar(spot.id) }.padding(6.dp))
                }
            }
        }
    }

    car.pending?.let { command ->
        AlertDialog(
            onDismissRequest = actions::cancelCarCommand,
            containerColor = Palette.BackgroundRaised,
            title = { Text(command.question, color = Palette.TextPrimary) },
            text = { Text(command.detail, color = Palette.TextSecondary) },
            confirmButton = { TextButton(onClick = actions::confirmCarCommand) { Text(command.title, color = Palette.TextPrimary) } },
            dismissButton = { TextButton(onClick = actions::cancelCarCommand) { Text("취소", color = Palette.TextSecondary) } },
        )
    }
}

/** Says plainly that the values are made up: a ruled note, not a coloured alert box. */
@Composable
private fun ConceptNotice() {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(2.dp).fillMaxHeight().background(Palette.Warning))
        Text(
            "실제 차량과 연결되지 않은 콘셉트 화면이에요. 휴대폰에서 차량을 원격 제어하는 공개 Pleos SDK가 나오면 연결할 예정이에요. 아래 값은 모두 예시예요.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/**
 * The car as a spec sheet: model, one large battery figure with a thin bar, then labelled rows
 * split by hairlines. Only a value that needs attention (a door left open) takes a colour.
 */
@Composable
private fun CarSheet(car: CarDemoState) {
    Column(Modifier.fillMaxWidth()) {
        Text(car.model, style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary)
        Text(
            "${car.nickname} · ${if (car.parked) "주차 중" else "주행 중"}",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary,
        )
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${car.batteryPercent}",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = (-2).sp).tabular(),
                color = Palette.TextPrimary,
            )
            Text("%", style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 6.dp)) {
                Text("주행 가능 거리", style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
                Text("${car.rangeKm} km", style = MaterialTheme.typography.titleLarge.tabular(), color = Palette.TextPrimary)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).background(Palette.Leaf)) {
            Box(Modifier.fillMaxWidth(car.batteryPercent / 100f).height(3.dp).background(Palette.TextPrimary))
        }
        Text(
            if (car.charging) "충전 중" else "충전 케이블 연결 안 됨",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
        )
        SheetRow("도어", if (car.doorsLocked) "잠김" else "열림", attention = !car.doorsLocked)
        SheetRow("창문", if (car.driverWindowOpen) "운전석 열림" else "모두 닫힘", attention = car.driverWindowOpen)
        SheetRow("실내 온도", "${car.cabinTemp}°C")
        SheetRow("공조", if (car.climateOn) "${car.targetTemp}°C로 켜짐" else "꺼짐")
        Hairline()
    }
}

@Composable
private fun SheetRow(label: String, value: String, attention: Boolean = false) {
    Hairline()
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall.tabular(), color = if (attention) Palette.Warning else Palette.TextPrimary)
    }
}

@Composable
private fun ControlRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    busy: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .clickable(enabled = enabled && !busy, role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, tint = if (enabled) Palette.TextSecondary else Palette.TextTertiary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (enabled) Palette.TextPrimary else Palette.TextTertiary)
            Text(if (busy) "차량에 요청하는 중…" else subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
        }
        if (busy) CircularProgressIndicator(Modifier.size(24.dp), color = Palette.TextSecondary, strokeWidth = 3.dp)
    }
    Hairline()
}

@Composable
private fun ClimateRow(car: CarDemoState, actions: PhoneViewModel) {
    val busy = car.processing == CarCommand.CLIMATE_ON || car.processing == CarCommand.CLIMATE_OFF
    Column(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Outlined.AcUnit, null, tint = Palette.TextSecondary)
            Column(Modifier.weight(1f)) {
                Text(if (car.climateOn) "공조 끄기" else "공조 켜기", style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
                Text(if (busy) "차량에 요청하는 중…" else "목표 온도 ${car.targetTemp}°C", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
            }
            if (busy) {
                CircularProgressIndicator(Modifier.size(24.dp), color = Palette.TextSecondary, strokeWidth = 3.dp)
            } else {
                SecondaryButton(if (car.climateOn) "끄기" else "켜기", { actions.requestCarCommand(if (car.climateOn) CarCommand.CLIMATE_OFF else CarCommand.CLIMATE_ON) })
            }
        }
        Row(Modifier.padding(start = 38.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stepper("−") { actions.setTargetTemp(car.targetTemp - 1) }
            Text("${car.targetTemp}°C", style = MaterialTheme.typography.headlineSmall.tabular(), color = Palette.TextPrimary, modifier = Modifier.weight(1f))
            Stepper("+") { actions.setTargetTemp(car.targetTemp + 1) }
        }
    }
    Hairline()
}

@Composable
private fun Stepper(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(4.dp)).background(Palette.Surface).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary)
    }
}

/** Links the head unit to this account: the car shows a 6-digit code, the phone types it here. */
@Composable
private fun CarLinkCard(state: PhoneUiState, actions: PhoneViewModel) {
    var code by rememberSaveable { mutableStateOf("") }
    // A successful link raises the count; clear the used code.
    LaunchedEffect(state.user.linkedCars) { code = "" }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Palette.BackgroundRaised).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Link, null, tint = Palette.TextSecondary)
            Text("차량 연결", style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary, modifier = Modifier.weight(1f))
            if (state.user.linkedCars > 0) StatusLabel("연결된 차량 ${state.user.linkedCars}대", Palette.Success)
        }
        if (state.account.session == null) {
            Text("차량 연결은 서버에 로그인한 뒤에 할 수 있어요.", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
            SecondaryButton("로그인하러 가기", actions::signOut, Modifier.fillMaxWidth())
        } else {
            Text(
                "차량 화면 왼쪽 아래 '계정 연결'을 누르면 6자리 코드가 나와요. 그 코드를 입력하면 차량이 ${state.me.name} 계정에 연결돼요.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    LabeledField("연결 코드", code, "예: 482913") { code = it.filter(Char::isDigit).take(6) }
                }
                PrimaryButton(
                    if (state.claimingCar) "연결 중…" else "연결",
                    { actions.claimCar(code) },
                    Modifier.padding(top = 22.dp),
                    enabled = code.length == 6 && !state.claimingCar,
                )
            }
        }
    }
}
