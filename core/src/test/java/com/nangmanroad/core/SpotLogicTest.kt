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
    fun `recommendations favour scenery the user already saved`() {
        val top = SpotQueries.recommended(spots, saved = setOf("s-jecheon"), liked = emptySet(), following = emptySet(), here = DefaultLocation)
        assertEquals(Category.STARS, top.first().category)
    }

    @Test
    fun `region title names a single province`() {
        assertEquals("강원의 낭만", SpotQueries.regionTitle(spots.filter { it.province == "강원" }))
        assertEquals("이 지역의 낭만", SpotQueries.regionTitle(spots))
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
