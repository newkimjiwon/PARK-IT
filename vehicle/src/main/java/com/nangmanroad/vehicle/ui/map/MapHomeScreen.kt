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
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.map.PhotoMap
import com.nangmanroad.vehicle.AppUiState
import com.nangmanroad.vehicle.AppViewModel
import com.nangmanroad.vehicle.ListenPurpose
import com.nangmanroad.vehicle.ParkitSuggestion
import com.nangmanroad.vehicle.VoiceUi
import com.nangmanroad.core.data.CarCamping
import com.nangmanroad.core.data.carCampingCheckedLabel
import com.nangmanroad.core.data.carCampingStale
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.ui.CarCampingBadge
import com.nangmanroad.core.data.FeedSort
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.SpotQueries
import com.nangmanroad.core.data.at
import com.nangmanroad.core.data.formatClock
import com.nangmanroad.core.data.sunMoment
import com.nangmanroad.core.data.sunTimesToday
import com.nangmanroad.core.data.distanceKm
import com.nangmanroad.core.data.goableNow
import com.nangmanroad.core.data.reach
import com.nangmanroad.core.ui.color
import com.nangmanroad.core.data.formatKm
import com.nangmanroad.vehicle.ui.components.ActionButton
import com.nangmanroad.vehicle.ui.components.EmptyState
import com.nangmanroad.vehicle.ui.components.Hairline
import com.nangmanroad.vehicle.ui.components.TextTab
import com.nangmanroad.core.ui.ToggleChip
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.vehicle.ui.feed.SpotCard
import com.nangmanroad.vehicle.ui.theme.Dimens
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.tabular

@Composable
fun MapHomeScreen(state: AppUiState, actions: AppViewModel) {
    val matching = SpotQueries.matching(state.spots, state.category, state.query)
        .let { if (state.onlyRoundTrip) it.goableNow(state.here, state.usableRangeKm) else it }
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
                    SearchField(query = state.query, onQueryChange = actions::setQuery, voice = state.voice, onMic = actions::listenForSearch)
                    CategoryFilters(selected = state.category, onSelect = actions::setCategory)
                    // One question while parked: where can we actually go tonight? Facility chips live on the phone.
                    Row(Modifier.padding(bottom = 8.dp)) {
                        ToggleChip(
                            icon = Icons.Outlined.CheckCircle,
                            label = if (state.usableRangeKm != null) "지금 갈 수 있는 곳만 · 배터리 ${state.battery!!.percent}%" else "지금 갈 수 있는 곳만",
                            on = state.onlyRoundTrip,
                            onToggle = actions::toggleOnlyRoundTrip,
                            onClickLabel = "배터리로 왕복 가능하고 차박이 금지되지 않은 곳만",
                        )
                    }
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
private fun SearchField(query: String, onQueryChange: (String) -> Unit, voice: VoiceUi, onMic: () -> Unit) {
    val focus = LocalFocusManager.current
    val listening = voice.listening == ListenPurpose.SEARCH
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
            if (listening) {
                // While the microphone is open the field shows what it hears instead of the typed text.
                Text(
                    voice.heard.ifBlank { "듣고 있어요. '태안 노을'처럼 말해 보세요" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (voice.heard.isBlank()) Palette.TextSecondary else Palette.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (query.isEmpty()) {
                Text(
                    if (voice.canListen) "장소, 지역, 작성자 검색 · 말로도 찾아요" else "장소, 지역, 작성자 검색",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Palette.TextTertiary,
                )
            }
            if (!listening) BasicTextField(
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
        if (query.isNotEmpty() && !listening) {
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
        if (voice.canListen) {
            Icon(
                Icons.Outlined.Mic,
                contentDescription = if (listening) "말하기 끝내기" else "말로 검색",
                tint = Palette.TextPrimary,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(Dimens.ControlRadius))
                    .background(if (listening) Palette.SurfaceHigh else Palette.Surface)
                    .clickable(role = Role.Button, onClick = onMic)
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
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Palette.BackgroundRaised.copy(alpha = 0.97f))
            .border(1.dp, Palette.Rule, shape)
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
                .clip(RoundedCornerShape(4.dp)),
        )
        // Three lines, readable in a glance from the driver's seat: what, may we, and how far/when.
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(spot.title, style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CarCampingBadge(spot.carCamping, iconSize = 20.dp, stale = spot.carCampingStale)
                state.usableRangeKm?.let { range ->
                    val verdict = reach(distanceKm(state.here, spot.location), range)
                    Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null, tint = verdict.color(), modifier = Modifier.size(20.dp))
                    Text(verdict.label, style = MaterialTheme.typography.bodyMedium.tabular(), color = verdict.color(), maxLines = 1, softWrap = false)
                }
                spot.carCampingCheckedDaysAgo?.let {
                    Text(
                        carCampingCheckedLabel(it),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (spot.carCampingStale) Palette.Warning else Palette.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.NearMe, contentDescription = null, tint = Palette.TextSecondary, modifier = Modifier.size(20.dp))
                val sun = spot.category.sunMoment?.let { moment -> sunTimesToday(spot.location)?.let { "${moment.label} ${formatClock(it.at(moment))}" } }
                Text(
                    listOfNotNull(distanceLabel(state, spot), spot.region, sun).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                    color = Palette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
            note = shown?.note.orEmpty(),
            voice = state.voice,
            onNote = actions::listenForParkitNote,
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
 * one is marked visited so the phone can ask whether its tips held up. The accent here marks the one
 * action the moment is about, as 길 안내 does on the preview card.
 */
@Composable
private fun ParkitBanner(
    nearSpot: Spot?,
    note: String,
    voice: VoiceUi,
    onNote: () -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listening = voice.listening == ListenPurpose.PARKIT_NOTE
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Palette.BackgroundRaised.copy(alpha = 0.97f))
            .border(1.dp, Palette.Accent, shape)
            .padding(start = 24.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(Icons.Outlined.PushPin, contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(44.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (nearSpot != null) "'${nearSpot.title}'에 왔어요" else "여기, 파킷할까요?",
                style = MaterialTheme.typography.titleLarge,
                color = Palette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            when {
                // A spoken line goes with the place, so the post on the phone starts with the moment itself.
                listening -> Text(
                    voice.heard.ifBlank { "듣고 있어요. 이곳에 대해 한마디 남겨 주세요" }.let { if (voice.heard.isBlank()) it else "“$it”" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                note.isNotBlank() -> Text(
                    "메모 “$note”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                else -> Text(
                    if (nearSpot != null) "방문 완료로 표시하고, 꿀팁이 맞았는지 휴대폰에서 알려줘요" else "방금 세운 곳을 휴대폰으로 보내 사진과 꿀팁을 더해요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (voice.canListen) {
            ActionButton(
                text = when {
                    listening -> "듣는 중"
                    note.isNotBlank() -> "다시 말하기"
                    else -> "말로 메모"
                },
                icon = Icons.Outlined.Mic,
                onClick = onNote,
                active = listening,
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
                onOpen = { actions.openSpot(spot.id) },
                onToggleSave = { actions.toggleSave(spot.id) },
            )
        }
    }
}
