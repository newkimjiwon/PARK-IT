package com.nangmanroad.core.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.nangmanroad.core.data.LatLng
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Equirectangular projection over South Korea. One world unit is one degree of latitude; longitude
 * is shrunk by cos(36°) so the peninsula keeps its shape. Accurate enough for a city-level map.
 */
object KoreaProjection {
    private const val REFERENCE_LAT = 36.0
    private val lngScale = cos(Math.toRadians(REFERENCE_LAT))

    /** Slightly north of the middle so the top search bar does not cover the DMZ-side spots. */
    val Center = LatLng(36.85, 127.9)

    /** Degrees of latitude that fit the view height at zoom 1. */
    const val SPAN_AT_ZOOM_1 = 5.6

    fun toWorld(point: LatLng): Offset =
        Offset((point.longitude * lngScale).toFloat(), (-point.latitude).toFloat())

    fun fromWorld(world: Offset): LatLng = LatLng(-world.y.toDouble(), world.x / lngScale)
}

/** What part of the map is on screen: a centre point and a zoom factor. */
data class MapCamera(val center: LatLng = KoreaProjection.Center, val zoom: Float = 1f) {

    fun pixelsPerUnit(viewSize: Size): Float = viewSize.height / KoreaProjection.SPAN_AT_ZOOM_1.toFloat() * zoom

    fun toScreen(point: LatLng, viewSize: Size): Offset {
        val scale = pixelsPerUnit(viewSize)
        val delta = KoreaProjection.toWorld(point) - KoreaProjection.toWorld(center)
        return Offset(viewSize.width / 2 + delta.x * scale, viewSize.height / 2 + delta.y * scale)
    }

    fun fromScreen(point: Offset, viewSize: Size): LatLng {
        val scale = pixelsPerUnit(viewSize)
        val world = KoreaProjection.toWorld(center) + (point - Offset(viewSize.width / 2, viewSize.height / 2)) / scale
        return KoreaProjection.fromWorld(world)
    }

    fun panBy(pan: Offset, viewSize: Size): MapCamera {
        val scale = pixelsPerUnit(viewSize)
        val world = KoreaProjection.toWorld(center) - pan / scale
        return copy(center = KoreaProjection.fromWorld(world).clamped())
    }

    /** Zooms around [focus] (a screen point) so the content under the fingers stays put. */
    fun zoomBy(factor: Float, focus: Offset, viewSize: Size): MapCamera {
        val newZoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val actual = newZoom / zoom
        if (actual == 1f) return this
        val scale = pixelsPerUnit(viewSize)
        val focusWorld = KoreaProjection.toWorld(center) +
            (focus - Offset(viewSize.width / 2, viewSize.height / 2)) / scale
        val centerWorld = focusWorld - (focusWorld - KoreaProjection.toWorld(center)) / actual
        return MapCamera(KoreaProjection.fromWorld(centerWorld).clamped(), newZoom)
    }

    fun isVisible(point: LatLng, viewSize: Size, margin: Float = 0f): Boolean {
        val p = toScreen(point, viewSize)
        return p.x >= -margin && p.y >= -margin && p.x <= viewSize.width + margin && p.y <= viewSize.height + margin
    }

    private fun LatLng.clamped() = LatLng(latitude.coerceIn(33.0, 38.8), longitude.coerceIn(124.8, 130.2))

    companion object {
        const val MIN_ZOOM = 0.8f
        const val MAX_ZOOM = 8f
    }
}

data class MarkerCluster<T>(val items: List<T>, val position: Offset) {
    val lead: T get() = items.first()
}

/**
 * Greedy screen-space clustering: walk the items (most important first) and attach each one to the
 * first cluster within [radius] pixels. Cheap and stable for the few dozen markers on screen.
 */
fun <T> clusterMarkers(items: List<Pair<T, Offset>>, radius: Float): List<MarkerCluster<T>> {
    val clusters = mutableListOf<Pair<MutableList<T>, Offset>>()
    for ((item, position) in items) {
        val home = clusters.firstOrNull { (_, anchor) -> hypot(anchor.x - position.x, anchor.y - position.y) < radius }
        if (home != null) home.first.add(item) else clusters.add(mutableListOf(item) to position)
    }
    return clusters.map { (members, anchor) -> MarkerCluster(members, anchor) }
}
