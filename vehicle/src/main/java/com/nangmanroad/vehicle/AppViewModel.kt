package com.nangmanroad.vehicle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nangmanroad.core.data.AlbumTab
import com.nangmanroad.core.data.Author
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.data.DefaultLocation
import com.nangmanroad.core.data.FeedSort
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.Product
import com.nangmanroad.core.data.SampleSpots
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.sync.PairingCode
import com.nangmanroad.core.sync.PairingState
import com.nangmanroad.core.sync.Session
import com.nangmanroad.core.sync.SpotRepository
import com.nangmanroad.core.sync.UserList
import com.nangmanroad.core.sync.UserState
import com.nangmanroad.core.sync.list
import com.nangmanroad.vehicle.pleos.ActiveDestination
import com.nangmanroad.vehicle.pleos.DrivingState
import com.nangmanroad.vehicle.pleos.DrivingStateMonitor
import com.nangmanroad.vehicle.pleos.NaviEvent
import com.nangmanroad.vehicle.pleos.NavigationBridge
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Map : Screen
    data object Album : Screen
    data object Account : Screen
    data class Detail(val spotId: String, val returnTo: Screen) : Screen
}

data class ToastMessage(val text: String, val id: Long = System.nanoTime())

/** What the car hands to the phone, and how it is named on both screens. */
enum class PhoneHandoff(val label: String, val toastObject: String) {
    SPOT("장소", "이 장소를"),
    PRODUCT("상품", "상품 링크를"),
    TIP("꿀팁 제보", "꿀팁 제보 화면을"),
    COMMENTS("댓글 쓰기", "댓글을"),
}

/** The 6-digit code shown while the car waits to be linked to a phone account. */
data class PairingUi(val code: PairingCode? = null, val error: String? = null)

