package com.nangmanroad.server

import com.nangmanroad.core.data.Author
import com.nangmanroad.core.data.Comment
import com.nangmanroad.core.data.HoneyTip
import com.nangmanroad.core.data.SampleSpots
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.Visibility
import com.nangmanroad.core.data.withConflictsFlagged
import com.nangmanroad.core.sync.Api
import com.nangmanroad.core.sync.DeviceKind
import com.nangmanroad.core.sync.NewComment
import com.nangmanroad.core.sync.NewPost
import com.nangmanroad.core.sync.NewTip
import com.nangmanroad.core.sync.PairingCode
import com.nangmanroad.core.sync.PairingState
import com.nangmanroad.core.sync.PairingStatus
import com.nangmanroad.core.sync.SendRequest
import com.nangmanroad.core.sync.SentItem
import com.nangmanroad.core.sync.Session
import com.nangmanroad.core.sync.UserList
import com.nangmanroad.core.sync.UserState
import kotlinx.serialization.Serializable
import java.io.File
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.TimeUnit

class UnauthorizedException : RuntimeException("로그인이 필요해요")

@Serializable
private data class StoredUser(
    val author: Author,
    val lists: Map<UserList, Set<String>> = emptyMap(),
    val carQueue: List<SentItem> = emptyList(),
    val phoneInbox: List<SentItem> = emptyList(),
)

@Serializable
private data class StoredSession(val token: String, val userId: String, val device: DeviceKind, val createdAt: Long)

@Serializable
private data class StoredPost(val spot: Spot, val createdAt: Long)

@Serializable
private data class StoredComment(
    val id: String,
    val spotId: String,
    val parentId: String?,
    val authorName: String,
    val text: String,
    val isQuestion: Boolean,
    val createdAt: Long,
)

@Serializable
private data class StoredTip(val spotId: String, val tip: HoneyTip, val createdAt: Long)

@Serializable
private data class Snapshot(
    val users: Map<String, StoredUser> = emptyMap(),
    val sessions: Map<String, StoredSession> = emptyMap(),
    val posts: List<StoredPost> = emptyList(),
    val comments: List<StoredComment> = emptyList(),
    val tips: List<StoredTip> = emptyList(),
)

private class Pairing(val id: String, val code: String, val expiresAt: Long) {
    var session: Session? = null
}

/**
 * All server state. Sample authors and spots are built in; everything users add is kept in memory
 * and written to `state.json` after each change, with uploaded photos beside it in `photos/`.
 * One lock guards everything, which is plenty for a development server.
 */
