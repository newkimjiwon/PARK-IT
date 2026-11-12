package com.nangmanroad.server

import com.nangmanroad.core.data.Author
import com.nangmanroad.core.data.Comment
import com.nangmanroad.core.data.HoneyTip
import com.nangmanroad.core.data.SampleSpots
import com.nangmanroad.core.data.VerifiedPlaces
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.Visibility
import com.nangmanroad.core.data.withConflictsFlagged
import com.nangmanroad.core.data.CarCamping
import com.nangmanroad.core.sync.CarTransferProgress
import com.nangmanroad.core.sync.CarTransferReceipt
import com.nangmanroad.core.sync.Api
import com.nangmanroad.core.sync.CarCampingReport
import com.nangmanroad.core.sync.DEMO_ACCOUNT_PASSWORD
import com.nangmanroad.core.sync.Limits
import com.nangmanroad.core.sync.MIN_PASSWORD_LENGTH
import com.nangmanroad.core.sync.DeviceKind
import com.nangmanroad.core.sync.NewComment
import com.nangmanroad.core.sync.NewPost
import com.nangmanroad.core.sync.NewTip
import com.nangmanroad.core.sync.PairingCode
import com.nangmanroad.core.sync.PairingState
import com.nangmanroad.core.sync.PairingStatus
import com.nangmanroad.core.sync.PARKIT_PREFIX
import com.nangmanroad.core.sync.isParkit
import com.nangmanroad.core.sync.SendRequest
import com.nangmanroad.core.sync.SentItem
import com.nangmanroad.core.sync.Session
import com.nangmanroad.core.sync.UserList
import com.nangmanroad.core.sync.UserState
import kotlinx.serialization.Serializable
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.util.UUID
import java.util.concurrent.TimeUnit

class WrongDeviceException : RuntimeException("차량에서만 수신 상태를 보낼 수 있어요")

class UnauthorizedException(message: String = "로그인이 필요해요") : RuntimeException(message)

/** Too many wrong pairing codes in a short time; the client is told to wait. */
class TooManyAttemptsException(message: String) : RuntimeException(message)

@Serializable
private data class StoredUser(
    val author: Author,
    val lists: Map<UserList, Set<String>> = emptyMap(),
    val carQueue: List<SentItem> = emptyList(),
    val phoneInbox: List<SentItem> = emptyList(),
    /** PBKDF2 hash and salt, hex. Null for the seeded sample accounts, which open with the demo password. */
    val passwordHash: String? = null,
    val passwordSalt: String? = null,
    val receivedParkitIds: Set<String> = emptySet(),
)

@Serializable
private data class StoredSession(val token: String, val userId: String, val device: DeviceKind, val createdAt: Long, val lastUsedAt: Long = createdAt)

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
private data class StoredCarCamping(val spotId: String, val status: CarCamping, val note: String, val reporter: String, val createdAt: Long)

@Serializable
private data class Snapshot(
    val users: Map<String, StoredUser> = emptyMap(),
    val sessions: Map<String, StoredSession> = emptyMap(),
    val posts: List<StoredPost> = emptyList(),
    val comments: List<StoredComment> = emptyList(),
    val tips: List<StoredTip> = emptyList(),
    val carCamping: List<StoredCarCamping> = emptyList(),
)

private class Pairing(val id: String, val code: String, val expiresAt: Long) {
    var session: Session? = null
}

/**
 * All server state. Sample authors and spots are built in; everything users add is kept in memory
 * and written to SQLite after each change, with uploaded photos beside it in `photos/`.
 * One lock guards everything, which is plenty for a development server.
 */
