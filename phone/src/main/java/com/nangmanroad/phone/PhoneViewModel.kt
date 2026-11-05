package com.nangmanroad.phone

import android.app.Application
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nangmanroad.core.data.Author
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.data.DefaultLocation
import com.nangmanroad.core.data.HoneyTip
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.Product
import com.nangmanroad.core.data.SampleSpots
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.TipKind
import com.nangmanroad.core.data.TipStatus
import com.nangmanroad.core.data.UserStore
import com.nangmanroad.core.data.Visibility
import com.nangmanroad.core.data.withConflictsFlagged
import com.nangmanroad.phone.data.CarCommand
import com.nangmanroad.phone.data.CarDemoState
import com.nangmanroad.phone.data.LocalComment
import com.nangmanroad.phone.data.LocalTip
import com.nangmanroad.phone.data.PostStore
import com.nangmanroad.phone.data.Region
import com.nangmanroad.phone.data.Regions
import com.nangmanroad.phone.data.mergeComments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) {
    fun canContinue(): Boolean = when (step) {
        0 -> photos.isNotEmpty() && !importing
        1 -> region != null
        2 -> title.isNotBlank() && category != null
        3 -> tips.isNotEmpty() && tips.values.all { it.summary.isNotBlank() }
        else -> true
    }

    companion object {
        const val STEP_COUNT = 6
    }
}

data class ToastMessage(val text: String, val id: Long = System.nanoTime())

data class PhoneUiState(
    val me: Author,
    val spots: List<Spot>,
    val tab: PhoneTab = PhoneTab.HOME,
    val stack: List<PhoneRoute> = emptyList(),
    val homeTab: HomeTab = HomeTab.FOR_YOU,
    val mapCategory: Category? = null,
    val mapSelectedId: String? = null,
    val profileTab: ProfileTab = ProfileTab.SHARED,
    val saved: Set<String> = emptySet(),
    val liked: Set<String> = emptySet(),
    val following: Set<String> = emptySet(),
    val helpfulTips: Set<String> = emptySet(),
    val visited: Set<String> = emptySet(),
    val savedProducts: Set<String> = emptySet(),
    val carQueue: Set<String> = emptySet(),
    val car: CarDemoState = CarDemoState(),
    val draft: ShareDraft? = null,
    val toast: ToastMessage? = null,
    val here: LatLng = DefaultLocation,
) {
    val route: PhoneRoute? get() = stack.lastOrNull()

    val myPosts: List<Spot> get() = spots.filter { it.author.id == me.id }

    fun spot(id: String?): Spot? = id?.let { wanted -> spots.firstOrNull { it.id == wanted } }

    fun likeCount(spot: Spot) = spot.likeCount + if (spot.id in liked) 1 else 0

    fun saveCount(spot: Spot) = spot.saveCount + if (spot.id in saved) 1 else 0
}

class PhoneViewModel(application: Application) : AndroidViewModel(application) {

    private val users = UserStore(application)
    private val posts = PostStore(application)
    private val photoDir = File(application.filesDir, "posts").apply { mkdirs() }
    private val me = SampleSpots.authors.first { it.id == ME_ID }

    private val _state = MutableStateFlow(
        PhoneUiState(
            me = me,
            spots = assembleSpots(),
            saved = users.savedSpots,
            liked = users.likedSpots,
            following = users.followedAuthors,
            helpfulTips = users.helpfulTips,
            visited = posts.visited,
            savedProducts = posts.savedProducts,
            carQueue = posts.carQueue,
        ),
    )
    val state: StateFlow<PhoneUiState> = _state.asStateFlow()

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

    fun setMapCategory(category: Category?) = _state.update { it.copy(mapCategory = category) }

    fun selectOnMap(spotId: String?) = _state.update { it.copy(mapSelectedId = spotId) }

    fun setProfileTab(tab: ProfileTab) = _state.update { it.copy(profileTab = tab) }

    private fun push(route: PhoneRoute) = _state.update { it.copy(stack = it.stack + route) }

    // Social actions

    fun toggleSave(spotId: String) {
        val nowSaved = spotId !in _state.value.saved
        _state.update { it.copy(saved = it.saved.toggle(spotId)) }
        users.savedSpots = _state.value.saved
        toast(if (nowSaved) "저장한 장소에 담았어요. 차량 화면에도 함께 보여요" else "저장을 취소했어요")
    }

    fun toggleLike(spotId: String) {
        _state.update { it.copy(liked = it.liked.toggle(spotId)) }
        users.likedSpots = _state.value.liked
    }

