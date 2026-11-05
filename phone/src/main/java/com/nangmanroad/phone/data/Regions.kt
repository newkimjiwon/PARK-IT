package com.nangmanroad.phone.data

import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.distanceKm

/** City/county a post can be pinned to. [address] is what the post shows; [short] is for cards. */
data class Region(val short: String, val address: String, val center: LatLng)

/** Regions people drive to for 차박 and scenery. Used for search and for "시·군까지만 공개". */
object Regions {
    val all = listOf(
        Region("서울 종로구", "서울특별시 종로구", LatLng(37.5730, 126.9794)),
        Region("인천 강화군", "인천광역시 강화군", LatLng(37.7470, 126.4878)),
        Region("경기 가평군", "경기도 가평군", LatLng(37.8315, 127.5105)),
        Region("경기 양평군", "경기도 양평군", LatLng(37.4917, 127.4876)),
        Region("강원 춘천시", "강원특별자치도 춘천시", LatLng(37.8813, 127.7298)),
        Region("강원 속초시", "강원특별자치도 속초시", LatLng(38.2070, 128.5918)),
        Region("강원 고성군", "강원특별자치도 고성군", LatLng(38.3806, 128.4679)),
        Region("강원 양양군", "강원특별자치도 양양군", LatLng(38.0754, 128.6190)),
        Region("강원 강릉시", "강원특별자치도 강릉시", LatLng(37.7519, 128.8761)),
        Region("강원 평창군", "강원특별자치도 평창군", LatLng(37.3708, 128.3903)),
        Region("강원 영월군", "강원특별자치도 영월군", LatLng(37.1837, 128.4617)),
        Region("강원 삼척시", "강원특별자치도 삼척시", LatLng(37.4499, 129.1652)),
        Region("충북 제천시", "충청북도 제천시", LatLng(37.1326, 128.1910)),
        Region("충북 단양군", "충청북도 단양군", LatLng(36.9845, 128.3655)),
        Region("충남 태안군", "충청남도 태안군", LatLng(36.7456, 126.2979)),
        Region("충남 보령시", "충청남도 보령시", LatLng(36.3334, 126.6128)),
        Region("전북 부안군", "전북특별자치도 부안군", LatLng(35.7317, 126.7334)),
        Region("전남 여수시", "전라남도 여수시", LatLng(34.7604, 127.6622)),
        Region("전남 순천시", "전라남도 순천시", LatLng(34.9506, 127.4872)),
        Region("경북 경주시", "경상북도 경주시", LatLng(35.8562, 129.2247)),
        Region("경북 울진군", "경상북도 울진군", LatLng(36.9930, 129.4004)),
        Region("경남 하동군", "경상남도 하동군", LatLng(35.0674, 127.7513)),
        Region("경남 남해군", "경상남도 남해군", LatLng(34.8376, 127.8924)),
        Region("부산 기장군", "부산광역시 기장군", LatLng(35.2445, 129.2222)),
        Region("제주 서귀포시", "제주특별자치도 서귀포시", LatLng(33.2541, 126.5600)),
    )

    fun search(query: String): List<Region> {
        val needle = query.trim()
        return if (needle.isEmpty()) all else all.filter { it.short.contains(needle) || it.address.contains(needle) }
    }

    fun nearest(point: LatLng): Region = all.minBy { distanceKm(it.center, point) }
}
