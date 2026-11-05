package com.nangmanroad.core.data

enum class FeedSort(val label: String) {
    POPULAR("인기"),
    LATEST("최신"),
    FOLLOWING("팔로잉"),
}

enum class AlbumTab(val label: String) {
    RECOMMENDED("추천"),
    FOLLOWING("팔로잉"),
    LATEST("최신"),
    SAVED("저장한 장소"),
}

/** Pure list logic shared by the map feed and the album, kept apart from Compose for tests. */
object SpotQueries {

    fun matching(spots: List<Spot>, category: Category?, query: String): List<Spot> {
        val needle = query.trim()
        return spots.filter { spot ->
            (category == null || spot.category == category) &&
                (needle.isEmpty() || listOf(spot.title, spot.region, spot.address, spot.author.name, spot.category.label)
                    .any { it.contains(needle, ignoreCase = true) })
        }
    }

    fun feed(
        spots: List<Spot>,
        visibleIds: Set<String>?,
        sort: FeedSort,
        following: Set<String>,
    ): List<Spot> {
        val inView = if (visibleIds == null) spots else spots.filter { it.id in visibleIds }
        return when (sort) {
            FeedSort.POPULAR -> inView.sortedByDescending { it.likeCount }
            FeedSort.LATEST -> inView.sortedBy { it.postedDaysAgo }
            FeedSort.FOLLOWING -> inView.filter { it.author.id in following }.sortedBy { it.postedDaysAgo }
        }
    }

    fun album(
        spots: List<Spot>,
        tab: AlbumTab,
        saved: Set<String>,
        liked: Set<String>,
        following: Set<String>,
        here: LatLng,
    ): List<Spot> = when (tab) {
        AlbumTab.RECOMMENDED -> recommended(spots, saved, liked, following, here)
        AlbumTab.FOLLOWING -> spots.filter { it.author.id in following }.sortedBy { it.postedDaysAgo }
        AlbumTab.LATEST -> spots.sortedBy { it.postedDaysAgo }
        AlbumTab.SAVED -> spots.filter { it.id in saved }
    }

    /**
     * Rule-based "당신이 좋아할 낭만": favour the scenery types the user already liked or saved,
     * creators they follow, popular and recent posts, and places within a day trip.
     */
    fun recommended(
        spots: List<Spot>,
        saved: Set<String>,
        liked: Set<String>,
        following: Set<String>,
        here: LatLng,
    ): List<Spot> {
        val preferred = spots.filter { it.id in saved || it.id in liked }.map { it.category }.toSet()
        return spots.sortedByDescending { spot ->
            var score = spot.likeCount / 100.0
            if (spot.category in preferred) score += 3
            if (spot.author.id in following) score += 2
            score += (14 - spot.postedDaysAgo).coerceAtLeast(0) / 7.0
            score -= distanceKm(here, spot.location) / 200.0
            score
        }
    }

    /** "강원의 낭만" when everything in view shares a province, otherwise a neutral title. */
    fun regionTitle(spots: List<Spot>): String {
        val provinces = spots.map { it.province }.distinct()
        return if (provinces.size == 1) "${provinces.single()}의 낭만" else "이 지역의 낭만"
    }
}
