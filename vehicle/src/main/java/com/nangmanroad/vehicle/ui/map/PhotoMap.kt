package com.nangmanroad.vehicle.ui.map

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.nangmanroad.vehicle.data.LatLng
import com.nangmanroad.vehicle.data.Spot
import com.nangmanroad.vehicle.ui.components.SpotPhoto
import com.nangmanroad.vehicle.ui.theme.Palette
import kotlin.math.roundToInt

private val CameraSaver = Saver<MapCamera, List<Double>>(
    save = { listOf(it.center.latitude, it.center.longitude, it.zoom.toDouble()) },
    restore = { MapCamera(LatLng(it[0], it[1]), it[2].toFloat()) },
)

/**
 * The map that shows places as their own photos. Nearby photos merge into one marker with a count;
 * tapping a merged marker zooms in, tapping a single one selects it.
 */
@Composable
fun PhotoMap(
    spots: List<Spot>,
    selectedId: String?,
    here: LatLng,
    onSelect: (Spot) -> Unit,
    onVisibleChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var camera by rememberSaveable(stateSaver = CameraSaver) { mutableStateOf(MapCamera()) }
    var viewSize by remember { mutableStateOf(Size.Zero) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val clusterRadius = with(density) { 96.dp.toPx() }

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
            .onSizeChanged { viewSize = it.toSize() }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    camera = camera.zoomBy(zoom, centroid, viewSize).panBy(pan, viewSize)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { camera = camera.zoomBy(2f, it, viewSize) })
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            with(KoreaBaseMap) { drawBaseMap(camera, measurer) }
        }

        if (viewSize != Size.Zero) {
            HereMarker(camera.toScreen(here, viewSize))
        }

        clusters.forEach { cluster ->
            key(cluster.lead.id) {
                PhotoMarker(
                    spot = cluster.lead,
                    count = cluster.items.size,
                    selected = cluster.items.any { it.id == selectedId },
                    position = cluster.position,
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

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Palette.BackgroundRaised.copy(alpha = 0.92f)),
        ) {
            MapButton(Icons.Outlined.Add, "확대") {
                camera = camera.zoomBy(1.6f, Offset(viewSize.width / 2, viewSize.height / 2), viewSize)
            }
            MapButton(Icons.Outlined.Remove, "축소") {
                camera = camera.zoomBy(1 / 1.6f, Offset(viewSize.width / 2, viewSize.height / 2), viewSize)
            }
            MapButton(Icons.Outlined.MyLocation, "선택한 장소로 이동") {
                val focus = spots.firstOrNull { it.id == selectedId }?.location ?: here
                camera = MapCamera(focus, camera.zoom.coerceAtLeast(2.4f))
            }
        }
    }
}

@Composable
private fun PhotoMarker(
    spot: Spot,
    count: Int,
    selected: Boolean,
    position: Offset,
    onClick: () -> Unit,
) {
    val size by animateDpAsState(if (selected) 104.dp else 84.dp, label = "markerSize")
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
                .shadow(12.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .border(if (selected) 5.dp else 3.dp, if (selected) Palette.Coral else Color.White, RoundedCornerShape(22.dp)),
        )
        if (count > 1) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = Palette.Background,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 12.dp, y = (-12).dp)
                    .clip(CircleShape)
                    .background(Palette.Coral)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun HereMarker(position: Offset) {
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

@Composable
private fun MapButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Palette.TextPrimary, modifier = Modifier.size(32.dp))
    }
}
