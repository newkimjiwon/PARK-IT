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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PushPin
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
import com.nangmanroad.core.data.AlbumTab
import com.nangmanroad.vehicle.ui.account.AccountScreen
import com.nangmanroad.vehicle.ui.album.AlbumScreen
import com.nangmanroad.vehicle.ui.detail.SpotDetailScreen
import com.nangmanroad.vehicle.ui.driving.DrivingScreen
import com.nangmanroad.vehicle.ui.map.MapHomeScreen
import com.nangmanroad.vehicle.ui.theme.Dimens
import com.nangmanroad.core.ui.ParkItMark
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.paperGrain
import kotlinx.coroutines.delay

@Composable
fun NangmanRoadApp(state: AppUiState, actions: AppViewModel) {
    val screens = rememberSaveableStateHolder()

    BackHandler(enabled = state.screen != Screen.Map && !state.showDrivingView) { actions.back() }

    Box(Modifier.fillMaxSize().background(Palette.Background).paperGrain()) {
        Crossfade(targetState = state.showDrivingView, label = "drivingMode") { driving ->
            if (driving) {
                DrivingScreen(
                    state,
                    onExitPreview = { actions.setDrivingPreview(false) },
                    onToggleBriefing = actions::toggleBriefing,
                    onListen = actions::listenForCommand,
                )
            } else {
                Row(Modifier.fillMaxSize()) {
                    SideRail(state, actions)
                    Box(Modifier.width(1.dp).fillMaxHeight().background(Palette.Hairline))
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        when (val screen = state.screen) {
                            Screen.Map -> screens.SaveableStateProvider("map") { MapHomeScreen(state, actions) }
                            Screen.Album -> screens.SaveableStateProvider("album") { AlbumScreen(state, actions) }
                            Screen.Account -> AccountScreen(state, actions)
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ParkItMark(size = 44.dp, modifier = Modifier.padding(bottom = 12.dp))
        RailButton(Icons.Outlined.Map, "지도", state.screen == Screen.Map, onClick = actions::showMap)
        RailButton(Icons.Outlined.BookmarkBorder, "저장", onAlbum && state.albumTab == AlbumTab.SAVED) {
            actions.showAlbum(AlbumTab.SAVED)
        }
        RailButton(Icons.Outlined.CheckCircle, "다녀온 곳", onAlbum && state.albumTab == AlbumTab.VISITED) {
            actions.showAlbum(AlbumTab.VISITED)
        }
        RailButton(Icons.Outlined.PhoneAndroid, "휴대폰", onAlbum && state.albumTab == AlbumTab.FROM_PHONE, badge = state.fromPhone.size) {
            actions.showAlbum(AlbumTab.FROM_PHONE)
        }
        RailButton(Icons.Outlined.PushPin, "파킷", state.parkit != null) {
            actions.showMap()
            actions.suggestParkit(manual = true)
        }
        Spacer(Modifier.weight(1f))
        RailButton(
            icon = if (state.session != null) Icons.Outlined.AccountCircle else Icons.Outlined.Link,
            label = if (state.session != null) "계정" else "계정 연결",
            selected = state.screen == Screen.Account,
            onClick = actions::openAccount,
        )
        RailButton(Icons.Outlined.DirectionsCar, "주행 화면", false) { actions.setDrivingPreview(true) }
    }
}

@Composable
private fun RailButton(icon: ImageVector, label: String, selected: Boolean, badge: Int = 0, onClick: () -> Unit) {
    Box {
        Column(
            modifier = Modifier
                .size(width = 88.dp, height = 88.dp)
                .clip(RoundedCornerShape(Dimens.ControlRadius))
                .background(if (selected) Palette.Surface else Palette.Background)
                .clickable(role = Role.Tab, onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) Palette.TextPrimary else Palette.TextSecondary, modifier = Modifier.size(32.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) Palette.TextPrimary else Palette.TextSecondary)
        }
        if (badge > 0) {
            Text(
                badge.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = Palette.Background,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Palette.TextPrimary)
                    .padding(horizontal = 8.dp, vertical = 1.dp),
            )
        }
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
                .clip(RoundedCornerShape(4.dp))
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
