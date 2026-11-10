package com.nangmanroad.vehicle.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.ThumbUp
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.Comment
import com.nangmanroad.core.data.HoneyTip
import com.nangmanroad.core.data.Product
import com.nangmanroad.core.data.Spot
import com.nangmanroad.vehicle.ui.components.ActionButton
import com.nangmanroad.core.ui.Avatar
import com.nangmanroad.vehicle.ui.components.Hairline
import com.nangmanroad.vehicle.ui.components.Marker
import com.nangmanroad.vehicle.ui.components.StatusLabel
import com.nangmanroad.vehicle.ui.theme.Dimens
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.tabular
import com.nangmanroad.core.data.byImportance
import com.nangmanroad.core.ui.color
import com.nangmanroad.core.ui.icon
import com.nangmanroad.core.ui.label
import java.text.NumberFormat
import java.util.Locale

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
    Column(modifier) {
        spot.lastConfirmedDaysAgo?.let {
            Text(
                "방문자가 남긴 정보 · 최근 확인 ${it}일 전",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        tips.forEach { tip ->
            TipRow(
                tip = tip,
                expanded = expanded == tip.id,
                markedHelpful = tip.id in helpful,
                onClick = { expanded = if (expanded == tip.id) null else tip.id },
                onToggleHelpful = { onToggleHelpful(tip.id) },
            )
            Hairline()
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "차박·주차 허용 여부는 현장 안내문으로 다시 확인하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextTertiary,
                modifier = Modifier.weight(1f),
            )
            ActionButton("휴대폰에서 꿀팁 제보", Icons.Outlined.PhoneAndroid, onSuggest)
        }
    }
}

@Composable
private fun TipRow(
    tip: HoneyTip,
    expanded: Boolean,
    markedHelpful: Boolean,
    onClick: () -> Unit,
    onToggleHelpful: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = if (expanded) "접기" else "자세히", onClick = onClick)
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Icon(tip.kind.icon(), contentDescription = null, tint = Palette.TextSecondary, modifier = Modifier.size(28.dp))
            Text(
                tip.kind.label,
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextSecondary,
                modifier = Modifier.width(88.dp),
            )
            Text(tip.summary, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary, modifier = Modifier.weight(1f))
            StatusLabel(tip.status.label(), tip.status.color())
            HelpfulButton(tip.helpfulCount + if (markedHelpful) 1 else 0, markedHelpful, onToggleHelpful)
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(start = 46.dp, end = 120.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tip.detail, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
                Text(
                    "${tip.reporter} · ${tip.confirmedDaysAgo}일 전 확인",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun HelpfulButton(count: Int, marked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = Dimens.TouchTarget)
            .widthIn(min = 96.dp)
            .clip(RoundedCornerShape(Dimens.ControlRadius))
            .background(if (marked) Palette.SurfaceHigh else Color.Transparent)
            .clickable(role = Role.Button, onClickLabel = if (marked) "도움이 됐어요 취소" else "도움이 됐어요", onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        Icon(
            if (marked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            contentDescription = null,
            tint = if (marked) Palette.TextPrimary else Palette.TextTertiary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium.tabular(),
            color = if (marked) Palette.TextPrimary else Palette.TextSecondary,
        )
    }
}

@Composable
fun ProductSection(spot: Spot, onOpenOnPhone: (Product) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            "방문자들이 이곳에 챙겨 간 물건이에요. 구매는 휴대폰에서 할 수 있어요.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (spot.products.isEmpty()) {
            Text("아직 등록된 준비물이 없어요.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        }
        spot.products.forEach { product ->
            ProductRow(product, onOpenOnPhone = { onOpenOnPhone(product) })
            Hairline()
        }
    }
}

@Composable
private fun ProductRow(product: Product, onOpenOnPhone: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SpotPhoto(
            product.imageUrl,
            contentDescription = null,
            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(Dimens.PhotoRadius)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary)
                if (product.isSponsored) Marker("광고·제휴", color = Palette.Warning)
            }
            Text(product.reason, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
            Text(
                (product.priceWon?.let { NumberFormat.getNumberInstance(Locale.KOREA).format(it) + "원" } ?: "가격 정보 없음") +
                    " · ${product.recommender} 추천",
                style = MaterialTheme.typography.labelMedium.tabular(),
                color = Palette.TextPrimary,
            )
        }
        ActionButton("휴대폰에서 보기", Icons.Outlined.PhoneAndroid, onOpenOnPhone)
    }
}

@Composable
fun CommentSection(spot: Spot, onReplyOnPhone: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "질문과 후기",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            ActionButton("휴대폰에서 댓글 쓰기", Icons.Outlined.PhoneAndroid, onReplyOnPhone)
        }
        if (spot.comments.isEmpty()) {
            Text("첫 질문이나 후기를 휴대폰에서 남겨 보세요.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        }
        spot.comments.forEach { comment ->
            Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CommentItem(comment)
                comment.replies.forEach { reply ->
                    Row {
                        Spacer(Modifier.width(58.dp))
                        CommentItem(reply)
                    }
                }
            }
            Hairline()
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
                if (comment.isQuestion) Marker("질문")
                Text(comment.postedAgo, style = MaterialTheme.typography.bodySmall, color = Palette.TextTertiary)
            }
            Text(comment.text, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
        }
    }
}
