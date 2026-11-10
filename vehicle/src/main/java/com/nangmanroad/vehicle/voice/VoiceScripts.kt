package com.nangmanroad.vehicle.voice

import com.nangmanroad.core.data.ARRIVAL_COMFORT_PERCENT
import com.nangmanroad.core.data.ARRIVAL_LOW_PERCENT
import com.nangmanroad.core.data.CarCamping
import com.nangmanroad.core.data.KoreaZone
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.data.SunMoment
import com.nangmanroad.core.data.TipStatus
import com.nangmanroad.core.data.arrivalPercent
import com.nangmanroad.core.data.arrivalTips
import com.nangmanroad.core.data.at
import com.nangmanroad.core.data.carCampingStale
import com.nangmanroad.core.data.sunMoment
import com.nangmanroad.core.data.sunPlan
import com.nangmanroad.core.data.sunTimesToday
import java.time.Instant
import kotlin.math.roundToInt

/*
 * What the car says out loud. Everything here is plain text for the Pleos TextToSpeech engine:
 * units written as words ("킬로미터", "퍼센트") and clock times as "오후 5시 23분", because a
 * synthesiser reads "5:23" or "km" unpredictably. Sentences stay short so a driver can follow
 * them without looking, and the order matches the driving screen top to bottom.
 */

/** The facts the briefings are built from, gathered by the view model at the moment of speaking. */
data class TripFacts(
    /** The known place the car is heading to, when the route matches one. */
    val spot: Spot?,
    /** The navigation's own name for the destination; falls back to [spot]'s title. */
    val destinationName: String?,
    val remainMeters: Int?,
    val remainSeconds: Int?,
    val batteryPercent: Int?,
    val capacityKwh: Double?,
    val here: LatLng,
    val now: Long,
) {
    val name: String? get() = destinationName ?: spot?.title
}

/** Read when the car leaves P with somewhere to go: the whole driving screen, in its order. */
fun departureBriefing(trip: TripFacts): String? {
    val name = trip.name ?: return null
    val lines = mutableListOf<String>()
    lines += if (trip.remainMeters != null && trip.remainSeconds != null) {
        "${name}까지 ${spokenDistance(trip.remainMeters)}, ${spokenDuration(trip.remainSeconds)} 걸려요."
    } else {
        "${name}까지 안내를 시작해요."
    }
    arrivalBatteryLine(trip)?.let(lines::add)
    sunLine(trip)?.let(lines::add)
    trip.spot?.let { lines += carCampingLine(it) }
    trip.spot?.let(::tipsLine)?.let(lines::add)
    return lines.joinToString(" ")
}

/** Read once when the route has about a kilometre left: the tips again, while they still matter. */
fun arrivalReminder(trip: TripFacts): String {
    val lines = mutableListOf<String>()
    lines += if (trip.remainMeters != null) "곧 도착해요. ${spokenDistance(trip.remainMeters)} 남았어요." else "곧 도착해요."
    val spot = trip.spot
    if (spot != null && spot.carCamping == CarCamping.BANNED) lines += carCampingLine(spot)
    spot?.let(::tipsLine)?.let(lines::add)
    return lines.joinToString(" ")
}

/** When the phone sends a place to the car. */
fun phoneSentAnnouncement(title: String): String = "휴대폰에서 장소를 보냈어요. $title."

/** When the car stops after a drive and offers a 파킷. */
fun parkitPrompt(nearSpotTitle: String?, canTakeNote: Boolean): String {
    val ask = if (nearSpotTitle != null) "${nearSpotTitle}에 왔어요. 방문 완료로 표시할까요?" else "여기, 파킷할까요?"
    return if (canTakeNote) "$ask 말로 메모를 남길 수도 있어요." else ask
}

fun tipsReadout(spot: Spot?): String {
    if (spot == null) return NO_DESTINATION
    return tipsLine(spot) ?: "${spot.title}에는 미리 알아 둘 꿀팁이 없어요."
}

fun batteryReadout(trip: TripFacts): String {
    val percent = trip.batteryPercent ?: return "배터리 정보를 읽을 수 없어요."
    val now = "배터리는 지금 ${percent}퍼센트예요."
    return listOfNotNull(now, arrivalBatteryLine(trip)).joinToString(" ")
}

