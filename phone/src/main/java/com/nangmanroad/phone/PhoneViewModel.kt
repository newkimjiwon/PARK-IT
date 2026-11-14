package com.nangmanroad.phone

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nangmanroad.core.data.Author
import com.nangmanroad.core.data.CarCamping
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.data.DefaultLocation
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.Product
import com.nangmanroad.core.data.SampleSpots
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.TipKind
import com.nangmanroad.core.data.TipStatus
import com.nangmanroad.core.data.Visibility
import com.nangmanroad.core.data.VisitCondition
import com.nangmanroad.core.data.distanceKm
import com.nangmanroad.core.map.MapFocus
import com.nangmanroad.core.sync.NewPost
import com.nangmanroad.core.sync.Limits
import com.nangmanroad.core.sync.NewTip
import com.nangmanroad.core.sync.SentItem
import com.nangmanroad.core.sync.isParkit
import com.nangmanroad.core.sync.Session
import com.nangmanroad.core.sync.SpotRepository
import com.nangmanroad.core.sync.SyncException
import com.nangmanroad.core.sync.UserList
import com.nangmanroad.core.sync.UserState
import com.nangmanroad.core.sync.list
import com.nangmanroad.phone.data.CarCommand
import com.nangmanroad.phone.data.CarDemoState
import com.nangmanroad.core.data.Region
import com.nangmanroad.core.data.Regions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.roundToInt

enum class PhoneTab { HOME, MAP, SHARE, CAR, MY }

sealed interface PhoneRoute {
    data class Detail(val spotId: String, val showReviews: Boolean = false) : PhoneRoute
    data object Notifications : PhoneRoute
    data object Share : PhoneRoute
}

enum class HomeTab(val label: String) { FOR_YOU("나를 위한 추천"), FOLLOWING("팔로잉"), NEARBY("가까운 곳") }

enum class ProfileTab(val label: String) { SHARED("공유한 장소"), SAVED("저장한 장소"), VISITED("방문 완료") }

data class DraftTip(val status: TipStatus = TipStatus.PREPARE, val summary: String = "")

/** What a location fix is for, so it lands in the right place: the nearby feed, the map tab or the post being written. */
enum class LocationPurpose { NEARBY, MAP, SHARE_DRAFT }

/** The post being written in the three-step share flow. */
data class ShareDraft(
    val step: Int = 0,
    /** Where the share map should move next: a picked region or the device's position. */
    val mapFocus: MapFocus? = null,
    /** A place already shared that the pin landed on or next to; it gets a review instead of a duplicate. */
    val existingSpotId: String? = null,
    val photos: List<String> = emptyList(),
    val importing: Boolean = false,
    val publishing: Boolean = false,
    val photoLocation: LatLng? = null,
    val region: Region? = null,
    val pin: LatLng? = null,
    val usesCurrentPin: Boolean = false,
    val locationRequestId: Long? = null,
    val title: String = "",
    val story: String = "",
    val category: Category? = null,
    val tips: Map<TipKind, DraftTip> = emptyMap(),
    val products: List<Product> = emptyList(),
    val visibility: Visibility = Visibility.PUBLIC,
    val approximate: Boolean = false,
    /** The author's answer to "can the night be spent here?", asked next to the category. */
    val carCamping: CarCamping = CarCamping.CHECK,
    val carCampingNote: String = "",
    /** "언제 가면 좋아요?" in the author's words; optional. */
    val bestTime: String = "",
    val visitConditions: Set<VisitCondition> = emptySet(),
    /** Set when the car's 파킷 started this draft; that inbox item is cleared once the post is up. */
    val parkitId: String? = null,
) {
    val fromCar: Boolean get() = parkitId != null

    /** Pick a location first. Photos and optional details follow. */
    fun canContinue(): Boolean = when (step) {
        0 -> region != null && pin != null && existingSpotId == null
        1 -> photos.isNotEmpty() && !importing
        else -> title.isNotBlank() && category != null && !publishing
    }

    companion object {
        const val STEP_COUNT = 3
    }
}

data class ToastMessage(val text: String, val id: Long = System.nanoTime(), val supporting: String? = null, val open: (() -> Unit)? = null)

