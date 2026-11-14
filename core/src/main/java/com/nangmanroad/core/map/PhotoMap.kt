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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FilterHdr
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.data.Category
import com.nangmanroad.core.data.Spot
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.core.ui.ParkItMark
import com.nangmanroad.core.ui.SpotPhoto
import kotlin.math.roundToInt

private val CameraSaver = Saver<MapCamera, List<Double>>(
    save = { listOf(it.center.latitude, it.center.longitude, it.zoom.toDouble()) },
    restore = { MapCamera(LatLng(it[0], it[1]), it[2].toFloat()) },
)

private val MapBadgeOutline = Color(0xFF332322)
private val MapControlShape = RoundedCornerShape(10.dp)

/** Sizes that differ between the head unit and the phone. */
data class PhotoMapStyle(
    val markerSize: Dp = 84.dp,
    val selectedMarkerSize: Dp = 104.dp,
    val clusterRadius: Dp = 96.dp,
    val buttonSize: Dp = 72.dp,
    val controlsPadding: PaddingValues = PaddingValues(20.dp),
    val initialCamera: MapCamera = MapCamera(),
    val naverMinZoom: Double = 5.5,
    val naverOverviewZoom: Double? = null,
)

enum class MapMarkerMode(val label: String, val icon: ImageVector) {
    PHOTO("사진", Icons.Outlined.Image),
    ICON("아이콘", Icons.Outlined.LocationOn),
    DOT("점", Icons.Outlined.FiberManualRecord),
}

@Composable
private fun MapMarkerModeMenu(mode: MapMarkerMode, onSelect: (MapMarkerMode) -> Unit, height: Dp) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier.size(height)
                .clip(MapControlShape)
                .background(Palette.BackgroundRaised)
                .border(1.dp, Palette.Rule, MapControlShape)
                .clickable(role = Role.Button, onClickLabel = "마커 표시 방식") { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Layers, contentDescription = "마커 표시 방식 · ${mode.label}", tint = Palette.TextPrimary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MapMarkerMode.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = { Icon(option.icon, contentDescription = null) },
                    modifier = Modifier.semantics { selected = mode == option },
                    trailingIcon = if (mode == option) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else null,
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * A request to move the camera, e.g. to a region the user just picked. A new [id] moves the map again
 * even when the centre is the same, so "go to my location" works twice in a row.
 */
data class MapFocus(val center: LatLng, val zoom: Float = 2.5f, val id: Long = System.nanoTime())

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
    /** The device's own position; null draws no dot and leaves "my location" to [onMyLocation]. */
    here: LatLng?,
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
    focus: MapFocus? = null,
    onMyLocation: (() -> Unit)? = null,
    droppedPin: LatLng? = null,
    markerMode: MapMarkerMode = MapMarkerMode.PHOTO,
    savedIds: Set<String> = emptySet(),
    myAuthorId: String? = null,
    likedIds: Set<String> = emptySet(),
    onMarkerModeChange: ((MapMarkerMode) -> Unit)? = null,
) {
    val mapConfig by produceState<MapConfig?>(initialValue = null) { value = loadMapConfig() }
    val resolvedConfig = mapConfig
    if (resolvedConfig != null && resolvedConfig.keyId.isNotBlank()) {
        NaverPhotoMap(spots, selectedId, here, onSelect, onVisibleChange, modifier, style, markerTint, onMapTap, herePulse, resolvedConfig, focus, onMyLocation, droppedPin, markerMode, savedIds, myAuthorId, likedIds, onMarkerModeChange)
    } else {
        CanvasPhotoMap(spots, selectedId, here, onSelect, onVisibleChange, modifier, style, markerTint, onMapTap, herePulse, focus, onMyLocation, droppedPin, markerMode, savedIds, myAuthorId, likedIds, onMarkerModeChange)
    }
}

