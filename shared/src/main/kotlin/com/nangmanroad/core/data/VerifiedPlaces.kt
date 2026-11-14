package com.nangmanroad.core.data

import kotlinx.serialization.Serializable

@Serializable
data class OfficialPlaceInfo(
    val sourceUrl: String,
    val checkedOn: String,
    val parking: String,
    val nightUse: String,
    val photoCredit: String? = null,
)

/** Official place records. Stable IDs include the provider so a refresh replaces the same place. */
object VerifiedPlaces {
    val all = listOf(
        camp(2778, "중랑가족캠핑장", "서울 중랑구", "서울특별시 중랑구 망우로87길 110",
            LatLng(37.6049896, 127.1093075), "예약할 때 사이트별 주차 조건을 확인하세요."),
        camp(3397, "한강공원 난지캠핑장", "서울 마포구", "서울특별시 마포구 한강난지로 28",
            LatLng(37.5699862, 126.8720578), "별도 주차장이 있어요. 이용 요금은 운영기관 안내를 확인하세요."),
        camp(2616, "자라섬 캠핑장", "경기 가평군", "경기도 가평군 가평읍 자라섬로 60",
            LatLng(37.8208749, 127.5209246), "자동차야영장이에요. 예약한 구역의 차량 진입 조건을 확인하세요."),
    ).distinctBy(Spot::id)

    private fun camp(id: Int, title: String, region: String, address: String, location: LatLng, parking: String) = Spot(
        id = "gocamping-$id",
        title = title,
        region = region,
        address = address,
        location = location,
        category = Category.QUIET,
        author = Author("official-places", "파킷 장소 안내", "공식 출처를 바탕으로 정리한 장소"),
        story = "방문 전에 예약할 수 있는지와 운영 시간을 확인하세요.",
        photos = (1..2).map { "file:///android_asset/place_photos/$id-$it.jpg" },
        likeCount = 0,
        saveCount = 0,
        postedDaysAgo = 0,
        tips = emptyList(),
        products = emptyList(),
        comments = emptyList(),
        carCampingNote = "야영장 예약과 차량에서 숙박할 수 있는지는 별도로 확인하세요.",
        officialPlace = OfficialPlaceInfo(
            sourceUrl = "https://www.gocamping.or.kr/bsite/camp/info/read.do?c_no=$id",
            checkedOn = "2026-11-12",
            parking = parking,
            nightUse = "야간 이용 시간과 차량 출입은 운영기관에 확인하세요.",
            photoCredit = "한국관광공사 고캠핑",
        ),
    )
}
