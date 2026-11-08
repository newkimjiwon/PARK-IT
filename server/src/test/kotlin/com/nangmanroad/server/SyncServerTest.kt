package com.nangmanroad.server

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
import com.nangmanroad.core.sync.PairingState
import com.nangmanroad.core.sync.SendRequest
import com.nangmanroad.core.sync.Session
import com.nangmanroad.core.sync.SignInRequest
import com.nangmanroad.core.sync.ToggleRequest
import com.nangmanroad.core.sync.UserList
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
        val session = store.signIn("윤슬을따라")
        assertEquals("u-yunseul", session.user.id)
        assertEquals(setOf("u-romance"), store.state(store.userFor(session.token)).following)
    }

    @Test
    fun `counts on a spot leave out the requester`() {
        val store = SyncStore(folder.root)
        val me = store.userFor(store.signIn("윤슬을따라").token)
        val other = store.userFor(store.signIn("새친구").token)
        store.toggle(me, UserList.LIKED, "s-gangneung", on = true)

        assertEquals(324, store.spots(me).first { it.id == "s-gangneung" }.likeCount)
        assertEquals(325, store.spots(other).first { it.id == "s-gangneung" }.likeCount)
    }

    @Test
    fun `car pairing links the car to the phone's account once`() {
        val store = SyncStore(folder.root)
        val phone = store.signIn("윤슬을따라")
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
        val phone = store.signIn("윤슬을따라")
        val code = store.startPairing()
        now += SyncStore.PAIRING_TTL_MS + 1
        val failure = runCatching { store.claimPairing(store.userFor(phone.token), code.code) }.exceptionOrNull()
        assertTrue(failure is NoSuchElementException)
    }

    @Test
    fun `follower-only posts reach followers and nobody else`() {
        val store = SyncStore(folder.root)
        val author = store.userFor(store.signIn("작성자").token)
        val stranger = store.userFor(store.signIn("지나가는사람").token)
        val spot = store.createPost(author, post("숨은 해변", Visibility.FOLLOWERS))

        assertTrue(store.spots(author).any { it.id == spot.id })
        assertFalse(store.spots(stranger).any { it.id == spot.id })
        store.toggle(stranger, UserList.FOLLOWING, store.state(author).user.id, on = true)
        assertTrue(store.spots(stranger).any { it.id == spot.id })
    }

    @Test
    fun `replies attach to their comment and new comments come first`() {
        val store = SyncStore(folder.root)
        val me = store.userFor(store.signIn("윤슬을따라").token)
        store.addComment(me, "s-gangneung", NewComment("다녀왔어요", isQuestion = false))
        val spot = store.addComment(me, "s-gangneung", NewComment("저도 궁금해요", isQuestion = false, parentId = "c-gn-1"))

        assertEquals("다녀왔어요", spot.comments.first().text)
        assertEquals("저도 궁금해요", spot.comments.first { it.id == "c-gn-1" }.replies.last().text)
    }

    @Test
    fun `state survives a restart`() {
        val first = SyncStore(folder.root)
        val token = first.signIn("윤슬을따라").token
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
        val me = first.userFor(first.signIn("윤슬을따라").token)
        val parked = LatLng(37.7519, 128.8761)
        first.sendToPhone(me, SendRequest("${PARKIT_PREFIX}1700000000000", "파킷 · 강원 강릉시 근처 · 11월 8일 오후 9:40", null, parked))

        val inbox = SyncStore(folder.root).state(me).phoneInbox
        val item = inbox.single()
        assertTrue(item.isParkit)
        assertEquals(parked, item.location)
        assertEquals(null, item.spotId)
    }

    @Test
    fun `http api requires a token and round-trips state`() = testApplication {
        val store = SyncStore(folder.root)
        application { syncModule(store) }
        val client = createClient { install(ContentNegotiation) { json(Api.json) } }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/state").status)

        val session = client.post("/api/session") {
            contentType(ContentType.Application.Json)
            setBody(SignInRequest("윤슬을따라"))
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
