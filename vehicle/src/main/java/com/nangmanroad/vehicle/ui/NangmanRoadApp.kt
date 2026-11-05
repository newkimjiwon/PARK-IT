package com.nangmanroad.vehicle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nangmanroad.vehicle.AppUiState
import com.nangmanroad.vehicle.AppViewModel
import com.nangmanroad.vehicle.Screen
import com.nangmanroad.vehicle.data.AlbumTab
import com.nangmanroad.vehicle.ui.album.AlbumScreen
import com.nangmanroad.vehicle.ui.detail.SpotDetailScreen
import com.nangmanroad.vehicle.ui.driving.DrivingScreen
import com.nangmanroad.vehicle.ui.map.MapHomeScreen
import com.nangmanroad.vehicle.ui.theme.Dimens
import com.nangmanroad.vehicle.ui.theme.Palette
import kotlinx.coroutines.delay

@Composable
fun NangmanRoadApp(state: AppUiState, actions: AppViewModel) {
    val screens = rememberSaveableStateHolder()

    BackHandler(enabled = state.screen != Screen.Map && !state.showDrivingView) { actions.back() }

    Box(Modifier.fillMaxSize().background(Palette.Background)) {
        Crossfade(targetState = state.showDrivingView, label = "drivingMode") { driving ->
            if (driving) {
                DrivingScreen(state, onExitPreview = { actions.setDrivingPreview(false) })
            } else {
                Row(Modifier.fillMaxSize()) {
                    SideRail(state, actions)
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        when (val screen = state.screen) {
                            Screen.Map -> screens.SaveableStateProvider("map") { MapHomeScreen(state, actions) }
                            Screen.Album -> screens.SaveableStateProvider("album") { AlbumScreen(state, actions) }
                            is Screen.Detail -> {
                                val spot = state.spot(screen.spotId)
                                if (spot != null) {
                                    screens.SaveableStateProvider("detail:${spot.id}") { SpotDetailScreen(spot, state, actions) }
                                }
                            }
                        }
                    }
                }
            }
        }

        Toast(state, actions, Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp))
    }
}

@Composable
private fun SideRail(state: AppUiState, actions: AppViewModel) {
    val onAlbum = state.screen == Screen.Album
    Column(
        modifier = Modifier
            .width(Dimens.RailWidth)
            .fillMaxHeight()
            .background(Palette.Background)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.Coral),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(Palette.Background))
        }
        Spacer(Modifier.size(18.dp))
        RailButton(Icons.Outlined.Map, "지도", state.screen == Screen.Map, actions::showMap)
        RailButton(Icons.Outlined.PhotoLibrary, "앨범", onAlbum && state.albumTab != AlbumTab.SAVED) {
            actions.showAlbum(if (state.albumTab == AlbumTab.SAVED) AlbumTab.RECOMMENDED else null)
        }
        RailButton(Icons.Outlined.BookmarkBorder, "저장", onAlbum && state.albumTab == AlbumTab.SAVED) {
            actions.showAlbum(AlbumTab.SAVED)
        }
        Spacer(Modifier.weight(1f))
        RailButton(Icons.Outlined.DirectionsCar, "주행 화면", false) { actions.setDrivingPreview(true) }
    }
}

@Composable
private fun RailButton(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(width = 88.dp, height = 88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Palette.Surface else Palette.Background)
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Palette.Coral else Palette.TextSecondary, modifier = Modifier.size(32.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) Palette.TextPrimary else Palette.TextSecondary)
    }
}

@Composable
private fun Toast(state: AppUiState, actions: AppViewModel, modifier: Modifier = Modifier) {
    val toast = state.toast
    LaunchedEffect(toast?.id) {
        if (toast != null) {
            delay(2_800)
            actions.consumeToast(toast.id)
        }
    }
    AnimatedVisibility(visible = toast != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(Palette.SurfaceHigh)
                .padding(horizontal = 32.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Palette.Success, modifier = Modifier.size(30.dp))
            Text(toast?.text.orEmpty(), style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
        }
    }
}
