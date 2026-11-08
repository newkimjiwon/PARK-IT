package com.nangmanroad.core.sync

import com.nangmanroad.core.data.Author
import com.nangmanroad.core.data.CarCamping
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.Product
import com.nangmanroad.core.data.TipKind
import com.nangmanroad.core.data.TipStatus
import com.nangmanroad.core.data.Visibility
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The contract between the sync server and the car and phone apps. Every request except sign-in,
 * pairing and photos carries `Authorization: Bearer <token>`.
 *
 * | Method | Path                      | Body            | Response        |
 * | ------ | ------------------------- | --------------- | --------------- |
 * | POST   | /api/session              | SignInRequest   | Session         |
 * | DELETE | /api/session              |                 |                 |
 * | GET    | /api/state                |                 | UserState       |
 * | POST   | /api/state/toggle         | ToggleRequest   | UserState       |
 * | POST   | /api/car-queue            | SendRequest     | UserState       |
 * | DELETE | /api/car-queue/{id}       |                 | UserState       |
 * | POST   | /api/phone-inbox          | SendRequest     | UserState       |
 * | DELETE | /api/phone-inbox/{id}     |                 | UserState       |
 * | GET    | /api/spots                |                 | List<Spot>      |
 * | POST   | /api/spots                | NewPost         | Spot            |
 * | POST   | /api/spots/{id}/comments  | NewComment      | Spot            |
 * | POST   | /api/spots/{id}/tips      | NewTip          | Spot            |
 * | POST   | /api/photos               | image bytes     | UploadedPhoto   |
 * | POST   | /api/pairings             |                 | PairingCode     |
 * | GET    | /api/pairings/{id}        |                 | PairingStatus   |
 * | POST   | /api/pairings/claim       | ClaimRequest    | UserState       |
 *
 * Counts on a returned Spot (likes, saves, "도움이 됐어요") leave out the requesting user, so a client
 * adds its own like on top exactly as it does offline.
 */
object Api {
    const val DEFAULT_PORT = 8080

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}

@Serializable
enum class DeviceKind { PHONE, CAR }

/** Demo sign-in: a nickname is enough. The server must not be exposed beyond development. */
@Serializable
data class SignInRequest(val nickname: String)

@Serializable
data class Session(val token: String, val user: Author, val device: DeviceKind)

/** Something one device handed to the other: a spot for the car, or a spot/product for the phone. */
@Serializable
data class SentItem(val id: String, val label: String, val spotId: String? = null, val sentAt: Long, val location: LatLng? = null)

/**
 * "파킷": the car captured where it stopped and handed it to the phone. Such an item's id starts
 * with this prefix, its [SentItem.location] is where the car parked and its [SentItem.spotId] is the
 * known spot it stopped at, if any. The phone turns a new place into a post and a known one into a
 * tip check.
 */
const val PARKIT_PREFIX = "parkit:"

val SentItem.isParkit: Boolean get() = id.startsWith(PARKIT_PREFIX)

/** The account's own lists. Every device linked to the account sees the same state. */
@Serializable
data class UserState(
    val user: Author,
    val saved: Set<String> = emptySet(),
    val liked: Set<String> = emptySet(),
    val following: Set<String> = emptySet(),
    val helpfulTips: Set<String> = emptySet(),
    val visited: Set<String> = emptySet(),
    val savedProducts: Set<String> = emptySet(),
    /** Spots sent from the phone to the car, newest first. */
    val carQueue: List<SentItem> = emptyList(),
    /** Spots and products sent from the car to open on the phone, newest first. */
    val phoneInbox: List<SentItem> = emptyList(),
    val linkedCars: Int = 0,
)

@Serializable
enum class UserList { SAVED, LIKED, FOLLOWING, HELPFUL_TIPS, VISITED, SAVED_PRODUCTS }

@Serializable
data class ToggleRequest(val list: UserList, val id: String, val on: Boolean)

@Serializable
data class SendRequest(val id: String, val label: String, val spotId: String? = null, val location: LatLng? = null)

@Serializable
data class PairingCode(val pairingId: String, val code: String, val expiresAt: Long)

@Serializable
enum class PairingState { WAITING, LINKED, EXPIRED }

/** [session] is filled in once, when the phone has claimed the code. */
@Serializable
data class PairingStatus(val state: PairingState, val session: Session? = null)

@Serializable
data class ClaimRequest(val code: String)

@Serializable
data class NewTip(val kind: TipKind, val status: TipStatus, val summary: String, val detail: String)

@Serializable
data class NewComment(val text: String, val isQuestion: Boolean, val parentId: String? = null)

@Serializable
data class NewPost(
    val title: String,
    val region: String,
    val address: String,
    val location: LatLng,
    val category: Category,
    val story: String,
    /** Absolute URLs or paths returned by POST /api/photos. */
    val photos: List<String>,
    val tips: List<NewTip>,
    val products: List<Product>,
    val visibility: Visibility,
    val approximateLocation: Boolean,
    /** Whether the author says the night can be spent here; defaults keep older phones publishing. */
    val carCamping: CarCamping = CarCamping.CHECK,
    val carCampingNote: String = "",
)

/** [path] is relative to the server, e.g. "/photos/3f2a.jpg". */
@Serializable
data class UploadedPhoto(val path: String)

@Serializable
data class ApiError(val message: String)
