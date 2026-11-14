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
import com.nangmanroad.core.data.SpotQueries
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.Regions
import com.nangmanroad.core.data.TipKind
import com.nangmanroad.core.data.Reach
import com.nangmanroad.core.data.PlaceQuestion
import com.nangmanroad.core.data.answerAboutPlace
import com.nangmanroad.core.data.placeQuestions
import com.nangmanroad.core.data.reach
import com.nangmanroad.core.data.reachForRoadDistance
import com.nangmanroad.core.data.usableRangeKm
import com.nangmanroad.core.sync.Limits
import com.nangmanroad.core.data.distanceKm
import com.nangmanroad.core.sync.PARKIT_PREFIX
import com.nangmanroad.core.sync.Api
import com.nangmanroad.core.sync.CarTransferProgress
import com.nangmanroad.core.sync.CarTransferReceipt
import com.nangmanroad.core.sync.SendRequest
import com.nangmanroad.core.sync.PairingCode
import com.nangmanroad.core.sync.PairingState
import com.nangmanroad.core.sync.Session
import com.nangmanroad.core.sync.SpotRepository
import com.nangmanroad.core.sync.UserList
import com.nangmanroad.core.sync.UserState
import com.nangmanroad.core.sync.list
import com.nangmanroad.vehicle.pleos.ActiveDestination
import com.nangmanroad.vehicle.pleos.DrivingState
import com.nangmanroad.vehicle.pleos.EvBatteryStatus
import com.nangmanroad.vehicle.pleos.DrivingStateMonitor
import com.nangmanroad.vehicle.pleos.NaviEvent
import com.nangmanroad.vehicle.pleos.NavigationBridge
import com.nangmanroad.vehicle.pleos.Heard
import com.nangmanroad.vehicle.pleos.VoiceGuide
import com.nangmanroad.vehicle.pleos.VoiceInput
import com.nangmanroad.vehicle.voice.COMMAND_HELP
import com.nangmanroad.vehicle.voice.NO_DESTINATION
import com.nangmanroad.vehicle.voice.TripFacts
import com.nangmanroad.vehicle.voice.VoiceCommand
import com.nangmanroad.vehicle.voice.arrivalReminder
import com.nangmanroad.vehicle.voice.batteryReadout
import com.nangmanroad.vehicle.voice.carCampingReadout
import com.nangmanroad.vehicle.voice.departureBriefing
import com.nangmanroad.vehicle.voice.etaReadout
import com.nangmanroad.vehicle.voice.parkitPrompt
import com.nangmanroad.vehicle.voice.parseCommand
import com.nangmanroad.vehicle.voice.phoneSentAnnouncement
import com.nangmanroad.vehicle.voice.searchQueryFromSpeech
import com.nangmanroad.vehicle.voice.sunReadout
import com.nangmanroad.vehicle.voice.tipsReadout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface Screen {
    data object Map : Screen
    data object Album : Screen
    data object Account : Screen
    data class Detail(val spotId: String, val returnTo: Screen) : Screen
}

/** [openSpotId] turns the toast into "open this place": used when the phone sends one over. */
data class ToastMessage(val text: String, val id: Long = System.nanoTime(), val openSpotId: String? = null)

/** What the car hands to the phone, and how it is named on both screens. */
enum class PhoneHandoff(val label: String, val toastObject: String) {
    SPOT("장소", "이 장소를"),
    PRODUCT("상품", "상품 링크를"),
    TIP("꿀팁 제보", "꿀팁 제보 화면을"),
    COMMENTS("댓글 쓰기", "댓글을"),
}

/** The 6-digit code shown while the car waits to be linked to a phone account. */
data class PairingUi(val code: PairingCode? = null, val error: String? = null)

/**
 * Where the car just stopped, offered as a "파킷": a new place to post from the phone, or, when it is
 * [nearSpotId], a visit to a known spot whose tips the phone can confirm.
 */
