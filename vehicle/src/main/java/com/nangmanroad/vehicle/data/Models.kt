package com.nangmanroad.vehicle.data

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
) {
    /** First word of the region, e.g. "강원" for "강원 강릉시". */
    val province: String get() = region.substringBefore(' ')

    val commentCount: Int get() = comments.sumOf { 1 + it.replies.size }

    val lastConfirmedDaysAgo: Int? get() = tips.minOfOrNull { it.confirmedDaysAgo }
}
