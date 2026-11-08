package com.nangmanroad.vehicle.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
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
import com.nangmanroad.vehicle.AppUiState
import com.nangmanroad.vehicle.AppViewModel
import com.nangmanroad.core.data.AlbumTab
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.SpotQueries
import com.nangmanroad.core.data.SunMoment
import com.nangmanroad.core.data.SunPlan
import com.nangmanroad.core.data.distanceKm
import com.nangmanroad.core.data.formatKm
import com.nangmanroad.core.data.sunPlan
import com.nangmanroad.vehicle.ui.theme.Dimens
import com.nangmanroad.core.ui.tabular
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
        // Saved sunset and sunrise places answer "can I still make it today?" before anything else.
        val plans = if (state.albumTab == AlbumTab.SAVED) spots.mapNotNull { spot -> sunPlan(spot, state.here)?.let { spot to it } } else emptyList()
        if (plans.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                SunPlanStrip(plans.sortedWith(compareByDescending<Pair<Spot, SunPlan>> { it.second.makesIt }.thenBy { it.second.minutesToSpare }), state, actions)
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

/**
 * One card per saved place that lives by the sun, makeable ones first: the title, whether leaving
 * now gets there in time, and the distance the estimate rests on. Tapping opens the place.
 */
@Composable
private fun SunPlanStrip(plans: List<Pair<Spot, SunPlan>>, state: AppUiState, actions: AppViewModel) {
    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("오늘 해를 보러 간다면", style = MaterialTheme.typography.headlineSmall, color = Palette.TextPrimary)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            plans.forEach { (spot, plan) ->
                val tone = if (plan.makesIt) Palette.Warning else Palette.TextTertiary
                Row(
                    modifier = Modifier
                        .widthIn(min = 420.dp, max = 560.dp)
                        .heightIn(min = Dimens.TouchTarget)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Palette.Surface)
                        .clickable(role = Role.Button, onClickLabel = "${spot.title} 보기") { actions.openSpot(spot.id) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        if (plan.moment == SunMoment.SUNSET) Icons.Outlined.WbTwilight else Icons.Outlined.WbSunny,
                        contentDescription = null,
                        tint = tone,
                        modifier = Modifier.size(36.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(spot.title, style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(plan.verdict(), style = MaterialTheme.typography.bodyLarge.tabular(), color = if (plan.makesIt) Palette.TextPrimary else Palette.TextSecondary)
                        Text(
                            "직선 ${formatKm(distanceKm(state.here, spot.location))} · 시속 60km 기준",
                            style = MaterialTheme.typography.labelMedium.tabular(),
                            color = Palette.TextTertiary,
                        )
                    }
                }
            }
        }
    }
}
