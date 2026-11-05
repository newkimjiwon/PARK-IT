package com.nangmanroad.vehicle.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nangmanroad.vehicle.data.Comment
import com.nangmanroad.vehicle.data.HoneyTip
import com.nangmanroad.vehicle.data.Product
import com.nangmanroad.vehicle.data.Spot
import com.nangmanroad.vehicle.data.TipKind
import com.nangmanroad.vehicle.data.TipStatus
import com.nangmanroad.vehicle.ui.components.ActionButton
import com.nangmanroad.vehicle.ui.components.Avatar
import com.nangmanroad.vehicle.ui.components.Badge
import com.nangmanroad.vehicle.ui.components.SectionLabel
import com.nangmanroad.vehicle.ui.components.SpotPhoto
import com.nangmanroad.vehicle.ui.theme.Palette
import java.text.NumberFormat
import java.util.Locale

fun TipKind.icon(): ImageVector = when (this) {
    TipKind.TOILET -> Icons.Outlined.Wc
    TipKind.PARKING -> Icons.Outlined.LocalParking
    TipKind.POWER -> Icons.Outlined.ElectricalServices
    TipKind.SIGNAL -> Icons.Outlined.SignalCellularAlt
    TipKind.NIGHT -> Icons.Outlined.NightsStay
    TipKind.STORE -> Icons.Outlined.Storefront
    TipKind.ACCESS -> Icons.Outlined.DirectionsCar
    TipKind.CAUTION -> Icons.Outlined.ReportProblem
}

fun TipStatus.color(): Color = when (this) {
    TipStatus.GOOD -> Palette.Success
    TipStatus.PREPARE -> Palette.Warning
    TipStatus.NEEDS_CHECK -> Palette.Coral
}

private fun TipStatus.label(): String = when (this) {
    TipStatus.GOOD -> "괜찮아요"
    TipStatus.PREPARE -> "준비하세요"
    TipStatus.NEEDS_CHECK -> "확인 필요"
}

/** Problems first, then things to prepare, then the reassuring ones. */
fun List<HoneyTip>.byImportance(): List<HoneyTip> =
    sortedWith(compareBy<HoneyTip> { it.status != TipStatus.NEEDS_CHECK }.thenBy { it.status != TipStatus.PREPARE }.thenByDescending { it.helpfulCount })

@Composable
fun HoneyTipSection(
    spot: Spot,
    helpful: Set<String>,
    onToggleHelpful: (String) -> Unit,
    onSuggest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(spot.id) { mutableStateOf<String?>(null) }
    val tips = spot.tips.byImportance()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                SectionLabel("BEFORE YOU GO")
                Text("방문 전에 알아두세요", style = MaterialTheme.typography.headlineSmall, color = Palette.TextPrimary)
            }
            spot.lastConfirmedDaysAgo?.let {
                Text("최근 확인 ${it}일 전", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
            }
        }
        TwoColumnGrid(tips.size) { index ->
            val tip = tips[index]
            TipCard(
                tip = tip,
                expanded = expanded == tip.id,
                markedHelpful = tip.id in helpful,
                onClick = { expanded = if (expanded == tip.id) null else tip.id },
                onToggleHelpful = { onToggleHelpful(tip.id) },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().card().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null, tint = Palette.TextSecondary, modifier = Modifier.size(30.dp))
            Text(
                "방문자가 남긴 정보예요. 차박·주차 허용 여부는 현장 안내문을 꼭 확인하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            ActionButton("휴대폰에서 꿀팁 제보", Icons.Outlined.PhoneAndroid, onSuggest)
        }
    }
}

@Composable
private fun TipCard(
    tip: HoneyTip,
    expanded: Boolean,
    markedHelpful: Boolean,
    onClick: () -> Unit,
    onToggleHelpful: () -> Unit,
) {
    val tone = tip.status.color()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .card()
            .clickable(role = Role.Button, onClickLabel = if (expanded) "접기" else "자세히", onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tone.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tip.kind.icon(), contentDescription = null, tint = tone, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(tip.kind.label, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                Text(tip.summary, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
            }
            Badge(tip.status.label(), tone)
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tip.detail, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
                Text(
                    "${tip.reporter} · ${tip.confirmedDaysAgo}일 전 확인",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary,
                )
            }
        }
        Row(
            modifier = Modifier
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (markedHelpful) Palette.Coral.copy(alpha = 0.18f) else Palette.SurfaceHigh)
                .clickable(role = Role.Button, onClick = onToggleHelpful)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (markedHelpful) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                contentDescription = null,
                tint = if (markedHelpful) Palette.Coral else Palette.TextSecondary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                "도움이 됐어요 ${tip.helpfulCount + if (markedHelpful) 1 else 0}",
                style = MaterialTheme.typography.labelMedium,
                color = if (markedHelpful) Palette.Coral else Palette.TextSecondary,
            )
        }
    }
}

@Composable
fun ProductSection(spot: Spot, onOpenOnPhone: (Product) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                SectionLabel("TRAVEL KIT")
                Text("이 낭만을 만든 아이템", style = MaterialTheme.typography.headlineSmall, color = Palette.TextPrimary)
            }
            Text("구매는 휴대폰에서 할 수 있어요", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
        }
        if (spot.products.isEmpty()) {
            Text("아직 추천된 아이템이 없어요.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        }
        spot.products.forEach { product -> ProductRow(product, onOpenOnPhone = { onOpenOnPhone(product) }) }
    }
}

@Composable
private fun ProductRow(product: Product, onOpenOnPhone: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().card(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpotPhoto(product.imageUrl, contentDescription = null, modifier = Modifier.width(180.dp).aspectRatio(1.1f))
        Column(Modifier.weight(1f).padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${product.recommender} 추천", style = MaterialTheme.typography.labelMedium, color = Palette.Peach)
                if (product.isSponsored) Badge("광고·제휴", Palette.Warning)
            }
            Text(product.name, style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary)
            Text(product.reason, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
            Text(
                product.priceWon?.let { NumberFormat.getNumberInstance(Locale.KOREA).format(it) + "원" } ?: "가격 정보 없음",
                style = MaterialTheme.typography.labelLarge,
                color = Palette.TextPrimary,
            )
        }
        ActionButton("휴대폰에서 보기", Icons.Outlined.PhoneAndroid, onOpenOnPhone, modifier = Modifier.padding(end = 20.dp))
    }
}

@Composable
fun CommentSection(spot: Spot, onReplyOnPhone: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                SectionLabel("COMMUNITY")
                Text("방문자 이야기", style = MaterialTheme.typography.headlineSmall, color = Palette.TextPrimary)
            }
            ActionButton("휴대폰에서 댓글 쓰기", Icons.Outlined.PhoneAndroid, onReplyOnPhone)
        }
        if (spot.comments.isEmpty()) {
            Text("첫 질문이나 후기를 휴대폰에서 남겨 보세요.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        }
        spot.comments.forEach { comment ->
            Column(Modifier.fillMaxWidth().card().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CommentItem(comment)
                comment.replies.forEach { reply ->
                    Row {
                        Spacer(Modifier.width(36.dp))
                        CommentItem(reply)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Avatar(comment.author, size = 44.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(comment.author, style = MaterialTheme.typography.labelLarge, color = Palette.TextPrimary, fontWeight = FontWeight.SemiBold)
                if (comment.isQuestion) Badge("질문", Palette.Peach)
                Text(comment.postedAgo, style = MaterialTheme.typography.bodySmall, color = Palette.TextTertiary)
            }
            Text(comment.text, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
        }
    }
}
