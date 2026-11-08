package com.nangmanroad.phone

import android.app.Application
import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nangmanroad.core.data.Author
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.data.DefaultLocation
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.Product
import com.nangmanroad.core.data.SampleSpots
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.TipKind
import com.nangmanroad.core.data.TipStatus
import com.nangmanroad.core.data.Visibility
import com.nangmanroad.core.sync.NewPost
import com.nangmanroad.core.sync.NewTip
import com.nangmanroad.core.sync.SentItem
import com.nangmanroad.core.sync.Session
import com.nangmanroad.core.sync.SpotRepository
import com.nangmanroad.core.sync.UserList
import com.nangmanroad.core.sync.UserState
import com.nangmanroad.core.sync.list
import com.nangmanroad.phone.data.CarCommand
import com.nangmanroad.phone.data.CarDemoState
import com.nangmanroad.phone.data.Region
import com.nangmanroad.phone.data.Regions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class PhoneTab { HOME, MAP, SHARE, CAR, MY }

sealed interface PhoneRoute {
    data class Detail(val spotId: String) : PhoneRoute
    data object Notifications : PhoneRoute
    data object Share : PhoneRoute
}

enum class HomeTab(val label: String) { FOR_YOU("나를 위한 추천"), FOLLOWING("팔로잉"), NEARBY("가까운 곳") }

enum class ProfileTab(val label: String) { SHARED("공유한 장소"), SAVED("저장한 장소"), VISITED("방문 완료") }

data class DraftTip(val status: TipStatus = TipStatus.PREPARE, val summary: String = "")

/** The post being written in the six-step share flow. */
data class ShareDraft(
    val step: Int = 0,
    val photos: List<String> = emptyList(),
    val importing: Boolean = false,
    val publishing: Boolean = false,
    val photoLocation: LatLng? = null,
    val region: Region? = null,
    val pin: LatLng? = null,
    val title: String = "",
    val story: String = "",
    val category: Category? = null,
    val tips: Map<TipKind, DraftTip> = emptyMap(),
    val products: List<Product> = emptyList(),
    val visibility: Visibility = Visibility.PUBLIC,
    val approximate: Boolean = false,
    /** Set when the car's 파킷 started this draft; that inbox item is cleared once the post is up. */
    val parkitId: String? = null,
) {
    val fromCar: Boolean get() = parkitId != null

    fun canContinue(): Boolean = when (step) {
        0 -> photos.isNotEmpty() && !importing
        1 -> region != null
        2 -> title.isNotBlank() && category != null
        3 -> tips.isNotEmpty() && tips.values.all { it.summary.isNotBlank() }
        STEP_COUNT - 1 -> !publishing
        else -> true
    }

    companion object {
        const val STEP_COUNT = 6
    }
}

