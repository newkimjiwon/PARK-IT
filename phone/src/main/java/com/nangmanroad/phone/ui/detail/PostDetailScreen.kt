package com.nangmanroad.phone.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.TaskAlt
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.CarCamping
import com.nangmanroad.core.data.Comment
import com.nangmanroad.core.data.HoneyTip
import com.nangmanroad.core.data.Product
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.TipStatus
import com.nangmanroad.core.ui.Avatar
import com.nangmanroad.core.ui.CarCampingBadge
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.tabular
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.core.ui.byImportance
import com.nangmanroad.core.ui.color
import com.nangmanroad.core.ui.icon
import com.nangmanroad.core.ui.label
import com.nangmanroad.phone.PhoneUiState
import com.nangmanroad.phone.PhoneViewModel
import com.nangmanroad.phone.ui.components.Chip
import com.nangmanroad.phone.ui.components.PrimaryButton
import com.nangmanroad.phone.ui.components.SecondaryButton
import com.nangmanroad.phone.ui.components.Hairline
import com.nangmanroad.phone.ui.components.StatusLabel
import com.nangmanroad.phone.ui.components.TabRow
import com.nangmanroad.phone.ui.components.Tag
import com.nangmanroad.phone.ui.components.TextTab
import com.nangmanroad.phone.ui.components.distanceFromHere
import com.nangmanroad.phone.ui.home.VisibilityTag
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

private enum class DetailTab(val label: String) { TIPS("꿀팁"), ITEMS("준비물"), COMMENTS("댓글") }

