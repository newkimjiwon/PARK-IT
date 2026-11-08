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
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import com.nangmanroad.vehicle.PhoneHandoff
import com.nangmanroad.core.data.CarCamping
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.ui.CarCampingBadge
import com.nangmanroad.vehicle.ui.components.ActionButton
import com.nangmanroad.core.ui.Avatar
import com.nangmanroad.vehicle.ui.components.Hairline
import com.nangmanroad.vehicle.ui.components.TextTab
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.vehicle.ui.map.distanceLabel
import com.nangmanroad.vehicle.ui.theme.Dimens
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.tabular

private enum class DetailTab(val label: String) { TIPS("방문 꿀팁"), ITEMS("준비물"), COMMENTS("댓글") }

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
                    contentPadding = PaddingValues(horizontal = 40.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
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
    item { RouteRow(spot, state, actions, pad) }
    item { AuthorRow(spot, state, actions, pad) }
    item {
        Column(pad) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextTab(DetailTab.TIPS.label, tab == DetailTab.TIPS, { onTab(DetailTab.TIPS) }, count = spot.tips.size)
                TextTab(DetailTab.ITEMS.label, tab == DetailTab.ITEMS, { onTab(DetailTab.ITEMS) }, count = spot.products.size)
                TextTab(DetailTab.COMMENTS.label, tab == DetailTab.COMMENTS, { onTab(DetailTab.COMMENTS) }, count = spot.commentCount)
            }
            Hairline()
        }
    }
    when (tab) {
        DetailTab.TIPS -> item { HoneyTipSection(spot, state.helpfulTips, actions::toggleHelpful, onSuggest = { actions.sendToPhone(PhoneHandoff.TIP, spot) }, modifier = pad) }
        DetailTab.ITEMS -> item { ProductSection(spot, onOpenOnPhone = { product -> actions.sendToPhone(PhoneHandoff.PRODUCT, spot, product) }, modifier = pad) }
        DetailTab.COMMENTS -> item { CommentSection(spot, onReplyOnPhone = { actions.sendToPhone(PhoneHandoff.COMMENTS, spot) }, modifier = pad) }
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
                .clip(RoundedCornerShape(Dimens.ControlRadius))
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
            style = MaterialTheme.typography.labelLarge.tabular(),
            color = Palette.TextPrimary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .clip(RoundedCornerShape(Dimens.ControlRadius))
                .background(Palette.Scrim)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${spot.province} · ${spot.category.label}", style = MaterialTheme.typography.labelLarge, color = Palette.TextSecondary)
            Text(spot.title, style = MaterialTheme.typography.displaySmall, color = Palette.TextPrimary)
            Text("“${spot.story}”", style = MaterialTheme.typography.bodyLarge, color = Palette.TextPrimary.copy(alpha = 0.86f))
            if (spot.photos.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(spot.photos.size) { index ->
                        Box(
                            Modifier
                                .size(width = 28.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(if (index == pager.currentPage) Palette.TextPrimary else Palette.TextPrimary.copy(alpha = 0.3f)),
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
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatar(spot.author.name, size = 56.dp)
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(spot.author.name, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
            Text(spot.author.bio, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        ActionButton(if (following) "팔로잉" else "팔로우", null, { actions.toggleFollow(spot.author.id) }, active = following)
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
        ActionButton("휴대폰", Icons.Outlined.PhoneAndroid, { actions.sendToPhone(PhoneHandoff.SPOT, spot) })
    }
}

@Composable
private fun RouteRow(spot: Spot, state: AppUiState, actions: AppViewModel, modifier: Modifier) {
    val routing = state.routedSpotId == spot.id && state.destination != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(spot.address, style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary)
                CarCampingBadge(spot.carCamping, iconSize = 20.dp)
            }
            Text(distanceLabel(state, spot), style = MaterialTheme.typography.bodyMedium.tabular(), color = Palette.TextSecondary)
            if (spot.bestTime.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Schedule, contentDescription = "가기 좋은 때", tint = Palette.TextSecondary, modifier = Modifier.size(20.dp))
                    Text(spot.bestTime, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
                }
            }
            if (spot.carCampingNote.isNotBlank()) {
                Text(
                    spot.carCampingNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (spot.carCamping == CarCamping.BANNED) Palette.Alert else Palette.TextSecondary,
                )
            }
        }
        ActionButton(
            text = if (routing) "안내 중" else "길 안내",
            icon = Icons.Outlined.Directions,
            onClick = { actions.startRoute(spot.id) },
            primary = !routing,
            active = routing,
            modifier = Modifier.width(240.dp),
        )
    }
}
