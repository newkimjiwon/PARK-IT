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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.SpotQueries
import com.nangmanroad.core.map.MapCamera
import com.nangmanroad.core.map.MapMarkerMode
import com.nangmanroad.core.map.PhotoMap
import com.nangmanroad.core.map.PhotoMapStyle
import com.nangmanroad.core.ui.FacilityFilterRow
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.tabular
import com.nangmanroad.core.ui.SpotPhoto
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.nangmanroad.phone.LocationPurpose
import com.nangmanroad.phone.PhoneUiState
import com.nangmanroad.phone.PhoneViewModel
import com.nangmanroad.phone.ui.components.FilterSummary
import com.nangmanroad.phone.ui.components.TabRow
import com.nangmanroad.phone.ui.components.TextTab
import com.nangmanroad.phone.ui.components.VisitConditionChips
import com.nangmanroad.phone.ui.components.distanceFromHere

/** Phone-sized markers; zoomed out so the whole peninsula fits a portrait screen. */
val PhoneMapStyle = PhotoMapStyle(
    markerSize = 56.dp,
    selectedMarkerSize = 70.dp,
    clusterRadius = 64.dp,
    buttonSize = 48.dp,
    controlsPadding = PaddingValues(top = 12.dp, end = 12.dp),
    initialCamera = MapCamera(LatLng(36.3, 127.9), zoom = 0.9f),
)

@Composable
fun PhoneMapScreen(state: PhoneUiState, actions: PhoneViewModel) {
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        actions.useCurrentLocation(LocationPurpose.MAP)
    }
    // The same facility chips as the home feed, so a filter chosen there carries over to the map.
    val spots = SpotQueries.matching(state.spots, state.mapCategory, "", state.facilities)
        .filter { it.visitConditions.containsAll(state.visitConditions) }
    val selected = state.spot(state.mapSelectedId)?.takeIf { it in spots }
    var markerMode by rememberSaveable { mutableStateOf(MapMarkerMode.PHOTO) }

    Column(Modifier.fillMaxSize()) {
        Text("지도", style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary, modifier = Modifier.padding(start = 20.dp, top = 12.dp))
        TabRow {
            TextTab("전체", state.mapCategory == null, { actions.setMapCategory(null) })
            Category.entries.forEach { category ->
                TextTab(category.label, state.mapCategory == category, { actions.setMapCategory(if (state.mapCategory == category) null else category) })
            }
        }
        FacilityFilterRow(
            selected = state.facilities,
            onToggle = actions::toggleFacility,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            height = 36.dp,
            iconSize = 18.dp,
            gap = 6.dp,
            trailing = { VisitConditionChips(state.visitConditions, actions::toggleVisitCondition) },
        )
        FilterSummary(
            conditions = state.facilities.size + state.visitConditions.size,
            matches = spots.size,
            onClear = actions::clearFilters,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            PhotoMap(
                spots = spots,
                selectedId = selected?.id,
                // Seoul is only the default for distances; the dot shows once the phone really knows where it is.
                here = state.here.takeIf { state.usesDeviceLocation },
                onSelect = { actions.selectOnMap(it.id) },
                onVisibleChange = {},
                style = PhoneMapStyle,
                markerMode = markerMode,
                savedIds = state.saved,
                myAuthorId = state.me.id,
                likedIds = state.liked,
                onMarkerModeChange = { markerMode = it },
                focus = state.mapFocus,
                onMyLocation = {
                    permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (selected != null) {
                val saved = selected.id in state.saved
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Palette.BackgroundRaised)
                        .clickable(role = Role.Button, onClickLabel = "자세히 보기") { actions.openSpot(selected.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SpotPhoto(selected.photos.first(), selected.title, Modifier.size(84.dp).clip(RoundedCornerShape(10.dp)))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("테마 · ${selected.category.label}", style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
                        Text(selected.title, style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${selected.region} · ${distanceFromHere(state.here, selected, state.usesDeviceLocation)}", style = MaterialTheme.typography.bodySmall.tabular(), color = Palette.TextSecondary)
                        Text("꿀팁 ${selected.tips.size} · 좋아요 ${state.likeCount(selected)}", style = MaterialTheme.typography.bodySmall.tabular(), color = Palette.TextSecondary)
                    }
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Palette.Surface)
                            .clickable(role = Role.Button, onClickLabel = if (saved) "저장 취소" else "저장") { actions.toggleSave(selected.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (saved) Icons.Rounded.Star else Icons.Rounded.StarBorder, null, tint = Palette.TextPrimary)
                    }
                }
            }
        }
    }
}
