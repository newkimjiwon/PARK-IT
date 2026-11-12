package com.nangmanroad.core.data

import kotlinx.serialization.Serializable

@Serializable
data class LatLng(val latitude: Double, val longitude: Double)

@Serializable
enum class Category(val label: String) {
    SEA_SUNSET("바다와 노을"),
    STARS("별이 보이는 곳"),
    FOREST("산과 숲"),
    LAKE_RIVER("호수와 강"),
    DRIVE("드라이브 명소"),
    QUIET("조용히 쉬기 좋은 곳"),
}

@Serializable
data class Author(
    val id: String,
    val name: String,
    val bio: String,
)

@Serializable
enum class TipKind(val label: String) {
    TOILET("화장실"),
    PARKING("주차"),
    POWER("전기"),
    /** EV charging nearby: the first thing a Pleos driver asks before a night away from home. */
    CHARGING("충전"),
    SIGNAL("통신"),
    NIGHT("야간"),
    STORE("편의점"),
    ACCESS("진입로"),
    CAUTION("주의"),
}

/** The facilities a card answers at a glance, in the order a 차박 visitor asks about them. */
val FacilityKinds: List<TipKind> = listOf(TipKind.TOILET, TipKind.POWER, TipKind.CHARGING, TipKind.SIGNAL, TipKind.STORE, TipKind.PARKING)

/** A car-camping status older than this is shown faded and called out as stale: rules change by season. */
const val STALE_CHECK_DAYS = 90

/** True when the last confirmation is older than [STALE_CHECK_DAYS]; never for an unconfirmed author's word. */
val Spot.carCampingStale: Boolean get() = (carCampingCheckedDaysAgo ?: 0) > STALE_CHECK_DAYS

/** "오늘 확인", "어제 확인", "12일 전 확인", or "130일 전 확인 · 오래된 정보" past the stale line. */
fun carCampingCheckedLabel(daysAgo: Int): String = when {
    daysAgo == 0 -> "오늘 확인"
    daysAgo == 1 -> "어제 확인"
    daysAgo > STALE_CHECK_DAYS -> "${daysAgo}일 전 확인 · 오래된 정보"
    else -> "${daysAgo}일 전 확인"
}

/** How a tip should read at a glance: fine, worth preparing for, or reports that disagree. */
@Serializable
enum class TipStatus { GOOD, PREPARE, NEEDS_CHECK }

@Serializable
data class HoneyTip(
    val id: String,
    val kind: TipKind,
    val summary: String,
    val detail: String,
    val status: TipStatus,
    val helpfulCount: Int,
    val confirmedDaysAgo: Int,
    val reporter: String,
)

@Serializable
data class Product(
    val id: String,
    val name: String,
    val reason: String,
    val recommender: String,
    val priceWon: Int?,
    val isSponsored: Boolean,
    val imageUrl: String,
)

@Serializable
data class Comment(
    val id: String,
    val author: String,
    val text: String,
    val postedAgo: String,
    val isQuestion: Boolean = false,
    val replies: List<Comment> = emptyList(),
)

@Serializable
enum class Visibility(val label: String) {
    PUBLIC("전체 공개"),
    FOLLOWERS("팔로워 공개"),
    PRIVATE("나만 보기"),
}

/**
 * Whether you can actually sleep in the car there. Camping apps rarely say, and councils such as
 * 강릉 now fine and tow overnight campers in public lots, so a spot states it up front.
 */
@Serializable
enum class CarCamping(val label: String) {
    ALLOWED("차박 가능"),
    CHECK("현장 확인"),
    BANNED("차박 금지"),
}

@Serializable
data class Spot(
    val id: String,
    val title: String,
    val region: String,
    val address: String,
    val location: LatLng,
    val category: Category,
    val author: Author,
    val story: String,
    val photos: List<String>,
    val likeCount: Int,
    val saveCount: Int,
    val postedDaysAgo: Int,
    val tips: List<HoneyTip>,
    val products: List<Product>,
    val comments: List<Comment>,
    val visibility: Visibility = Visibility.PUBLIC,
    /** True when the author chose to show only the city/county; [location] is then its centre. */
    val approximateLocation: Boolean = false,
    val carCamping: CarCamping = CarCamping.CHECK,
    /** Why, in the reporter's words: a council notice, a sign at the lot, a ranger's warning. */
    val carCampingNote: String = "",
    /** When the place is at its best, in the author's words: "맑은 날 일몰 30분 전", "가을 새벽 물안개". */
    val bestTime: String = "",
    /** Days since a visitor last confirmed [carCamping]; null when only the author's word exists. */
    val carCampingCheckedDaysAgo: Int? = null,
    val officialPlace: OfficialPlaceInfo? = null,
) {
    /** First word of the region, e.g. "강원" for "강원 강릉시". */
    val province: String get() = region.substringBefore(' ')

    val commentCount: Int get() = comments.sumOf { 1 + it.replies.size }

    val lastConfirmedDaysAgo: Int? get() = tips.minOfOrNull { it.confirmedDaysAgo }
}

/**
 * Visitors sometimes disagree (one says there is a toilet, another says there is none). When tips of
 * the same kind point in different directions, every one of them is flagged as needing a check.
 */
fun List<HoneyTip>.withConflictsFlagged(): List<HoneyTip> {
    val conflicting = groupBy { it.kind }
        .filterValues { tips -> tips.map { it.status }.filter { it != TipStatus.NEEDS_CHECK }.distinct().size > 1 }
        .keys
    return map { if (it.kind in conflicting) it.copy(status = TipStatus.NEEDS_CHECK) else it }
}

/**
 * The one status that sums up every tip of a kind: a disagreement wins over a warning, a warning
 * over a reassurance. Null when nobody has reported on that facility yet.
 */
fun List<HoneyTip>.facilityStatus(kind: TipKind): TipStatus? =
    filter { it.kind == kind }.map { it.status }.minByOrNull { status ->
        when (status) {
            TipStatus.NEEDS_CHECK -> 0
            TipStatus.PREPARE -> 1
            TipStatus.GOOD -> 2
        }
    }

/**
 * Whether visitors say the facility is there. A caveat ("준비하세요") still counts as present;
 * disagreeing reports or silence do not, so a filter never promises a toilet nobody confirmed.
 */
fun Spot.hasFacility(kind: TipKind): Boolean =
    when (tips.facilityStatus(kind)) {
        TipStatus.GOOD, TipStatus.PREPARE -> true
        TipStatus.NEEDS_CHECK, null -> false
    }

/** Problems first, then things to prepare, then the reassuring ones. */
fun List<HoneyTip>.byImportance(): List<HoneyTip> = sortedWith(
    compareBy<HoneyTip> { it.status != TipStatus.NEEDS_CHECK }
        .thenBy { it.status != TipStatus.PREPARE }
        .thenByDescending { it.helpfulCount },
)

/**
 * The few tips worth knowing before pulling in: anything to check or prepare, most important
 * first. The driving screen shows these and the voice guide reads the same ones.
 */
fun Spot.arrivalTips(limit: Int = 3): List<HoneyTip> =
    tips.filter { it.status != TipStatus.GOOD }.byImportance().take(limit)