data class AppUiState(
    val user: UserState,
    val spots: List<Spot> = SampleSpots.all,
    val session: Session? = null,
    val online: Boolean? = null,
    val serverUrl: String = "",
    val pairing: PairingUi? = null,
    val screen: Screen = Screen.Map,
    val selectedSpotId: String? = SampleSpots.all.first().id,
    val category: Category? = null,
    val query: String = "",
    val feedSort: FeedSort = FeedSort.POPULAR,
    val albumTab: AlbumTab = AlbumTab.RECOMMENDED,
    val visibleSpotIds: Set<String>? = null,
    val here: LatLng = DefaultLocation,
    val hereFromNavigation: Boolean = false,
    val drivingState: DrivingState = DrivingState.UNKNOWN,
    val drivingPreview: Boolean = false,
    val naviConnected: Boolean = false,
    val destination: ActiveDestination? = null,
    val routedSpotId: String? = null,
    val toast: ToastMessage? = null,
) {
    val saved: Set<String> get() = user.saved
    val liked: Set<String> get() = user.liked
    val following: Set<String> get() = user.following
    val helpfulTips: Set<String> get() = user.helpfulTips

    /** Spot ids the phone sent to this car, newest first. */
    val fromPhone: List<String> get() = user.carQueue.mapNotNull { it.spotId }.distinct()

    /** Driving hides photos, feeds and long text; the preview lets us demo it while parked. */
    val showDrivingView: Boolean get() = drivingState == DrivingState.DRIVING || drivingPreview

    fun spot(id: String?): Spot? = id?.let { wanted -> spots.firstOrNull { it.id == wanted } }

    fun likeCount(spot: Spot): Int = spot.likeCount + if (spot.id in liked) 1 else 0

    fun saveCount(spot: Spot): Int = spot.saveCount + if (spot.id in saved) 1 else 0
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SpotRepository(application, localUser = Author("u-this-car", "이 차량", ""))
    private val driving = DrivingStateMonitor(application, viewModelScope)
    private val navigation = NavigationBridge(application)
    private var pairingJob: Job? = null

    private val _state = MutableStateFlow(
        AppUiState(
            user = repository.state.value,
            spots = repository.spots.value,
            session = repository.session.value,
            serverUrl = repository.serverUrl,
        ),
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repository.state.collect { user -> _state.update { it.copy(user = user) } } }
        viewModelScope.launch { repository.spots.collect { spots -> _state.update { it.copy(spots = spots) } } }
        viewModelScope.launch { repository.session.collect { session -> _state.update { it.copy(session = session) } } }
        viewModelScope.launch { repository.online.collect { online -> _state.update { it.copy(online = online) } } }
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
        viewModelScope.launch {
            // Pick up what the phone saved or sent while parked; driving shows no feed to update.
            repository.refresh()
            while (true) {
                delay(SYNC_REFRESH_MS)
                val current = _state.value
                if (current.session != null && !current.showDrivingView) {
                    val before = current.fromPhone
                    repository.refresh()
                    val arrived = _state.value.fromPhone.firstOrNull { it !in before }
                    arrived?.let { id -> _state.value.spot(id)?.let { toast("휴대폰에서 '${it.title}'을(를) 보냈어요") } }
                }
            }
        }
        driving.start()
        navigation.start()
    }

    fun openSpot(spotId: String) = _state.update {
        it.copy(screen = Screen.Detail(spotId, returnTo = it.screen.base()), selectedSpotId = spotId)
    }

    fun selectSpot(spotId: String) = _state.update { it.copy(selectedSpotId = spotId) }

    fun showMap() = leaveAccount { it.copy(screen = Screen.Map) }

    fun showAlbum(tab: AlbumTab? = null) = leaveAccount {
        it.copy(screen = Screen.Album, albumTab = tab ?: it.albumTab)
    }

    fun back() = leaveAccount { current ->
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

    // Account and pairing

    fun openAccount() {
        _state.update { it.copy(screen = Screen.Account) }
        if (_state.value.session == null) startPairing()
    }

    /** Shows a fresh code and waits for the phone to claim it; a code that expires is replaced. */
    fun startPairing() {
        pairingJob?.cancel()
        pairingJob = viewModelScope.launch {
            _state.update { it.copy(pairing = PairingUi()) }
            val code = repository.startPairing().getOrElse { error ->
                _state.update { it.copy(pairing = PairingUi(error = error.message)) }
                return@launch
            }
            _state.update { it.copy(pairing = PairingUi(code)) }
            while (isActive) {
                delay(PAIRING_POLL_MS)
                val status = repository.pairingStatus(code.pairingId).getOrNull() ?: continue
                when (status.state) {
                    PairingState.LINKED -> {
                        _state.update { it.copy(pairing = null) }
                        toast("${status.session?.user?.name} 계정에 연결했어요")
                        return@launch
                    }
                    PairingState.EXPIRED -> {
                        startPairing()
                        return@launch
                    }
                    PairingState.WAITING -> Unit
                }
            }
        }
    }

    fun unlinkAccount() {
        viewModelScope.launch {
            repository.signOut()
            toast("계정 연결을 해제했어요")
            startPairing()
        }
    }

    private fun leaveAccount(transform: (AppUiState) -> AppUiState) {
        if (_state.value.screen == Screen.Account) {
            pairingJob?.cancel()
            _state.update { it.copy(pairing = null) }
        }
        _state.update(transform)
    }

    // Lists

    fun toggleSave(spotId: String) = toggle(UserList.SAVED, spotId, "저장한 장소에 담았어요", "저장을 취소했어요")

    fun toggleLike(spotId: String) = toggle(UserList.LIKED, spotId)

    fun toggleFollow(authorId: String) = toggle(UserList.FOLLOWING, authorId, "팔로우했어요", "팔로우를 취소했어요")

    fun toggleHelpful(tipId: String) = toggle(UserList.HELPFUL_TIPS, tipId)

    private fun toggle(list: UserList, id: String, onMessage: String? = null, offMessage: String? = null) {
        val on = id !in _state.value.user.list(list)
        viewModelScope.launch {
            repository.toggle(list, id, on)
                .onSuccess { (if (on) onMessage else offMessage)?.let(::toast) }
                .onFailure { toast(it.message ?: "요청을 처리하지 못했어요") }
        }
    }

    /** Hands a spot, product or follow-up action to the phone app on the same account. */
    fun sendToPhone(kind: PhoneHandoff, spot: Spot, product: Product? = null) {
        val label = when (kind) {
            PhoneHandoff.SPOT -> spot.title
            PhoneHandoff.PRODUCT -> "${product?.name} · ${spot.title}"
            else -> "${kind.label} · ${spot.title}"
        }
        viewModelScope.launch {
            repository.sendToPhone("${kind.name.lowercase()}:${product?.id ?: spot.id}", label, spot.id)
                .onSuccess {
                    toast(
                        if (_state.value.session != null) "${kind.toastObject} 휴대폰으로 보냈어요"
                        else "${kind.toastObject} 담아 두었어요. 계정을 연결하면 휴대폰으로 보낼 수 있어요",
                    )
                }
                .onFailure { toast(it.message ?: "휴대폰으로 보내지 못했어요") }
        }
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

    private fun Screen.base(): Screen = when (this) {
        is Screen.Detail -> returnTo
        Screen.Account -> Screen.Map
        else -> this
    }

    private companion object {
        const val DESTINATION_REFRESH_MS = 15_000L
        const val SYNC_REFRESH_MS = 10_000L
        const val PAIRING_POLL_MS = 2_000L
    }
}