class SyncStore(
    private val dataDir: File?,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val random = SecureRandom()
    private val stateFile = dataDir?.let { File(it, "state.json") }
    private val database = dataDir?.let { SnapshotDatabase(File(it, "parkit.sqlite")) }
    private var officialPlaces = VerifiedPlaces.all
    val photoDir: File? = dataDir?.let { File(it, "photos") }

    private var snapshot: Snapshot = load()
    private val pairings = mutableMapOf<String, Pairing>()

    init {
        photoDir?.mkdirs()
    }

    // Accounts

    /**
     * A new nickname creates an account with the given password; a known one must match. The
     * seeded sample accounts have no hash and open with [DEMO_ACCOUNT_PASSWORD] only.
     */
    @Synchronized
    fun signIn(nickname: String, password: String): Session {
        val name = nickname.trim()
        require(name.length in 2..Limits.NICKNAME) { "닉네임은 2~${Limits.NICKNAME}자로 정해 주세요" }
        val existing = snapshot.users.values.firstOrNull { it.author.name == name }
        val user = when {
            existing == null -> {
                require(password.length >= MIN_PASSWORD_LENGTH) { "비밀번호는 ${MIN_PASSWORD_LENGTH}자 이상으로 정해 주세요" }
                val salt = newSalt()
                StoredUser(
                    Author("u-${UUID.randomUUID().toString().take(8)}", name, "아직 올린 장소가 없어요"),
                    passwordHash = hashPassword(password, salt),
                    passwordSalt = salt,
                )
            }
            existing.passwordHash == null -> {
                if (!MessageDigest.isEqual(password.toByteArray(), DEMO_ACCOUNT_PASSWORD.toByteArray())) throw UnauthorizedException("비밀번호가 맞지 않아요")
                existing
            }
            else -> {
                val expected = existing.passwordHash.hexToBytes()
                val actual = hashPassword(password, existing.passwordSalt.orEmpty()).hexToBytes()
                if (!MessageDigest.isEqual(expected, actual)) throw UnauthorizedException("비밀번호가 맞지 않아요")
                existing
            }
        }
        val session = StoredSession(newToken(), user.author.id, DeviceKind.PHONE, clock())
        update { it.copy(users = it.users + (user.author.id to user), sessions = it.sessions + (session.token to session)) }
        return Session(session.token, user.author, DeviceKind.PHONE)
    }

    @Synchronized
    fun signOut(token: String) = update { it.copy(sessions = it.sessions - token) }

    /**
     * Returns the user id behind a bearer token, or throws [UnauthorizedException]. A token unused
     * for [SESSION_TTL_MS] is dropped; use is recorded at most once an hour to spare the disk.
     */
    @Synchronized
    fun userFor(token: String?, device: DeviceKind? = null): String {
        val session = snapshot.sessions[token] ?: throw UnauthorizedException()
        val now = clock()
        if (now - session.lastUsedAt > SESSION_TTL_MS) {
            update { it.copy(sessions = it.sessions - session.token) }
            throw UnauthorizedException("오래 쓰지 않아 로그아웃됐어요. 다시 로그인해 주세요")
        }
        if (now - session.lastUsedAt > SESSION_TOUCH_MS) {
            update { it.copy(sessions = it.sessions + (session.token to session.copy(lastUsedAt = now))) }
        }
        if (device != null && session.device != device) throw WrongDeviceException()
        return session.userId
    }

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
    fun acknowledgeCarQueue(token: String?, receipts: List<CarTransferReceipt>) {
        val userId = userFor(token, DeviceKind.CAR)
        val user = user(userId)
        val byTransfer = receipts.groupBy { it.id to it.sentAt }
        val queue = user.carQueue.map { item ->
            val progress = byTransfer[item.id to item.sentAt]?.maxOf { it.progress }
            if (progress != null && progress > item.carProgress) item.copy(carProgress = progress) else item
        }
        if (queue != user.carQueue) editUser(userId) { it.copy(carQueue = queue) }
    }

    @Synchronized
    fun removeFromCar(userId: String, id: String): UserState {
        editUser(userId) { it.copy(carQueue = it.carQueue.filter { item -> item.id != id }) }
        return state(userId)
    }

    @Synchronized
    fun sendToPhone(userId: String, request: SendRequest): UserState {
        val current = user(userId)
        val isParkit = request.id.startsWith(PARKIT_PREFIX)
        if (isParkit && (request.id in current.receivedParkitIds || current.phoneInbox.any { it.id == request.id })) {
            return state(userId)
        }
        val item = request.toItem()
        val spotId = request.spotId
        editUser(userId) {
            val visited = if (isParkit && spotId != null) {
                it.lists + (UserList.VISITED to (it.list(UserList.VISITED) + spotId))
            } else it.lists
            it.copy(
                phoneInbox = listOf(item) + it.phoneInbox.filter { existing -> existing.id != request.id },
                receivedParkitIds = if (isParkit) it.receivedParkitIds + request.id else it.receivedParkitIds,
                lists = visited,
            )
        }
        return state(userId)
    }

    @Synchronized
    fun removeFromPhone(userId: String, id: String): UserState {
        editUser(userId) {
            val received = if (it.phoneInbox.any { item -> item.id == id && item.isParkit }) {
                it.receivedParkitIds + id
            } else it.receivedParkitIds
            it.copy(phoneInbox = it.phoneInbox.filter { item -> item.id != id }, receivedParkitIds = received)
        }
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

    private val claimFailures = mutableMapOf<String, MutableList<Long>>()

    /**
     * A six-digit code only holds up if nobody can try a million of them: after
     * [MAX_CLAIM_ATTEMPTS] wrong codes in [CLAIM_WINDOW_MS] the account has to wait.
     */
    @Synchronized
    fun claimPairing(userId: String, code: String): UserState {
        val now = clock()
        val failures = claimFailures.getOrPut(userId) { mutableListOf() }
        failures.removeAll { it < now - CLAIM_WINDOW_MS }
        if (failures.size >= MAX_CLAIM_ATTEMPTS) {
            throw TooManyAttemptsException("코드를 너무 여러 번 틀렸어요. ${CLAIM_WINDOW_MS / 60_000}분 뒤에 다시 해 주세요")
        }
        val pairing = pairings.values.firstOrNull { it.code == code.trim() && it.session == null && it.expiresAt >= now }
        if (pairing == null) {
            failures += now
            throw NoSuchElementException("코드가 맞지 않거나 시간이 지났어요. 차량 화면의 새 코드를 입력해 주세요")
        }
        claimFailures.remove(userId)
        val stored = StoredSession(newToken(), userId, DeviceKind.CAR, clock())
        update { it.copy(sessions = it.sessions + (stored.token to stored)) }
        pairing.session = Session(stored.token, user(userId).author, DeviceKind.CAR)
        return state(userId)
    }

    // Spots

    @Synchronized
    internal fun setOfficialPhotos(photosByPlace: Map<String, List<String>>) {
        officialPlaces = VerifiedPlaces.all.map { place ->
            val photos = photosByPlace[place.id].orEmpty()
            if (photos.isEmpty()) place else place.copy(photos = photos)
        }
    }

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
        return (posts + SampleSpots.all + officialPlaces).map { decorate(it, userId) }
    }

    @Synchronized
    fun createPost(userId: String, post: NewPost): Spot {
        require(post.title.isNotBlank()) { "제목을 입력해 주세요" }
        require(post.photos.isNotEmpty()) { "사진을 한 장 이상 골라 주세요" }
        require(post.title.length <= Limits.TITLE) { "제목은 ${Limits.TITLE}자까지예요" }
        require(post.story.length <= Limits.STORY) { "경험은 ${Limits.STORY}자까지예요" }
        require(post.region.length <= Limits.REGION && post.address.length <= Limits.ADDRESS) { "지역 이름이 너무 길어요" }
        require(post.carCampingNote.length <= Limits.NOTE) { "근거는 ${Limits.NOTE}자까지예요" }
        require(post.bestTime.length <= Limits.BEST_TIME) { "가기 좋은 때는 ${Limits.BEST_TIME}자까지예요" }
        require(post.photos.size <= Limits.PHOTOS) { "사진은 ${Limits.PHOTOS}장까지예요" }
        require(post.tips.size <= Limits.TIPS && post.products.size <= Limits.PRODUCTS) { "꿀팁과 준비물이 너무 많아요" }
        post.tips.forEach { tip -> tip.checkLength() }
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
            visitConditions = post.visitConditions,
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
        require(comment.text.length <= Limits.COMMENT) { "댓글은 ${Limits.COMMENT}자까지예요" }
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
        tip.checkLength()
        val spot = findSpot(spotId)
        val stored = StoredTip(spotId, tip.toHoneyTip("tip-${UUID.randomUUID().toString().take(12)}", user(userId).author.name), clock())
        update { it.copy(tips = it.tips + stored) }
        return decorate(spot, userId)
    }

    /**
     * Reports are weighed per person (a reporter's newer word replaces their older one). A ban or
     * a "check" from one visitor is applied at once; lifting to "allowed" takes two visitors, and
     * two who disagree send the place back to "현장 확인".
     */
    @Synchronized
    fun reportCarCamping(userId: String, spotId: String, report: CarCampingReport): Spot {
        require(report.note.length <= Limits.NOTE) { "근거는 ${Limits.NOTE}자까지예요" }
        val spot = findSpot(spotId)
        val stored = StoredCarCamping(spotId, report.status, report.note.trim(), user(userId).author.name, clock())
        update { it.copy(carCamping = it.carCamping + stored) }
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
        // One voice per reporter: the same person reporting twice cannot outvote anyone.
        val reports = snapshot.carCamping.filter { it.spotId == spot.id }.sortedByDescending { it.createdAt }.distinctBy { it.reporter }
        val latest = reports.getOrNull(0)
        val previous = reports.getOrNull(1)
        val disputed = latest != null && previous != null &&
            latest.status != previous.status && latest.status != CarCamping.CHECK && previous.status != CarCamping.CHECK
        // Saying a place is fine to sleep at is the claim that can get someone towed: it needs a second visitor.
        val liftNeedsSecond = latest != null && !disputed && latest.status == CarCamping.ALLOWED &&
            spot.carCamping != CarCamping.ALLOWED && previous?.status != CarCamping.ALLOWED
        return spot.copy(
            carCamping = when {
                latest == null -> spot.carCamping
                disputed || liftNeedsSecond -> CarCamping.CHECK
                else -> latest.status
            },
            carCampingNote = when {
                latest == null -> spot.carCampingNote
                disputed -> "최근 제보가 엇갈려요 · ${latest.reporter}: ${latest.status.label}, ${previous!!.reporter}: ${previous.status.label}"
                liftNeedsSecond -> "${latest.reporter}님이 '차박 가능'으로 제보했어요 · 한 명이 더 확인하면 바뀌어요"
                latest.note.isNotBlank() -> latest.note
                else -> spot.carCampingNote
            },
            carCampingCheckedDaysAgo = latest?.let { daysSince(it.createdAt) } ?: spot.carCampingCheckedDaysAgo,
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
            ?: officialPlaces.firstOrNull { it.id == spotId }
            ?: throw NoSuchElementException("장소를 찾을 수 없어요")

    private fun user(userId: String): StoredUser =
        snapshot.users[userId] ?: throw UnauthorizedException()

    private fun StoredUser.list(list: UserList): Set<String> = lists[list].orEmpty()

    private fun editUser(userId: String, transform: (StoredUser) -> StoredUser) {
        val user = user(userId)
        update { it.copy(users = it.users + (userId to transform(user))) }
    }

    private fun SendRequest.toItem(): SentItem {
        require(note.length <= Limits.NOTE) { "메모는 ${Limits.NOTE}자까지예요" }
        return SentItem(id, label, spotId, clock(), location, note.trim(), approximateLocation)
    }

    private fun NewTip.checkLength() {
        require(summary.length <= Limits.TIP_SUMMARY) { "한 줄 꿀팁은 ${Limits.TIP_SUMMARY}자까지예요" }
        require(detail.length <= Limits.TIP_DETAIL) { "꿀팁 설명은 ${Limits.TIP_DETAIL}자까지예요" }
    }

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

    private fun newSalt(): String = ByteArray(16).also(random::nextBytes).joinToString("") { "%02x".format(it) }

    private fun hashPassword(password: String, saltHex: String): String {
        val spec = PBEKeySpec(password.toCharArray(), saltHex.hexToBytes(), PBKDF2_ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }
    }

    private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun update(transform: (Snapshot) -> Snapshot) {
        val next = transform(snapshot)
        database?.write(Api.json.encodeToString(Snapshot.serializer(), next))
        snapshot = next
    }

    private fun load(): Snapshot {
        val stored = database?.read()
        if (stored != null) return Api.json.decodeFromString(Snapshot.serializer(), stored)
        val imported = stateFile?.takeIf { it.isFile }?.readText() ?: return seed()
        val decoded = Api.json.decodeFromString(Snapshot.serializer(), imported)
        database?.write(imported)
        return decoded
    }

    /** Sample authors exist from the start so "윤슬을따라" signs in to the account that owns 태안. */
    private fun seed(): Snapshot = Snapshot(
        users = SampleSpots.authors.associate { author ->
            val following = if (author.id == "u-yunseul") setOf("u-romance") else emptySet()
            author.id to StoredUser(author, lists = mapOf(UserList.FOLLOWING to following))
        },
    )

    companion object {
        const val PAIRING_TTL_MS = 10 * 60 * 1000L
        const val SESSION_TTL_MS = 90L * 24 * 60 * 60 * 1000
        const val SESSION_TOUCH_MS = 60 * 60 * 1000L
        const val PBKDF2_ITERATIONS = 120_000
        const val MAX_CLAIM_ATTEMPTS = 5
        const val CLAIM_WINDOW_MS = 10 * 60 * 1000L

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