@Composable
fun PostDetailScreen(spot: Spot, state: PhoneUiState, actions: PhoneViewModel) {
    var tab by rememberSaveable { mutableStateOf(DetailTab.TIPS) }
    var replyTo by rememberSaveable { mutableStateOf<String?>(null) }
    var suggesting by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { Gallery(spot, onBack = actions::back) }
            item { Header(spot, state, actions) }
            item {
                TabRow(Modifier.padding(top = 20.dp)) {
                    TextTab(DetailTab.TIPS.label, tab == DetailTab.TIPS, { tab = DetailTab.TIPS }, count = spot.tips.size)
                    TextTab(DetailTab.ITEMS.label, tab == DetailTab.ITEMS, { tab = DetailTab.ITEMS }, count = spot.products.size)
                    TextTab(DetailTab.COMMENTS.label, tab == DetailTab.COMMENTS, { tab = DetailTab.COMMENTS }, count = spot.commentCount)
                }
            }
            when (tab) {
                DetailTab.TIPS -> {
                    item {
                        Meta(
                            "방문자가 남긴 정보" + (spot.lastConfirmedDaysAgo?.let { days -> if (days == 0) " · 오늘 확인" else " · 최근 확인 ${days}일 전" } ?: ""),
                        )
                    }
                    items(spot.tips.byImportance(), key = { it.id }) { tip ->
                        TipRow(tip, tip.id in state.helpfulTips, onHelpful = { actions.toggleHelpful(tip.id) }, modifier = Modifier.padding(horizontal = 20.dp))
                        Hairline(Modifier.padding(horizontal = 20.dp))
                    }
                    item {
                        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (suggesting) {
                                TipSuggestionForm(onCancel = { suggesting = false }) { kind, status, summary, detail ->
                                    actions.suggestTip(spot.id, kind, status, summary, detail)
                                    suggesting = false
                                }
                            } else {
                                SecondaryButton("꿀팁 제보하기", { suggesting = true }, Modifier.fillMaxWidth(), Icons.Outlined.TaskAlt)
                            }
                            Text(
                                "차박·주차 허용 여부는 현장 안내문으로 다시 확인하세요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Palette.TextTertiary,
                            )
                        }
                    }
                }
                DetailTab.ITEMS -> {
                    item { Meta("방문자들이 이곳에 챙겨 간 물건이에요.") }
                    if (spot.products.isEmpty()) item { Empty("아직 등록된 준비물이 없어요.") }
                    items(spot.products, key = { it.id }) { product ->
                        ProductRow(product, saved = product.id in state.savedProducts, onSave = { actions.toggleProduct(product.id) }, modifier = Modifier.padding(horizontal = 20.dp))
                        Hairline(Modifier.padding(horizontal = 20.dp))
                    }
                }
                DetailTab.COMMENTS -> {
                    item { Meta("질문과 후기") }
                    if (spot.comments.isEmpty()) item { Empty("첫 질문이나 후기를 남겨 보세요.") }
                    items(spot.comments, key = { it.id }) { comment ->
                        CommentThread(comment, onReply = { replyTo = comment.id }, modifier = Modifier.padding(horizontal = 20.dp))
                        Hairline(Modifier.padding(horizontal = 20.dp))
                    }
                }
            }
        }
        if (tab == DetailTab.COMMENTS) {
            CommentComposer(
                replyingTo = spot.comments.firstOrNull { it.id == replyTo }?.author,
                onCancelReply = { replyTo = null },
                onSend = { text, question ->
                    actions.addComment(spot.id, text, question, replyTo)
                    replyTo = null
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Gallery(spot: Spot, onBack: () -> Unit) {
    val pager = rememberPagerState(pageCount = { spot.photos.size })
    Box(Modifier.fillMaxWidth().aspectRatio(0.9f)) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page -> SpotPhoto(spot.photos[page], "${spot.title} 사진 ${page + 1}", Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Palette.Background)))
        Box(
            Modifier
                .padding(16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Palette.Scrim)
                .clickable(role = Role.Button, onClickLabel = "뒤로", onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = Palette.TextPrimary)
        }
        if (spot.photos.size > 1) {
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(spot.photos.size) { index ->
                    Box(
                        Modifier
                            .size(width = 18.dp, height = 3.dp)
                            .clip(CircleShape)
                            .background(if (index == pager.currentPage) Palette.TextPrimary else Palette.TextPrimary.copy(alpha = 0.3f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(spot: Spot, state: PhoneUiState, actions: PhoneViewModel) {
    val liked = spot.id in state.liked
    val saved = spot.id in state.saved
    val visited = spot.id in state.visited
    val isMine = spot.author.id == state.me.id
    val following = spot.author.id in state.following
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${spot.province} · ${spot.category.label}", style = MaterialTheme.typography.labelLarge, color = Palette.TextSecondary)
            Text(spot.title, style = MaterialTheme.typography.headlineLarge, color = Palette.TextPrimary)
            Text(
                "${spot.address}${if (spot.approximateLocation) " (대략적인 위치)" else ""} · ${distanceFromHere(state.here, spot)}",
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = Palette.TextSecondary,
            )
            // The author's answer to "can the night be spent here?", with their reason when given.
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CarCampingBadge(spot.carCamping)
                if (spot.carCampingNote.isNotBlank()) {
                    Text(
                        spot.carCampingNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (spot.carCamping == CarCamping.BANNED) Palette.Alert else Palette.TextSecondary,
                    )
                }
            }
        }
        if (spot.story.isNotBlank()) Text("“${spot.story}”", style = MaterialTheme.typography.bodyLarge, color = Palette.TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(spot.author.name, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(spot.author.name, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
                Text(spot.author.bio, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
            }
            if (isMine) VisibilityTag(spot.visibility)
            else SecondaryButton(if (following) "팔로잉" else "팔로우", { actions.toggleFollow(spot.author.id) }, active = following)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(state.likeCount(spot).toString(), { actions.toggleLike(spot.id) }, Modifier.weight(1f), if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, active = liked)
            SecondaryButton(state.saveCount(spot).toString(), { actions.toggleSave(spot.id) }, Modifier.weight(1f), if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, active = saved)
            SecondaryButton(if (visited) "다녀왔어요" else "다녀왔다면", { actions.toggleVisited(spot.id) }, Modifier.weight(1.3f), Icons.Outlined.CheckCircle, active = visited)
        }
        val queued = spot.id in state.carQueue
        if (queued) {
            SecondaryButton("차량으로 보낼 목록에 있어요", { actions.removeFromCar(spot.id) }, Modifier.fillMaxWidth().heightIn(min = 52.dp), Icons.Outlined.DirectionsCar, active = true)
        } else {
            PrimaryButton("차량으로 보내기", { actions.sendToCar(spot.id) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.DirectionsCar)
        }
    }
}

@Composable
private fun TipRow(tip: HoneyTip, helpful: Boolean, onHelpful: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(tip.kind.icon(), contentDescription = null, tint = Palette.TextSecondary, modifier = Modifier.padding(top = 2.dp).size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tip.kind.label, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary, modifier = Modifier.weight(1f))
                StatusLabel(tip.status.label(), tip.status.color())
            }
            Text(tip.summary, style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary)
            if (tip.detail != tip.summary) Text(tip.detail, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
            if (tip.status == TipStatus.NEEDS_CHECK) {
                Text("방문자 정보가 서로 달라요. 최근 정보를 확인해 주세요.", style = MaterialTheme.typography.bodySmall, color = Palette.Alert)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${tip.reporter} · ${if (tip.confirmedDaysAgo == 0) "오늘" else "${tip.confirmedDaysAgo}일 전"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextTertiary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "도움이 됐어요 ${tip.helpfulCount + if (helpful) 1 else 0}",
                    style = MaterialTheme.typography.labelLarge.tabular(),
                    color = if (helpful) Palette.TextPrimary else Palette.TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (helpful) Palette.SurfaceHigh else Palette.Surface)
                        .clickable(role = Role.Button, onClick = onHelpful)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ProductRow(product: Product, saved: Boolean, onSave: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SpotPhoto(product.imageUrl, null, Modifier.size(88.dp).clip(RoundedCornerShape(8.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary, modifier = Modifier.weight(1f, fill = false))
                    if (product.isSponsored) Tag("광고·제휴", Palette.Warning)
                }
                Text(product.reason, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                Text(
                    (product.priceWon?.let { NumberFormat.getNumberInstance(Locale.KOREA).format(it) + "원" } ?: "가격 정보 없음") +
                        " · ${product.recommender} 추천",
                    style = MaterialTheme.typography.labelLarge.tabular(),
                    color = Palette.TextPrimary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(if (saved) "관심 상품" else "관심 상품 담기", onSave, Modifier.weight(1f), if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, active = saved)
            SecondaryButton("쇼핑 검색", {
                val query = URLEncoder.encode(product.name, "UTF-8")
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://search.shopping.naver.com/search/all?query=$query")))
            }, Modifier.weight(1f), Icons.Outlined.OpenInNew)
        }
    }
}

@Composable
private fun CommentThread(comment: Comment, onReply: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CommentLine(comment)
        comment.replies.forEach { reply ->
            Row {
                Spacer(Modifier.width(42.dp))
                CommentLine(reply)
            }
        }
        Text(
            "답글 달기",
            style = MaterialTheme.typography.labelLarge,
            color = Palette.TextSecondary,
            modifier = Modifier
                .padding(start = 42.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onReply)
                .padding(vertical = 4.dp, horizontal = 2.dp),
        )
    }
}

@Composable
private fun CommentLine(comment: Comment) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Avatar(comment.author, size = 32.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(comment.author, style = MaterialTheme.typography.labelLarge, color = Palette.TextPrimary, fontWeight = FontWeight.SemiBold)
                if (comment.isQuestion) Tag("질문", Palette.TextSecondary)
                Text(comment.postedAgo, style = MaterialTheme.typography.bodySmall, color = Palette.TextTertiary)
            }
            Text(comment.text, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
        }
    }
}

@Composable
private fun CommentComposer(replyingTo: String?, onCancelReply: () -> Unit, onSend: (String, Boolean) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var question by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Palette.Background)
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (replyingTo != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$replyingTo 님에게 답글", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.Close, "답글 취소", tint = Palette.TextSecondary, modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onCancelReply).padding(6.dp))
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("후기", !question, { question = false })
                Chip("질문", question, { question = true }, icon = Icons.Outlined.HelpOutline)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Palette.Surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (text.isEmpty()) {
                    Text(
                        if (question && replyingTo == null) "이 장소에 대해 궁금한 점을 물어보세요" else "따뜻한 이야기를 남겨 주세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.TextTertiary,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Palette.TextPrimary),
                    cursorBrush = SolidColor(Palette.TextPrimary),
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (text.isBlank()) Palette.Surface else Palette.TextPrimary)
                    .clickable(enabled = text.isNotBlank(), role = Role.Button, onClickLabel = "보내기") {
                        onSend(text, question && replyingTo == null)
                        text = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "보내기", tint = if (text.isBlank()) Palette.TextTertiary else Palette.Background)
            }
        }
    }
}

@Composable
private fun Meta(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextSecondary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 2.dp),
    )
}

@Composable
private fun Empty(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary, modifier = Modifier.padding(horizontal = 20.dp))
}
