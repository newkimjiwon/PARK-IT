package com.nangmanroad.vehicle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nangmanroad.core.data.AlbumTab
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.data.DefaultLocation
import com.nangmanroad.core.data.FeedSort
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.SampleSpots
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.UserStore
import com.nangmanroad.vehicle.pleos.ActiveDestination
import com.nangmanroad.vehicle.pleos.DrivingState
import com.nangmanroad.vehicle.pleos.DrivingStateMonitor
import com.nangmanroad.vehicle.pleos.NaviEvent
import com.nangmanroad.vehicle.pleos.NavigationBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Map : Screen
    data object Album : Screen
    data class Detail(val spotId: String, val returnTo: Screen) : Screen
}

data class ToastMessage(val text: String, val id: Long = System.nanoTime())

data class AppUiState(
    val spots: List<Spot> = SampleSpots.all,
    val screen: Screen = Screen.Map,
    val selectedSpotId: String? = SampleSpots.all.first().id,
    val category: Category? = null,
    val query: String = "",
    val feedSort: FeedSort = FeedSort.POPULAR,
    val albumTab: AlbumTab = AlbumTab.RECOMMENDED,
    val visibleSpotIds: Set<String>? = null,
    val saved: Set<String> = emptySet(),
    val liked: Set<String> = emptySet(),
    val following: Set<String> = emptySet(),
    val helpfulTips: Set<String> = emptySet(),
    val phoneOutbox: Set<String> = emptySet(),
    val here: LatLng = DefaultLocation,
    val hereFromNavigation: Boolean = false,
    val drivingState: DrivingState = DrivingState.UNKNOWN,
    val drivingPreview: Boolean = false,
    val naviConnected: Boolean = false,
    val destination: ActiveDestination? = null,
    val routedSpotId: String? = null,
    val toast: ToastMessage? = null,
) {
    /** Driving hides photos, feeds and long text; the preview lets us demo it while parked. */
    val showDrivingView: Boolean get() = drivingState == DrivingState.DRIVING || drivingPreview

    fun spot(id: String?): Spot? = id?.let { wanted -> spots.firstOrNull { it.id == wanted } }

    fun likeCount(spot: Spot): Int = spot.likeCount + if (spot.id in liked) 1 else 0

    fun saveCount(spot: Spot): Int = spot.saveCount + if (spot.id in saved) 1 else 0
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val store = UserStore(application)
    private val driving = DrivingStateMonitor(application, viewModelScope)
    private val navigation = NavigationBridge(application)

    private val _state = MutableStateFlow(
        AppUiState(
            saved = store.savedSpots,
            liked = store.likedSpots,
            following = store.followedAuthors,
            helpfulTips = store.helpfulTips,
            phoneOutbox = store.phoneOutbox,
        ),
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            driving.state.collect { drivingState -> _state.update { it.copy(drivingState = drivingState) } }
        }
        viewModelScope.launch {
            navigation.connected.collect { connected -> _state.update { it.copy(naviConnected = connected) } }
        }
        viewModelScope.launch {
            navigation.location.collect { location ->
                if (location != null) _state.update { it.copy(here = location, hereFromNavigation = true) }
            }
        }
        viewModelScope.launch {
            navigation.destination.collect { destination -> _state.update { it.copy(destination = destination) } }
        }
        viewModelScope.launch {
            navigation.events.collect { event ->
                when (event) {
                    is NaviEvent.RouteStarted -> toast("차량 내비게이션으로 목적지를 보냈어요")
                    is NaviEvent.Failed -> toast(event.reason)
                    NaviEvent.Arrived -> toast("목적지에 도착했어요")
                }
            }
        }
        viewModelScope.launch {
            // Keep the remaining distance and time on the driving screen current.
            while (true) {
                delay(DESTINATION_REFRESH_MS)
                val current = _state.value
                if (current.showDrivingView && current.destination != null) navigation.refresh()
            }
        }
        driving.start()
        navigation.start()
    }

    fun openSpot(spotId: String) = _state.update {
        it.copy(screen = Screen.Detail(spotId, returnTo = it.screen.base()), selectedSpotId = spotId)
    }

    fun selectSpot(spotId: String) = _state.update { it.copy(selectedSpotId = spotId) }

    fun showMap() = _state.update { it.copy(screen = Screen.Map) }

    fun showAlbum(tab: AlbumTab? = null) = _state.update {
        it.copy(screen = Screen.Album, albumTab = tab ?: it.albumTab)
    }

    fun back() = _state.update { current ->
        val screen = current.screen
        current.copy(screen = if (screen is Screen.Detail) screen.returnTo else Screen.Map)
    }

    fun setCategory(category: Category?) = _state.update { it.copy(category = category) }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    fun setFeedSort(sort: FeedSort) = _state.update { it.copy(feedSort = sort) }

    fun setAlbumTab(tab: AlbumTab) = _state.update { it.copy(albumTab = tab) }

    fun onVisibleSpotsChanged(ids: Set<String>) = _state.update {
        if (it.visibleSpotIds == ids) it else it.copy(visibleSpotIds = ids)
    }

    fun toggleSave(spotId: String) {
        val nowSaved = spotId !in _state.value.saved
        _state.update { it.copy(saved = it.saved.toggle(spotId)) }
        store.savedSpots = _state.value.saved
        toast(if (nowSaved) "저장한 장소에 담았어요" else "저장을 취소했어요")
    }

    fun toggleLike(spotId: String) {
        _state.update { it.copy(liked = it.liked.toggle(spotId)) }
        store.likedSpots = _state.value.liked
    }

    fun toggleFollow(authorId: String) {
        val nowFollowing = authorId !in _state.value.following
        _state.update { it.copy(following = it.following.toggle(authorId)) }
        store.followedAuthors = _state.value.following
        toast(if (nowFollowing) "팔로우했어요" else "팔로우를 취소했어요")
    }

    fun toggleHelpful(tipId: String) {
        _state.update { it.copy(helpfulTips = it.helpfulTips.toggle(tipId)) }
        store.helpfulTips = _state.value.helpfulTips
    }

    /** Queues an item for the phone app; the queue is synced once accounts exist. */
    fun sendToPhone(itemId: String, what: String) {
        _state.update { it.copy(phoneOutbox = it.phoneOutbox + itemId) }
        store.phoneOutbox = _state.value.phoneOutbox
        toast("$what 휴대폰에서 볼 수 있게 담아 두었어요")
    }

    fun startRoute(spotId: String) {
        val spot = _state.value.spot(spotId) ?: return
        _state.update { it.copy(routedSpotId = spotId) }
        if (!navigation.requestRoute(spot)) {
            toast("차량 내비게이션에 연결할 수 없어요")
        }
    }

    fun setDrivingPreview(enabled: Boolean) = _state.update { it.copy(drivingPreview = enabled) }

    fun toast(text: String) = _state.update { it.copy(toast = ToastMessage(text)) }

    fun consumeToast(id: Long) = _state.update { if (it.toast?.id == id) it.copy(toast = null) else it }

    override fun onCleared() {
        driving.stop()
        navigation.stop()
    }

    private fun Screen.base(): Screen = if (this is Screen.Detail) returnTo else this

    private fun Set<String>.toggle(id: String) = if (id in this) this - id else this + id

    private companion object {
        const val DESTINATION_REFRESH_MS = 15_000L
    }
}
