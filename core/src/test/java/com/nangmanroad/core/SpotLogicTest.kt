package com.nangmanroad.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.nangmanroad.core.data.AlbumTab
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.data.DefaultLocation
import com.nangmanroad.core.data.FeedSort
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.SampleSpots
import com.nangmanroad.core.data.SpotQueries
import com.nangmanroad.core.data.HoneyTip
import com.nangmanroad.core.data.TipKind
import com.nangmanroad.core.data.TipStatus
import com.nangmanroad.core.data.distanceKm
import com.nangmanroad.core.data.withConflictsFlagged
import com.nangmanroad.core.data.KoreaZone
import com.nangmanroad.core.data.formatClock
import com.nangmanroad.core.data.sunPlan
import com.nangmanroad.core.data.sunTimes
import com.nangmanroad.core.data.travelSummary
import java.time.LocalTime
import java.time.Instant
import java.time.LocalDate
import com.nangmanroad.core.map.MapCamera
import com.nangmanroad.core.map.clusterMarkers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotLogicTest {

    private val spots = SampleSpots.all

    @Test
    fun `category filter and text search narrow the list`() {
        val stars = SpotQueries.matching(spots, Category.STARS, "")
        assertTrue(stars.isNotEmpty())
        assertTrue(stars.all { it.category == Category.STARS })

        val gangneung = SpotQueries.matching(spots, null, "강릉")
        assertEquals(listOf("s-gangneung"), gangneung.map { it.id })
    }

    @Test
    fun `facility filter keeps only places visitors confirmed`() {
        val withToilet = SpotQueries.matching(spots, null, "", setOf(TipKind.TOILET))
        assertTrue(withToilet.isNotEmpty())
        // 강릉's toilet reports disagree, so it must not be promised.
        assertTrue(withToilet.none { it.id == "s-gangneung" })
        val toiletAndPower = SpotQueries.matching(spots, null, "", setOf(TipKind.TOILET, TipKind.POWER))
        assertEquals(listOf("s-goseong"), toiletAndPower.map { it.id })
        // EV charging is its own facility: 강릉 and 양평 have confirmed chargers nearby, 고성 says nothing.
        val charging = SpotQueries.matching(spots, null, "", setOf(TipKind.CHARGING)).map { it.id }
        assertTrue(charging.containsAll(listOf("s-gangneung", "s-yangpyeong")) && "s-goseong" !in charging)
    }

    @Test
    fun `feed keeps only spots in view and respects following`() {
        val visible = setOf("s-gangneung", "s-jecheon")
        val popular = SpotQueries.feed(spots, visible, FeedSort.POPULAR, emptySet())
        assertEquals(listOf("s-gangneung", "s-jecheon"), popular.map { it.id })

        val following = SpotQueries.feed(spots, visible, FeedSort.FOLLOWING, setOf("u-nightsky"))
        assertEquals(listOf("s-jecheon"), following.map { it.id })
    }

    @Test
    fun `saved album tab lists exactly the saved spots`() {
        val saved = SpotQueries.album(spots, AlbumTab.SAVED, setOf("s-taean"), emptySet(), emptySet(), DefaultLocation)
        assertEquals(listOf("s-taean"), saved.map { it.id })
    }

    @Test
    fun `visited album tab lists the places been to, farthest first, and sums them up`() {
        val visited = SpotQueries.album(spots, AlbumTab.VISITED, emptySet(), emptySet(), emptySet(), DefaultLocation, visited = setOf("s-taean", "s-gangneung"))
        assertEquals(listOf("s-gangneung", "s-taean"), visited.map { it.id })
        val summary = travelSummary(visited, DefaultLocation)
        assertEquals(2, summary.places)
        assertEquals(setOf("강원", "충남"), summary.provinces)
        assertEquals("s-gangneung", summary.farthest?.id)
        assertEquals(0, travelSummary(emptyList(), DefaultLocation).places)
    }

    @Test
    fun `recommendations favour scenery the user already saved`() {
        val top = SpotQueries.recommended(spots, saved = setOf("s-jecheon"), liked = emptySet(), following = emptySet(), here = DefaultLocation)
        assertEquals(Category.STARS, top.first().category)
    }

    @Test
    fun `region title names a single province`() {
        assertEquals("강원", SpotQueries.regionTitle(spots.filter { it.province == "강원" }))
        assertEquals("지도에 보이는 곳", SpotQueries.regionTitle(spots))
    }

    @Test
    fun `distance from Seoul to Gangneung is about 160km`() {
        val km = distanceKm(DefaultLocation, LatLng(37.75, 128.88))
        assertTrue("was $km", km in 150.0..175.0)
    }

    @Test
    fun `nearby markers merge and distant ones stay apart`() {
        val clusters = clusterMarkers(
            listOf("a" to Offset(0f, 0f), "b" to Offset(30f, 0f), "c" to Offset(500f, 0f)),
            radius = 96f,
        )
        assertEquals(listOf(listOf("a", "b"), listOf("c")), clusters.map { it.items })
    }

    @Test
    fun `zooming keeps the focus point under the finger`() {
        val view = Size(1600f, 900f)
        val camera = MapCamera()
        val focus = Offset(400f, 300f)
        val before = camera.toScreen(LatLng(37.75, 128.88), view)
        val zoomed = camera.zoomBy(2f, focus, view)
        val after = zoomed.toScreen(LatLng(37.75, 128.88), view)
        // A point under the focus scales away from it by exactly the zoom factor.
        assertEquals(focus.x + (before.x - focus.x) * 2, after.x, 0.5f)
        assertEquals(focus.y + (before.y - focus.y) * 2, after.y, 0.5f)
    }

    @Test
    fun `sun times in Gangneung match the published table within a few minutes`() {
        // badatime.com lists 강릉 2026-11-01 as 06:49 / 17:26; the equation is good to about two minutes.
        val sun = sunTimes(spots.first { it.id == "s-gangneung" }.location, LocalDate.of(2026, 11, 1))!!
        val rise = Instant.ofEpochMilli(sun.sunrise).atZone(KoreaZone).toLocalTime()
        val set = Instant.ofEpochMilli(sun.sunset).atZone(KoreaZone).toLocalTime()
        assertTrue("sunrise $rise", rise.hour == 6 && rise.minute in 45..53)
        assertTrue("sunset $set", set.hour == 17 && set.minute in 22..30)
        assertTrue(formatClock(sun.sunset).startsWith("오후 5:"))
    }

    @Test
    fun `the sun sets later on the west coast than on the east coast`() {
        val day = LocalDate.of(2026, 11, 8)
        val gangneung = sunTimes(spots.first { it.id == "s-gangneung" }.location, day)!!
        val taean = sunTimes(spots.first { it.id == "s-taean" }.location, day)!!
        assertTrue(taean.sunset - gangneung.sunset > 5 * 60_000L)
    }

    @Test
    fun `leaving Seoul at noon makes the Gangneung sunset, leaving at five does not`() {
        val gangneung = spots.first { it.id == "s-gangneung" }
        fun at(time: LocalTime) = LocalDate.of(2026, 11, 1).atTime(time).atZone(KoreaZone).toInstant().toEpochMilli()
        val noon = sunPlan(gangneung, DefaultLocation, now = at(LocalTime.NOON))!!
        assertTrue(noon.toString(), noon.makesIt)
        // The sunset must be the one on the day of departure, not the day before.
        assertTrue(noon.sunAt - at(LocalTime.NOON) in 4 * 3_600_000L..7 * 3_600_000L)
        assertTrue(noon.verdict(), noon.verdict().startsWith("지금 출발하면 일몰"))
        // Leaving by about half past one keeps 15 minutes to settle before a 17:26 sunset.
        assertTrue(noon.departureHint(), noon.departureHint()!!.startsWith("늦어도 오후 1:"))
        val five = sunPlan(gangneung, DefaultLocation, now = at(LocalTime.of(17, 0)))!!
        assertTrue(!five.makesIt)
        assertTrue(five.verdict(), five.verdict().startsWith("오늘 일몰은 늦었어요"))
        assertTrue(five.departureHint(), five.departureHint()!!.startsWith("내일은 늦어도 오후 1:"))
        // A live route that says ten minutes to go beats the straight-line guess.
        val almostThere = sunPlan(gangneung, DefaultLocation, now = at(LocalTime.of(17, 0)), driveSeconds = 600)!!
        assertTrue(almostThere.makesIt && almostThere.fromRoute)
        // A drive spot has no sun moment, so no plan.
        assertEquals(null, sunPlan(spots.first { it.category == Category.DRIVE }, DefaultLocation))
    }

    @Test
    fun `tips of one kind that disagree are all flagged for checking`() {
        fun tip(id: String, kind: TipKind, status: TipStatus) = HoneyTip(id, kind, "", "", status, 0, 0, "")
        val flagged = listOf(
            tip("a", TipKind.TOILET, TipStatus.GOOD),
            tip("b", TipKind.TOILET, TipStatus.PREPARE),
            tip("c", TipKind.PARKING, TipStatus.PREPARE),
        ).withConflictsFlagged()
        assertEquals(listOf(TipStatus.NEEDS_CHECK, TipStatus.NEEDS_CHECK, TipStatus.PREPARE), flagged.map { it.status })
    }
}
