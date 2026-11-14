package com.nangmanroad.vehicle.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.rounded.FavoriteBorder
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
import com.nangmanroad.core.data.rangeLabel
import com.nangmanroad.core.data.formatKm
import com.nangmanroad.core.data.Reach
import com.nangmanroad.core.data.Regions
import com.nangmanroad.core.data.carCampingCheckedLabel
import com.nangmanroad.core.data.carCampingStale
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.ui.CarCampingBadge
import com.nangmanroad.core.ui.color
import com.nangmanroad.vehicle.ui.components.ActionButton
import com.nangmanroad.core.ui.Avatar
import com.nangmanroad.vehicle.ui.components.Hairline
import com.nangmanroad.vehicle.ui.components.TextTab
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.vehicle.ui.map.distanceLabel
import com.nangmanroad.vehicle.ui.theme.Dimens
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.OfficialPlaceSection
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
                item { PhotoGallery(spot, onBack = actions::back, modifier = Modifier.fillMaxWidth().height(900.dp)) }
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
    spot.officialPlace?.let { info -> item { OfficialPlaceSection(info, pad) } }
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
    // A square photo on top and the caption set beneath it, like a plate in a printed guide.
    Column(modifier) {
        Box(Modifier.fillMaxWidth().weight(1f, fill = false).aspectRatio(1f)) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                SpotPhoto(spot.photos[page], contentDescription = "${spot.title} 사진 ${page + 1}", modifier = Modifier.fillMaxSize())
            }
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
            if (spot.photos.size > 1) {
                Row(
                    Modifier.align(Alignment.BottomStart).padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(spot.photos.size) { index ->
                        Box(
                            Modifier
                                .size(width = 28.dp, height = 4.dp)
                                .background(if (index == pager.currentPage) Palette.Mist else Palette.Mist.copy(alpha = 0.4f)),
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${spot.province} · ${spot.category.label}", style = MaterialTheme.typography.labelLarge, color = Palette.TextSecondary)
            Text(spot.title, style = MaterialTheme.typography.displaySmall, color = Palette.TextPrimary)
            Text("“${spot.story}”", style = MaterialTheme.typography.bodyLarge, color = Palette.TextSecondary)
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
            if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            { actions.toggleLike(spot.id) },
            active = liked,
        )
        ActionButton(
            state.saveCount(spot).toString(),
            if (saved) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            { actions.toggleSave(spot.id) },
            active = saved,
        )
        ActionButton("휴대폰으로 보내기", Icons.Outlined.PhoneAndroid, { actions.sendToPhone(PhoneHandoff.SPOT, spot) })
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
                CarCampingBadge(spot.carCamping, iconSize = 20.dp, stale = spot.carCampingStale)
                spot.carCampingCheckedDaysAgo?.let {
                    Text(carCampingCheckedLabel(it), style = MaterialTheme.typography.labelMedium, color = if (spot.carCampingStale) Palette.Warning else Palette.TextTertiary, maxLines = 1, softWrap = false)
                }
            }
            Text(distanceLabel(state, spot), style = MaterialTheme.typography.bodyMedium.tabular(), color = Palette.TextSecondary)
            val range = state.usableRangeKm
            val verdict = state.tripReach(spot)
            if (range != null && verdict != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null, tint = verdict.color(), modifier = Modifier.size(20.dp))
                    Text(
                        "${verdict.label} · ${rangeLabel(state.battery!!.percent, range)}",
                        style = MaterialTheme.typography.bodyMedium.tabular(),
                        color = verdict.color(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val distanceBasis = state.remainingRouteKm(spot)
                    ?.let { "남은 경로 ${formatKm(it)} 기준 추정" }
                    ?: "직선거리를 보정한 추정치"
                Text(distanceBasis, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                if (verdict != Reach.ROUND_TRIP) {
                    val region = if (verdict == Reach.SHORT) Regions.nearest(state.here).address else spot.region
                    val charger = when {
                        "중랑" in region -> "중랑캠핑숲 2주차장 충전소"
                        "마포" in region -> "서울월드컵경기장 충전소"
                        "가평" in region -> "가평휴게소 E-pit · 진행 방향 확인"
                        else -> null
                    }
                    if (charger != null) {
                        val area = if (verdict == Reach.SHORT) "출발지 주변" else "목적지 주변"
                        Text("$area · $charger", style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
                        Text("충전기 이용 가능 여부는 출발 전에 확인하세요.", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                    }
                }
            } else {
                Text("배터리 또는 현재 위치를 확인하면 이동 가능 여부를 표시합니다.", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
            }
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
