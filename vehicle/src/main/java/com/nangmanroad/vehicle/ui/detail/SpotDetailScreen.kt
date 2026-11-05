package com.nangmanroad.vehicle.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhoneAndroid
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nangmanroad.vehicle.AppUiState
import com.nangmanroad.vehicle.AppViewModel
import com.nangmanroad.vehicle.data.Spot
import com.nangmanroad.vehicle.ui.components.ActionButton
import com.nangmanroad.vehicle.ui.components.Avatar
import com.nangmanroad.vehicle.ui.components.Pill
import com.nangmanroad.vehicle.ui.components.SpotPhoto
import com.nangmanroad.vehicle.ui.map.distanceLabel
import com.nangmanroad.vehicle.ui.theme.Dimens
import com.nangmanroad.vehicle.ui.theme.Palette

private enum class DetailTab(val label: String) { TIPS("방문 꿀팁"), ITEMS("이 낭만을 만든 아이템"), COMMENTS("댓글") }

@Composable
fun SpotDetailScreen(spot: Spot, state: AppUiState, actions: AppViewModel) {
    var tab by rememberSaveable(spot.id) { mutableStateOf(DetailTab.TIPS) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1100.dp
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                PhotoGallery(spot, onBack = actions::back, modifier = Modifier.weight(0.46f).fillMaxHeight())
                LazyColumn(
                    modifier = Modifier.weight(0.54f).fillMaxHeight(),
                    contentPadding = PaddingValues(horizontal = 40.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    detailBody(spot, state, actions, tab) { tab = it }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item { PhotoGallery(spot, onBack = actions::back, modifier = Modifier.fillMaxWidth().height(560.dp)) }
                detailBody(spot, state, actions, tab, horizontalPadding = 28.dp) { tab = it }
            }
        }
    }
}

private fun LazyListScope.detailBody(
    spot: Spot,
    state: AppUiState,
    actions: AppViewModel,
    tab: DetailTab,
    horizontalPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onTab: (DetailTab) -> Unit,
) {
    val pad = Modifier.padding(horizontal = horizontalPadding)
    item { AuthorRow(spot, state, actions, pad) }
    item { RouteRow(spot, state, actions, pad) }
    item {
        Row(pad, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill(DetailTab.TIPS.label, tab == DetailTab.TIPS, { onTab(DetailTab.TIPS) }, trailing = spot.tips.size.toString())
            Pill(DetailTab.ITEMS.label, tab == DetailTab.ITEMS, { onTab(DetailTab.ITEMS) }, trailing = spot.products.size.toString())
            Pill(DetailTab.COMMENTS.label, tab == DetailTab.COMMENTS, { onTab(DetailTab.COMMENTS) }, trailing = spot.commentCount.toString())
        }
    }
    when (tab) {
        DetailTab.TIPS -> item { HoneyTipSection(spot, state.helpfulTips, actions::toggleHelpful, onSuggest = { actions.sendToPhone("tip:${spot.id}", "꿀팁 제보 화면을") }, modifier = pad) }
        DetailTab.ITEMS -> item { ProductSection(spot, onOpenOnPhone = { product -> actions.sendToPhone("product:${product.id}", "상품 링크를") }, modifier = pad) }
        DetailTab.COMMENTS -> item { CommentSection(spot, onReplyOnPhone = { actions.sendToPhone("comments:${spot.id}", "댓글을") }, modifier = pad) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGallery(spot: Spot, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState(pageCount = { spot.photos.size })
    Box(modifier.background(Palette.Background)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            SpotPhoto(spot.photos[page], contentDescription = "${spot.title} 사진 ${page + 1}", modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Palette.Background.copy(alpha = 0.92f))),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .heightIn(min = Dimens.TouchTarget)
                .clip(RoundedCornerShape(20.dp))
                .background(Palette.Scrim)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = Palette.TextPrimary, modifier = Modifier.size(30.dp))
            Text("돌아가기", style = MaterialTheme.typography.labelLarge, color = Palette.TextPrimary)
        }
        Text(
            "${pager.currentPage + 1} / ${spot.photos.size}",
            style = MaterialTheme.typography.labelLarge,
            color = Palette.TextPrimary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Palette.Scrim)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${spot.province} · ${spot.category.label}", style = MaterialTheme.typography.labelLarge, color = Palette.Peach)
            Text(spot.title, style = MaterialTheme.typography.displaySmall, color = Palette.TextPrimary)
            Text("“${spot.story}”", style = MaterialTheme.typography.bodyLarge, color = Palette.TextPrimary.copy(alpha = 0.86f))
            if (spot.photos.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(spot.photos.size) { index ->
                        Box(
                            Modifier
                                .size(width = if (index == pager.currentPage) 28.dp else 10.dp, height = 10.dp)
                                .clip(CircleShape)
                                .background(if (index == pager.currentPage) Palette.Coral else Palette.TextPrimary.copy(alpha = 0.4f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorRow(spot: Spot, state: AppUiState, actions: AppViewModel, modifier: Modifier) {
    val following = spot.author.id in state.following
    val liked = spot.id in state.liked
    val saved = spot.id in state.saved
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Avatar(spot.author.name, size = 64.dp)
        Column(Modifier.weight(1f)) {
            Text(spot.author.name, style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary)
            Text(spot.author.bio, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        ActionButton(if (following) "팔로잉" else "팔로우", null, { actions.toggleFollow(spot.author.id) }, active = following, primary = !following)
        ActionButton(
            state.likeCount(spot).toString(),
            if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            { actions.toggleLike(spot.id) },
            active = liked,
        )
        ActionButton(
            state.saveCount(spot).toString(),
            if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            { actions.toggleSave(spot.id) },
            active = saved,
        )
        ActionButton("휴대폰", Icons.Outlined.PhoneAndroid, { actions.sendToPhone("spot:${spot.id}", "이 장소를") })
    }
}

@Composable
private fun RouteRow(spot: Spot, state: AppUiState, actions: AppViewModel, modifier: Modifier) {
    val routing = state.routedSpotId == spot.id && state.destination != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.CardRadius))
            .background(Palette.Surface)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Map, contentDescription = null, tint = Palette.Peach, modifier = Modifier.size(30.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(spot.address, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
            Text(distanceLabel(state, spot), style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
        }
        ActionButton(
            text = if (routing) "안내 중" else "길 안내 시작",
            icon = Icons.Outlined.Directions,
            onClick = { actions.startRoute(spot.id) },
            primary = !routing,
            active = routing,
            modifier = Modifier.width(240.dp),
        )
    }
}

internal fun Modifier.card(): Modifier = this
    .clip(RoundedCornerShape(Dimens.CardRadius))
    .background(Palette.Surface)

@Composable
internal fun TwoColumnGrid(count: Int, cell: @Composable (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        for (row in 0 until (count + 1) / 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.weight(1f)) { cell(row * 2) }
                Box(Modifier.weight(1f)) { if (row * 2 + 1 < count) cell(row * 2 + 1) else Spacer(Modifier.width(0.dp)) }
            }
        }
    }
}