@Composable
internal fun CanvasPhotoMap(
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
    focus: MapFocus?,
    onMyLocation: (() -> Unit)?,
    droppedPin: LatLng?,
    markerMode: MapMarkerMode,
    savedIds: Set<String>,
    myAuthorId: String?,
    likedIds: Set<String>,
    onMarkerModeChange: ((MapMarkerMode) -> Unit)?,
) {
    var camera by rememberSaveable(stateSaver = CameraSaver) { mutableStateOf(style.initialCamera) }
    LaunchedEffect(focus?.id) { focus?.let { camera = MapCamera(it.center, it.zoom) } }
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

        val herePosition = if (viewSize != Size.Zero) here?.let { camera.toScreen(it, viewSize) } else null
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
                    mode = markerMode,
                    saved = cluster.items.any { it.id in savedIds },
                    mine = cluster.items.any { it.author.id == myAuthorId },
                    liked = cluster.items.any { it.id in likedIds },
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
        if (viewSize != Size.Zero) droppedPin?.let { DroppedPin(camera.toScreen(it, viewSize)) }

        MapControls(
            style = style,
            onZoomIn = { camera = camera.zoomBy(1.6f, Offset(viewSize.width / 2, viewSize.height / 2), viewSize) },
            onZoomOut = { camera = camera.zoomBy(1 / 1.6f, Offset(viewSize.width / 2, viewSize.height / 2), viewSize) },
            onHere = { onMyLocation?.invoke(); here?.let { camera = MapCamera(it, camera.zoom.coerceAtLeast(2.4f)) } },
            onFocus = (droppedPin ?: spots.firstOrNull { it.id == selectedId }?.location)?.let { point ->
                { camera = MapCamera(point, camera.zoom.coerceAtLeast(2.4f)) }
            },
            showHere = onMyLocation != null || (here != null && onMapTap == null),
            markerMode = markerMode,
            onMarkerModeChange = onMarkerModeChange,
        )
    }
}

@Composable
internal fun DroppedPin(position: Offset) {
    val size = 40.dp
    val pixels = with(LocalDensity.current) { size.toPx() }
    Icon(
        Icons.Outlined.LocationOn,
        contentDescription = "등록할 위치",
        tint = Palette.Accent,
        modifier = Modifier
            .offset { IntOffset((position.x - pixels / 2).roundToInt(), (position.y - pixels).roundToInt()) }
            .size(size)
            .background(Palette.BackgroundRaised, CircleShape)
            .padding(4.dp),
    )
}

@Composable
internal fun PhotoMarker(
    spot: Spot,
    count: Int,
    selected: Boolean,
    position: Offset,
    style: PhotoMapStyle,
    tint: Color?,
    mode: MapMarkerMode,
    saved: Boolean,
    mine: Boolean,
    liked: Boolean,
    onClick: () -> Unit,
) {
    val photoSize = if (selected) style.selectedMarkerSize else style.markerSize
    val size by animateDpAsState(
        when (mode) {
            MapMarkerMode.PHOTO -> photoSize
            MapMarkerMode.ICON -> (photoSize * 0.68f).coerceAtLeast(44.dp)
            MapMarkerMode.DOT -> (photoSize * 0.50f).coerceAtLeast(44.dp)
        },
        label = "markerSize",
    )
    val corner = RoundedCornerShape(size * 0.12f)
    val half = with(LocalDensity.current) { (size / 2).toPx() }
    val outline = if (selected) Palette.Accent else tint ?: Color.White.copy(alpha = 0.92f)
    Box(
        modifier = Modifier
            .offset { IntOffset((position.x - half).roundToInt(), (position.y - half).roundToInt()) }
            .size(size)
            .semantics {
                val title = if (count > 1) "${spot.title} 외 ${count - 1}곳" else spot.title
                val suffix = if (count > 1) " 포함" else ""
                contentDescription = buildString {
                    append(title)
                    if (saved) append(" · 저장한 장소$suffix")
                    if (liked) append(" · 좋아요 누른 장소$suffix")
                    if (mine) append(" · 내가 올린 파킷$suffix")
                }
            }
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        when (mode) {
            MapMarkerMode.PHOTO -> SpotPhoto(
                url = spot.photos.first(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
                    .shadow(if (selected) 6.dp else 3.dp, corner)
                    .clip(corner)
                    .border(if (selected) 4.dp else 2.5f.dp, outline, corner),
            )
            MapMarkerMode.ICON -> Box(
                Modifier.fillMaxSize().shadow(3.dp, CircleShape)
                    .background(if (selected) Palette.Accent else Palette.BackgroundRaised, CircleShape)
                    .border(2.dp, if (selected) Palette.OnAccent else tint ?: Palette.Rule, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(spot.category.markerIcon, contentDescription = null, tint = if (selected) Palette.OnAccent else Palette.TextPrimary,
                    modifier = Modifier.size(size / 2))
            }
            MapMarkerMode.DOT -> Box(
                Modifier.align(Alignment.Center).size(if (selected) 24.dp else 16.dp)
                    .shadow(3.dp, CircleShape)
                    .background(if (selected) Palette.Accent else tint ?: Palette.TextPrimary, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
            )
        }
        if (count > 1) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = Palette.Background,
                modifier = Modifier
                    .align(if (saved) Alignment.TopStart else Alignment.TopEnd)
                    .offset(x = if (saved) (-5).dp else 7.dp, y = (-6).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Palette.TextPrimary)
                    .padding(horizontal = if (mode == MapMarkerMode.PHOTO) 8.dp else 4.dp, vertical = 1.dp),
            )
        }
        if (saved) {
            MapBadgeIcon(
                icon = Icons.Rounded.Star,
                tint = Color(0xFFFFC400),
                size = if (mode == MapMarkerMode.PHOTO) 29.dp else 24.dp,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp),
            )
        }
        if (mine) {
            val pinSize = if (mode == MapMarkerMode.PHOTO) 38.dp else 32.dp
            Box(Modifier.align(Alignment.BottomStart).offset(x = (-7).dp, y = 6.dp).size(pinSize), contentAlignment = Alignment.Center) {
                ParkItMark(pinSize, tint = MapBadgeOutline, contentDescription = null)
                ParkItMark(pinSize - 5.dp, contentDescription = null)
            }
        }
        if (liked) {
            MapBadgeIcon(
                icon = Icons.Rounded.Favorite,
                tint = Color(0xFFF45770),
                size = if (mode == MapMarkerMode.PHOTO) 28.dp else 23.dp,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 5.dp, y = 5.dp),
            )
        }
    }
}

