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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import com.nangmanroad.core.ui.Avatar
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.phone.HomeTab
import com.nangmanroad.phone.PhoneUiState
import com.nangmanroad.phone.PhoneViewModel
import com.nangmanroad.phone.ui.components.Chip
import com.nangmanroad.phone.ui.components.IconCircle
import com.nangmanroad.phone.ui.components.SectionHeader
import com.nangmanroad.phone.ui.components.Tag
import com.nangmanroad.phone.ui.components.distanceFromHere
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(state: PhoneUiState, actions: PhoneViewModel) {
    val feed = when (state.homeTab) {
        HomeTab.FOR_YOU -> SpotQueries.recommended(state.spots, state.saved, state.liked, state.following, state.here)
        HomeTab.FOLLOWING -> state.spots.filter { it.author.id in state.following }.sortedBy { it.postedDaysAgo }
        HomeTab.NEARBY -> state.spots.sortedBy { distanceKm(state.here, it.location) }
    }
    val nearby = state.spots.sortedBy { distanceKm(state.here, it.location) }.take(6)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일, EEEE", Locale.KOREAN)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextSecondary,
                    )
                    Text("오늘의 낭만", style = MaterialTheme.typography.headlineLarge, color = Palette.TextPrimary)
                }
                IconCircle(Icons.Outlined.Notifications, "알림", actions::openNotifications, badge = state.fromCar.isNotEmpty())
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HomeTab.entries) { tab -> Chip(tab.label, state.homeTab == tab, { actions.setHomeTab(tab) }) }
            }
        }
        if (feed.isEmpty()) {
            item {
                Text(
                    "팔로우한 사람의 게시물이 아직 없어요. 마음에 드는 작성자를 팔로우해 보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        feed.forEachIndexed { index, spot ->
            item(key = spot.id) {
                PostCard(spot, state, actions, highlight = index == 0 && state.homeTab == HomeTab.FOR_YOU)
            }
            if (index == 0) {
                item(key = "nearby") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("NEAR YOU", "이번 주말, 가까운 곳", Modifier.padding(horizontal = 20.dp))
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
fun PostCard(spot: Spot, state: PhoneUiState, actions: PhoneViewModel, highlight: Boolean = false) {
    val liked = spot.id in state.liked
    val saved = spot.id in state.saved
    val isMine = spot.author.id == state.me.id
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(spot.author.name, size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text(spot.author.name, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
                Text("${spot.region} · ${distanceFromHere(state.here, spot)}", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
            }
            when {
                isMine -> VisibilityTag(spot.visibility)
                spot.author.id !in state.following -> Text(
                    "팔로우",
                    style = MaterialTheme.typography.labelLarge,
                    color = Palette.Coral,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { actions.toggleFollow(spot.author.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        val pager = rememberPagerState(pageCount = { spot.photos.size })
        Box(
            Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .aspectRatio(0.86f)
                .clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button, onClickLabel = "${spot.title} 보기") { actions.openSpot(spot.id) },
        ) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                SpotPhoto(spot.photos[page], "${spot.title} 사진 ${page + 1}", Modifier.fillMaxSize())
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Palette.Background.copy(alpha = 0.85f))))
            if (highlight) Tag("당신이 좋아할 낭만", Palette.Peach, Modifier.padding(16.dp).background(Palette.Background.copy(alpha = 0.6f), RoundedCornerShape(6.dp)))
            if (spot.photos.size > 1) {
                Text(
                    "${pager.currentPage + 1}/${spot.photos.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Palette.TextPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Palette.Scrim)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${spot.province} · ${spot.category.label}", style = MaterialTheme.typography.labelLarge, color = Palette.Peach)
                Text(spot.title, style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
            }
        }
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Action(if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, state.likeCount(spot).toString(), if (liked) Palette.Coral else Palette.TextPrimary) { actions.toggleLike(spot.id) }
            Action(Icons.Outlined.ChatBubbleOutline, spot.commentCount.toString()) { actions.openSpot(spot.id) }
            Action(Icons.Outlined.DirectionsCar, "차로 보내기") { actions.sendToCar(spot.id) }
            Spacer(Modifier.weight(1f))
            Action(Icons.Outlined.TipsAndUpdates, "꿀팁 ${spot.tips.size}", Palette.Peach) { actions.openSpot(spot.id) }
            Action(if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, null, if (saved) Palette.Coral else Palette.TextPrimary) { actions.toggleSave(spot.id) }
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
private fun Action(icon: ImageVector, label: String?, tint: Color = Palette.TextPrimary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        if (label != null) Text(label, style = MaterialTheme.typography.labelLarge, color = if (tint == Palette.TextPrimary) Palette.TextSecondary else tint)
    }
}

@Composable
fun VisibilityTag(visibility: Visibility) {
    when (visibility) {
        Visibility.PUBLIC -> Tag("내 게시물", Palette.Success)
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
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SpotPhoto(spot.photos.first(), spot.title, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(18.dp)))
        Text(spot.title, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(distanceFromHere(state.here, spot), style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
    }
}