data class ToastMessage(val text: String, val id: Long = System.nanoTime())

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
    val mapCategory: Category? = null,
    val mapSelectedId: String? = null,
    val profileTab: ProfileTab = ProfileTab.SHARED,
    val car: CarDemoState = CarDemoState(),
    val claimingCar: Boolean = false,
    val draft: ShareDraft? = null,
    val toast: ToastMessage? = null,
    val here: LatLng = DefaultLocation,
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
                _state.update { it.copy(user = user, spots = spots, account = it.account.copy(session = session, online = online)) }
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

    // Account

    fun signIn(nickname: String) {
        _state.update { it.copy(account = it.account.copy(busy = true, error = null)) }
        viewModelScope.launch {
            repository.signIn(nickname)
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

    fun openSpot(spotId: String) = push(PhoneRoute.Detail(spotId))

    fun openNotifications() = push(PhoneRoute.Notifications)

    fun back() = _state.update { current ->
        val leaving = current.route
        current.copy(stack = current.stack.dropLast(1), draft = if (leaving == PhoneRoute.Share) null else current.draft)
    }

    fun setHomeTab(tab: HomeTab) = _state.update { it.copy(homeTab = tab) }

    fun toggleFacility(kind: TipKind) = _state.update {
        it.copy(facilities = if (kind in it.facilities) it.facilities - kind else it.facilities + kind)
    }

    fun setMapCategory(category: Category?) = _state.update { it.copy(mapCategory = category) }

    fun selectOnMap(spotId: String?) = _state.update { it.copy(mapSelectedId = spotId) }

    fun setProfileTab(tab: ProfileTab) = _state.update { it.copy(profileTab = tab) }

    private fun push(route: PhoneRoute) = _state.update { it.copy(stack = it.stack + route) }

    // Social actions

    fun toggleSave(spotId: String) = toggle(UserList.SAVED, spotId, onMessage = "저장한 장소에 담았어요. 차량 화면에도 함께 보여요", offMessage = "저장을 취소했어요")

    fun toggleLike(spotId: String) = toggle(UserList.LIKED, spotId)

    fun toggleFollow(authorId: String) = toggle(UserList.FOLLOWING, authorId, onMessage = "팔로우했어요", offMessage = "팔로우를 취소했어요")

    fun toggleHelpful(tipId: String) = toggle(UserList.HELPFUL_TIPS, tipId)

    fun toggleVisited(spotId: String) =
        toggle(UserList.VISITED, spotId, onMessage = "방문 완료로 표시했어요. 꿀팁을 남겨 주시면 다음 방문자에게 큰 도움이 돼요")

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
                .onSuccess { toast(if (_state.value.account.session != null) "차량으로 보냈어요" else "차량으로 보낼 목록에 담았어요") }
                .onFailure(::showError)
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
            it.copy(
                draft = ShareDraft(pin = location, region = Regions.nearest(location), parkitId = item.id),
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

    fun addSamplePhoto(url: String) = updateDraft { if (url in it.photos) it else it.copy(photos = it.photos + url) }

    fun removeDraftPhoto(url: String) = updateDraft { it.copy(photos = it.photos - url) }

    /** Copies picked photos into app storage (the picker's URIs are temporary) and reads any GPS tag. */
    fun importPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        updateDraft { it.copy(importing = true) }
        viewModelScope.launch {
            val imported = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    runCatching {
                        val file = File(photoDir, "${UUID.randomUUID()}.jpg")
                        getApplication<Application>().contentResolver.openInputStream(uri)!!.use { input ->
                            file.outputStream().use { input.copyTo(it) }
                        }
                        val gps = FloatArray(2)
                        val location = if (ExifInterface(file.path).getLatLong(gps)) LatLng(gps[0].toDouble(), gps[1].toDouble()) else null
                        "file://${file.path}" to location
                    }.getOrNull()
                }
            }
            val location = imported.firstNotNullOfOrNull { it.second }
            updateDraft { draft ->
                draft.copy(
                    importing = false,
                    photos = draft.photos + imported.map { it.first },
                    photoLocation = draft.photoLocation ?: location,
                    region = draft.region ?: location?.let(Regions::nearest),
                    pin = draft.pin ?: location,
                )
            }
            if (imported.size < uris.size) toast("일부 사진을 불러오지 못했어요")
        }
    }

    fun chooseRegion(region: Region) = updateDraft { it.copy(region = region, pin = region.center) }

    fun dropPin(point: LatLng) = updateDraft { it.copy(pin = point, region = Regions.nearest(point)) }

    fun publish() {
        val draft = _state.value.draft ?: return
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
            tips = draft.tips.map { (kind, tip) -> NewTip(kind, tip.status, tip.summary.trim(), tip.summary.trim()) },
            products = draft.products,
            visibility = draft.visibility,
            approximateLocation = draft.approximate,
        )
        updateDraft { it.copy(publishing = true) }
        viewModelScope.launch {
            val result = repository.publish(post)
            result
                .onSuccess { spot ->
                    _state.update { it.copy(draft = null, tab = PhoneTab.HOME, stack = listOf(PhoneRoute.Detail(spot.id)), mapSelectedId = spot.id) }
                    toast(if (draft.visibility == Visibility.PRIVATE) "나만 볼 수 있게 저장했어요" else "지도와 피드에 공개했어요")
                }
                .onFailure { error ->
                    updateDraft { it.copy(publishing = false) }
                    showError(error)
                }
            // The car's 파킷 has done its job once the post is up.
            if (result.isSuccess) draft.parkitId?.let { id -> repository.removeFromPhone(id) }
        }
    }

    // Car (simulated)

    fun requestCarCommand(command: CarCommand) = _state.update { it.copy(car = it.car.copy(pending = command)) }

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

    fun toast(text: String) = _state.update { it.copy(toast = ToastMessage(text)) }

    fun consumeToast(id: Long) = _state.update { if (it.toast?.id == id) it.copy(toast = null) else it }

    private fun showError(error: Throwable) = toast(error.message ?: "요청을 처리하지 못했어요")

    private companion object {
        /** The demo account used offline; matches a sample author so their posts show up as "mine". */
        const val DEMO_USER_ID = "u-yunseul"
        const val KEY_OFFLINE = "offline"
        const val REFRESH_MS = 10_000L
    }
}
