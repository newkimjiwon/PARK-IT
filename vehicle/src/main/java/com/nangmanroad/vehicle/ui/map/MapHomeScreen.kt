package com.nangmanroad.vehicle.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.map.PhotoMap
import com.nangmanroad.vehicle.AppUiState
import com.nangmanroad.vehicle.AppViewModel
import com.nangmanroad.vehicle.ParkitSuggestion
import com.nangmanroad.core.data.CarCamping
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.ui.CarCampingBadge
import com.nangmanroad.core.data.FeedSort
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.SpotQueries
import com.nangmanroad.core.data.SunMoment
import com.nangmanroad.core.data.at
import com.nangmanroad.core.data.formatClock
import com.nangmanroad.core.data.sunMoment
import com.nangmanroad.core.data.sunTimesToday
import com.nangmanroad.core.data.distanceKm
import com.nangmanroad.core.data.formatKm
import com.nangmanroad.vehicle.ui.components.ActionButton
import com.nangmanroad.vehicle.ui.components.EmptyState
import com.nangmanroad.vehicle.ui.components.Hairline
import com.nangmanroad.vehicle.ui.components.TextTab
import com.nangmanroad.core.ui.FacilityFilterRow
import com.nangmanroad.core.ui.FacilityStrip
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.vehicle.ui.feed.SpotCard
import com.nangmanroad.vehicle.ui.feed.Stat
import com.nangmanroad.vehicle.ui.theme.Dimens
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.tabular

@Composable
fun MapHomeScreen(state: AppUiState, actions: AppViewModel) {
    val matching = SpotQueries.matching(state.spots, state.category, state.query, state.facilities)
    val selected = state.spot(state.selectedSpotId)?.takeIf { it in matching }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1100.dp
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(if (wide) 0.62f else 1f)
                    .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Dimens.Gutter, top = Dimens.Gutter, end = Dimens.Gutter),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SearchField(query = state.query, onQueryChange = actions::setQuery)
                    CategoryFilters(selected = state.category, onSelect = actions::setCategory)
                    FacilityFilterRow(selected = state.facilities, onToggle = actions::toggleFacility, modifier = Modifier.padding(bottom = 8.dp))
                }
                Hairline()
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    PhotoMap(
                        spots = matching,
                        selectedId = selected?.id,
                        here = state.here,
                        onSelect = { actions.selectSpot(it.id) },
                        onVisibleChange = actions::onVisibleSpotsChanged,
                        herePulse = state.parkitPulse,
                        // A red ring warns off overnight stays before the card is even read.
                        markerTint = { if (it.carCamping == CarCamping.BANNED) Palette.Alert else null },
                        modifier = Modifier.fillMaxSize(),
                    )
                    ParkitBannerHost(state, actions, Modifier.align(Alignment.TopStart))
                    if (selected != null) {
                        PreviewCard(
                            spot = selected,
                            state = state,
                            onOpen = { actions.openSpot(selected.id) },
                            onToggleSave = { actions.toggleSave(selected.id) },
                            onRoute = { actions.startRoute(selected.id) },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(Dimens.Gutter)
                                .widthIn(max = 820.dp),
                        )
                    }
                }
            }
            if (wide) {
                FeedPanel(
                    state = state,
                    spots = SpotQueries.feed(matching, state.visibleSpotIds, state.feedSort, state.following),
                    actions = actions,
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                        .background(Palette.BackgroundRaised),
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.TouchTarget)
            .clip(RoundedCornerShape(Dimens.ControlRadius))
            .background(Palette.Surface)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = Palette.TextSecondary, modifier = Modifier.size(28.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("장소, 지역, 작성자 검색", style = MaterialTheme.typography.bodyLarge, color = Palette.TextTertiary)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Palette.TextPrimary),
                cursorBrush = SolidColor(Palette.TextPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "검색어 지우기",
                tint = Palette.TextSecondary,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(Dimens.ControlRadius))
                    .clickable(role = Role.Button) { onQueryChange("") }
                    .padding(10.dp),
            )
        }
    }
}

@Composable
private fun CategoryFilters(selected: Category?, onSelect: (Category?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TextTab("전체", selected == null, onClick = { onSelect(null) })
        Category.entries.forEach { category ->
            TextTab(category.label, selected == category, onClick = { onSelect(if (selected == category) null else category) })
        }
    }
}

