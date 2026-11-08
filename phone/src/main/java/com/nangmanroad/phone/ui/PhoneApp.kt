package com.nangmanroad.phone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.phone.PhoneRoute
import com.nangmanroad.phone.PhoneTab
import com.nangmanroad.phone.PhoneUiState
import com.nangmanroad.phone.PhoneViewModel
import com.nangmanroad.phone.ui.account.SignInScreen
import com.nangmanroad.phone.ui.car.CarScreen
import com.nangmanroad.phone.ui.detail.PostDetailScreen
import com.nangmanroad.phone.ui.home.HomeScreen
import com.nangmanroad.phone.ui.map.PhoneMapScreen
import com.nangmanroad.phone.ui.notifications.NotificationsScreen
import com.nangmanroad.phone.ui.profile.ProfileScreen
import com.nangmanroad.phone.ui.share.ShareFlow
import kotlinx.coroutines.delay

@Composable
fun PhoneApp(state: PhoneUiState, actions: PhoneViewModel) {
    val screens = rememberSaveableStateHolder()
    val route = state.route

    BackHandler(enabled = route != null || state.tab != PhoneTab.HOME) {
        when {
            route == PhoneRoute.Share -> actions.draftBack()
            route != null -> actions.back()
            else -> actions.selectTab(PhoneTab.HOME)
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.Background)) {
        if (state.account.needsSignIn) {
            Box(Modifier.fillMaxSize().statusBarsPadding()) { SignInScreen(state.account, actions) }
            Toast(state, actions, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 40.dp))
            return@Box
        }
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f)) {
                when (route) {
                    is PhoneRoute.Detail -> state.spot(route.spotId)?.let { spot ->
                        screens.SaveableStateProvider("detail:${spot.id}") { PostDetailScreen(spot, state, actions) }
                    }
                    PhoneRoute.Notifications -> NotificationsScreen(state, actions)
                    PhoneRoute.Share -> state.draft?.let { ShareFlow(it, state, actions) }
                    null -> screens.SaveableStateProvider(state.tab.name) {
                        when (state.tab) {
                            PhoneTab.HOME, PhoneTab.SHARE -> HomeScreen(state, actions)
                            PhoneTab.MAP -> PhoneMapScreen(state, actions)
                            PhoneTab.CAR -> CarScreen(state, actions)
                            PhoneTab.MY -> ProfileScreen(state, actions)
                        }
                    }
                }
            }
            if (route != PhoneRoute.Share) BottomBar(state, actions)
        }
        Toast(state, actions, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp))
    }
}

@Composable
private fun BottomBar(state: PhoneUiState, actions: PhoneViewModel) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Hairline))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Palette.Background)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem(Icons.Outlined.Home, "홈", state.tab == PhoneTab.HOME && state.route == null) { actions.selectTab(PhoneTab.HOME) }
        NavItem(Icons.Outlined.Map, "지도", state.tab == PhoneTab.MAP && state.route == null) { actions.selectTab(PhoneTab.MAP) }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Tab) { actions.selectTab(PhoneTab.SHARE) }
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(vertical = 2.dp)
                    .size(width = 48.dp, height = 34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Palette.TextPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = Palette.Background, modifier = Modifier.size(24.dp))
            }
            Text("장소 공유", style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
        }
        NavItem(Icons.Outlined.DirectionsCar, "차량", state.tab == PhoneTab.CAR && state.route == null) { actions.selectTab(PhoneTab.CAR) }
        NavItem(Icons.Outlined.Person, "마이", state.tab == PhoneTab.MY && state.route == null) { actions.selectTab(PhoneTab.MY) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Palette.TextPrimary else Palette.TextTertiary, modifier = Modifier.size(26.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) Palette.TextPrimary else Palette.TextSecondary)
    }
}

@Composable
private fun Toast(state: PhoneUiState, actions: PhoneViewModel, modifier: Modifier = Modifier) {
    val toast = state.toast
    var lastText by remember { mutableStateOf("") }
    if (toast != null) lastText = toast.text
    LaunchedEffect(toast?.id) {
        if (toast != null) {
            delay(2_600)
            actions.consumeToast(toast.id)
        }
    }
    AnimatedVisibility(visible = toast != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Palette.SurfaceHigh)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Palette.Success, modifier = Modifier.size(22.dp))
            Text(lastText, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
        }
    }
}
