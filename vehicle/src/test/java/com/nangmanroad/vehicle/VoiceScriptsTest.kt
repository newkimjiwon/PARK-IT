package com.nangmanroad.vehicle

import com.nangmanroad.core.data.KoreaZone
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.SampleSpots
import com.nangmanroad.core.data.SpotQueries
import com.nangmanroad.vehicle.voice.TripFacts
import com.nangmanroad.vehicle.voice.VoiceCommand
import com.nangmanroad.vehicle.voice.arrivalReminder
import com.nangmanroad.vehicle.voice.departureBriefing
import com.nangmanroad.vehicle.voice.endsInConsonant
import com.nangmanroad.vehicle.voice.ieyo
import com.nangmanroad.vehicle.voice.parkitPrompt
import com.nangmanroad.vehicle.voice.parseCommand
import com.nangmanroad.vehicle.voice.searchQueryFromSpeech
import com.nangmanroad.vehicle.voice.spokenClock
import com.nangmanroad.vehicle.voice.spokenDistance
import com.nangmanroad.vehicle.voice.spokenDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class VoiceScriptsTest {

    private val seoul = LatLng(37.5665, 126.9780)
    private val gangneung = SampleSpots.all.first { it.id == "s-gangneung" }
    private val oneOClock = millis(2026, 11, 10, 13, 0)

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, KoreaZone).toInstant().toEpochMilli()

    private fun trip(remainMeters: Int? = 120_000, remainSeconds: Int? = 6_000, battery: Int? = 80) = TripFacts(
        spot = gangneung,
        destinationName = null,
        remainMeters = remainMeters,
        remainSeconds = remainSeconds,
        batteryPercent = battery,
        capacityKwh = 77.4,
        here = seoul,
        now = oneOClock,
    )

    @Test
    fun `the departure briefing reads the driving screen top to bottom`() {
        val text = departureBriefing(trip())!!

        val route = text.indexOf("강릉의 조용한 노을 명소까지 120킬로미터, 1시간 40분 걸려요.")
        val battery = text.indexOf("도착하면 배터리가 약 49퍼센트 남아요.")
        val sun = text.indexOf("일몰 오후 5시")
        val night = text.indexOf("차박이 금지된 곳이에요.")
        val tips = text.indexOf("도착 전에 알아 두세요.")
        assertTrue(text, listOf(route, battery, sun, night, tips).all { it >= 0 })
        assertTrue(text, route < battery && battery < sun && sun < night && night < tips)
        assertTrue(text, "먼저 도착해요" in text)
        // The disputed toilet comes first and says why it needs a look.
        assertTrue(text, "화장실, 확인이 필요해요. 제보가 엇갈리니 확인하세요." in text)
        assertFalse("units stay words for the synthesiser", "km" in text || "%" in text)
    }

    @Test
    fun `a low battery on arrival is said out loud`() {
        val text = departureBriefing(trip(battery = 40))!!
        assertTrue(text, "가는 길에 충전이 필요해요." in text)
    }

    @Test
    fun `no destination means nothing to brief`() {
        assertNull(departureBriefing(trip().copy(spot = null)))
    }

    @Test
    fun `the arrival reminder keeps the ban and the tips`() {
        val text = arrivalReminder(trip(remainMeters = 820, remainSeconds = 70))
        assertTrue(text, text.startsWith("곧 도착해요. 800미터 남았어요."))
        assertTrue(text, "차박이 금지된 곳이에요." in text)
        assertTrue(text, "도착 전에 알아 두세요." in text)
    }

    @Test
    fun `numbers are written the way they are said`() {
        assertEquals("800미터", spokenDistance(820))
        assertEquals("3.5킬로미터", spokenDistance(3_450))
        assertEquals("3킬로미터", spokenDistance(3_000))
        assertEquals("120킬로미터", spokenDistance(120_400))
        assertEquals("1시간 40분", spokenDuration(6_000))
        assertEquals("1시간", spokenDuration(3_600))
        assertEquals("2분", spokenDuration(61))
        assertEquals("오후 5시 23분", spokenClock(millis(2026, 11, 10, 17, 23)))
        assertEquals("오전 7시", spokenClock(millis(2026, 11, 10, 7, 0)))
        assertEquals("오전 12시 5분", spokenClock(millis(2026, 11, 10, 0, 5)))
    }

    @Test
    fun `particles follow the last syllable`() {
        assertEquals("오후 5시예요.", "오후 5시".ieyo())
        assertEquals("오후 5시 23분이에요.", "오후 5시 23분".ieyo())
        assertTrue("3".endsInConsonant())
        assertFalse("2".endsInConsonant())
    }

    @Test
    fun `spoken commands map to what the driver asked`() {
        assertEquals(VoiceCommand.TIPS, parseCommand("꿀팁 다시 읽어 줘"))
        assertEquals(VoiceCommand.REPEAT, parseCommand("다시 읽어 줘"))
        assertEquals(VoiceCommand.STOP, parseCommand("그만"))
        assertEquals(VoiceCommand.BATTERY, parseCommand("배터리 얼마나 남았어"))
        assertEquals(VoiceCommand.ETA, parseCommand("언제 도착해"))
        assertEquals(VoiceCommand.CAR_CAMPING, parseCommand("여기서 차박해도 돼?"))
        assertEquals(VoiceCommand.SUN, parseCommand("일몰 몇 시야"))
        assertEquals(VoiceCommand.UNKNOWN, parseCommand("안녕"))
    }

    @Test
    fun `a spoken search keeps only the words to match`() {
        assertEquals("태안 노을", searchQueryFromSpeech("태안 노을 보여 줘."))
        assertEquals("강릉", searchQueryFromSpeech("강릉 근처 장소 찾아줘"))
        assertEquals("태안 노을", searchQueryFromSpeech("태안에서 노을 검색"))
        val found = SpotQueries.matching(SampleSpots.all, null, searchQueryFromSpeech("태안 노을 보여 줘"))
        assertEquals(listOf("s-taean"), found.map { it.id })
    }

    @Test
    fun `the 파킷 prompt offers a spoken note only when the microphone works`() {
        assertEquals("여기, 파킷할까요? 음성 메모를 남길 수도 있어요.", parkitPrompt(null, canTakeNote = true))
        assertEquals("강릉의 조용한 노을 명소에 왔어요. 방문 완료로 표시할까요?", parkitPrompt(gangneung.title, canTakeNote = false))
    }
}