fun sunReadout(trip: TripFacts): String {
    sunLine(trip)?.let { return it }
    // No sun place on the route: say when the sun goes down where the car is.
    val sunset = sunTimesToday(trip.spot?.location ?: trip.here, trip.now)?.sunset ?: return "오늘 해 지는 시각을 계산할 수 없어요."
    return if (sunset > trip.now) "오늘 일몰은 ${spokenClock(sunset).ieyo()}" else "오늘 해는 이미 졌어요."
}

fun carCampingReadout(spot: Spot?): String = spot?.let(::carCampingLine) ?: NO_DESTINATION

fun etaReadout(trip: TripFacts): String {
    val name = trip.name ?: return NO_DESTINATION
    val meters = trip.remainMeters
    val seconds = trip.remainSeconds
    if (meters == null || seconds == null) return "${name}까지 남은 거리를 아직 받지 못했어요."
    return "${name}까지 ${spokenDistance(meters)}, ${spokenDuration(seconds)} 남았어요. " +
        "${spokenClock(trip.now + seconds * 1000L)}쯤 도착해요."
}

const val NO_DESTINATION = "안내 중인 목적지가 없어요."
const val COMMAND_HELP = "'다시 읽어 줘', '꿀팁 알려 줘', '배터리 얼마나 남았어'처럼 말해 보세요."

private fun arrivalBatteryLine(trip: TripFacts): String? {
    val percent = trip.batteryPercent ?: return null
    val capacity = trip.capacityKwh ?: return null
    val meters = trip.remainMeters ?: return null
    val left = arrivalPercent(percent, capacity, meters / 1000.0)
    val base = "도착하면 배터리가 약 ${left}퍼센트 남아요."
    return when {
        left < ARRIVAL_LOW_PERCENT -> "$base 가는 길에 충전이 필요해요."
        left < ARRIVAL_COMFORT_PERCENT -> "$base 돌아올 때는 충전하세요."
        else -> base
    }
}

/** Only for places whose point is the sun: will we be there before it sets (or after it rises)? */
private fun sunLine(trip: TripFacts): String? {
    val spot = trip.spot ?: return null
    spot.category.sunMoment ?: return null
    val plan = sunPlan(spot, trip.here, trip.now, trip.remainSeconds) ?: return null
    val label = plan.moment.label
    return if (plan.makesIt) {
        "$label ${spokenClock(plan.sunAt)}보다 ${spokenDuration(plan.minutesToSpare * 60)} 먼저 도착해요."
    } else {
        val after = if (plan.moment == SunMoment.SUNSET) "해가 진 뒤에" else "해가 뜬 뒤에"
        "${label}은 ${spokenClock(plan.sunAt).ira()} $after 도착해요."
    }
}

private fun carCampingLine(spot: Spot): String {
    val line = when (spot.carCamping) {
        CarCamping.ALLOWED -> "차박할 수 있는 곳이에요."
        CarCamping.BANNED -> "차박이 금지된 곳이에요."
        CarCamping.CHECK -> "차박 가능 여부는 현장 안내문으로 확인하세요."
    }
    val note = spot.carCampingNote.trim().takeIf { it.isNotEmpty() && spot.carCamping != CarCamping.CHECK }
    val stale = if (spot.carCampingStale && spot.carCamping != CarCamping.CHECK) "확인한 지 오래된 정보예요." else null
    return listOfNotNull(line, note?.let { it.trimEnd('.') + "." }, stale).joinToString(" ")
}

private fun tipsLine(spot: Spot): String? {
    val tips = spot.arrivalTips()
    if (tips.isEmpty()) return null
    val items = tips.joinToString(" ") { tip ->
        val check = if (tip.status == TipStatus.NEEDS_CHECK) " 제보가 엇갈리니 확인하세요." else ""
        "${tip.kind.label}, ${tip.summary.trimEnd('.')}.$check"
    }
    return "도착 전에 알아 두세요. $items"
}

/** "800미터", "3.5킬로미터", "120킬로미터". */
fun spokenDistance(meters: Int): String = when {
    meters < 1_000 -> "${(meters / 100.0).roundToInt().coerceAtLeast(1) * 100}미터"
    meters < 10_000 -> {
        val km = (meters / 100.0).roundToInt() / 10.0
        if (km % 1.0 == 0.0) "${km.toInt()}킬로미터" else "${km}킬로미터"
    }
    else -> "${(meters / 1000.0).roundToInt()}킬로미터"
}

