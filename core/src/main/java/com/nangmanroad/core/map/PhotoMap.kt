package com.nangmanroad.core.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.nangmanroad.core.BuildConfig
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.SpotPhoto
import kotlin.math.roundToInt

private val CameraSaver = Saver<MapCamera, List<Double>>(
    save = { listOf(it.center.latitude, it.center.longitude, it.zoom.toDouble()) },
    restore = { MapCamera(LatLng(it[0], it[1]), it[2].toFloat()) },
)

/** Sizes that differ between the head unit and the phone. */
data class PhotoMapStyle(
    val markerSize: Dp = 84.dp,
    val selectedMarkerSize: Dp = 104.dp,
    val clusterRadius: Dp = 96.dp,
    val buttonSize: Dp = 72.dp,
    val controlsPadding: PaddingValues = PaddingValues(20.dp),
    val initialCamera: MapCamera = MapCamera(),
)

/** True when a NAVER Maps key was supplied at build time (see core/build.gradle.kts). */
val naverMapsAvailable: Boolean get() = BuildConfig.NAVER_NCP_KEY_ID.isNotBlank()

/**
 * The map that shows places as their own photos. Nearby photos merge into one marker with a count;
 * tapping a merged marker zooms in, tapping a single one selects it.
 *
 * With a NAVER Maps key this is a real road map (night or day to match the theme); without one it falls back to the
 * hand-drawn Canvas map so the apps still build and run.
 */
@Composable
fun PhotoMap(
    spots: List<Spot>,
    selectedId: String?,
    here: LatLng,
    onSelect: (Spot) -> Unit,
    onVisibleChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    style: PhotoMapStyle = PhotoMapStyle(),
    /** Border colour per spot, e.g. to tell "shared" from "saved" on a profile map. */
    markerTint: (Spot) -> Color? = { null },
    /** Pin drop for choosing a location; null keeps taps for markers only. */
    onMapTap: ((LatLng) -> Unit)? = null,
    /** Change this value to play the 파킷 pin drop at [here]; null shows the plain position dot. */
    herePulse: Long? = null,
) {
    if (naverMapsAvailable) {
        NaverPhotoMap(spots, selectedId, here, onSelect, onVisibleChange, modifier, style, markerTint, onMapTap, herePulse)
    } else {
        CanvasPhotoMap(spots, selectedId, here, onSelect, onVisibleChange, modifier, style, markerTint, onMapTap, herePulse)
    }
}

@Composable
internal fun CanvasPhotoMap(
    spots: List<Spot>,
    selectedId: String?,
    here: LatLng,
    onSelect: (Spot) -> Unit,
    onVisibleChange: (Set<String>) -> Unit,
    modifier: Modifier,
    style: PhotoMapStyle,
    markerTint: (Spot) -> Color?,
    onMapTap: ((LatLng) -> Unit)?,
    herePulse: Long?,
) {
    var camera by rememberSaveable(stateSaver = CameraSaver) { mutableStateOf(style.initialCamera) }
    var viewSize by remember { mutableStateOf(Size.Zero) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val clusterRadius = with(density) { style.clusterRadius.toPx() }

    val clusters = remember(spots, selectedId, camera, viewSize) {
        if (viewSize == Size.Zero) emptyList() else {
            // The selected place goes first so it always leads its cluster and stays visible.
            val ordered = spots.sortedWith(
                compareByDescending<Spot> { it.id == selectedId }.thenByDescending { it.likeCount },
            )
            clusterMarkers(ordered.map { it to camera.toScreen(it.location, viewSize) }, clusterRadius)
        }
    }

    LaunchedEffect(spots, camera, viewSize) {
        if (viewSize != Size.Zero) {
            onVisibleChange(spots.filter { camera.isVisible(it.location, viewSize) }.map { it.id }.toSet())
        }
    }

    Box(
        modifier = modifier
            // The base map and markers extend past the view while panning; keep them inside it.
            .clipToBounds()
            .onSizeChanged { viewSize = it.toSize() }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    camera = camera.zoomBy(zoom, centroid, viewSize).panBy(pan, viewSize)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { camera = camera.zoomBy(2f, it, viewSize) },
                    onTap = { point -> onMapTap?.invoke(camera.fromScreen(point, viewSize)) },
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            with(KoreaBaseMap) { drawBaseMap(camera, measurer) }
        }

        val herePosition = if (viewSize != Size.Zero) camera.toScreen(here, viewSize) else null
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
                            camera = camera.zoomBy(2.2f, cluster.position, viewSize)
                        } else {
                            onSelect(cluster.lead)
                        }
                    },
                )
            }
        }

        herePosition?.let { HerePulse(it, herePulse) }

        MapControls(
            style = style,
            onZoomIn = { camera = camera.zoomBy(1.6f, Offset(viewSize.width / 2, viewSize.height / 2), viewSize) },
            onZoomOut = { camera = camera.zoomBy(1 / 1.6f, Offset(viewSize.width / 2, viewSize.height / 2), viewSize) },
            onFocus = {
                val focus = spots.firstOrNull { it.id == selectedId }?.location ?: here
                camera = MapCamera(focus, camera.zoom.coerceAtLeast(2.4f))
            },
        )
    }
}

