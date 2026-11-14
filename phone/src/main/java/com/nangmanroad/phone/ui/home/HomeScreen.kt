package com.nangmanroad.phone.ui.home

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.SpotQueries
import com.nangmanroad.core.data.Visibility
import com.nangmanroad.core.data.distanceKm
import com.nangmanroad.core.data.carCampingStale
import com.nangmanroad.core.data.hasFacility
import com.nangmanroad.core.ui.Avatar
import com.nangmanroad.core.ui.CarCampingBadge
import com.nangmanroad.core.ui.FacilityFilterRow
import com.nangmanroad.core.ui.FacilityStrip
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.ParkItWordmark
import com.nangmanroad.core.ui.tabular
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.phone.HomeTab
import com.nangmanroad.phone.PhoneUiState
import com.nangmanroad.phone.PhoneViewModel
import com.nangmanroad.phone.ui.components.IconCircle
import com.nangmanroad.phone.ui.components.TabRow
import com.nangmanroad.phone.ui.components.TextTab
import com.nangmanroad.phone.ui.components.SectionHeader
import com.nangmanroad.phone.ui.components.Tag
import com.nangmanroad.phone.ui.components.VisitConditionChips
import com.nangmanroad.phone.ui.components.distanceFromHere
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.TextButton

@Composable
fun HomeScreen(state: PhoneUiState, actions: PhoneViewModel) {
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        actions.useCurrentLocation()
    }
    val feed = when (state.homeTab) {
        HomeTab.FOR_YOU -> SpotQueries.recommended(state.spots, state.saved, state.liked, state.following, state.here)
        HomeTab.FOLLOWING -> state.spots.filter { it.author.id in state.following }.sortedBy { it.postedDaysAgo }
        HomeTab.NEARBY -> state.spots.sortedBy { distanceKm(state.here, it.location) }
    }.filter { spot -> state.facilities.all { spot.hasFacility(it) } && spot.visitConditions.containsAll(state.visitConditions) }
    val nearby = state.spots.filter { spot ->
        state.facilities.all { spot.hasFacility(it) } && spot.visitConditions.containsAll(state.visitConditions)
    }.sortedBy { distanceKm(state.here, it.location) }.take(6)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column {
                Row(Modifier.padding(start = 20.dp, end = 10.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ParkItWordmark(Modifier.weight(1f), markSize = 32.dp, textStyle = MaterialTheme.typography.titleLarge)
                    IconCircle(Icons.Outlined.Notifications, "알림", actions::openNotifications, badge = state.fromCar.isNotEmpty())
                }
                TabRow {
                    HomeTab.entries.forEach { tab -> TextTab(tab.label, state.homeTab == tab, { actions.setHomeTab(tab) }) }
                }
                if (state.homeTab == HomeTab.NEARBY) {
                    SectionHeader(
                        if (state.usesDeviceLocation) "내 위치 기준" else "서울 기준",
                        Modifier.padding(horizontal = 20.dp),
                    ) {
                        TextButton(
                            enabled = !state.locating,
                            onClick = {
                                permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            },
                        ) { Text(if (state.locating) "위치 확인 중" else "내 위치 사용") }
                    }
                }
                FacilityFilterRow(
                    selected = state.facilities,
                    onToggle = actions::toggleFacility,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    height = 36.dp,
                    iconSize = 18.dp,
                    gap = 6.dp,
                    trailing = { VisitConditionChips(state.visitConditions, actions::toggleVisitCondition) },
                )
                if (state.facilities.isNotEmpty() || state.visitConditions.isNotEmpty()) {
                    Text("조건에 맞는 장소 ${feed.size}곳", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary, modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }
        if (feed.isEmpty()) {
            item {
                Text(
                    if (state.facilities.isNotEmpty() || state.visitConditions.isNotEmpty()) "선택한 조건을 모두 확인한 장소가 없어요."
                    else "팔로우한 사람의 게시물이 아직 없어요. 마음에 드는 작성자를 팔로우해 보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        feed.forEachIndexed { index, spot ->
            item(key = spot.id) {
                PostCard(spot, state, actions)
            }
            if (index == 0) {
                item(key = "nearby") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("가까운 곳", Modifier.padding(horizontal = 20.dp))
                        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(nearby, key = { "near-${it.id}" }) { NearbyCard(it, state) { actions.openSpot(it.id) } }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(spot: Spot, state: PhoneUiState, actions: PhoneViewModel) {
    val liked = spot.id in state.liked
    val saved = spot.id in state.saved
    val isMine = spot.author.id == state.me.id
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(spot.author.name, size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text(spot.author.name, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
                Text("${spot.region} · ${distanceFromHere(state.here, spot, state.usesDeviceLocation)}", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
            }
            when {
                isMine -> VisibilityTag(spot.visibility)
                spot.author.id !in state.following -> Text(
                    "팔로우",
                    style = MaterialTheme.typography.labelLarge,
                    color = Palette.TextPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(role = Role.Button) { actions.toggleFollow(spot.author.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        val pager = rememberPagerState(pageCount = { spot.photos.size })
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClickLabel = "${spot.title} 보기") { actions.openSpot(spot.id) },
        ) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                SpotPhoto(spot.photos[page], "${spot.title} 사진 ${page + 1}", Modifier.fillMaxSize())
            }
            // Darken only the bottom so the title reads while the sky keeps its colour.
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Palette.Background.copy(alpha = 0.88f))))
            if (spot.photos.size > 1) {
                Text(
                    "${pager.currentPage + 1}/${spot.photos.size}",
                    style = MaterialTheme.typography.labelMedium.tabular(),
                    color = Palette.TextPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Palette.Scrim)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PhotoChip("${spot.province} · ${spot.category.label}")
                    CarCampingBadge(spot.carCamping, Modifier.background(Palette.Scrim, RoundedCornerShape(3.dp)), iconSize = 14.dp, stale = spot.carCampingStale)
                    if (spot.tips.isNotEmpty()) PhotoChip("꿀팁 ${spot.tips.size}", accent = true)
                }
                // Facilities on their own line so the five icons never get clipped by the chips.
                FacilityStrip(
                    spot.tips,
                    Modifier.clip(RoundedCornerShape(3.dp)).background(Palette.Scrim).padding(horizontal = 8.dp, vertical = 5.dp),
                    iconSize = 16.dp,
                    gap = 8.dp,
                )
                if (spot.bestTime.isNotBlank()) PhotoChip(spot.bestTime, icon = Icons.Outlined.Schedule)
                Text(spot.title, style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
            }
        }
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Action(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, state.likeCount(spot).toString()) { actions.toggleLike(spot.id) }
            Action(Icons.Outlined.ChatBubbleOutline, spot.commentCount.toString()) { actions.openSpot(spot.id) }
            Action(Icons.Outlined.DirectionsCar, "차량으로 보내기", description = "이 장소를 차량으로 보내기") { actions.sendToCar(spot.id) }
            Spacer(Modifier.weight(1f))
            Action(Icons.Outlined.TipsAndUpdates, "꿀팁 ${spot.tips.size}") { actions.openSpot(spot.id) }
            Action(if (saved) Icons.Rounded.Star else Icons.Rounded.StarBorder, null) { actions.toggleSave(spot.id) }
        }
        if (spot.story.isNotBlank()) {
            Text(
                "${spot.author.name}  ${spot.story}",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
private fun Action(icon: ImageVector, label: String?, description: String? = label, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = description, tint = Palette.TextPrimary, modifier = Modifier.size(24.dp))
        if (label != null) Text(label, style = MaterialTheme.typography.labelLarge.tabular(), color = Palette.TextSecondary)
    }
}

@Composable
fun VisibilityTag(visibility: Visibility) {
    when (visibility) {
        Visibility.PUBLIC -> Tag("내 게시물", Palette.TextSecondary)
        Visibility.FOLLOWERS -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.PeopleOutline, null, tint = Palette.Warning, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Tag(visibility.label, Palette.Warning)
        }
        Visibility.PRIVATE -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, null, tint = Palette.TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Tag(visibility.label, Palette.TextSecondary)
        }
    }
}

@Composable
private fun NearbyCard(spot: Spot, state: PhoneUiState, onClick: () -> Unit) {
    Column(
        Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(4.dp))) {
            SpotPhoto(spot.photos.first(), spot.title, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Palette.Background.copy(alpha = 0.8f))))
            PhotoChip(distanceFromHere(state.here, spot, state.usesDeviceLocation), Modifier.align(Alignment.BottomStart).padding(8.dp))
        }
        Text(spot.title, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        FacilityStrip(spot.tips, iconSize = 15.dp, gap = 5.dp)
    }
}

/** A small label on a photo: a light veil, or solid ink when [accent] marks the tip count. */
@Composable
private fun PhotoChip(text: String, modifier: Modifier = Modifier, accent: Boolean = false, icon: ImageVector? = null) {
    val color = if (accent) Palette.Background else Palette.TextPrimary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (accent) Palette.TextPrimary.copy(alpha = 0.9f) else Palette.Scrim)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium.tabular(), color = color)
    }
}
