package com.nangmanroad.core.map

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.ui.Palette
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.Projection
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.MapProperties
import com.naver.maps.map.compose.MapType
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import java.util.Locale
import com.naver.maps.geometry.LatLng as NaverLatLng

private const val TAG = "NaverPhotoMap"

/** Zoom level at which the selected place fills the view with its surroundings. */
private const val FOCUS_ZOOM = 13.0

private fun LatLng.toNaver() = NaverLatLng(latitude, longitude)

/** Our Canvas zoom (1 = the peninsula fits the view) in NAVER zoom levels. */
private fun naverZoomFor(zoom: Float): Double =
    if (zoom < 1.5f) 5.6 + zoom * 0.6 else (10.0 + zoom * 0.8).coerceAtMost(FOCUS_ZOOM)

private fun Projection.toScreen(point: LatLng): Offset = toScreenLocation(point.toNaver()).let { Offset(it.x, it.y) }

private fun Offset.inside(size: IntSize): Boolean = x >= 0f && y >= 0f && x <= size.width && y <= size.height

/**
 * [PhotoMap] on a real NAVER map, in night mode while the apps are dark and day style otherwise. The map draws the roads; the photo markers, the
 * car's position and the controls stay our own composables laid over it, placed with the map's
 * projection, so they look exactly as they do on the Canvas map and animate the same way.
 */
@OptIn(ExperimentalNaverMapApi::class)
@Composable
internal fun NaverPhotoMap(
    spots: List<Spot>,
    selectedId: String?,
    here: LatLng?,
    onSelect: (Spot) -> Unit,
    onVisibleChange: (Set<String>) -> Unit,
    modifier: Modifier,
    style: PhotoMapStyle,
    markerTint: (Spot) -> Color?,
    onMapTap: ((LatLng) -> Unit)?,
    herePulse: Long?,
    mapConfig: MapConfig,
    focus: MapFocus?,
    onMyLocation: (() -> Unit)?,
    pin: LatLng?,
) {
    val context = LocalContext.current
    remember(context, mapConfig) {
        NaverMapSdk.getInstance(context).apply {
            client = if (mapConfig.clientType == "legacy") {
                NaverMapSdk.NaverCloudPlatformClient(mapConfig.keyId)
            } else {
                NaverMapSdk.NcpKeyClient(mapConfig.keyId)
            }
            onAuthFailedListener = NaverMapSdk.OnAuthFailedListener { error ->
                Log.w(TAG, "NAVER Maps auth failed: ${error.errorCode} ${error.message} (${error.javaClass.simpleName})")
            }
        }
    }

    val camera = rememberCameraPositionState {
        position = CameraPosition(style.initialCamera.center.toNaver(), naverZoomFor(style.initialCamera.zoom))
    }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val clusterRadius = with(LocalDensity.current) { style.clusterRadius.toPx() }
    LaunchedEffect(focus?.id) {
        focus?.let { camera.move(CameraUpdate.scrollAndZoomTo(it.center.toNaver(), naverZoomFor(it.zoom)).animate(CameraAnimation.Easing)) }
    }

    // Reading the camera here re-runs the projection whenever the map moves.
    val cameraPosition = camera.position
    val moving = camera.isMoving
    val projection = camera.projection
    val clusters = remember(spots, selectedId, cameraPosition, moving, projection, viewSize) {
        if (projection == null || viewSize == IntSize.Zero) emptyList() else {
            val ordered = spots.sortedWith(
                compareByDescending<Spot> { it.id == selectedId }.thenByDescending { it.likeCount },
            )
            clusterMarkers(ordered.map { it to projection.toScreen(it.location) }, clusterRadius)
        }
    }
    val herePosition = remember(cameraPosition, moving, projection, here) { here?.let { projection?.toScreen(it) } }
    val pinPosition = remember(cameraPosition, moving, projection, pin) { pin?.let { projection?.toScreen(it) } }

    LaunchedEffect(clusters, viewSize) {
        if (projection != null && viewSize != IntSize.Zero) {
            onVisibleChange(spots.filter { projection.toScreen(it.location).inside(viewSize) }.map { it.id }.toSet())
        }
    }

    Box(modifier.clipToBounds().onSizeChanged { viewSize = it }) {
        NaverMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            properties = MapProperties(
                mapType = MapType.Navi,
                isNightModeEnabled = Palette.dark,
                isBuildingLayerGroupEnabled = false,
                minZoom = 5.5,
                maxZoom = 18.0,
                symbolScale = 0.9f,
                backgroundColor = Palette.MapSea,
            ),
            uiSettings = MapUiSettings(
                isTiltGesturesEnabled = false,
                isRotateGesturesEnabled = false,
                isCompassEnabled = false,
                isScaleBarEnabled = false,
                isZoomControlEnabled = false,
                logoMargin = PaddingValues(start = 12.dp, bottom = 12.dp),
            ),
            // Korean labels whatever the device language; the places and tips are Korean too.
            locale = Locale.KOREA,
            onMapClick = { _, point -> onMapTap?.invoke(LatLng(point.latitude, point.longitude)) },
        )

        herePosition?.let { HereMarker(it) }

        clusters.forEach { cluster ->
            key(cluster.lead.id) {
                PhotoMarker(
                    spot = cluster.lead,
                    count = cluster.items.size,
                    selected = cluster.items.any { it.id == selectedId },
                    position = cluster.position,
                    style = style,
                    tint = markerTint(cluster.lead),
                    onClick = {
                        if (cluster.items.size > 1) {
                            val target = CameraUpdate.scrollAndZoomTo(cluster.lead.location.toNaver(), cameraPosition.zoom + 2)
                            camera.move(target.animate(CameraAnimation.Easing))
                        } else {
                            onSelect(cluster.lead)
                        }
                    },
                )
            }
        }

        herePosition?.let { HerePulse(it, herePulse) }
        pinPosition?.let { PinMarker(it) }

        MapControls(
            style = style,
            onZoomIn = { camera.move(CameraUpdate.zoomIn().animate(CameraAnimation.Easing)) },
            onZoomOut = { camera.move(CameraUpdate.zoomOut().animate(CameraAnimation.Easing)) },
            onMyLocation = if (here == null && onMyLocation == null) null else {
                {
                    onMyLocation?.invoke()
                    here?.let { camera.move(CameraUpdate.scrollAndZoomTo(it.toNaver(), maxOf(cameraPosition.zoom, FOCUS_ZOOM)).animate(CameraAnimation.Easing)) }
                }
            },
            onSelected = spots.firstOrNull { it.id == selectedId }?.let { spot ->
                { camera.move(CameraUpdate.scrollAndZoomTo(spot.location.toNaver(), maxOf(cameraPosition.zoom, FOCUS_ZOOM)).animate(CameraAnimation.Easing)) }
            },
        )
    }
}