class SyncStore(
    private val dataDir: File?,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val random = SecureRandom()
    private val stateFile = dataDir?.let { File(it, "state.json") }
    val photoDir: File? = dataDir?.let { File(it, "photos") }

    private var snapshot: Snapshot = load()
    private val pairings = mutableMapOf<String, Pairing>()

    init {
        photoDir?.mkdirs()
    }

    // Accounts

    @Synchronized
    fun signIn(nickname: String): Session {
        val name = nickname.trim()
        require(name.length in 2..20) { "닉네임은 2~20자로 정해 주세요" }
        val existing = snapshot.users.values.firstOrNull { it.author.name == name }
        val user = existing ?: StoredUser(Author("u-${UUID.randomUUID().toString().take(8)}", name, "새로 낭만을 기록하기 시작했어요"))
        val session = StoredSession(newToken(), user.author.id, DeviceKind.PHONE, clock())
        update { it.copy(users = it.users + (user.author.id to user), sessions = it.sessions + (session.token to session)) }
        return Session(session.token, user.author, DeviceKind.PHONE)
    }

    @Synchronized
    fun signOut(token: String) = update { it.copy(sessions = it.sessions - token) }

    /** Returns the user id behind a bearer token, or throws [UnauthorizedException]. */
    @Synchronized
    fun userFor(token: String?): String = snapshot.sessions[token]?.userId ?: throw UnauthorizedException()

    @Synchronized
    fun state(userId: String): UserState {
        val user = user(userId)
        return UserState(
            user = user.author,
            saved = user.list(UserList.SAVED),
            liked = user.list(UserList.LIKED),
            following = user.list(UserList.FOLLOWING),
            helpfulTips = user.list(UserList.HELPFUL_TIPS),
            visited = user.list(UserList.VISITED),
            savedProducts = user.list(UserList.SAVED_PRODUCTS),
            carQueue = user.carQueue,
            phoneInbox = user.phoneInbox,
            linkedCars = snapshot.sessions.values.count { it.userId == userId && it.device == DeviceKind.CAR },
        )
    }

    @Synchronized
    fun toggle(userId: String, list: UserList, id: String, on: Boolean): UserState {
        editUser(userId) { user ->
            val current = user.list(list)
            user.copy(lists = user.lists + (list to if (on) current + id else current - id))
        }
        return state(userId)
    }

    @Synchronized
    fun sendToCar(userId: String, request: SendRequest): UserState {
        editUser(userId) { it.copy(carQueue = listOf(request.toItem()) + it.carQueue.filter { item -> item.id != request.id }) }
        return state(userId)
    }

    @Synchronized
    fun removeFromCar(userId: String, id: String): UserState {
        editUser(userId) { it.copy(carQueue = it.carQueue.filter { item -> item.id != id }) }
        return state(userId)
    }

    @Synchronized
    fun sendToPhone(userId: String, request: SendRequest): UserState {
        editUser(userId) { it.copy(phoneInbox = listOf(request.toItem()) + it.phoneInbox.filter { item -> item.id != request.id }) }
        return state(userId)
    }

    @Synchronized
    fun removeFromPhone(userId: String, id: String): UserState {
        editUser(userId) { it.copy(phoneInbox = it.phoneInbox.filter { item -> item.id != id }) }
        return state(userId)
    }

    // Car pairing

    @Synchronized
    fun startPairing(): PairingCode {
        val now = clock()
        pairings.values.removeAll { it.expiresAt < now }
        val active = pairings.values.map { it.code }.toSet()
        val code = generateSequence { "%06d".format(random.nextInt(1_000_000)) }.first { it !in active }
        val pairing = Pairing(UUID.randomUUID().toString(), code, now + PAIRING_TTL_MS)
        pairings[pairing.id] = pairing
        return PairingCode(pairing.id, pairing.code, pairing.expiresAt)
    }

    /** The car polls this. A linked pairing hands out its session once and is then forgotten. */
    @Synchronized
    fun pairingStatus(pairingId: String): PairingStatus {
        val pairing = pairings[pairingId] ?: return PairingStatus(PairingState.EXPIRED)
        val session = pairing.session
        return when {
            session != null -> {
                pairings.remove(pairingId)
                PairingStatus(PairingState.LINKED, session)
            }
            pairing.expiresAt < clock() -> {
                pairings.remove(pairingId)
                PairingStatus(PairingState.EXPIRED)
            }
            else -> PairingStatus(PairingState.WAITING)
        }
    }

    @Synchronized
    fun claimPairing(userId: String, code: String): UserState {
        val pairing = pairings.values.firstOrNull { it.code == code.trim() && it.session == null && it.expiresAt >= clock() }
            ?: throw NoSuchElementException("코드가 맞지 않거나 시간이 지났어요. 차량 화면의 새 코드를 입력해 주세요")
        val stored = StoredSession(newToken(), userId, DeviceKind.CAR, clock())
        update { it.copy(sessions = it.sessions + (stored.token to stored)) }
        pairing.session = Session(stored.token, user(userId).author, DeviceKind.CAR)
        return state(userId)
    }

    // Spots

    /** Sample spots plus user posts the requester may see, with comments, tips and counts merged in. */
    @Synchronized
    fun spots(userId: String): List<Spot> {
        val following = user(userId).list(UserList.FOLLOWING)
        val posts = snapshot.posts
            .filter { post ->
                val authorId = post.spot.author.id
                when (post.spot.visibility) {
                    Visibility.PUBLIC -> true
                    Visibility.FOLLOWERS -> authorId == userId || authorId in following
                    Visibility.PRIVATE -> authorId == userId
                }
            }
            .sortedByDescending { it.createdAt }
            .map { it.spot.copy(postedDaysAgo = daysSince(it.createdAt)) }
        return (posts + SampleSpots.all).map { decorate(it, userId) }
    }

    @Synchronized
    fun createPost(userId: String, post: NewPost): Spot {
        require(post.title.isNotBlank()) { "제목을 입력해 주세요" }
        require(post.photos.isNotEmpty()) { "사진을 한 장 이상 골라 주세요" }
        val author = user(userId).author
        val now = clock()
        val id = "post-${UUID.randomUUID().toString().take(12)}"
        val spot = Spot(
            id = id,
            title = post.title.trim(),
            region = post.region,
            address = post.address,
            location = post.location,
            category = post.category,
            author = author,
            story = post.story.trim(),
            photos = post.photos,
            likeCount = 0,
            saveCount = 0,
            postedDaysAgo = 0,
            tips = post.tips.mapIndexed { index, tip -> tip.toHoneyTip("$id-tip-$index", author.name) },
            products = post.products.map { it.copy(recommender = author.name) },
            comments = emptyList(),
            visibility = post.visibility,
            approximateLocation = post.approximateLocation,
            carCamping = post.carCamping,
            carCampingNote = post.carCampingNote.trim(),
            bestTime = post.bestTime.trim(),
        )
        update { it.copy(posts = it.posts + StoredPost(spot, now)) }
        return decorate(spot, userId)
    }

    @Synchronized
    fun addComment(userId: String, spotId: String, comment: NewComment): Spot {
        require(comment.text.isNotBlank()) { "내용을 입력해 주세요" }
        val spot = findSpot(spotId)
        val stored = StoredComment(
            id = "c-${UUID.randomUUID().toString().take(12)}",
            spotId = spotId,
            parentId = comment.parentId,
            authorName = user(userId).author.name,
            text = comment.text.trim(),
            isQuestion = comment.isQuestion && comment.parentId == null,
            createdAt = clock(),
        )
        update { it.copy(comments = it.comments + stored) }
        return decorate(spot, userId)
    }

    @Synchronized
    fun addTip(userId: String, spotId: String, tip: NewTip): Spot {
        require(tip.summary.isNotBlank()) { "한 줄 요약을 입력해 주세요" }
        val spot = findSpot(spotId)
        val stored = StoredTip(spotId, tip.toHoneyTip("tip-${UUID.randomUUID().toString().take(12)}", user(userId).author.name), clock())
        update { it.copy(tips = it.tips + stored) }
        return decorate(spot, userId)
    }

    fun newPhotoFile(extension: String): File {
        val dir = photoDir ?: throw IllegalStateException("사진 저장 폴더가 없어요")
        return File(dir, "${UUID.randomUUID()}.$extension")
    }

    // Internals

    private fun decorate(spot: Spot, userId: String): Spot {
        val others = snapshot.users.values.filter { it.author.id != userId }
        val now = clock()
        val extraTips = snapshot.tips.filter { it.spotId == spot.id }.map { it.tip.copy(confirmedDaysAgo = daysSince(it.createdAt)) }
        return spot.copy(
            likeCount = spot.likeCount + others.count { spot.id in it.list(UserList.LIKED) },
            saveCount = spot.saveCount + others.count { spot.id in it.list(UserList.SAVED) },
            comments = mergeComments(spot.comments, snapshot.comments.filter { it.spotId == spot.id }, now),
            tips = (spot.tips + extraTips)
                .map { tip -> tip.copy(helpfulCount = tip.helpfulCount + others.count { tip.id in it.list(UserList.HELPFUL_TIPS) }) }
                .withConflictsFlagged(),
        )
    }

    private fun mergeComments(base: List<Comment>, stored: List<StoredComment>, now: Long): List<Comment> {
        fun StoredComment.toComment() = Comment(id, authorName, text, relativeTime(createdAt, now), isQuestion)
        val replies = stored.filter { it.parentId != null }.sortedBy { it.createdAt }.groupBy { it.parentId }
        val newTopLevel = stored.filter { it.parentId == null }.sortedByDescending { it.createdAt }.map { top ->
            top.toComment().copy(replies = replies[top.id].orEmpty().map { it.toComment() })
        }
        return newTopLevel + base.map { comment ->
            comment.copy(replies = comment.replies + replies[comment.id].orEmpty().map { it.toComment() })
        }
    }

    private fun findSpot(spotId: String): Spot =
        snapshot.posts.firstOrNull { it.spot.id == spotId }?.spot
            ?: SampleSpots.byId(spotId)
            ?: throw NoSuchElementException("장소를 찾을 수 없어요")

    private fun user(userId: String): StoredUser =
        snapshot.users[userId] ?: throw UnauthorizedException()

    private fun StoredUser.list(list: UserList): Set<String> = lists[list].orEmpty()

    private fun editUser(userId: String, transform: (StoredUser) -> StoredUser) {
        val user = user(userId)
        update { it.copy(users = it.users + (userId to transform(user))) }
    }

    private fun SendRequest.toItem() = SentItem(id, label, spotId, clock(), location)

    private fun NewTip.toHoneyTip(id: String, reporter: String) = HoneyTip(
        id = id,
        kind = kind,
        summary = summary.trim(),
        detail = detail.trim().ifEmpty { summary.trim() },
        status = status,
        helpfulCount = 0,
        confirmedDaysAgo = 0,
        reporter = reporter,
    )

    private fun daysSince(time: Long): Int = TimeUnit.MILLISECONDS.toDays(clock() - time).toInt()

    private fun newToken(): String = ByteArray(24).also(random::nextBytes).joinToString("") { "%02x".format(it) }

    private fun update(transform: (Snapshot) -> Snapshot) {
        snapshot = transform(snapshot)
        save()
    }

    private fun load(): Snapshot {
        val stored = stateFile?.takeIf { it.exists() }?.let { Api.json.decodeFromString(Snapshot.serializer(), it.readText()) }
        return stored ?: seed()
    }

    /** Sample authors exist from the start so "윤슬을따라" signs in to the account that owns 태안. */
    private fun seed(): Snapshot = Snapshot(
        users = SampleSpots.authors.associate { author ->
            val following = if (author.id == "u-yunseul") setOf("u-romance") else emptySet()
            author.id to StoredUser(author, lists = mapOf(UserList.FOLLOWING to following))
        },
    )

    private fun save() {
        val file = stateFile ?: return
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(Api.json.encodeToString(Snapshot.serializer(), snapshot))
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
    }

    companion object {
        const val PAIRING_TTL_MS = 10 * 60 * 1000L

        fun relativeTime(time: Long, now: Long): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(now - time)
            return when {
                minutes < 1 -> "방금 전"
                minutes < 60 -> "${minutes}분 전"
                minutes < 24 * 60 -> "${minutes / 60}시간 전"
                minutes < 48 * 60 -> "어제"
                else -> "${minutes / (24 * 60)}일 전"
            }
        }
    }
}
