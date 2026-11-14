package com.nangmanroad.vehicle.ui.map

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.map.MapCamera
import com.nangmanroad.core.map.MapMarkerMode
import com.nangmanroad.core.map.PhotoMap
import com.nangmanroad.core.map.PhotoMapStyle
import com.nangmanroad.vehicle.AppUiState
import com.nangmanroad.vehicle.AppViewModel
import com.nangmanroad.vehicle.ListenPurpose
import com.nangmanroad.vehicle.VoiceUi
import com.nangmanroad.vehicle.pleos.DrivingState
import com.nangmanroad.core.data.CarCamping
import com.nangmanroad.core.data.PlaceQuestion
import com.nangmanroad.core.data.answerAboutPlace
import com.nangmanroad.core.data.carCampingCheckedLabel
import com.nangmanroad.core.data.carCampingStale
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.ui.CarCampingBadge
import com.nangmanroad.core.data.FeedSort
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.SpotQueries
import com.nangmanroad.core.data.formatDuration
import com.nangmanroad.core.data.distanceKm
import com.nangmanroad.core.data.Reach
import com.nangmanroad.core.ui.color
import com.nangmanroad.core.data.formatKm
import com.nangmanroad.vehicle.ui.components.ActionButton
import com.nangmanroad.vehicle.ui.components.EmptyState
import com.nangmanroad.vehicle.ui.components.Hairline
import com.nangmanroad.vehicle.ui.components.TextTab
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.vehicle.ui.feed.SpotCard
import com.nangmanroad.vehicle.ui.theme.Dimens
import com.nangmanroad.core.ui.ParkItMark
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.tabular

// Keep the overview inside the NAVER navigation map's coverage on the head unit.
private val VehicleMapStyle = PhotoMapStyle(
    initialCamera = MapCamera(LatLng(36.15, 128.0)),
    naverMinZoom = 6.7,
    naverOverviewZoom = 6.85,
)

private enum class MapFeedFilter(val label: String, val emptyTitle: String) {
    ALL("전체", "이 지역의 사진이 아직 없어요"),
    LIKED("좋아요", "이 지도 범위에 좋아요 한 곳이 없어요"),
    SAVED("저장", "이 지도 범위에 저장한 곳이 없어요"),
    MINE("내 파킷", "이 지도 범위에 내가 올린 곳이 없어요"),
}

