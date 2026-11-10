package com.nangmanroad.phone.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.sync.isParkit
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.phone.ui.components.Hairline
import com.nangmanroad.phone.PhoneUiState
import com.nangmanroad.phone.PhoneViewModel

private data class Notice(val icon: ImageVector, val text: String, val time: String, val spotId: String?)

/** Sample notifications for the four kinds the prompt asks for, until push delivery exists. */
private val samples = listOf(
    Notice(Icons.Outlined.ChatBubbleOutline, "오후다섯시님이 '서해 끝에서 보는 마지막 빛'에 댓글을 남겼어요", "2일 전", "s-taean"),
    Notice(Icons.Outlined.QuestionAnswer, "주말의기록님이 내 질문 '몇 시쯤 가야 물안개를 볼 수 있나요?'에 답했어요", "4일 전", "s-yangpyeong"),
    Notice(Icons.Outlined.ThumbUp, "내 꿀팁 '노을 시간엔 만차'가 도움이 됐어요 52개를 받았어요", "1주 전", "s-taean"),
    Notice(Icons.Outlined.PersonAddAlt, "밤하늘수집가님이 나를 팔로우했어요", "1주 전", null),
)

@Composable
fun NotificationsScreen(state: PhoneUiState, actions: PhoneViewModel) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack, "뒤로", tint = Palette.TextPrimary,
                    modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = actions::back).padding(10.dp),
                )
                Text("알림", style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
            }
        }
        if (state.fromCar.isNotEmpty()) {
            item { Text("차량에서 보낸 항목", style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) }
            items(state.fromCar, key = { "car-${it.id}" }) { item ->
                val spot = state.spot(item.spotId)
                // A 파킷 at a new place starts a post there; at a known spot it opens that spot's tips.
                val newPlace = item.isParkit && spot == null && item.location != null
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = newPlace || spot != null, role = Role.Button) {
                            if (newPlace) actions.startShareFromCar(item) else spot?.let { actions.openSpot(it.id) }
                        }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (item.isParkit) Icons.Outlined.PushPin else Icons.Outlined.DirectionsCar, null, tint = Palette.TextSecondary, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.label, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
                        if (item.note.isNotBlank()) {
                            Text("“${item.note}”", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
                        }
                        Text(
                            when {
                                newPlace -> "눌러서 이 위치로 장소 공유 시작"
                                item.isParkit && spot != null -> "눌러서 꿀팁이 맞았는지 알려주기"
                                spot != null -> "눌러서 장소 보기"
                                else -> "차량 화면에서 보낸 항목"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.TextTertiary,
                        )
                    }
                    Icon(
                        Icons.Outlined.Close, "목록에서 지우기", tint = Palette.TextSecondary,
                        modifier = Modifier.size(36.dp).clip(CircleShape).clickable { actions.dismissFromCar(item.id) }.padding(6.dp),
                    )
                }
                Hairline()
            }
            item { Text("활동 알림", style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary, modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)) }
        }
        items(samples) { notice ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = notice.spotId != null, role = Role.Button) { notice.spotId?.let(actions::openSpot) }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(notice.icon, null, tint = Palette.TextSecondary, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f)) {
                    Text(notice.text, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
                    Text(notice.time, style = MaterialTheme.typography.bodySmall, color = Palette.TextTertiary)
                }
            }
            Hairline()
        }
        item {
            Text("활동 알림은 아직 예시예요. 차량에서 보낸 항목은 실제로 동기화돼요.", style = MaterialTheme.typography.bodySmall, color = Palette.TextTertiary, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