@Composable
private fun PreviewCard(
    spot: Spot,
    state: AppUiState,
    onOpen: () -> Unit,
    onToggleSave: () -> Unit,
    onRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val saved = spot.id in state.saved
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(Palette.BackgroundRaised.copy(alpha = 0.97f))
            .border(1.dp, Palette.Hairline, shape)
            .clickable(role = Role.Button, onClickLabel = "자세히 보기", onClick = onOpen)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SpotPhoto(
            url = spot.photos.first(),
            contentDescription = spot.title,
            modifier = Modifier
                .size(152.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${spot.category.label} · ${spot.region}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Palette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                CarCampingBadge(spot.carCamping, iconSize = 20.dp)
            }
            Text(spot.title, style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.NearMe, contentDescription = null, tint = Palette.Coral, modifier = Modifier.size(20.dp))
                Text(
                    distanceLabel(state, spot),
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                    color = Palette.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // "When to go" is what separates a scenic spot from a pin: the author's own words, if any.
            if (spot.bestTime.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Schedule, contentDescription = "가기 좋은 때", tint = Palette.TextSecondary, modifier = Modifier.size(20.dp))
                    Text(spot.bestTime, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stat(Icons.Outlined.FavoriteBorder, state.likeCount(spot).toString())
                Stat(Icons.Outlined.TipsAndUpdates, "꿀팁 ${spot.tips.size}")
                // A sunset place is only worth the drive before the sun goes: say when that is.
                spot.category.sunMoment?.let { moment ->
                    sunTimesToday(spot.location)?.let { sun ->
                        Stat(if (moment == SunMoment.SUNSET) Icons.Outlined.WbTwilight else Icons.Outlined.WbSunny, "${moment.label} ${formatClock(sun.at(moment))}")
                    }
                }
                FacilityStrip(spot.tips, iconSize = 24.dp)
            }
        }
        Column(Modifier.width(280.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("길 안내", Icons.Outlined.Directions, onRoute, primary = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton(
                    text = if (saved) "저장됨" else "저장",
                    icon = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    onClick = onToggleSave,
                    active = saved,
                    modifier = Modifier.weight(1f),
                )
                ActionButton("자세히", Icons.AutoMirrored.Outlined.ArrowForward, onOpen, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Slides the banner in from the top and keeps the last suggestion while it slides away again. */
@Composable
private fun ParkitBannerHost(state: AppUiState, actions: AppViewModel, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(state.parkit) }
    if (state.parkit != null) shown = state.parkit
    AnimatedVisibility(
        visible = state.parkit != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        ParkitBanner(
            nearSpot = state.spot(shown?.nearSpotId),
            onSend = actions::sendParkit,
            onDismiss = actions::dismissParkit,
            modifier = Modifier
                .padding(Dimens.Gutter)
                .widthIn(max = 980.dp),
        )
    }
}

/**
 * Appears when the car has just stopped. A new place goes to the phone to become a post; a known
 * one is marked visited so the phone can ask whether its tips held up. Coral here marks the one
 * action the moment is about, as 길 안내 does on the preview card.
 */
@Composable
private fun ParkitBanner(nearSpot: Spot?, onSend: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(Palette.BackgroundRaised.copy(alpha = 0.97f))
            .border(1.dp, Palette.Coral.copy(alpha = 0.35f), shape)
            .padding(start = 24.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(Icons.Outlined.PushPin, contentDescription = null, tint = Palette.Coral, modifier = Modifier.size(44.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (nearSpot != null) "'${nearSpot.title}'에 왔어요" else "여기, 파킷할까요?",
                style = MaterialTheme.typography.titleLarge,
                color = Palette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (nearSpot != null) "방문 완료로 표시하고, 꿀팁이 맞았는지 휴대폰에서 알려줘요" else "방금 세운 곳을 휴대폰으로 보내 사진과 꿀팁을 더해요",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ActionButton("파킷하기", Icons.Outlined.PushPin, onSend, primary = true)
        ActionButton("닫기", Icons.Outlined.Close, onDismiss)
    }
}

fun distanceLabel(state: AppUiState, spot: Spot): String {
    val km = formatKm(distanceKm(state.here, spot.location))
    return if (state.hereFromNavigation) "내 위치에서 $km" else "서울시청에서 직선 $km"
}

@Composable
private fun FeedPanel(state: AppUiState, spots: List<Spot>, actions: AppViewModel, modifier: Modifier = Modifier) {
    val visibleIds = state.visibleSpotIds
    val inView = if (visibleIds == null) state.spots else state.spots.filter { it.id in visibleIds }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(Dimens.Gutter),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalItemSpacing = 16.dp,
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Column {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(SpotQueries.regionTitle(inView), style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
                    Text(
                        "${spots.size}곳",
                        style = MaterialTheme.typography.titleSmall.tabular(),
                        color = Palette.TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FeedSort.entries.forEach { sort ->
                        TextTab(sort.label, state.feedSort == sort, onClick = { actions.setFeedSort(sort) })
                    }
                }
                Hairline()
            }
        }
        if (spots.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                EmptyState(
                    icon = Icons.Outlined.PhotoCamera,
                    title = if (state.feedSort == FeedSort.FOLLOWING) "팔로잉한 사람의 사진이 없어요" else "이 지역의 사진이 아직 없어요",
                    body = "지도를 옮기거나 필터를 바꿔 보세요.",
                )
            }
        }
        items(spots, key = { it.id }) { spot ->
            SpotCard(
                spot = spot,
                likeCount = state.likeCount(spot),
                saved = spot.id in state.saved,
                photoAspect = if (spot.id.hashCode() % 2 == 0) 0.8f else 1.05f,
                onOpen = { actions.openSpot(spot.id) },
                onToggleSave = { actions.toggleSave(spot.id) },
            )
        }
    }
}