@Composable
fun MapHomeScreen(state: AppUiState, actions: AppViewModel) {
    var filter by rememberSaveable { mutableStateOf(MapFeedFilter.ALL) }
    val matching = SpotQueries.matching(state.spots, state.category, state.query)
        .let { spots ->
            when (filter) {
                MapFeedFilter.ALL -> spots
                MapFeedFilter.LIKED -> spots.filter { it.id in state.liked }
                MapFeedFilter.SAVED -> spots.filter { it.id in state.saved }
                MapFeedFilter.MINE -> spots.filter { it.author.id == state.user.user.id }
            }
        }
        .let { spots ->
            if (!state.onlyRoundTrip) spots
            else spots.filter { it.carCamping != CarCamping.BANNED && state.tripReach(it) == Reach.ROUND_TRIP }
        }
    val selected = state.spot(state.selectedSpotId)?.takeIf { it in matching }
    var markerMode by rememberSaveable { mutableStateOf(MapMarkerMode.PHOTO) }

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
                        .padding(start = Dimens.Gutter, top = 12.dp, end = Dimens.Gutter, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SearchField(query = state.query, onQueryChange = actions::setQuery, voice = state.voice,
                        onMic = actions::listenForSearch)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ReachFilter(state, actions::toggleOnlyRoundTrip)
                        CategoryFilters(selected = state.category, onSelect = actions::setCategory, modifier = Modifier.weight(1f))
                    }
                    if (!wide) MapFeedTabs(filter, { filter = it })
                }
                Hairline()
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    PhotoMap(
                        spots = matching,
                        selectedId = selected?.id,
                        // Without a navigation fix "here" is only a default; draw no dot rather than a wrong one.
                        here = state.here.takeIf { state.hereFromNavigation },

                        onSelect = { actions.selectSpot(it.id) },
                        onVisibleChange = actions::onVisibleSpotsChanged,
                        style = VehicleMapStyle,
                        markerMode = markerMode,
                        savedIds = state.saved,
                        myAuthorId = state.user.user.id,
                        likedIds = state.liked,
                        onMarkerModeChange = { markerMode = it },
                        herePulse = state.parkitPulse,
                        onMyLocation = if (state.hereFromNavigation) null else actions::showLocationUnavailable,
                        // A red ring warns off overnight stays before the card is even read.
                        markerTint = { if (it.carCamping == CarCamping.BANNED) Palette.Alert else null },
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (selected != null) {
                        PreviewCard(
                            spot = selected,
                            state = state,
                            onOpen = { actions.openSpot(selected.id) },
                            onToggleSave = { actions.toggleSave(selected.id) },
                            onRoute = { actions.startRoute(selected.id) },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = Dimens.Gutter, bottom = Dimens.Gutter, end = ParkitButtonWidth + Dimens.Gutter * 2)
                                .widthIn(max = 820.dp),
                        )
                    }
                    ParkitMapControl(state, actions, Modifier.align(Alignment.BottomEnd).padding(Dimens.Gutter))
                }
            }
            if (wide) {
                FeedPanel(
                    state = state,
                    matching = matching,
                    filter = filter,
                    onFilterChange = { filter = it },
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
private fun ReachFilter(state: AppUiState, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(Dimens.ControlRadius)
    val green = Palette.Success
    Row(
        modifier = Modifier.heightIn(min = 48.dp)
            .clip(shape)
            .background(green.copy(alpha = if (state.onlyRoundTrip) 0.19f else 0.08f))
            .border(1.dp, green.copy(alpha = if (state.onlyRoundTrip) 0.85f else 0.30f), shape)
            .toggleable(state.onlyRoundTrip, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null, tint = green, modifier = Modifier.size(23.dp))
        Column {
            Text("왕복 가능만", style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
            if (state.usableRangeKm != null) state.battery?.let {
                Text("배터리 ${it.percent}%", style = MaterialTheme.typography.labelMedium.tabular(), color = Palette.TextSecondary)
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
                    "장소, 지역, 작성자 검색",
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
private fun CategoryFilters(selected: Category?, onSelect: (Category?) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
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
            .clip(shape)
            .background(Palette.BackgroundRaised.copy(alpha = 0.97f))
            .border(1.dp, Palette.Rule, shape)
            .clickable(role = Role.Button, onClickLabel = "자세히 보기", onClick = onOpen)
            .heightIn(min = MapPlaceCardHeight)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SpotPhoto(
            url = spot.photos.first(),
            contentDescription = spot.title,
            modifier = Modifier
                .size(152.dp)
                .clip(RoundedCornerShape(Dimens.PhotoRadius)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(spot.title, style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CarCampingBadge(spot.carCamping, iconSize = 20.dp, stale = spot.carCampingStale)
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
            state.tripReach(spot)?.let { verdict ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null, tint = verdict.color(), modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text(verdict.label, style = MaterialTheme.typography.bodyMedium, color = verdict.color())
                        if (verdict != Reach.SHORT) state.tripArrivalPercent(spot)?.let {
                            Text("도착 시 배터리 약 $it%", style = MaterialTheme.typography.labelLarge.tabular(), color = Palette.TextSecondary)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.NearMe, contentDescription = null, tint = Palette.TextSecondary, modifier = Modifier.size(20.dp))
                val routeSeconds = state.remainingRouteSeconds(spot)
                val travelTime = when {
                    routeSeconds != null -> "${formatDuration(routeSeconds)} 소요"
                    else -> state.estimatedTripSeconds(spot)?.let { "약 ${formatDuration(it)} 소요" }
                }
                val tripSummary = if (travelTime != null) {
                    val distance = state.remainingRouteKm(spot) ?: distanceKm(state.here, spot.location)
                    "${formatKm(distance)} · ${travelTime.replace(' ', '\u00A0')}"
                } else "${distanceLabel(state, spot)} · ${spot.region}"
                Text(
                    tripSummary,
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                    color = Palette.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(Modifier.width(280.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("길 안내", Icons.Outlined.Directions, onRoute, primary = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton(
                    text = if (saved) "저장됨" else "저장",
                    icon = if (saved) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    onClick = onToggleSave,
                    active = saved,
                    modifier = Modifier.weight(1f),
                )
                ActionButton("자세히", Icons.AutoMirrored.Outlined.ArrowForward, onOpen, modifier = Modifier.weight(1f))
            }
        }
    }
}

private val ParkitButtonWidth = 124.dp
private val MapPlaceCardHeight = 184.dp

@Composable
private fun ParkitMapControl(state: AppUiState, actions: AppViewModel, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val suggestion = state.parkit
    LaunchedEffect(suggestion == null) {
        if (suggestion == null) expanded = false
    }
    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (expanded && suggestion != null) {
            ParkitActions(
                nearSpot = state.spot(suggestion.nearSpotId),
                suggestionAt = suggestion.at,
                canShowArrivalChecklist = state.drivingState == DrivingState.PARKED,
                note = suggestion.note,
                voice = state.voice,
                onNote = actions::listenForParkitNote,
                onSend = actions::sendParkit,
                modifier = Modifier.padding(bottom = 52.dp).widthIn(max = 400.dp),
            )
        }
        val shape = RoundedCornerShape(14.dp)
        Column(
            modifier = Modifier
                .width(ParkitButtonWidth)
                .heightIn(min = MapPlaceCardHeight)
                .clip(shape)
                .background(Palette.Accent)
                .then(if (expanded) Modifier.border(2.dp, Palette.OnAccent, shape) else Modifier)
                .clickable(role = Role.Button, onClickLabel = if (expanded) "파킷 접기" else "현재 위치 파킷하기") {
                    if (expanded) {
                        expanded = false
                        actions.dismissParkit()
                    } else if (suggestion != null || state.hereFromNavigation) {
                        if (suggestion == null) actions.suggestParkit(manual = true)
                        expanded = true
                    } else {
                        actions.showLocationUnavailable()
                    }
                }
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ParkItMark(56.dp, tint = Palette.OnAccent)
            Text("파킷", style = MaterialTheme.typography.titleLarge, color = Palette.OnAccent)
            Text("내 장소 공유", style = MaterialTheme.typography.labelMedium, color = Palette.OnAccent)
        }
    }
}

@Composable
private fun ParkitActions(
    nearSpot: Spot?,
    suggestionAt: Long?,
    canShowArrivalChecklist: Boolean,
    note: String,
    voice: VoiceUi,
    onNote: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listening = voice.listening == ListenPurpose.PARKIT_NOTE
    var showChecklist by remember(suggestionAt) { mutableStateOf(false) }
    var checked by remember(suggestionAt) { mutableStateOf(emptySet<String>()) }
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Palette.BackgroundRaised)
            .border(1.dp, Palette.Rule, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            nearSpot?.title ?: "현재 위치",
            style = MaterialTheme.typography.titleLarge,
            color = Palette.TextPrimary,
            maxLines = 2,
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
                if (nearSpot != null) "${nearSpot.title} 방문을 기록하고 휴대폰에서 후기를 쓸 수 있어요." else "현재 위치를 보내고 휴대폰에서 사진과 후기를 더할 수 있어요.",
                style = MaterialTheme.typography.bodyLarge,
                color = Palette.TextSecondary,
            )
        }
        if (canShowArrivalChecklist && nearSpot != null) {
            ActionButton(
                if (showChecklist) "쉬어가기 접기" else "쉬어가기",
                Icons.Outlined.CheckCircle,
                { showChecklist = !showChecklist },
            )
            if (showChecklist) {
                Text(
                    answerAboutPlace(PlaceQuestion.PARKING, nearSpot).orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextSecondary,
                )
                val items = nearSpot.products.map { it.name }.distinct().take(2)
                    .ifEmpty { listOf("예약과 운영 시간 확인", "현장 이용 안내 확인") }
                items.forEach { item ->
                    val done = item in checked
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(done, role = Role.Checkbox) {
                            checked = if (it) checked + item else checked - item
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Checkbox(checked = done, onCheckedChange = null)
                        Text(item, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
                    }
                }
            }
        }
        if (voice.canListen) {
            ActionButton(
                text = when {
                    listening -> "듣는 중"
                    note.isNotBlank() -> "다시 말하기"
                    else -> "음성 메모"
                },
                icon = Icons.Outlined.Mic,
                onClick = onNote,
                active = listening,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ActionButton("휴대폰으로 보내기", Icons.Outlined.PushPin, onSend, modifier = Modifier.fillMaxWidth(), primary = true)
    }
}

fun distanceLabel(state: AppUiState, spot: Spot): String {
    val km = formatKm(distanceKm(state.here, spot.location))
    return if (state.hereFromNavigation) "내 위치에서 $km" else "서울시청에서 직선 $km"
}

@Composable
private fun FeedPanel(
    state: AppUiState,
    matching: List<Spot>,
    filter: MapFeedFilter,
    onFilterChange: (MapFeedFilter) -> Unit,
    actions: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val visibleIds = state.visibleSpotIds
    val inView = if (visibleIds == null) state.spots else state.spots.filter { it.id in visibleIds }
    val sorts = if (filter == MapFeedFilter.ALL) FeedSort.entries else listOf(FeedSort.POPULAR, FeedSort.LATEST)
    val sort = state.feedSort.takeIf { it in sorts } ?: FeedSort.LATEST
    val spots = SpotQueries.feed(matching, visibleIds, sort, state.following)
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
                    val title = if (filter == MapFeedFilter.ALL) SpotQueries.regionTitle(inView) else "지도에 보이는 곳"
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
                    Text(
                        "${spots.size}곳",
                        style = MaterialTheme.typography.titleSmall.tabular(),
                        color = Palette.TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MapFeedTabs(filter, onFilterChange, Modifier.weight(1f))
                    FeedSortMenu(sort, sorts, actions::setFeedSort)
                }
                Hairline()
            }
        }
        if (spots.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                EmptyState(
                    icon = Icons.Outlined.PhotoCamera,
                    title = if (sort == FeedSort.FOLLOWING) "팔로잉한 사람의 사진이 없어요" else filter.emptyTitle,
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

@Composable
private fun MapFeedTabs(
    filter: MapFeedFilter,
    onSelect: (MapFeedFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        MapFeedFilter.entries.forEach { option ->
            TextTab(option.label, filter == option, onClick = { onSelect(option) })
        }
    }
}

@Composable
private fun FeedSortMenu(sort: FeedSort, options: List<FeedSort>, onSelect: (FeedSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier.size(Dimens.TouchTarget)
                .clip(RoundedCornerShape(Dimens.ControlRadius))
                .clickable(role = Role.Button) { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "정렬 · ${sort.label}", tint = Palette.TextPrimary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    modifier = Modifier.semantics { selected = sort == option },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