/** Sign-in screen state; [offline] means the user chose to try the app without the server. */
data class AccountUi(
    val session: Session? = null,
    val offline: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val serverUrl: String = "",
    val online: Boolean? = null,
) {
    val needsSignIn: Boolean get() = session == null && !offline
}

data class PhoneUiState(
    val user: UserState,
    val spots: List<Spot>,
    val account: AccountUi = AccountUi(),
    val tab: PhoneTab = PhoneTab.HOME,
    val stack: List<PhoneRoute> = emptyList(),
    val homeTab: HomeTab = HomeTab.FOR_YOU,
    /** Facilities a place must have (visitor-confirmed) to appear in the home feed. */
    val facilities: Set<TipKind> = emptySet(),
    val visitConditions: Set<VisitCondition> = emptySet(),
    val mapCategory: Category? = null,
    val mapSelectedId: String? = null,
    /** Moves the map tab's camera, e.g. to the device's position once it is known. */
    val mapFocus: MapFocus? = null,
    val profileTab: ProfileTab = ProfileTab.SHARED,
    val car: CarDemoState = CarDemoState(),
    val claimingCar: Boolean = false,
    val draft: ShareDraft? = null,
    val toast: ToastMessage? = null,
    val here: LatLng = DefaultLocation,
    val usesDeviceLocation: Boolean = false,
    val locating: Boolean = false,
) {
    val me: Author get() = user.user
    val saved: Set<String> get() = user.saved
    val liked: Set<String> get() = user.liked
    val following: Set<String> get() = user.following
    val helpfulTips: Set<String> get() = user.helpfulTips
    val visited: Set<String> get() = user.visited
    val savedProducts: Set<String> get() = user.savedProducts
    val carQueue: Set<String> get() = user.carQueue.mapNotNull { it.spotId }.toSet()
    val fromCar: List<SentItem> get() = user.phoneInbox

    val route: PhoneRoute? get() = stack.lastOrNull()

    val myPosts: List<Spot> get() = spots.filter { it.author.id == me.id }

    fun spot(id: String?): Spot? = id?.let { wanted -> spots.firstOrNull { it.id == wanted } }

    fun likeCount(spot: Spot) = spot.likeCount + if (spot.id in liked) 1 else 0

    fun saveCount(spot: Spot) = spot.saveCount + if (spot.id in saved) 1 else 0
}

class PhoneViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SpotRepository(application, localUser = SampleSpots.authors.first { it.id == DEMO_USER_ID })
    private val prefs = application.getSharedPreferences("phone_ui", Context.MODE_PRIVATE)
    private val photoDir = File(application.filesDir, "posts").apply { mkdirs() }
    private var foreground = false
    /** Ids already in the inbox, so only what the car sends while the app is open is announced. */

    private val _state = MutableStateFlow(
        PhoneUiState(
            user = repository.state.value,
            spots = repository.spots.value,
            account = AccountUi(
                session = repository.session.value,
                offline = prefs.getBoolean(KEY_OFFLINE, false),
                serverUrl = repository.serverUrl,
            ),
        ),
    )
    val state: StateFlow<PhoneUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.state, repository.spots, repository.session, repository.online) { user, spots, session, online ->
                Triple(user, spots, session to online)
            }.collect { (user, spots, sessionAndOnline) ->
                val (session, online) = sessionAndOnline
                val previous = _state.value
                val oldInbox = previous.fromCar.associate { it.id to it.sentAt }
                _state.update { it.copy(user = user, spots = spots, account = it.account.copy(session = session, online = online)) }
                if (session != null && foreground) {
                    val received = user.phoneInbox.firstOrNull { oldInbox[it.id] != it.sentAt }
                    if (received != null) {
                        toast(
                            if (received.isParkit) "차량에서 파킷이 왔어요" else "차량에서 보낸 항목이 왔어요",
                            if (received.isParkit) {
                                val place = spots.firstOrNull { it.id == received.spotId }
                                val name = place?.title ?: received.location?.let { Regions.nearest(it).address } ?: received.label
                                val next = if (place == null) "사진과 후기를 더해 공유할 수 있어요." else "장소를 열어 방문 후기를 남길 수 있어요."
                                "$name · $next"
                            } else received.label,
                        ) { openNotifications() }
                    }
                }
            }
        }
        viewModelScope.launch {
            // Pick up changes made on the car (saves, items sent to the phone) while the app is open.
            while (true) {
                if (foreground && repository.session.value != null) repository.refresh()
                delay(REFRESH_MS)
            }
        }
    }

    fun setForeground(visible: Boolean) {
        foreground = visible
        if (visible) viewModelScope.launch { repository.refresh() }
    }

    fun useCurrentLocation(purpose: LocationPurpose = LocationPurpose.NEARBY) {
        if (_state.value.locating) return
        val context = getApplication<Application>()
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            toast(
                when (purpose) {
                    LocationPurpose.NEARBY -> "위치 권한이 없어요. 서울 기준으로 가까운 곳을 보여드려요."
                    LocationPurpose.MAP -> "위치 권한이 없어 내 위치로 이동할 수 없어요. 설정에서 허용할 수 있어요."
                    LocationPurpose.SHARE_DRAFT -> "위치 권한이 없어요. 지역을 고르거나 지도를 눌러 위치를 찍어 주세요."
                },
            )
            return
        }
        val requestId = if (purpose == LocationPurpose.SHARE_DRAFT) System.nanoTime() else null
        if (requestId != null) updateDraft { it.copy(locationRequestId = requestId) }
        _state.update { it.copy(locating = true) }
        viewModelScope.launch {
            val manager = context.getSystemService(LocationManager::class.java)
            val providers = if (fine) listOf("fused", LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                else listOf("fused", LocationManager.NETWORK_PROVIDER)
            val position: LatLng? = if (manager == null) null else providers.asSequence()
                .filter { it in manager.allProviders && manager.isProviderEnabled(it) }
                .firstNotNullOfOrNull { provider ->
                    withTimeoutOrNull(5_000) {
                        suspendCancellableCoroutine<LatLng?> { continuation ->
                            val signal = CancellationSignal()
                            continuation.invokeOnCancellation { signal.cancel() }
                            try {
                                LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) {
                                    continuation.resume(it?.let { fix -> LatLng(fix.latitude, fix.longitude) })
                                }
                            } catch (_: SecurityException) {
                                continuation.resume(null)
                            } catch (_: IllegalArgumentException) {
                                continuation.resume(null)
                            }
                        }
                    }
                }
            _state.update {
                it.copy(
                    here = position ?: DefaultLocation,
                    usesDeviceLocation = position != null,
                    locating = false,
                    mapFocus = if (purpose == LocationPurpose.MAP && position != null) MapFocus(position, 3f) else it.mapFocus,
                )
            }
            when {
                position == null -> toast(
                    if (purpose == LocationPurpose.SHARE_DRAFT) "현재 위치를 찾지 못했어요. 지도를 눌러 위치를 찍어 주세요."
                    else "현재 위치를 찾지 못했어요. 위치 설정을 확인해 주세요.",
                )
            }
            if (requestId != null) updateDraft { draft ->
                when {
                    draft.locationRequestId != requestId || draft.step != 0 -> draft
                    position == null -> draft.copy(locationRequestId = null)
                    else -> draft.copy(
                        pin = position, region = Regions.nearest(position), mapFocus = MapFocus(position, 3f),
                        existingSpotId = existingSpotNear(position)?.id,
                        usesCurrentPin = true, locationRequestId = null,
                    )
                }
            }
        }
    }

    // Account

    fun signIn(nickname: String, password: String) {
        _state.update { it.copy(account = it.account.copy(busy = true, error = null)) }
        viewModelScope.launch {
            repository.signIn(nickname, password)
                .onSuccess { session ->
                    prefs.edit().putBoolean(KEY_OFFLINE, false).apply()
                    _state.update { it.copy(account = it.account.copy(busy = false, offline = false)) }
                    toast("${session.user.name} 계정으로 시작했어요")
                }
                .onFailure { error -> _state.update { it.copy(account = it.account.copy(busy = false, error = error.message)) } }
        }
    }

    fun continueOffline() {
        prefs.edit().putBoolean(KEY_OFFLINE, true).apply()
        _state.update { it.copy(account = it.account.copy(offline = true, error = null)) }
    }

    /** Signs out of the server (or leaves offline mode) and shows the sign-in screen again. */
    fun signOut() {
        prefs.edit().putBoolean(KEY_OFFLINE, false).apply()
        viewModelScope.launch {
            repository.signOut()
            _state.update { it.copy(account = it.account.copy(offline = false), tab = PhoneTab.HOME, stack = emptyList()) }
        }
    }

    // Navigation

    fun selectTab(tab: PhoneTab) {
        if (tab == PhoneTab.SHARE) return startShare()
        _state.update { it.copy(tab = tab, stack = emptyList()) }
    }

    fun openSpot(spotId: String, showReviews: Boolean = false) = push(PhoneRoute.Detail(spotId, showReviews))

    fun openNotifications() = push(PhoneRoute.Notifications)

    fun back() = _state.update { current ->
        val leaving = current.route
        current.copy(stack = current.stack.dropLast(1), draft = if (leaving == PhoneRoute.Share) null else current.draft)
    }

    fun setHomeTab(tab: HomeTab) = _state.update { it.copy(homeTab = tab) }

    fun toggleVisitCondition(condition: VisitCondition) = _state.update {
        it.copy(visitConditions = if (condition in it.visitConditions) it.visitConditions - condition else it.visitConditions + condition)
    }

    fun toggleFacility(kind: TipKind) = _state.update {
        it.copy(facilities = if (kind in it.facilities) it.facilities - kind else it.facilities + kind)
    }

    fun clearFilters() = _state.update { it.copy(facilities = emptySet(), visitConditions = emptySet()) }

    fun setMapCategory(category: Category?) = _state.update { it.copy(mapCategory = category) }

    fun selectOnMap(spotId: String?) = _state.update { it.copy(mapSelectedId = spotId) }

    fun setProfileTab(tab: ProfileTab) = _state.update { it.copy(profileTab = tab) }

    private fun push(route: PhoneRoute) = _state.update { it.copy(stack = it.stack + route) }

    // Social actions

    fun toggleSave(spotId: String) = toggle(UserList.SAVED, spotId, onMessage = "저장한 장소에 담았어요. 차량 화면에도 함께 보여요", offMessage = "저장을 취소했어요")

    fun toggleLike(spotId: String) = toggle(UserList.LIKED, spotId)

    fun toggleFollow(authorId: String) = toggle(UserList.FOLLOWING, authorId, onMessage = "팔로우했어요", offMessage = "팔로우를 취소했어요")

    fun toggleHelpful(tipId: String) = toggle(UserList.HELPFUL_TIPS, tipId)

    /** A personal record only: it fills 마이 > 방문 완료 and tells nobody else anything. */
    fun toggleVisited(spotId: String) =
        toggle(UserList.VISITED, spotId, onMessage = "방문 기록에 남겼어요. 나만 보는 기록이에요", offMessage = "방문 기록에서 뺐어요")

    fun toggleProduct(productId: String) = toggle(UserList.SAVED_PRODUCTS, productId, onMessage = "관심 상품에 담았어요", offMessage = "관심 상품에서 뺐어요")

    private fun toggle(list: UserList, id: String, onMessage: String? = null, offMessage: String? = null) {
        val on = id !in _state.value.user.list(list)
        viewModelScope.launch {
            repository.toggle(list, id, on)
                .onSuccess { (if (on) onMessage else offMessage)?.let(::toast) }
                .onFailure(::showError)
        }
    }

    fun sendToCar(spotId: String) {
        val spot = _state.value.spot(spotId) ?: return
        viewModelScope.launch {
            repository.sendToCar(spot)
                .onSuccess {
                    // "Sent" only when it really left: waiting for a sign-in or a linked car is said as waiting.
                    val current = _state.value
                    toast(
                        when {
                            current.account.session == null -> "전송 대기 · 이 휴대폰에만 담았어요. 로그인하면 차량으로 보내져요"
                            current.user.linkedCars == 0 -> "전송 대기 · 차량 탭에서 차량을 연결하면 전달돼요"
                            else -> "차량으로 보냈어요. 차량이 받으면 '차량 수신'으로 바뀌어요"
                        },
                    )
                }
                .onFailure { toast("차량으로 보내지 못했어요. ${it.message ?: "연결 상태를 확인해 주세요"} · 다시 눌러 주세요") }
        }
    }

    fun removeFromCar(spotId: String) {
        viewModelScope.launch { repository.removeFromCar(spotId).onFailure(::showError) }
    }

    fun dismissFromCar(itemId: String) {
        viewModelScope.launch { repository.removeFromPhone(itemId).onFailure(::showError) }
    }

    fun addComment(spotId: String, text: String, isQuestion: Boolean, parentId: String?) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addComment(spotId, text, isQuestion, parentId)
                .onSuccess { toast(if (parentId != null) "답글을 남겼어요" else if (isQuestion) "질문을 남겼어요" else "댓글을 남겼어요") }
                .onFailure(::showError)
        }
    }

    fun suggestTip(spotId: String, kind: TipKind, status: TipStatus, summary: String, detail: String) {
        if (summary.isBlank()) return
        viewModelScope.launch {
            repository.addTip(spotId, kind, status, summary, detail)
                .onSuccess { toast("꿀팁을 제보했어요. 고마워요!") }
                .onFailure(::showError)
        }
    }

    fun reportCarCamping(spotId: String, status: CarCamping) {
        viewModelScope.launch {
            repository.reportCarCamping(spotId, status, "")
                .onSuccess { toast("차박 상태 제보를 접수했어요. 닉네임과 함께 모두에게 보여요") }
                .onFailure(::showError)
        }
    }

    /** Links the car whose screen shows [code] to this account. */
    fun claimCar(code: String) {
        val digits = code.filter(Char::isDigit)
        if (digits.length != 6) return toast("차량 화면의 6자리 숫자를 입력해 주세요")
        _state.update { it.copy(claimingCar = true) }
        viewModelScope.launch {
            repository.claimPairing(digits)
                .onSuccess { toast("차량을 연결했어요. 잠시 후 차량 화면에 계정이 표시돼요") }
                .onFailure(::showError)
            _state.update { it.copy(claimingCar = false) }
        }
    }

    // Share flow

    private fun startShare() = _state.update { it.copy(draft = ShareDraft(), stack = it.stack + PhoneRoute.Share) }

    /** A 파킷 from the car: the post starts pinned where the car parked, photos and tips come here. */
    fun startShareFromCar(item: SentItem) {
        val location = item.location ?: return toast("차량이 보낸 위치 정보가 없어요")
        _state.update {
            if (it.draft?.parkitId == item.id) return@update it
            it.copy(
                // A car set to share only the city/county sends that region's centre; keep the post approximate too.
                draft = ShareDraft(
                    pin = location,
                    region = Regions.nearest(location),
                    parkitId = item.id,
                    approximate = item.approximateLocation ?: (Regions.nearest(location).center == location),
                    // What the driver said at the wheel becomes the first line of the story.
                    story = item.note,
                ),
                stack = it.stack.filter { route -> route != PhoneRoute.Share } + PhoneRoute.Share,
            )
        }
    }

    fun updateDraft(transform: (ShareDraft) -> ShareDraft) = _state.update { current ->
        current.copy(draft = current.draft?.let(transform))
    }

    fun draftNext() = updateDraft { if (it.canContinue()) it.copy(step = (it.step + 1).coerceAtMost(ShareDraft.STEP_COUNT - 1)) else it }

    fun draftBack() {
        val draft = _state.value.draft ?: return
        if (draft.step == 0) back() else updateDraft { it.copy(step = it.step - 1) }
    }

    fun addSamplePhoto(url: String) = updateDraft {
        if (url in it.photos || it.importing || it.photos.size >= Limits.PHOTOS) it
        else it.copy(photos = it.photos + url)
    }

    fun removeDraftPhoto(url: String) = updateDraft { it.copy(photos = it.photos - url) }

    /** Reads any GPS tag and saves the picker photo in the JPEG format accepted by the server. */
    fun importPhotos(uris: List<Uri>) {
        val draft = _state.value.draft ?: return
        if (uris.isEmpty() || draft.importing) return
        val picked = uris.take((Limits.PHOTOS - draft.photos.size).coerceAtLeast(0))
        if (picked.isEmpty()) {
            toast("사진은 ${Limits.PHOTOS}장까지 올릴 수 있어요")
            return
        }
        updateDraft { it.copy(importing = true) }
        viewModelScope.launch {
            val imported = withContext(Dispatchers.IO) {
                picked.mapNotNull(::preparePhoto)
            }
            if (_state.value.draft?.importing != true) {
                withContext(Dispatchers.IO) { imported.forEach { (file, _) -> file.delete() } }
                return@launch
            }
            val location = imported.firstNotNullOfOrNull { it.second }
            updateDraft { current ->
                current.copy(
                    importing = false,
                    photos = current.photos + imported.map { (file, _) -> "file://${file.path}" },
                    photoLocation = current.photoLocation ?: location,
                    region = current.region ?: location?.let(Regions::nearest),
                    pin = current.pin ?: location,
                )
            }
            when {
                imported.isEmpty() -> toast("사진을 불러오지 못했어요. 다른 사진을 골라 주세요")
                imported.size < picked.size -> toast("일부 사진을 불러오지 못했어요")
                picked.size < uris.size -> toast("사진은 ${Limits.PHOTOS}장까지 올릴 수 있어요")
            }
        }
    }

    private fun preparePhoto(uri: Uri): Pair<File, LatLng?>? {
        val resolver = getApplication<Application>().contentResolver
        val gps = FloatArray(2)
        val location = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                if (ExifInterface(input).getLatLong(gps)) LatLng(gps[0].toDouble(), gps[1].toDouble()) else null
            }
        }.getOrNull()
        val file = File(photoDir, "${UUID.randomUUID()}.jpg")
        return runCatching {
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val (width, height) = info.size.run { width to height }
                val scale = 1920.0 / maxOf(width, height)
                if (scale < 1.0) decoder.setTargetSize(
                    (width * scale).roundToInt().coerceAtLeast(1),
                    (height * scale).roundToInt().coerceAtLeast(1),
                )
            }
            try {
                val saved = file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                if (!saved) throw IOException("Could not encode the selected photo")
            } finally {
                bitmap.recycle()
            }
            file to location
        }.onFailure { file.delete() }.getOrNull()
    }

    /** A region moves the map there and leaves a provisional pin at its centre; a tap then sets the exact spot. */
    fun chooseRegion(region: Region) = updateDraft {
        it.copy(region = region, pin = region.center, mapFocus = MapFocus(region.center, 2.2f), existingSpotId = null, usesCurrentPin = false, locationRequestId = null)
    }

    fun dropPin(point: LatLng) {
        val near = existingSpotNear(point)
        updateDraft { it.copy(pin = point, region = Regions.nearest(point), existingSpotId = near?.id, usesCurrentPin = false, locationRequestId = null) }
    }

    /** Tapping a place that is already on the share map. */
    fun pickExistingSpot(spotId: String) {
        val spot = _state.value.spot(spotId) ?: return
        updateDraft { it.copy(existingSpotId = spotId, usesCurrentPin = false, locationRequestId = null, pin = spot.location, region = Regions.nearest(spot.location)) }
    }

    /** "다른 곳이에요": keep the pin and go on with a new place. */
    fun keepAsNewPlace() = updateDraft { it.copy(existingSpotId = null) }

    /** Leaves the share flow for the existing place's reviews, so no second copy of it is created. */
    fun reviewExistingSpot() {
        val spotId = _state.value.draft?.existingSpotId ?: return
        _state.update {
            it.copy(draft = null, stack = it.stack.filter { route -> route != PhoneRoute.Share } + PhoneRoute.Detail(spotId, showReviews = true))
        }
        toast("이 장소에 후기를 남겨 주세요. 새 장소로 중복 등록되지 않아요")
    }

    private fun existingSpotNear(point: LatLng): Spot? = _state.value.spots
        .minByOrNull { distanceKm(it.location, point) }
        ?.takeIf { distanceKm(it.location, point) <= SAME_PLACE_KM }

    fun publish() {
        val draft = _state.value.draft ?: return
        if (draft.step != ShareDraft.STEP_COUNT - 1 || !draft.canContinue()) return
        val region = draft.region ?: return
        val category = draft.category ?: return
        val post = NewPost(
            title = draft.title.trim(),
            region = region.short,
            address = region.address,
            location = if (draft.approximate) region.center else draft.pin ?: region.center,
            category = category,
            story = draft.story.trim(),
            photos = draft.photos,
            tips = draft.tips.mapNotNull { (kind, tip) ->
                val summary = tip.summary.trim()
                if (summary.isEmpty()) null else NewTip(kind, tip.status, summary, summary)
            },
            products = draft.products,
            visibility = draft.visibility,
            approximateLocation = draft.approximate,
            visitConditions = draft.visitConditions,
            carCamping = draft.carCamping,
            carCampingNote = draft.carCampingNote.trim(),
            bestTime = draft.bestTime.trim(),
        )
        updateDraft { it.copy(publishing = true) }
        viewModelScope.launch {
            val serverPost = repository.session.value != null
            val result = repository.publish(post)
            result
                .onSuccess { spot ->
                    _state.update { it.copy(draft = null, tab = PhoneTab.HOME, stack = listOf(PhoneRoute.Detail(spot.id)), mapSelectedId = spot.id) }
                    when {
                        !serverPost -> toast("이 휴대폰에 장소를 저장했어요")
                        draft.visibility == Visibility.PRIVATE -> toast("나만 볼 수 있게 올렸어요")
                        draft.visibility == Visibility.FOLLOWERS -> toast("팔로워에게만 공유했어요")
                        else -> toast("지도와 피드에 공개했어요")
                    }
                }
                .onFailure { error ->
                    updateDraft { it.copy(publishing = false) }
                    if (error is SyncException && error.status == null) {
                        toast("장소를 올리지 못했어요", "로컬 서버를 확인한 뒤 작성한 글을 다시 올려 주세요")
                    } else {
                        showError(error)
                    }
                }
            // The car's 파킷 has done its job once the post is up.
            if (result.isSuccess) draft.parkitId?.let { id -> repository.removeFromPhone(id) }
        }
    }

    // Car (simulated)

    fun requestCarCommand(command: CarCommand) = _state.update {
        if (it.car.processing != null) it else it.copy(car = it.car.copy(pending = command))
    }

    fun toggleDemoAuxBattery() = _state.update {
        it.copy(car = it.car.copy(lowAuxBatteryExample = !it.car.lowAuxBatteryExample))
    }

    fun toggleDemoCo2() = _state.update {
        it.copy(car = it.car.copy(highCo2Example = !it.car.highCo2Example, freshAir = false))
    }

    fun cancelCarCommand() = _state.update { it.copy(car = it.car.copy(pending = null)) }

    fun setTargetTemp(temp: Int) = _state.update { it.copy(car = it.car.copy(targetTemp = temp.coerceIn(17, 28))) }

    fun confirmCarCommand() {
        val command = _state.value.car.pending ?: return
        _state.update { it.copy(car = it.car.copy(pending = null, processing = command)) }
        viewModelScope.launch {
            delay(1_600)
            _state.update { it.copy(car = it.car.apply(command).copy(processing = null)) }
            toast("${command.successMessage} (데모)")
        }
    }

    fun toast(text: String) = toast(text, null)

    private fun toast(text: String, supporting: String?, open: (() -> Unit)? = null) =
        _state.update { it.copy(toast = ToastMessage(text, supporting = supporting, open = open)) }

    fun consumeToast(id: Long) = _state.update { if (it.toast?.id == id) it.copy(toast = null) else it }

    private fun showError(error: Throwable) = toast(error.message ?: "요청을 처리하지 못했어요")

    private companion object {
        /** The demo account used offline; matches a sample author so their posts show up as "mine". */
        const val DEMO_USER_ID = "u-yunseul"
        const val KEY_OFFLINE = "offline"
        const val REFRESH_MS = 10_000L
        /** A pin this close to a shared place is most likely that place. */
        const val SAME_PLACE_KM = 0.3
    }
}
