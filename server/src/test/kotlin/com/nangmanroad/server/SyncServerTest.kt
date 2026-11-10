package com.nangmanroad.server

import com.nangmanroad.core.data.CarCamping
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.TipKind
import com.nangmanroad.core.data.TipStatus
import com.nangmanroad.core.data.Visibility
import com.nangmanroad.core.sync.Api
import com.nangmanroad.core.sync.DeviceKind
import com.nangmanroad.core.sync.NewComment
import com.nangmanroad.core.sync.NewPost
import com.nangmanroad.core.sync.NewTip
import com.nangmanroad.core.sync.PARKIT_PREFIX
import com.nangmanroad.core.sync.Limits
import com.nangmanroad.core.sync.PairingState
import com.nangmanroad.core.sync.SendRequest
import com.nangmanroad.core.sync.Session
import com.nangmanroad.core.sync.SignInRequest
import com.nangmanroad.core.sync.ToggleRequest
import com.nangmanroad.core.sync.UserList
import com.nangmanroad.core.sync.CarCampingReport
import com.nangmanroad.core.sync.DEMO_ACCOUNT_PASSWORD
import com.nangmanroad.core.sync.UserState
import com.nangmanroad.core.sync.isParkit
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SyncServerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun post(title: String, visibility: Visibility) = NewPost(
        title = title,
        region = "강원 강릉시",
        address = "강원특별자치도 강릉시",
        location = LatLng(37.75, 128.88),
        category = Category.SEA_SUNSET,
        story = "",
        photos = listOf("/photos/a.jpg"),
        tips = listOf(NewTip(TipKind.TOILET, TipStatus.PREPARE, "화장실이 없어요", "")),
        products = emptyList(),
        visibility = visibility,
        approximateLocation = false,
    )

    @Test
    fun `sample author signs in to the seeded account`() {
        val store = SyncStore(folder.root)
        val session = store.signIn("윤슬을따라", DEMO_ACCOUNT_PASSWORD)
        assertEquals("u-yunseul", session.user.id)
        assertEquals(setOf("u-romance"), store.state(store.userFor(session.token)).following)
    }

    @Test
    fun `counts on a spot leave out the requester`() {
        val store = SyncStore(folder.root)
        val me = store.userFor(store.signIn("윤슬을따라", DEMO_ACCOUNT_PASSWORD).token)
        val other = store.userFor(store.signIn("새친구", "test-pass").token)
        store.toggle(me, UserList.LIKED, "s-gangneung", on = true)

        assertEquals(324, store.spots(me).first { it.id == "s-gangneung" }.likeCount)
        assertEquals(325, store.spots(other).first { it.id == "s-gangneung" }.likeCount)
    }

    @Test
    fun `car pairing links the car to the phone's account once`() {
        val store = SyncStore(folder.root)
        val phone = store.signIn("윤슬을따라", DEMO_ACCOUNT_PASSWORD)
        val code = store.startPairing()
        assertEquals(PairingState.WAITING, store.pairingStatus(code.pairingId).state)

        val afterClaim = store.claimPairing(store.userFor(phone.token), code.code)
        assertEquals(1, afterClaim.linkedCars)

        val linked = store.pairingStatus(code.pairingId)
        assertEquals(PairingState.LINKED, linked.state)
        val car = assertNotNull(linked.session)
        assertEquals(DeviceKind.CAR, car.device)
        assertEquals(phone.user.id, store.userFor(car.token))
        assertEquals(PairingState.EXPIRED, store.pairingStatus(code.pairingId).state)
    }

    @Test
    fun `expired codes cannot be claimed`() {
        var now = 0L
        val store = SyncStore(folder.root) { now }
        val phone = store.signIn("윤슬을따라", DEMO_ACCOUNT_PASSWORD)
        val code = store.startPairing()
        now += SyncStore.PAIRING_TTL_MS + 1
        val failure = runCatching { store.claimPairing(store.userFor(phone.token), code.code) }.exceptionOrNull()
        assertTrue(failure is NoSuchElementException)
    }

    @Test
    fun `a published post keeps the author's car-camping answer`() {
        val store = SyncStore(folder.root)
        val author = store.userFor(store.signIn("작성자", "test-pass").token)
        val banned = post("단속 주차장", Visibility.PUBLIC).copy(carCamping = CarCamping.BANNED, carCampingNote = " 야간 주차 금지 안내판 ", bestTime = " 맑은 날 일몰 ")
        val spot = store.createPost(author, banned)
        assertEquals(CarCamping.BANNED, spot.carCamping)
        assertEquals("야간 주차 금지 안내판", spot.carCampingNote)
        assertEquals("맑은 날 일몰", spot.bestTime)
        // Older phones that send no answer land on the honest default.
        assertEquals(CarCamping.CHECK, store.createPost(author, post("모르는 곳", Visibility.PUBLIC)).carCamping)
    }

    @Test
    fun `car-camping reports update the badge and disagreement falls back to check`() {
        val store = SyncStore(folder.root)
        val first = store.userFor(store.signIn("첫째", "test-pass").token)
        val second = store.userFor(store.signIn("둘째", "test-pass").token)
        val banned = store.reportCarCamping(first, "s-taean", CarCampingReport(CarCamping.BANNED, " 입구에 금지 안내판 "))
        assertEquals(CarCamping.BANNED, banned.carCamping)
        assertEquals("입구에 금지 안내판", banned.carCampingNote)
        assertEquals(0, banned.carCampingCheckedDaysAgo)
        val disputed = store.reportCarCamping(second, "s-taean", CarCampingReport(CarCamping.ALLOWED))
        assertEquals(CarCamping.CHECK, disputed.carCamping)
        assertTrue(disputed.carCampingNote.startsWith("최근 제보가 엇갈려요"))
        // A later report that agrees with the previous one settles it again.
        assertEquals(CarCamping.ALLOWED, store.reportCarCamping(first, "s-taean", CarCampingReport(CarCamping.ALLOWED)).carCamping)
    }

    @Test
    fun `lifting a ban takes two visitors and one person cannot vote twice`() {
        val store = SyncStore(folder.root)
        val first = store.userFor(store.signIn("첫째", "test-pass").token)
        val second = store.userFor(store.signIn("둘째", "test-pass").token)
        // 강릉 is banned in the sample data. One visitor saying "allowed" only flags it for checking.
        val once = store.reportCarCamping(first, "s-gangneung", CarCampingReport(CarCamping.ALLOWED))
        assertEquals(CarCamping.CHECK, once.carCamping)
        assertTrue(once.carCampingNote.contains("한 명이 더 확인하면"))
        // The same person again changes nothing.
        assertEquals(CarCamping.CHECK, store.reportCarCamping(first, "s-gangneung", CarCampingReport(CarCamping.ALLOWED)).carCamping)
        // A second visitor agreeing does.
        assertEquals(CarCamping.ALLOWED, store.reportCarCamping(second, "s-gangneung", CarCampingReport(CarCamping.ALLOWED)).carCamping)
        // A ban from one visitor is applied at once: that is the safe direction.
        assertEquals(CarCamping.BANNED, store.reportCarCamping(second, "s-taean", CarCampingReport(CarCamping.BANNED)).carCamping)
    }

    @Test
    fun `too many wrong pairing codes lock the account for a while`() {
        var now = 1_700_000_000_000L
        val store = SyncStore(folder.root, clock = { now })
        val me = store.userFor(store.signIn("윤슬을따라", DEMO_ACCOUNT_PASSWORD).token)
        store.startPairing()
        repeat(5) { assertFailsWith<NoSuchElementException> { store.claimPairing(me, "000000") } }
        assertFailsWith<TooManyAttemptsException> { store.claimPairing(me, "000000") }
        now += 11 * 60 * 1000L
        assertFailsWith<NoSuchElementException> { store.claimPairing(me, "000000") }
    }

    @Test
    fun `over-long text is refused with a clear message`() {
        val store = SyncStore(folder.root)
        val me = store.userFor(store.signIn("윤슬을따라", DEMO_ACCOUNT_PASSWORD).token)
        val error = assertFailsWith<IllegalArgumentException> { store.createPost(me, post("x".repeat(61), Visibility.PUBLIC)) }
        assertTrue(error.message!!.contains("60자"))
        assertFailsWith<IllegalArgumentException> { store.addComment(me, "s-gangneung", NewComment("y".repeat(501), isQuestion = false)) }
    }

    @Test
    fun `passwords are required, hashed and checked`() {
        val store = SyncStore(folder.root)
        // The seeded demo account opens only with the demo password.
        assertFailsWith<UnauthorizedException> { store.signIn("윤슬을따라", "wrong-pass") }
        assertEquals("u-yunseul", store.signIn("윤슬을따라", DEMO_ACCOUNT_PASSWORD).user.id)
        // A new nickname needs a real password and then keeps it.
        assertFailsWith<IllegalArgumentException> { store.signIn("새사람", "123") }
        val created = store.signIn("새사람", "secret-7")
        assertEquals(created.user.id, store.signIn("새사람", "secret-7").user.id)
        assertFailsWith<UnauthorizedException> { store.signIn("새사람", "secret-8") }
        // The password itself never lands in the state file.
        val saved = folder.root.resolve("state.json").readText()
        assertTrue("secret-7" !in saved)
    }

    @Test
    fun `sessions expire after ninety idle days`() {
        var now = 1_700_000_000_000L
        val store = SyncStore(folder.root, clock = { now })
        val token = store.signIn("윤슬을따라", DEMO_ACCOUNT_PASSWORD).token
        now += 89L * 24 * 60 * 60 * 1000
        store.userFor(token)   // use keeps it alive
        now += 89L * 24 * 60 * 60 * 1000
        store.userFor(token)
        now += 91L * 24 * 60 * 60 * 1000
        assertFailsWith<UnauthorizedException> { store.userFor(token) }
    }

    @Test
    fun `follower-only posts reach followers and nobody else`() {
        val store = SyncStore(folder.root)
        val author = store.userFor(store.signIn("작성자", "test-pass").token)
        val stranger = store.userFor(store.signIn("지나가는사람", "test-pass").token)
        val spot = store.createPost(author, post("숨은 해변", Visibility.FOLLOWERS))

        assertTrue(store.spots(author).any { it.id == spot.id })
        assertFalse(store.spots(stranger).any { it.id == spot.id })
        store.toggle(stranger, UserList.FOLLOWING, store.state(author).user.id, on = true)
        assertTrue(store.spots(stranger).any { it.id == spot.id })
    }

    @Test
    fun `replies attach to their comment and new comments come first`() {
        val store = SyncStore(folder.root)
        val me = store.userFor(store.signIn("윤슬을따라", DEMO_ACCOUNT_PASSWORD).token)
        store.addComment(me, "s-gangneung", NewComment("다녀왔어요", isQuestion = false))
        val spot = store.addComment(me, "s-gangneung", NewComment("저도 궁금해요", isQuestion = false, parentId = "c-gn-1"))

        assertEquals("다녀왔어요", spot.comments.first().text)
        assertEquals("저도 궁금해요", spot.comments.first { it.id == "c-gn-1" }.replies.last().text)
    }

    @Test
    fun `state survives a restart`() {
        val first = SyncStore(folder.root)
        val token = first.signIn("윤슬을따라", DEMO_ACCOUNT_PASSWORD).token
        val me = first.userFor(token)
        val spot = first.createPost(me, post("다시 켜도 남는 장소", Visibility.PUBLIC))
        first.sendToCar(me, SendRequest(spot.id, spot.title, spot.id))

        val second = SyncStore(folder.root)
        assertEquals(me, second.userFor(token))
        assertTrue(second.spots(me).any { it.id == spot.id })
        assertEquals(listOf(spot.id), second.state(me).carQueue.map { it.spotId })
    }

    @Test
    fun `a 파킷 keeps where the car parked across a restart`() {
        val first = SyncStore(folder.root)
        val me = first.userFor(first.signIn("윤슬을따라", DEMO_ACCOUNT_PASSWORD).token)
        val parked = LatLng(37.7519, 128.8761)
        first.sendToPhone(
            me,
            SendRequest("${PARKIT_PREFIX}1700000000000", "파킷 · 강원 강릉시 근처 · 11월 8일 오후 9:40", null, parked, note = " 화장실이 깨끗하고 조용해요 "),
        )

        val inbox = SyncStore(folder.root).state(me).phoneInbox
        val item = inbox.single()
        assertTrue(item.isParkit)
        assertEquals(parked, item.location)
        assertEquals(null, item.spotId)
        assertEquals("화장실이 깨끗하고 조용해요", item.note)
    }

    @Test
    fun `a spoken 파킷 note longer than the limit is refused`() {
        val store = SyncStore(folder.root)
        val me = store.userFor(store.signIn("윤슬을따라", DEMO_ACCOUNT_PASSWORD).token)
        assertFailsWith<IllegalArgumentException> {
            store.sendToPhone(me, SendRequest("${PARKIT_PREFIX}1", "파킷", null, LatLng(37.0, 127.0), note = "가".repeat(Limits.NOTE + 1)))
        }
    }

    @Test
    fun `http api requires a token and round-trips state`() = testApplication {
        val store = SyncStore(folder.root)
        application { syncModule(store) }
        val client = createClient { install(ContentNegotiation) { json(Api.json) } }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/state").status)

        val session = client.post("/api/session") {
            contentType(ContentType.Application.Json)
            setBody(SignInRequest("윤슬을따라", DEMO_ACCOUNT_PASSWORD))
        }.body<Session>()
        val state = client.post("/api/state/toggle") {
            bearerAuth(session.token)
            contentType(ContentType.Application.Json)
            setBody(ToggleRequest(UserList.SAVED, "s-taean", on = true))
        }.body<UserState>()
        assertEquals(setOf("s-taean"), state.saved)
        assertEquals(HttpStatusCode.OK, client.get("/api/spots") { bearerAuth(session.token) }.status)
    }
}
