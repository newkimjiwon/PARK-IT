package com.nangmanroad.phone.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.SpotQueries
import com.nangmanroad.core.map.MapCamera
import com.nangmanroad.core.map.PhotoMap
import com.nangmanroad.core.map.PhotoMapStyle
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.SpotPhoto
import com.nangmanroad.phone.PhoneUiState
import com.nangmanroad.phone.PhoneViewModel
import com.nangmanroad.phone.ui.components.Chip
import com.nangmanroad.phone.ui.components.distanceFromHere

/** Phone-sized markers; zoomed out so the whole peninsula fits a portrait screen. */
val PhoneMapStyle = PhotoMapStyle(
    markerSize = 56.dp,
    selectedMarkerSize = 70.dp,
    clusterRadius = 64.dp,
    buttonSize = 48.dp,
    controlsPadding = PaddingValues(top = 116.dp, end = 12.dp),
    initialCamera = MapCamera(LatLng(36.3, 127.9), zoom = 0.9f),
)

@Composable
fun PhoneMapScreen(state: PhoneUiState, actions: PhoneViewModel) {
    val spots = SpotQueries.matching(state.spots, state.mapCategory, "")
    val selected = state.spot(state.mapSelectedId)?.takeIf { it in spots }

    Box(Modifier.fillMaxSize()) {
        PhotoMap(
            spots = spots,
            selectedId = selected?.id,
            here = state.here,
            onSelect = { actions.selectOnMap(it.id) },
            onVisibleChange = {},
            style = PhoneMapStyle,
            modifier = Modifier.fillMaxSize(),
        )
        Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("낭만 지도", style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary, modifier = Modifier.padding(horizontal = 20.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip("전체", state.mapCategory == null, { actions.setMapCategory(null) }) }
                items(Category.entries) { category ->
                    Chip(category.label, state.mapCategory == category, { actions.setMapCategory(if (state.mapCategory == category) null else category) })
                }
            }
        }
        if (selected != null) {
            val saved = selected.id in state.saved
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Palette.BackgroundRaised)
                    .clickable(role = Role.Button, onClickLabel = "자세히 보기") { actions.openSpot(selected.id) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SpotPhoto(selected.photos.first(), selected.title, Modifier.size(84.dp).clip(RoundedCornerShape(16.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(selected.category.label, style = MaterialTheme.typography.labelMedium, color = Palette.Peach)
                    Text(selected.title, style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${selected.region} · ${distanceFromHere(state.here, selected)}", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                    Text("꿀팁 ${selected.tips.size} · 좋아요 ${state.likeCount(selected)}", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
                }
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Palette.Surface)
                        .clickable(role = Role.Button, onClickLabel = if (saved) "저장 취소" else "저장") { actions.toggleSave(selected.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, null, tint = if (saved) Palette.Coral else Palette.TextPrimary)
                }
            }
        }
    }
}
