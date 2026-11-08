package com.nangmanroad.core.sync

import android.content.Context
import com.nangmanroad.core.BuildConfig
import com.nangmanroad.core.data.Author
import com.nangmanroad.core.data.HoneyTip
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.LocalComment
import com.nangmanroad.core.data.LocalTip
import com.nangmanroad.core.data.PostStore
import com.nangmanroad.core.data.SampleSpots
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.TipKind
import com.nangmanroad.core.data.TipStatus
import com.nangmanroad.core.data.UserStore
import com.nangmanroad.core.data.mergeComments
import com.nangmanroad.core.data.withConflictsFlagged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Where spots and the user's lists come from. Signed in, the sync server is the source of truth and
 * every device on the account sees the same state. Signed out, everything stays on this device, as
 * before the server existed. Both apps talk only to this class.
 *
 * Calls return [Result]; a failure carries a [SyncException] whose message can be shown as is.
 */
class SpotRepository(
    context: Context,
    /** Who "I" am while signed out. */
    private val localUser: Author,
    baseUrl: String = BuildConfig.SYNC_URL,
) {
    private val sessions = SessionStore(context)
    private val users = UserStore(context)
    private val posts = PostStore(context)
    private val client = SyncClient(baseUrl, token = { _session.value?.token })

    private val _session = MutableStateFlow(sessions.load())
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<UserState> = _state.asStateFlow()

    private val _spots = MutableStateFlow(if (_session.value == null) localSpots() else SampleSpots.all)
    val spots: StateFlow<List<Spot>> = _spots.asStateFlow()

    /** Whether the last server call worked; null until one has been made. */
    private val _online = MutableStateFlow<Boolean?>(null)
    val online: StateFlow<Boolean?> = _online.asStateFlow()

    val serverUrl: String = baseUrl

    private val signedIn: Boolean get() = _session.value != null

    // Session

    suspend fun refresh(): Result<Unit> {
        if (!signedIn) {
            _state.value = localState()
            _spots.value = localSpots()
            return Result.success(Unit)
        }
        return remote {
            coroutineScope {
                val state = async { client.state() }
                val spots = async { client.spots() }
                _state.value = state.await()
                _spots.value = spots.await()
            }
        }
    }

    suspend fun signIn(nickname: String): Result<Session> = remote {
        client.signIn(nickname).also { adopt(it) }
    }

    /** Signs this device out; the account and its data stay on the server. */
    suspend fun signOut() {
        if (signedIn) runCatching { client.signOut() }
        sessions.save(null)
        _session.value = null
        _online.value = null
        refresh()
    }

    suspend fun startPairing(): Result<PairingCode> = remote { client.startPairing() }

    /** Polled by the car. Once the phone claims the code, the car is signed in to that account. */
    suspend fun pairingStatus(pairingId: String): Result<PairingStatus> = remote {
        client.pairingStatus(pairingId).also { status -> status.session?.let { adopt(it) } }
    }

    suspend fun claimPairing(code: String): Result<UserState> = remote {
        client.claimPairing(code).also { _state.value = it }
    }

    private suspend fun adopt(session: Session) {
        sessions.save(session)
        _session.value = session
        _state.value = UserState(session.user)
        refresh()
    }

    // Lists

    suspend fun toggle(list: UserList, id: String, on: Boolean): Result<Unit> {
        val before = _state.value
        _state.value = before.with(list, id, on)
        if (!signedIn) {
            saveLocal(_state.value)
            return Result.success(Unit)
        }
        return remote { _state.value = client.toggle(list, id, on) }.onFailure { _state.value = before }
    }

    suspend fun sendToCar(spot: Spot): Result<Unit> {
        if (!signedIn) {
            posts.carQueue = posts.carQueue + spot.id
            _state.value = localState()
            return Result.success(Unit)
        }
        return remote { _state.value = client.sendToCar(SendRequest(spot.id, spot.title, spot.id)) }
    }

    suspend fun removeFromCar(id: String): Result<Unit> {
        if (!signedIn) {
            posts.carQueue = posts.carQueue - id
            _state.value = localState()
            return Result.success(Unit)
        }
        return remote { _state.value = client.removeFromCar(id) }
    }

    suspend fun sendToPhone(id: String, label: String, spotId: String?, location: LatLng? = null): Result<Unit> {
        if (!signedIn) {
            users.phoneOutbox = users.phoneOutbox + id
            _state.value = localState()
            return Result.success(Unit)
        }
        return remote { _state.value = client.sendToPhone(SendRequest(id, label, spotId, location)) }
    }

    suspend fun removeFromPhone(id: String): Result<Unit> {
        if (!signedIn) {
            users.phoneOutbox = users.phoneOutbox - id
            _state.value = localState()
            return Result.success(Unit)
        }
        return remote { _state.value = client.removeFromPhone(id) }
    }

    // Posts, comments, tips

    suspend fun addComment(spotId: String, text: String, isQuestion: Boolean, parentId: String?): Result<Unit> {
        if (!signedIn) {
            posts.addComment(LocalComment(UUID.randomUUID().toString(), spotId, parentId, text.trim(), isQuestion, System.currentTimeMillis()))
            _spots.value = localSpots()
            return Result.success(Unit)
        }
        return remote { replaceSpot(client.addComment(spotId, NewComment(text, isQuestion, parentId))) }
    }

    suspend fun addTip(spotId: String, kind: TipKind, status: TipStatus, summary: String, detail: String): Result<Unit> {
        if (!signedIn) {
            val tip = HoneyTip("tip-${UUID.randomUUID()}", kind, summary.trim(), detail.trim().ifEmpty { summary.trim() }, status, 0, 0, localUser.name)
            posts.addTip(LocalTip(spotId, tip))
            _spots.value = localSpots()
            return Result.success(Unit)
        }
        return remote { replaceSpot(client.addTip(spotId, NewTip(kind, status, summary, detail))) }
    }

    /** Publishes a post. Photos given as file:// URIs are uploaded first when signed in. */
    suspend fun publish(post: NewPost): Result<Spot> {
        if (!signedIn) {
            val spot = post.toLocalSpot()
            posts.addPost(spot)
            _spots.value = localSpots()
            return Result.success(spot)
        }
        return remote {
            val photos = post.photos.map { photo ->
                if (photo.startsWith("file://")) withContext(Dispatchers.IO) { client.uploadPhoto(File(photo.removePrefix("file://"))) } else photo
            }
            client.createPost(post.copy(photos = photos)).also { created ->
                _spots.value = listOf(created) + _spots.value.filter { it.id != created.id }
            }
        }
    }

    // Internals

    private suspend fun <T> remote(block: suspend () -> T): Result<T> =
        try {
            Result.success(block()).also { _online.value = true }
        } catch (e: SyncException) {
            if (e.status == null) _online.value = false
            if (e.status == 401) {
                // The server forgot this device (e.g. its data folder was reset); fall back to local.
                sessions.save(null)
                _session.value = null
                _state.value = localState()
                _spots.value = localSpots()
            }
            Result.failure(e)
        }

    private fun replaceSpot(spot: Spot) {
        _spots.value = _spots.value.map { if (it.id == spot.id) spot else it }
    }

    private fun initialState(): UserState = _session.value?.let { UserState(it.user) } ?: localState()

    private fun localState() = UserState(
        user = localUser,
        saved = users.savedSpots,
        liked = users.likedSpots,
        following = users.followedAuthors,
        helpfulTips = users.helpfulTips,
        visited = posts.visited,
        savedProducts = posts.savedProducts,
        carQueue = posts.carQueue.map { SentItem(it, it, it, 0) },
        phoneInbox = users.phoneOutbox.map { SentItem(it, it, null, 0) },
    )

    private fun saveLocal(state: UserState) {
        users.savedSpots = state.saved
        users.likedSpots = state.liked
        users.followedAuthors = state.following
        users.helpfulTips = state.helpfulTips
        posts.visited = state.visited
        posts.savedProducts = state.savedProducts
    }

    private fun localSpots(): List<Spot> {
        val comments = posts.loadComments().groupBy { it.spotId }
        val tips = posts.loadTips().groupBy({ it.spotId }, { it.tip })
        val mine = posts.loadPosts(localUser).sortedBy { it.postedDaysAgo }
        return (mine + SampleSpots.all).map { spot ->
            spot.copy(
                comments = mergeComments(spot.comments, comments[spot.id].orEmpty(), localUser.name),
                tips = (spot.tips + tips[spot.id].orEmpty()).withConflictsFlagged(),
            )
        }
    }

    private fun NewPost.toLocalSpot(): Spot {
        val id = "my-${System.currentTimeMillis()}"
        return Spot(
            id = id,
            title = title.trim(),
            region = region,
            address = address,
            location = location,
            category = category,
            author = localUser,
            story = story.trim(),
            photos = photos,
            likeCount = 0,
            saveCount = 0,
            postedDaysAgo = 0,
            tips = tips.mapIndexed { index, tip ->
                HoneyTip("$id-tip-$index", tip.kind, tip.summary.trim(), tip.detail.trim().ifEmpty { tip.summary.trim() }, tip.status, 0, 0, localUser.name)
            },
            products = products,
            comments = emptyList(),
            visibility = visibility,
            approximateLocation = approximateLocation,
        )
    }
}

fun UserState.list(list: UserList): Set<String> = when (list) {
    UserList.SAVED -> saved
    UserList.LIKED -> liked
    UserList.FOLLOWING -> following
    UserList.HELPFUL_TIPS -> helpfulTips
    UserList.VISITED -> visited
    UserList.SAVED_PRODUCTS -> savedProducts
}

fun UserState.with(list: UserList, id: String, on: Boolean): UserState {
    fun Set<String>.set() = if (on) this + id else this - id
    return when (list) {
        UserList.SAVED -> copy(saved = saved.set())
        UserList.LIKED -> copy(liked = liked.set())
        UserList.FOLLOWING -> copy(following = following.set())
        UserList.HELPFUL_TIPS -> copy(helpfulTips = helpfulTips.set())
        UserList.VISITED -> copy(visited = visited.set())
        UserList.SAVED_PRODUCTS -> copy(savedProducts = savedProducts.set())
    }
}