    fun toggleFollow(authorId: String) {
        val now = authorId !in _state.value.following
        _state.update { it.copy(following = it.following.toggle(authorId)) }
        users.followedAuthors = _state.value.following
        toast(if (now) "팔로우했어요" else "팔로우를 취소했어요")
    }

    fun toggleHelpful(tipId: String) {
        _state.update { it.copy(helpfulTips = it.helpfulTips.toggle(tipId)) }
        users.helpfulTips = _state.value.helpfulTips
    }

    fun toggleVisited(spotId: String) {
        val now = spotId !in _state.value.visited
        _state.update { it.copy(visited = it.visited.toggle(spotId)) }
        posts.visited = _state.value.visited
        if (now) toast("방문 완료로 표시했어요. 꿀팁을 남겨 주시면 다음 방문자에게 큰 도움이 돼요")
    }

    fun toggleProduct(productId: String) {
        val now = productId !in _state.value.savedProducts
        _state.update { it.copy(savedProducts = it.savedProducts.toggle(productId)) }
        posts.savedProducts = _state.value.savedProducts
        toast(if (now) "관심 상품에 담았어요" else "관심 상품에서 뺐어요")
    }

    fun sendToCar(spotId: String) {
        _state.update { it.copy(carQueue = it.carQueue + spotId) }
        posts.carQueue = _state.value.carQueue
        toast("차량으로 보낼 목록에 담았어요")
    }

    fun removeFromCar(spotId: String) {
        _state.update { it.copy(carQueue = it.carQueue - spotId) }
        posts.carQueue = _state.value.carQueue
    }

    fun addComment(spotId: String, text: String, isQuestion: Boolean, parentId: String?) {
        if (text.isBlank()) return
        posts.addComment(LocalComment(UUID.randomUUID().toString(), spotId, parentId, text.trim(), isQuestion, System.currentTimeMillis()))
        reloadSpots()
        toast(if (parentId != null) "답글을 남겼어요" else if (isQuestion) "질문을 남겼어요" else "댓글을 남겼어요")
    }

    fun suggestTip(spotId: String, kind: TipKind, status: TipStatus, summary: String, detail: String) {
        if (summary.isBlank()) return
        val tip = HoneyTip(
            id = "tip-${UUID.randomUUID()}",
            kind = kind,
            summary = summary.trim(),
            detail = detail.trim().ifEmpty { summary.trim() },
            status = status,
            helpfulCount = 0,
            confirmedDaysAgo = 0,
            reporter = me.name,
        )
        posts.addTip(LocalTip(spotId, tip))
        reloadSpots()
        toast("꿀팁을 제보했어요. 고마워요!")
    }

    // Share flow

    private fun startShare() = _state.update { it.copy(draft = ShareDraft(), stack = it.stack + PhoneRoute.Share) }

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
        val id = "my-${System.currentTimeMillis()}"
        val post = Spot(
            id = id,
            title = draft.title.trim(),
            region = region.short,
            address = region.address,
            location = if (draft.approximate) region.center else draft.pin ?: region.center,
            category = category,
            author = me,
            story = draft.story.trim(),
            photos = draft.photos,
            likeCount = 0,
            saveCount = 0,
            postedDaysAgo = 0,
            tips = draft.tips.map { (kind, tip) ->
                HoneyTip("$id-${kind.name}", kind, tip.summary.trim(), tip.summary.trim(), tip.status, 0, 0, me.name)
            },
            products = draft.products,
            comments = emptyList(),
            visibility = draft.visibility,
            approximateLocation = draft.approximate,
        )
        posts.addPost(post)
        reloadSpots()
        _state.update { it.copy(draft = null, tab = PhoneTab.HOME, stack = listOf(PhoneRoute.Detail(id)), mapSelectedId = id) }
        toast(if (draft.visibility == Visibility.PRIVATE) "나만 볼 수 있게 저장했어요" else "새로운 낭만이 지도와 피드에 공개됐어요")
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

    private fun reloadSpots() = _state.update { it.copy(spots = assembleSpots()) }

    private fun assembleSpots(): List<Spot> {
        val comments = posts.loadComments().groupBy { it.spotId }
        val tips = posts.loadTips().groupBy({ it.spotId }, { it.tip })
        val mine = posts.loadPosts(me).sortedBy { it.postedDaysAgo }
        return (mine + SampleSpots.all).map { spot ->
            spot.copy(
                comments = mergeComments(spot.comments, comments[spot.id].orEmpty(), me.name),
                tips = (spot.tips + tips[spot.id].orEmpty()).withConflictsFlagged(),
            )
        }
    }

    private fun Set<String>.toggle(id: String) = if (id in this) this - id else this + id

    private companion object {
        /** The demo account; matches a sample author so their posts show up as "mine". */
        const val ME_ID = "u-yunseul"
    }
}
