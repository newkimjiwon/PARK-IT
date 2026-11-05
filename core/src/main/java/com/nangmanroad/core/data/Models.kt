package com.nangmanroad.core.data

data class LatLng(val latitude: Double, val longitude: Double)

enum class Category(val label: String) {
    SEA_SUNSET("바다와 노을"),
    STARS("별이 보이는 곳"),
    FOREST("산과 숲"),
    LAKE_RIVER("호수와 강"),
    DRIVE("드라이브 명소"),
    QUIET("조용히 쉬기 좋은 곳"),
}

data class Author(
    val id: String,
    val name: String,
    val bio: String,
)

enum class TipKind(val label: String) {
    TOILET("화장실"),
    PARKING("주차"),
    POWER("전기"),
    SIGNAL("통신"),
    NIGHT("야간"),
    STORE("편의점"),
    ACCESS("진입로"),
    CAUTION("주의"),
}

/** How a tip should read at a glance: fine, worth preparing for, or reports that disagree. */
enum class TipStatus { GOOD, PREPARE, NEEDS_CHECK }

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

data class Product(
    val id: String,
    val name: String,
    val reason: String,
    val recommender: String,
    val priceWon: Int?,
    val isSponsored: Boolean,
    val imageUrl: String,
)

data class Comment(
    val id: String,
    val author: String,
    val text: String,
    val postedAgo: String,
    val isQuestion: Boolean = false,
    val replies: List<Comment> = emptyList(),
)

enum class Visibility(val label: String) {
    PUBLIC("전체 공개"),
    FOLLOWERS("팔로워 공개"),
    PRIVATE("나만 보기"),
}

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