@Composable
internal fun PhotoMarker(
    spot: Spot,
    count: Int,
    selected: Boolean,
    position: Offset,
    style: PhotoMapStyle,
    tint: Color?,
    onClick: () -> Unit,
) {
    val size by animateDpAsState(if (selected) style.selectedMarkerSize else style.markerSize, label = "markerSize")
    val corner = RoundedCornerShape(size * 0.05f)
    val half = with(LocalDensity.current) { (size / 2).toPx() }
    Box(
        modifier = Modifier
            .offset { IntOffset((position.x - half).roundToInt(), (position.y - half).roundToInt()) }
            .size(size)
            .semantics { contentDescription = if (count > 1) "${spot.title} 외 ${count - 1}곳" else spot.title }
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        SpotPhoto(
            url = spot.photos.first(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .shadow(if (selected) 6.dp else 3.dp, corner)
                .clip(corner)
                .border(if (selected) 4.dp else 2.5f.dp, if (selected) Palette.Accent else tint ?: Color.White.copy(alpha = 0.92f), corner),
        )
        if (count > 1) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = Palette.Background,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Palette.TextPrimary)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
    }
}

/** The car's position: a blue dot under the photo markers. */
@Composable
internal fun HereMarker(position: Offset) {
    val half = with(LocalDensity.current) { 14.dp.toPx() }
    Box(
        modifier = Modifier
            .offset { IntOffset((position.x - half).roundToInt(), (position.y - half).roundToInt()) }
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(0x5554A0FF)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(Color(0xFF54A0FF))
                .border(2.dp, Color.White, CircleShape),
        )
    }
}

/**
 * The "파킷" moment, drawn above the markers: when [pulse] changes a pine pin drops onto the car's
 * position with a bounce and a ring spreads out from it, so the name is something you can see
 * happen. Nothing is drawn while [pulse] is null or once the animation has finished.
 */
@Composable
internal fun HerePulse(position: Offset, pulse: Long?) {
    val ring = remember { Animatable(1f) }
    val drop = remember { Animatable(0f) }
    LaunchedEffect(pulse) {
        if (pulse != null) {
            ring.snapTo(0f)
            drop.snapTo(1f)
            drop.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            ring.animateTo(1f, tween(1_300, easing = LinearOutSlowInEasing))
        }
    }
    if (pulse == null || ring.value >= 1f) return

    val density = LocalDensity.current
    val ringMax = with(density) { 90.dp.toPx() }
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            color = Palette.Accent.copy(alpha = (1f - ring.value) * 0.9f),
            radius = 14.dp.toPx() + ringMax * ring.value,
            center = position,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
    val pinSize = 60.dp
    val pinHalf = with(density) { (pinSize / 2).toPx() }
    val lift = with(density) { (26.dp + 110.dp * drop.value).toPx() }
    Icon(
        Icons.Filled.PushPin,
        contentDescription = null,
        tint = Palette.Accent,
        modifier = Modifier
            .offset { IntOffset((position.x - pinHalf).roundToInt(), (position.y - pinHalf - lift).roundToInt()) }
            .size(pinSize)
            .alpha((1f - ring.value * 0.8f).coerceIn(0f, 1f)),
    )
}

/** Zoom in, zoom out and "go to the selected place", stacked in the top-right corner. */
@Composable
internal fun BoxScope.MapControls(style: PhotoMapStyle, onZoomIn: () -> Unit, onZoomOut: () -> Unit, onFocus: () -> Unit) {
    Column(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(style.controlsPadding)
            .clip(RoundedCornerShape(4.dp))
            .background(Palette.BackgroundRaised.copy(alpha = 0.96f))
            .border(1.dp, Palette.Rule, RoundedCornerShape(4.dp)),
    ) {
        MapButton(Icons.Outlined.Add, "확대", style.buttonSize, onZoomIn)
        MapButton(Icons.Outlined.Remove, "축소", style.buttonSize, onZoomOut)
        MapButton(Icons.Outlined.MyLocation, "선택한 장소로 이동", style.buttonSize, onFocus)
    }
}

@Composable
private fun MapButton(icon: ImageVector, label: String, size: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Palette.TextPrimary, modifier = Modifier.size(32.dp))
    }
}