/** "25분", "1시간", "1시간 40분"; rounds up so it never promises less time than the route says. */
fun spokenDuration(seconds: Int): String {
    val minutes = ((seconds + 59) / 60).coerceAtLeast(1)
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}분"
        rest == 0 -> "${hours}시간"
        else -> "${hours}시간 ${rest}분"
    }
}

/** "오후 5시 23분", "오전 7시" on the Korean clock. */
fun spokenClock(epochMillis: Long): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(KoreaZone)
    val half = if (time.hour < 12) "오전" else "오후"
    val hour = (time.hour % 12).let { if (it == 0) 12 else it }
    return if (time.minute == 0) "$half ${hour}시" else "$half ${hour}시 ${time.minute}분"
}

/*
 * Korean particles depend on whether the last syllable ends in a consonant. Place names come from
 * can end in anything, so the sentence picks 이에요/예요 and 이라/라 to fit; digits read as Sino-Korean.
 */

/** Whether the word, read aloud, ends in a final consonant (받침). Unknown endings count as open. */
internal fun String.endsInConsonant(): Boolean {
    val last = trimEnd().lastOrNull { it.isLetterOrDigit() } ?: return false
    return when {
        last in '가'..'힣' -> (last - '가') % 28 != 0
        // 영 일 이 삼 사 오 육 칠 팔 구
        last.isDigit() -> last in "013678"
        else -> false
    }
}

/** "오후 5시 23분이라", "오후 5시라". */
internal fun String.ira(): String = this + if (endsInConsonant()) "이라" else "라"

/** "오후 5시 23분이에요", "오후 5시예요". */
internal fun String.ieyo(): String = this + if (endsInConsonant()) "이에요." else "예요."

/** What a driver can ask for with the microphone on the driving screen. */
enum class VoiceCommand { STOP, TIPS, BATTERY, SUN, CAR_CAMPING, ETA, REPEAT, UNKNOWN }

/**
 * Speech-to-text gives words, not intents, so the car matches a few keywords itself. Topics are
 * checked before "다시", so "꿀팁 다시 읽어 줘" reads the tips rather than the whole briefing.
 */
fun parseCommand(heard: String): VoiceCommand {
    val text = heard.replace(Regex("\\s+"), "")
    fun has(vararg words: String) = words.any { it in text }
    return when {
        text.isEmpty() -> VoiceCommand.UNKNOWN
        has("그만", "멈춰", "조용", "중지", "닥쳐", "끄기", "꺼줘") -> VoiceCommand.STOP
        has("꿀팁", "팁", "알아둘", "주의할") -> VoiceCommand.TIPS
        has("배터리", "충전", "전비") -> VoiceCommand.BATTERY
        has("일몰", "일출", "노을", "해지", "해가", "해뜨", "해는") -> VoiceCommand.SUN
        has("차박", "자도돼", "잘수있", "자고가") -> VoiceCommand.CAR_CAMPING
        has("얼마나", "남았", "언제도착", "도착시간", "몇분", "몇시", "도착예정") -> VoiceCommand.ETA
        has("다시", "한번더", "브리핑", "읽어", "안내해") -> VoiceCommand.REPEAT
        else -> VoiceCommand.UNKNOWN
    }
}

private val REQUEST_ENDINGS = listOf(
    "찾아 줘", "찾아줘", "찾아 주세요", "찾아봐", "검색해 줘", "검색해줘", "검색", "보여 줘", "보여줘", "보여 주세요",
    "알려 줘", "알려줘", "가고 싶어", "가자", "어디야", "어디",
)
private val FILLER_WORDS = setOf("근처", "주변", "쪽", "곳", "장소", "좀", "데", "있는")

/** "태안 노을 보여 줘." → "태안 노을": the words a spoken search actually wants matched. */
fun searchQueryFromSpeech(heard: String): String {
    var text = heard.replace(Regex("[.,!?~]"), " ").trim()
    var changed = true
    while (changed) {
        changed = false
        for (ending in REQUEST_ENDINGS) {
            if (text.endsWith(ending)) {
                text = text.removeSuffix(ending).trim()
                changed = true
            }
        }
    }
    return text.split(Regex("\\s+"))
        .map { it.removeSuffix("에서").removeSuffix("에") }
        .filter { it.isNotEmpty() && it !in FILLER_WORDS }
        .joinToString(" ")
}