data class ParkitSuggestion(
    val location: LatLng,
    val at: Long,
    val nearSpotId: String?,
    /** A line the driver said into the microphone; it travels with the 파킷 and opens the phone's story. */
    val note: String = "",
)

/** Why the microphone is open, so the right screen shows what it hears. */
enum class ListenPurpose { SEARCH, COMMAND, PARKIT_NOTE }

/** The Pleos speech services as the screens need them. */
data class VoiceUi(
    /** Spoken guidance on departure, near arrival and when the phone sends a place. Asking always answers. */
    val guidance: Boolean = true,
    val canSpeak: Boolean = false,
    val canListen: Boolean = false,
    /** Why the car cannot speak or listen even though the Pleos speech service answered, e.g. no on-device model. */
    val problem: String? = null,
    val speaking: Boolean = false,
    val listening: ListenPurpose? = null,
    /** Words heard so far in the current turn, or the last turn's words until the next one starts. */
    val heard: String = "",
    /** Which microphone [heard] came from, so the driving screen never shows a map search. */
    val heardFor: ListenPurpose? = null,
)

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
    /** Facilities a place must have (visitor-confirmed) to show on the map and feed. */
    val facilities: Set<TipKind> = emptySet(),
    /** Keep only places the current battery can reach and return from; ignored when the pack is unknown. */
    val onlyRoundTrip: Boolean = false,
    val query: String = "",
    val feedSort: FeedSort = FeedSort.POPULAR,
    val albumTab: AlbumTab = AlbumTab.SAVED,
    val visibleSpotIds: Set<String>? = null,
    val here: LatLng = DefaultLocation,
    val hereFromNavigation: Boolean = false,
    val drivingState: DrivingState = DrivingState.UNKNOWN,
    /** Traction battery from the Vehicle SDK; null on a car (or emulator) that does not report one. */
    val battery: EvBatteryStatus? = null,
    val drivingPreview: Boolean = false,
    val naviConnected: Boolean = false,
    val destination: ActiveDestination? = null,
    val routedSpotId: String? = null,
    val parkit: ParkitSuggestion? = null,
    /** Bumped when a 파킷 goes out, so the map plays the pin drop at the car's position. */
    val parkitPulse: Long? = null,
    /** Send only the city/county centre with a 파킷, so a stop in front of home never pins the house. */
    val approximateParkit: Boolean = false,
    val toast: ToastMessage? = null,
    val voice: VoiceUi = VoiceUi(),
) {
    val saved: Set<String> get() = user.saved
    val liked: Set<String> get() = user.liked
    val following: Set<String> get() = user.following
    val helpfulTips: Set<String> get() = user.helpfulTips
    val visited: Set<String> get() = user.visited

    /** Spot ids the phone sent to this car, newest first. */
    val fromPhone: List<String> get() = user.carQueue.mapNotNull { it.spotId }.distinct()

    /** Kilometres the pack can still cover with a reserve kept back, or null when unknown. */
    val usableRangeKm: Double? get() = battery?.let { usableRangeKm(it.percent, it.capacityKwh) }

    fun remainingRouteKm(spot: Spot): Double? = destination
        ?.takeIf { it.name == spot.title && it.remainMeters > 0 && (routedSpotId == null || routedSpotId == spot.id) }
        ?.let { it.remainMeters / 1000.0 }

    fun tripReach(spot: Spot): Reach? = usableRangeKm?.let { range ->
        val roadKm = remainingRouteKm(spot)
        when {
            roadKm != null -> reachForRoadDistance(roadKm, range)
            hereFromNavigation -> reach(distanceKm(here, spot.location), range)
            else -> null
        }
    }

    /** Driving hides photos, feeds and long text; the preview lets us demo it while parked. */
    val showDrivingView: Boolean get() = drivingState == DrivingState.DRIVING || drivingPreview

    fun spot(id: String?): Spot? = id?.let { wanted -> spots.firstOrNull { it.id == wanted } }

    /** The known place the car is heading to; after a restart only the navigation remembers it, so match by name. */
    val routedSpot: Spot? get() = spot(routedSpotId) ?: spots.firstOrNull { it.title == destination?.name }

    fun likeCount(spot: Spot): Int = spot.likeCount + if (spot.id in liked) 1 else 0

    fun saveCount(spot: Spot): Int = spot.saveCount + if (spot.id in saved) 1 else 0
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private data class PendingGuidance(val session: Session, val title: String, val receipt: CarTransferReceipt)

    private var pendingGuidance: PendingGuidance? = null
    private val confirmedGuidance = mutableMapOf<CarTransferReceipt, Session>()
    private val repository = SpotRepository(application, localUser = Author("u-this-car", "이 차량", ""))
    private val driving = DrivingStateMonitor(application, viewModelScope)
    private val navigation = NavigationBridge(application)
    private val voiceGuide = VoiceGuide(application)
    private val voiceInput = VoiceInput(application, viewModelScope)
    private val settings = application.getSharedPreferences("vehicle_settings", Application.MODE_PRIVATE)
    private val parkitOutbox = application.getSharedPreferences("parkit_outbox", Application.MODE_PRIVATE)
    private val parkitSendLock = Mutex()
    private var pairingJob: Job? = null
    private var briefingJob: Job? = null
    /** What "다시 듣기" repeats: the last departure briefing or arrival reminder. */
    private var lastBriefing: String? = null
    /** The destination whose one-kilometre reminder has been read, so it is read once per trip. */
    private var remindedFor: String? = null

    private val _state = MutableStateFlow(
        AppUiState(
            user = repository.state.value,
            spots = repository.spots.value,
            session = repository.session.value,
            serverUrl = repository.serverUrl,
            approximateParkit = settings.getBoolean(KEY_APPROXIMATE_PARKIT, false),
            voice = VoiceUi(guidance = settings.getBoolean(KEY_VOICE_GUIDANCE, true)),
        ),
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repository.state.collect { user -> _state.update { it.copy(user = user) } } }
        viewModelScope.launch { repository.spots.collect { spots -> _state.update { it.copy(spots = spots) } } }
        viewModelScope.launch { repository.session.collect { session -> _state.update { it.copy(session = session) } } }
        viewModelScope.launch { repository.online.collect { online -> _state.update { it.copy(online = online) } } }
        viewModelScope.launch {
            var previous = DrivingState.UNKNOWN
            driving.state.collect { drivingState ->
                // Moving off drops a pending suggestion; stopping after a drive is the moment to 파킷.
                _state.update { it.copy(drivingState = drivingState, parkit = if (drivingState == DrivingState.DRIVING) null else it.parkit) }
                if (previous == DrivingState.DRIVING && drivingState == DrivingState.PARKED) suggestParkit()
                if (previous != DrivingState.DRIVING && drivingState == DrivingState.DRIVING) {
                    // A note half-said at the 파킷 banner has nowhere to go once the car moves.
                    if (_state.value.voice.listening == ListenPurpose.PARKIT_NOTE) voiceInput.cancel()
                    briefOnDeparture()
                }
                previous = drivingState
            }
        }
        viewModelScope.launch { driving.battery.collect { battery -> _state.update { it.copy(battery = battery) } } }
        viewModelScope.launch {
            navigation.connected.collect { connected -> _state.update { it.copy(naviConnected = connected) } }
        }
        viewModelScope.launch {
            navigation.location.collect { location ->
                if (location != null) _state.update { it.copy(here = location, hereFromNavigation = true) }
            }
        }
        viewModelScope.launch {
            navigation.destination.collect { destination ->
                _state.update { it.copy(destination = destination) }
                remindNearArrival()
            }
        }
        viewModelScope.launch {
            navigation.events.collect { event ->
                when (event) {
                    NaviEvent.RouteRequested -> toast("차량 내비게이션으로 목적지를 보냈어요")
                    is NaviEvent.RouteStarted -> {
                        val request = pendingGuidance
                        if (request != null && request.title == event.name && repository.session.value == request.session) {
                            confirmedGuidance[request.receipt] = request.session
                            pendingGuidance = null
                            sendCarReceipts(request.session, includeReceived = false)
                        }
                    }
                    is NaviEvent.Failed -> {
                        pendingGuidance = null
                        toast(event.reason)
                    }
                    NaviEvent.Arrived -> {
                        toast("목적지에 도착했어요")
                        // The spot we routed to is the one we arrived at, however far the fix says.
                        suggestParkit(nearSpotId = _state.value.routedSpotId)
                    }
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
                    if (repository.refresh().isSuccess) {
                        retryParkits(current.session.user.id)
                        sendCarReceipts(current.session, includeReceived = true)
                    }
                    val arrived = _state.value.fromPhone.firstOrNull { it !in before }
                    arrived?.let { id ->
                        _state.value.spot(id)?.let {
                            toast("휴대폰에서 '${it.title}'을(를) 받았어요", openSpotId = id)
                            announce(phoneSentAnnouncement(it.title))
                        }
                    }
                }
            }
        }
        viewModelScope.launch { voiceGuide.ready.collect { ready -> updateVoice { it.copy(canSpeak = ready) } } }
        viewModelScope.launch { voiceGuide.speaking.collect { speaking -> updateVoice { it.copy(speaking = speaking) } } }
        viewModelScope.launch { voiceInput.ready.collect { ready -> updateVoice { it.copy(canListen = ready) } } }
        viewModelScope.launch {
            combine(voiceGuide.problem, voiceInput.problem) { speak, listen -> listOfNotNull(speak, listen) }.collect { problems ->
                if (problems.isEmpty()) return@collect
                // Say once why the speaker or microphone buttons just went away.
                val message = if (problems.size == 2) "이 차량에는 Pleos 음성 모델이 없어 음성 안내와 인식을 쓸 수 없어요" else problems.single()
                updateVoice { it.copy(problem = message) }
                toast(message)
            }
        }
        viewModelScope.launch {
            voiceInput.partial.collect { words -> if (_state.value.voice.listening != null) updateVoice { it.copy(heard = words) } }
        }
        viewModelScope.launch {
            voiceInput.listening.collect { on -> if (!on) updateVoice { it.copy(listening = null) } }
        }
        driving.start()
        navigation.start()
        voiceGuide.start()
        voiceInput.start()
    }

    // Leaving the map (or pressing 지도 again) closes the 파킷 banner, so the rail's highlight and the
    // screen never disagree about what is open.

    fun openSpot(spotId: String) {
        dismissParkit()
        _state.update { it.copy(screen = Screen.Detail(spotId, returnTo = it.screen.base()), selectedSpotId = spotId) }
    }

    fun selectSpot(spotId: String) = _state.update { it.copy(selectedSpotId = spotId) }

    fun showMap() {
        dismissParkit()
        leaveAccount { it.copy(screen = Screen.Map) }
    }

    /** The rail's 파킷하기 button: the map with the offer for where the car is now. */
    fun openParkit() {
        leaveAccount { it.copy(screen = Screen.Map) }
        suggestParkit(manual = true)
    }

    fun showAlbum(tab: AlbumTab? = null) {
        dismissParkit()
        leaveAccount { it.copy(screen = Screen.Album, albumTab = tab ?: it.albumTab) }
    }

    /**
     * The map's crosshair button. The map moves to the car by itself once the navigation has a fix;
     * without one "here" is only a default, so say so instead of jumping somewhere else.
     */
    fun myLocationPressed() {
        if (!_state.value.hereFromNavigation) toast("차량 내비게이션에 연결되지 않아 현재 위치를 알 수 없어요. 연결되면 다시 눌러 주세요")
    }

    fun back() = leaveAccount { current ->
        val screen = current.screen
        current.copy(screen = if (screen is Screen.Detail) screen.returnTo else Screen.Map)
    }

    fun setCategory(category: Category?) = _state.update { it.copy(category = category) }

    fun toggleOnlyRoundTrip() = _state.update { it.copy(onlyRoundTrip = !it.onlyRoundTrip) }

    fun toggleFacility(kind: TipKind) = _state.update {
        it.copy(facilities = if (kind in it.facilities) it.facilities - kind else it.facilities + kind)
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    fun setFeedSort(sort: FeedSort) = _state.update { it.copy(feedSort = sort) }

    fun setAlbumTab(tab: AlbumTab) = _state.update { it.copy(albumTab = tab) }

    fun onVisibleSpotsChanged(ids: Set<String>) = _state.update {
        if (it.visibleSpotIds == ids) it else it.copy(visibleSpotIds = ids)
    }

    // Account and pairing

    fun openAccount() {
        dismissParkit()
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

    fun setApproximateParkit(on: Boolean) {
        settings.edit().putBoolean(KEY_APPROXIMATE_PARKIT, on).apply()
        _state.update { it.copy(approximateParkit = on) }
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
                        if (_state.value.session != null) "${kind.toastObject} 휴대폰으로 보냈어요. 휴대폰 알림에서 열 수 있어요"
                        else "전송 대기 · ${kind.toastObject} 담아 두었어요. 왼쪽 아래 '계정 연결'을 마치면 휴대폰으로 보내져요",
                    )
                }
                .onFailure { toast("휴대폰으로 보내지 못했어요. ${it.message ?: "연결 상태를 확인해 주세요"} · 다시 눌러 주세요") }
        }
    }

    // 파킷

    /**
     * Offers the current position as a 파킷. Without the navigation fix "here" is only Seoul City
     * Hall, so nothing is offered; the rail button says why. A known spot within [NEAR_SPOT_KM] turns
     * the offer into a visit.
     */
    fun suggestParkit(nearSpotId: String? = null, manual: Boolean = false) {
        val current = _state.value
        if (!current.hereFromNavigation) {
            if (manual) toast("차량 내비게이션에 연결되지 않아 지금 위치를 알 수 없어요")
            return
        }
        val near = nearSpotId ?: current.spots
            .minByOrNull { distanceKm(it.location, current.here) }
            ?.takeIf { distanceKm(it.location, current.here) <= NEAR_SPOT_KM }
            ?.id
        // The banner lives on the map, so an offer made on another screen brings the map forward.
        leaveAccount { it.copy(parkit = ParkitSuggestion(current.here, System.currentTimeMillis(), near), screen = Screen.Map) }
        if (!manual) announce(parkitPrompt(current.spot(near)?.title, canTakeNote = current.voice.canListen))
    }

    fun dismissParkit() {
        if (_state.value.voice.listening == ListenPurpose.PARKIT_NOTE) voiceInput.cancel()
        _state.update { it.copy(parkit = null) }
    }

    /**
     * Hands the parked position to the phone, where photos and tips turn it into a post. A known
     * spot is also marked visited, so the phone can ask whether its tips held up.
     */
    fun sendParkit() {
        val current = _state.value
        val suggestion = current.parkit ?: return
        val userId = current.session?.user?.id ?: return toast("휴대폰 계정을 연결한 뒤 파킷을 보내세요")
        val near = current.spot(suggestion.nearSpotId)
        val time = SimpleDateFormat("M월 d일 a h:mm", Locale.KOREA).format(Date(suggestion.at))
        val label = listOfNotNull("파킷", near?.title ?: nearestRegion(suggestion.location), time).joinToString(" · ")
        val location = if (current.approximateParkit) Regions.nearest(suggestion.location).center else suggestion.location
        val request = SendRequest("$PARKIT_PREFIX${suggestion.at}", label, near?.id, location, suggestion.note, current.approximateParkit)
        viewModelScope.launch {
            val key = "$userId/${request.id}"
            parkitSendLock.withLock {
                if (_state.value.parkit?.at != suggestion.at) return@withLock
                val stored = parkitOutbox.edit().putString(key, Api.json.encodeToString(SendRequest.serializer(), request)).commit()
                if (!stored) {
                    toast("파킷을 저장하지 못했어요. 다시 시도해 주세요")
                    return@withLock
                }
                _state.update { it.copy(parkit = null, parkitPulse = System.currentTimeMillis()) }
                if (repository.sendParkit(userId, request).isSuccess) {
                    parkitOutbox.edit().remove(key).apply()
                    toast("휴대폰으로 보냈어요")
                } else {
                    toast("전송 대기 중 · 정차 중 연결되면 다시 보낼게요")
                }
            }
        }
    }

    private suspend fun retryParkits(userId: String) = parkitSendLock.withLock {
        val pending = parkitOutbox.all.filterKeys { it.startsWith("$userId/") }
        var sent = false
        for ((key, value) in pending) {
            if (_state.value.showDrivingView || repository.session.value?.user?.id != userId) break
            val request = (value as? String)?.let {
                runCatching { Api.json.decodeFromString(SendRequest.serializer(), it) }.getOrNull()
            } ?: continue
            if (repository.sendParkit(userId, request).isFailure) break
            parkitOutbox.edit().remove(key).apply()
            sent = true
        }
        if (sent) toast("대기 중이던 파킷을 휴대폰으로 보냈어요")
    }

    /** Names the area by the closest known spot, when one is close enough for its region to apply. */
    private fun nearestRegion(at: LatLng): String? = _state.value.spots
        .minByOrNull { distanceKm(it.location, at) }
        ?.takeIf { distanceKm(it.location, at) <= SAME_AREA_KM }
        ?.let { "${it.region} 근처" }

    fun startRoute(spotId: String) {
        val spot = _state.value.spot(spotId) ?: return
        val session = repository.session.value
        val sent = repository.state.value.carQueue.firstOrNull { it.spotId == spotId }
        pendingGuidance = if (session != null && sent != null) {
            PendingGuidance(session, spot.title, CarTransferReceipt(sent.id, sent.sentAt, CarTransferProgress.GUIDANCE_STARTED))
        } else null
        _state.update { it.copy(routedSpotId = spotId) }
        if (!navigation.requestRoute(spot)) {
            pendingGuidance = null
            toast("차량 내비게이션에 연결할 수 없어요")
        }
    }

    private suspend fun sendCarReceipts(session: Session, includeReceived: Boolean) {
        if (repository.session.value != session) return
        confirmedGuidance.entries.removeAll { it.value != session }
        val guidance = confirmedGuidance.keys.toList()
        val received = if (includeReceived) repository.state.value.carQueue
            .filter { it.carProgress == CarTransferProgress.SENT }
            .map { CarTransferReceipt(it.id, it.sentAt, CarTransferProgress.RECEIVED) }
        else emptyList()
        if (repository.acknowledgeCarQueue(session, guidance + received).isSuccess) {
            guidance.forEach { confirmedGuidance.remove(it, session) }
        }
    }

    fun setDrivingPreview(enabled: Boolean) {
        _state.update { it.copy(drivingPreview = enabled) }
        // The preview stands in for pulling away, so it gets the same spoken briefing.
        if (enabled) briefOnDeparture() else if (_state.value.drivingState != DrivingState.DRIVING) quietDown()
    }

    // Voice

    fun setVoiceGuidance(on: Boolean) {
        settings.edit().putBoolean(KEY_VOICE_GUIDANCE, on).apply()
        updateVoice { it.copy(guidance = on) }
        if (!on) voiceGuide.stop()
    }

    /** The driving screen's speaker button: stop if talking, otherwise say the briefing again. */
    fun toggleBriefing() {
        if (_state.value.voice.speaking) {
            voiceGuide.stop()
            return
        }
        reply(lastBriefing ?: departureBriefing(tripFacts()) ?: NO_DESTINATION)
    }

    /** Microphone on the map's search field: the spoken words become the search. */
    fun listenForSearch() = listen(ListenPurpose.SEARCH) { heard ->
        val query = searchQueryFromSpeech(heard)
        if (query.isEmpty()) return@listen toast("무엇을 찾을지 다시 말해 주세요")
        _state.update { it.copy(query = query) }
        val current = _state.value
        val found = SpotQueries.matching(current.spots, current.category, query)
        if (found.isEmpty()) toast("'$query'에 맞는 장소가 없어요") else selectSpot(found.first().id)
    }

    /** Microphone on the driving screen: a few spoken questions, answered out loud. */
    fun listenForCommand() = listen(ListenPurpose.COMMAND) { heard ->
        val trip = tripFacts()
        val current = _state.value
        val topics = placeQuestions(heard)
        val command = parseCommand(heard)
        if (command != VoiceCommand.STOP && trip.spot != null && topics.isNotEmpty()) {
            reply(topics.take(if (current.showDrivingView) 1 else 2).joinToString(" ") { placeAnswer(trip.spot, it) })
            return@listen
        }
        when (command) {
            VoiceCommand.STOP -> voiceGuide.stop()
            VoiceCommand.REPEAT -> reply(lastBriefing ?: departureBriefing(trip) ?: NO_DESTINATION)
            VoiceCommand.TIPS -> reply(tipsReadout(trip.spot))
            VoiceCommand.BATTERY -> reply(batteryReadout(trip))
            VoiceCommand.SUN -> reply(sunReadout(trip))
            VoiceCommand.CAR_CAMPING -> reply(carCampingReadout(trip.spot))
            VoiceCommand.ETA -> reply(etaReadout(trip))
            VoiceCommand.UNKNOWN -> reply(COMMAND_HELP)
        }
    }

    fun placeAnswer(spot: Spot, question: PlaceQuestion): String {
        val current = _state.value
        return when (question) {
            PlaceQuestion.BATTERY -> if (current.showDrivingView) {
                batteryReadout(tripFacts())
            } else {
                val verdict = current.tripReach(spot)
                if (verdict == null) "차량의 배터리와 현재 위치를 확인하면 이동 가능 여부를 알 수 있어요."
                else {
                    val basis = if (current.remainingRouteKm(spot) != null) "남은 도로 경로" else "직선거리를 보정한 값"
                    "배터리 ${current.battery?.percent}% · ${verdict.label}. $basis 기준 추정이에요."
                }
            }
            PlaceQuestion.ETA -> if (current.remainingRouteKm(spot) == null) {
                "이 장소의 길 안내를 시작하면 도착 시간을 확인할 수 있어요."
            } else etaReadout(tripFacts())
            else -> requireNotNull(answerAboutPlace(question, spot))
        }
    }

    /** Microphone on the 파킷 banner: one spoken line to send along with the place. */
    fun listenForParkitNote() = listen(ListenPurpose.PARKIT_NOTE) { heard ->
        val note = heard.trim().take(Limits.NOTE)
        _state.update { current -> current.copy(parkit = current.parkit?.copy(note = note)) }
    }

    /** Tapping the microphone again ends the turn early with what was heard so far. */
    fun finishListening() = voiceInput.finishListening()

    private fun listen(purpose: ListenPurpose, onWords: (String) -> Unit) {
        if (_state.value.voice.listening == purpose) return finishListening()
        // The speakers would talk straight into the microphone.
        voiceGuide.stop()
        updateVoice { it.copy(listening = purpose, heard = "", heardFor = purpose) }
        voiceInput.listen { heard ->
            updateVoice { it.copy(listening = null) }
            when (heard) {
                is Heard.Words -> {
                    updateVoice { it.copy(heard = heard.text) }
                    onWords(heard.text)
                }
                Heard.Silence -> {
                    if (purpose == ListenPurpose.COMMAND) reply("잘 듣지 못했어요. $COMMAND_HELP") else toast("잘 듣지 못했어요. 다시 말해 주세요")
                }
                is Heard.Failed -> toast(heard.reason)
            }
        }
    }

    /**
     * Pulling away (or opening the driving preview): wait a moment for the navigation to report the
     * route, then read the driving screen out loud. Nothing is said without somewhere to go.
     */
    private fun briefOnDeparture() {
        briefingJob?.cancel()
        briefingJob = viewModelScope.launch {
            navigation.refresh()
            withTimeoutOrNull(ROUTE_WAIT_MS) { _state.first { it.destination != null } }
            val text = departureBriefing(tripFacts()) ?: return@launch
            lastBriefing = text
            remindedFor = null
            announce(text)
        }
    }

    /** About a kilometre out, read the tips again while there is still time to act on them. */
    private fun remindNearArrival() {
        val current = _state.value
        val destination = current.destination ?: return
        if (!current.showDrivingView || remindedFor == destination.name) return
        if (destination.remainMeters !in 1..ARRIVAL_REMINDER_METERS) return
        remindedFor = destination.name
        val text = arrivalReminder(tripFacts())
        lastBriefing = text
        announce(text)
    }

    private fun tripFacts(): TripFacts {
        val current = _state.value
        return TripFacts(
            spot = current.routedSpot,
            destinationName = current.destination?.name,
            remainMeters = current.destination?.remainMeters,
            remainSeconds = current.destination?.remainSeconds,
            batteryPercent = current.battery?.percent,
            capacityKwh = current.battery?.capacityKwh,
            here = current.here,
            now = System.currentTimeMillis(),
        )
    }

    /** Something the car volunteers; muted when the driver turned guidance off. */
    private fun announce(text: String) {
        if (_state.value.voice.guidance) voiceGuide.say(text)
    }

    /** An answer to something the driver asked for, so it is spoken whatever the guidance setting. */
    private fun reply(text: String) {
        voiceGuide.say(text)
        if (!_state.value.voice.canSpeak) toast(text)
    }

    private fun quietDown() {
        briefingJob?.cancel()
        voiceGuide.stop()
    }

    private fun updateVoice(transform: (VoiceUi) -> VoiceUi) = _state.update { it.copy(voice = transform(it.voice)) }

    fun toast(text: String, openSpotId: String? = null) = _state.update { it.copy(toast = ToastMessage(text, openSpotId = openSpotId)) }

    fun consumeToast(id: Long) = _state.update { if (it.toast?.id == id) it.copy(toast = null) else it }

    override fun onCleared() {
        driving.stop()
        navigation.stop()
        voiceInput.release()
        voiceGuide.release()
    }

    private fun Screen.base(): Screen = when (this) {
        is Screen.Detail -> returnTo
        Screen.Account -> Screen.Map
        else -> this
    }

    private companion object {
        const val KEY_APPROXIMATE_PARKIT = "approximate_parkit"
        const val KEY_VOICE_GUIDANCE = "voice_guidance"
        /** How long the departure briefing waits for the navigation to report the route. */
        const val ROUTE_WAIT_MS = 4_000L
        const val ARRIVAL_REMINDER_METERS = 1_000
        const val DESTINATION_REFRESH_MS = 15_000L
        const val SYNC_REFRESH_MS = 10_000L
        const val PAIRING_POLL_MS = 2_000L
        /** Stopping this close to a known spot counts as visiting it rather than finding a new one. */
        const val NEAR_SPOT_KM = 0.3
        /** Beyond this the closest spot's region says nothing about where the car is. */
        const val SAME_AREA_KM = 30.0
    }
}
