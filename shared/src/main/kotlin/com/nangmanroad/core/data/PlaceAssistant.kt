package com.nangmanroad.core.data

enum class PlaceQuestion(val label: String) {
    PARKING("주차"), NIGHT("야간 이용"), CAMPING("차박"), TIPS("방문 팁"),
    BATTERY("배터리"), ETA("도착 시간"),
}

fun placeQuestions(text: String): List<PlaceQuestion> {
    val words = text.lowercase().filter(Char::isLetterOrDigit)
    return PlaceQuestion.entries.filter { question ->
        val clues = when (question) {
            PlaceQuestion.PARKING -> listOf("주차", "차세울", "차대도")
            PlaceQuestion.NIGHT -> listOf("야간", "밤에도", "밤에들어", "늦게가도")
            PlaceQuestion.CAMPING -> listOf("차박", "숙박", "자도", "잘수", "자고")
            PlaceQuestion.TIPS -> listOf("꿀팁", "방문팁", "주의", "확인해야", "알아둘", "준비")
            PlaceQuestion.BATTERY -> listOf("배터리", "충전", "왕복", "갔다올")
            PlaceQuestion.ETA -> listOf("언제도착", "도착시간", "얼마나남", "몇분", "얼마나걸")
        }
        clues.any(words::contains)
    }
}

/** Answers only from the selected place. A guest can ask even without a sync connection. */
fun answerAboutPlace(question: PlaceQuestion, spot: Spot): String? = when (question) {
    PlaceQuestion.PARKING -> spot.officialPlace?.let { "${it.parking} 출처는 고캠핑입니다." }
        ?: spot.visitorAnswer(TipKind.PARKING)
        ?: "주차 조건을 확인하지 못했어요. 운영기관이나 현장 안내를 확인하세요."
    PlaceQuestion.NIGHT -> spot.officialPlace?.let { "${it.nightUse} 출처는 고캠핑입니다." }
        ?: spot.visitorAnswer(TipKind.NIGHT)
        ?: "야간 이용 조건을 확인하지 못했어요."
    PlaceQuestion.CAMPING -> when {
        spot.carCampingStale -> "확인한 지 오래된 차박 정보입니다. 방문 전에 현장 안내를 확인하세요."
        spot.carCamping == CarCamping.CHECK -> "차박 허용 여부는 현장 확인이 필요해요."
        else -> "${spot.carCamping.label}으로 기록되어 있어요. 현장 안내도 확인하세요."
    }
    PlaceQuestion.TIPS -> spot.tips.byImportance().firstOrNull()?.let {
        if (it.status == TipStatus.NEEDS_CHECK) "${it.kind.label} 제보가 엇갈려요. 현장에서 확인하세요."
        else "방문자 제보 · ${it.kind.label}: ${it.summary}"
    } ?: "등록된 방문자 팁이 없어요."
    PlaceQuestion.BATTERY, PlaceQuestion.ETA -> null
}

private fun Spot.visitorAnswer(kind: TipKind): String? = tips.byImportance().firstOrNull { it.kind == kind }?.let {
    if (it.status == TipStatus.NEEDS_CHECK) "${kind.label} 제보가 엇갈려요. 현장에서 확인하세요."
    else "방문자 제보 · ${it.summary}"
}
