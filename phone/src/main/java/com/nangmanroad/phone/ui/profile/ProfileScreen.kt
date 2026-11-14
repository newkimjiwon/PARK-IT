package com.nangmanroad.phone.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.map.MapCamera
import com.nangmanroad.core.map.PhotoMap
import com.nangmanroad.core.ui.Avatar
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.tabular
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.phone.PhoneUiState
import com.nangmanroad.phone.PhoneViewModel
import com.nangmanroad.phone.ProfileTab
import com.nangmanroad.phone.ui.components.Hairline
import com.nangmanroad.phone.ui.components.TextTab
import com.nangmanroad.phone.ui.map.PhoneMapStyle

private val SharedTint get() = Palette.Warning
private val SavedTint get() = Palette.Info
private val VisitedTint get() = Palette.Success

@Composable
fun ProfileScreen(state: PhoneUiState, actions: PhoneViewModel) {
    val shared = state.myPosts
    val saved = state.spots.filter { it.id in state.saved }
    val visited = state.spots.filter { it.id in state.visited }
    val onMap = (shared + saved + visited).distinctBy { it.id }
    val listed = when (state.profileTab) {
        ProfileTab.SHARED -> shared
        ProfileTab.SAVED -> saved
        ProfileTab.VISITED -> visited
    }
    val tipsGiven = shared.sumOf { spot -> spot.tips.count { it.reporter == state.me.name } } +
        state.spots.filter { it.author.id != state.me.id }.sumOf { spot -> spot.tips.count { it.reporter == state.me.name } }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Avatar(state.me.name, size = 64.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(state.me.name, style = MaterialTheme.typography.headlineSmall, color = Palette.TextPrimary)
                    Text(state.me.bio, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                    Text(
                        if (state.account.session != null) "동기화 중 · ${state.account.serverUrl}" else "이 휴대폰에만 저장 중",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.account.session != null) Palette.TextTertiary else Palette.Warning,
                    )
                }
                Text(
                    if (state.account.session != null) "로그아웃" else "로그인",
                    style = MaterialTheme.typography.labelLarge,
                    color = Palette.TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .clickable(role = Role.Button, onClick = actions::signOut)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        item(span = { GridItemSpan(2) }) {
            Column {
                Hairline()
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                    Stat("공유", shared.size)
                    Stat("방문", visited.size)
                    Stat("받은 좋아요", shared.sumOf { state.likeCount(it) })
                    Stat("꿀팁", tipsGiven)
                    Stat("팔로잉", state.following.size)
                }
                Hairline()
            }
        }
        item(span = { GridItemSpan(2) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("내 장소 지도", style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary)
                PhotoMap(
                    spots = onMap,
                    selectedId = null,
                    here = LatLng(0.0, 0.0),
                    onSelect = { actions.openSpot(it.id) },
                    onVisibleChange = {},
                    style = PhoneMapStyle.copy(markerSize = 44.dp, clusterRadius = 48.dp, controlsPadding = PaddingValues(8.dp), initialCamera = MapCamera(LatLng(36.4, 127.9), 0.9f)),
                    savedIds = state.saved,
                    myAuthorId = state.me.id,
                    likedIds = state.liked,
                    markerTint = { spot ->
                        when {
                            spot.author.id == state.me.id -> SharedTint
                            spot.id in state.visited -> VisitedTint
                            else -> SavedTint
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(3.dp)),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Legend(SharedTint, "내가 공유한 장소")
                    Legend(SavedTint, "저장한 장소")
                    Legend(VisitedTint, "방문 완료")
                }
            }
        }
        item(span = { GridItemSpan(2) }) {
            Column {
                Row(Modifier.offset(x = (-10).dp)) {
                    ProfileTab.entries.forEach { tab -> TextTab(tab.label, state.profileTab == tab, { actions.setProfileTab(tab) }) }
                }
                Hairline()
            }
        }
        if (listed.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    when (state.profileTab) {
                        ProfileTab.SHARED -> "아래 '장소 공유'로 첫 장소를 남겨 보세요."
                        ProfileTab.SAVED -> "마음에 드는 장소의 책갈피를 눌러 저장해 보세요."
                        ProfileTab.VISITED -> "다녀온 장소에서 '다녀왔다면'을 눌러 기록해 보세요."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextSecondary,
                )
            }
        }
        items(listed, key = { it.id }) { spot -> MiniCard(spot) { actions.openSpot(spot.id) } }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Stat(label: String, value: Int) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (value >= 1000) String.format("%.1fk", value / 1000f) else value.toString(), style = MaterialTheme.typography.titleLarge.tabular(), color = Palette.TextPrimary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
    }
}

@Composable
private fun MiniCard(spot: Spot, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(3.dp)).clickable(role = Role.Button, onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SpotPhoto(spot.photos.first(), spot.title, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(3.dp)))
        Text(spot.title, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(spot.region, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
    }
}