@Composable
private fun MapBadgeIcon(icon: ImageVector, tint: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = MapBadgeOutline, modifier = Modifier.fillMaxSize())
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size - 3.dp))
    }
}

private val Category.markerIcon: ImageVector
    get() = when (this) {
        Category.SEA_SUNSET -> Icons.Outlined.WbTwilight
        Category.STARS -> Icons.Outlined.NightsStay
        Category.FOREST -> Icons.Outlined.Forest
        Category.LAKE_RIVER -> Icons.Outlined.Water
        Category.DRIVE -> Icons.Outlined.DirectionsCar
        Category.QUIET -> Icons.Outlined.FilterHdr
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

/** The location being chosen: a pin whose tip sits exactly on the point. */
@Composable
internal fun PinMarker(position: Offset) {
    val size = 52.dp
    val px = with(LocalDensity.current) { size.toPx() }
    Box(
        Modifier
            .offset { IntOffset((position.x - px / 2).roundToInt(), (position.y - px * 0.94f).roundToInt()) }
            .size(size)
            .semantics { contentDescription = "고른 위치" },
        contentAlignment = Alignment.Center,
    ) {
        // A white pin slightly larger underneath gives the accent pin an outline on any map colour.
        Icon(Icons.Filled.Place, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize())
        Icon(Icons.Filled.Place, contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(size * 0.88f))
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

/**
 * Zoom in, zoom out, "my location" and, while a place is selected, "go to the selected place",
 * stacked in the top-right corner. The crosshair always means the device's own position and the pin
 * the selected place, so neither surprises with the other's destination.
 */
@Composable
internal fun BoxScope.MapControls(
    style: PhotoMapStyle,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onHere: () -> Unit,
    onFocus: (() -> Unit)?,
    showHere: Boolean,
    markerMode: MapMarkerMode,
    onMarkerModeChange: ((MapMarkerMode) -> Unit)?,
) {
    Column(Modifier.align(Alignment.TopEnd).padding(style.controlsPadding)) {
        Column(
            Modifier.clip(MapControlShape)
                .background(Palette.BackgroundRaised.copy(alpha = 0.96f))
                .border(1.dp, Palette.Rule, MapControlShape),
        ) {
            MapButton(Icons.Outlined.Add, "확대", style.buttonSize, onZoomIn)
            MapButton(Icons.Outlined.Remove, "축소", style.buttonSize, onZoomOut)
            if (showHere) MapButton(Icons.Outlined.MyLocation, "내 위치로 이동", style.buttonSize, onHere)
            if (onFocus != null) MapButton(Icons.Outlined.LocationOn, "선택한 장소로 이동", style.buttonSize, onFocus)
        }
        if (onMarkerModeChange != null) {
            Spacer(Modifier.height(8.dp))
            MapMarkerModeMenu(markerMode, onMarkerModeChange, style.buttonSize)
        }
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
