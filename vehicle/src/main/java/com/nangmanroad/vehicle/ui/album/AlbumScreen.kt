package com.nangmanroad.vehicle.ui.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nangmanroad.vehicle.AppUiState
import com.nangmanroad.vehicle.AppViewModel
import com.nangmanroad.core.data.AlbumTab
import com.nangmanroad.core.data.SpotQueries
import com.nangmanroad.vehicle.ui.components.EmptyState
import com.nangmanroad.vehicle.ui.components.Hairline
import com.nangmanroad.vehicle.ui.components.TextTab
import com.nangmanroad.vehicle.ui.feed.SpotCard
import com.nangmanroad.core.ui.Palette

private val aspects = listOf(0.78f, 1.0f, 0.68f, 1.2f, 0.9f)

@Composable
fun AlbumScreen(state: AppUiState, actions: AppViewModel) {
    val spots = SpotQueries.album(state.spots, state.albumTab, state.saved, state.liked, state.following, state.here, state.fromPhone)
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(320.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(40.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalItemSpacing = 22.dp,
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text("앨범", style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AlbumTab.entries.forEach { tab ->
                        TextTab(
                            text = tab.label,
                            selected = state.albumTab == tab,
                            onClick = { actions.setAlbumTab(tab) },
                            count = when (tab) {
                                AlbumTab.SAVED -> state.saved.size
                                AlbumTab.FROM_PHONE -> state.fromPhone.size
                                else -> null
                            },
                        )
                    }
                }
                Hairline()
            }
        }
        if (spots.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                when (state.albumTab) {
                    AlbumTab.SAVED -> EmptyState(Icons.Outlined.BookmarkBorder, "저장한 장소가 없어요", "마음에 드는 사진의 책갈피를 눌러 저장해 보세요.")
                    AlbumTab.FROM_PHONE -> EmptyState(
                        Icons.Outlined.PhoneAndroid,
                        "휴대폰에서 보낸 장소가 없어요",
                        if (state.session == null) "왼쪽 아래 '계정'에서 휴대폰과 연결하면 휴대폰에서 고른 장소가 여기에 와요."
                        else "휴대폰 낭만로드 앱의 장소 상세에서 '차량으로 보내기'를 눌러 보세요.",
                    )
                    else -> EmptyState(Icons.Outlined.PersonAddAlt, "팔로잉한 사람이 없어요", "장소 상세에서 작성자를 팔로우해 보세요.")
                }
            }
        }
        items(spots, key = { it.id }) { spot ->
            SpotCard(
                spot = spot,
                likeCount = state.likeCount(spot),
                saved = spot.id in state.saved,
                photoAspect = aspects[(spot.id.hashCode() and 0x7fffffff) % aspects.size],
                onOpen = { actions.openSpot(spot.id) },
                onToggleSave = { actions.toggleSave(spot.id) },
            )
        }
    }
}
