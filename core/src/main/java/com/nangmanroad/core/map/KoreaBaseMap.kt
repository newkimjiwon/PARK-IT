package com.nangmanroad.core.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.nangmanroad.core.data.LatLng
import com.nangmanroad.core.ui.Palette

/**
 * A deliberately simple base map: the mainland outline, Jeju, the main expressway corridors and
 * city names. It gives photo markers a sense of place without a tile provider or API key.
 */
internal object KoreaBaseMap {

    private val mainland = listOf(
        LatLng(37.75, 126.52), LatLng(37.98, 126.68), LatLng(38.18, 127.05), LatLng(38.30, 127.45),
        LatLng(38.32, 127.90), LatLng(38.28, 128.25), LatLng(38.61, 128.36), LatLng(38.20, 128.60),
        LatLng(37.80, 128.92), LatLng(37.48, 129.13), LatLng(37.05, 129.40), LatLng(36.55, 129.43),
        LatLng(36.08, 129.57), LatLng(35.82, 129.48), LatLng(35.48, 129.43), LatLng(35.10, 129.10),
        LatLng(34.98, 128.70), LatLng(34.85, 128.25), LatLng(34.76, 127.75), LatLng(34.62, 127.35),
        LatLng(34.48, 126.95), LatLng(34.30, 126.55), LatLng(34.55, 126.30), LatLng(34.82, 126.38),
        LatLng(35.25, 126.36), LatLng(35.62, 126.48), LatLng(35.98, 126.62), LatLng(36.32, 126.52),
        LatLng(36.62, 126.18), LatLng(36.92, 126.38), LatLng(37.02, 126.78), LatLng(37.38, 126.60),
    )

    private val jejuCenter = LatLng(33.38, 126.55)

    private val roads = listOf(
        // 경부
        listOf(LatLng(37.56, 126.98), LatLng(37.26, 127.03), LatLng(36.81, 127.15), LatLng(36.35, 127.38),
            LatLng(36.12, 128.11), LatLng(35.87, 128.60), LatLng(35.84, 129.21), LatLng(35.18, 129.07)),
        // 영동
        listOf(LatLng(37.46, 126.70), LatLng(37.27, 127.05), LatLng(37.34, 127.92), LatLng(37.56, 128.42),
            LatLng(37.75, 128.88)),
        // 서울양양
        listOf(LatLng(37.56, 126.98), LatLng(37.88, 127.73), LatLng(38.08, 128.62)),
        // 중앙
        listOf(LatLng(37.34, 127.92), LatLng(37.13, 128.19), LatLng(36.98, 128.37), LatLng(36.81, 128.62),
            LatLng(36.57, 128.73), LatLng(35.87, 128.60)),
        // 중부내륙
        listOf(LatLng(37.30, 127.63), LatLng(36.99, 127.93), LatLng(36.59, 128.19), LatLng(35.87, 128.60)),
        // 서해안
        listOf(LatLng(37.46, 126.70), LatLng(37.00, 127.10), LatLng(36.89, 126.63), LatLng(36.78, 126.45),
            LatLng(35.97, 126.71), LatLng(34.81, 126.39)),
        // 호남·남해
        listOf(LatLng(36.35, 127.38), LatLng(35.82, 127.15), LatLng(35.16, 126.85), LatLng(34.95, 127.49),
            LatLng(35.18, 128.11), LatLng(35.18, 129.07)),
        // 동해
        listOf(LatLng(37.75, 128.88), LatLng(37.45, 129.17), LatLng(36.02, 129.34), LatLng(35.54, 129.31),
            LatLng(35.18, 129.07)),
    )

    private data class City(val name: String, val location: LatLng, val major: Boolean)

    private val cities = listOf(
        City("서울", LatLng(37.56, 126.98), true),
        City("강릉", LatLng(37.75, 128.88), true),
        City("대전", LatLng(36.35, 127.38), true),
        City("대구", LatLng(35.87, 128.60), true),
        City("광주", LatLng(35.16, 126.85), true),
        City("부산", LatLng(35.18, 129.07), true),
        City("제주", LatLng(33.50, 126.53), true),
        City("인천", LatLng(37.46, 126.70), false),
        City("춘천", LatLng(37.88, 127.73), false),
        City("원주", LatLng(37.34, 127.92), false),
        City("충주", LatLng(36.99, 127.93), false),
        City("전주", LatLng(35.82, 127.15), false),
        City("포항", LatLng(36.02, 129.34), false),
        City("속초", LatLng(38.20, 128.59), false),
    )

    private val labelStyle = TextStyle(color = Palette.MapLabel, fontSize = 18.sp)

    fun DrawScope.drawBaseMap(camera: MapCamera, measurer: TextMeasurer) {
        drawRect(Palette.MapSea)

        val land = Path().apply {
            mainland.forEachIndexed { index, point ->
                val p = camera.toScreen(point, size)
                if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(land, Palette.MapLand)

        val jeju = camera.toScreen(jejuCenter, size)
        val jejuWidth = (camera.toScreen(LatLng(33.38, 126.95), size).x - jeju.x) * 2
        drawOval(
            color = Palette.MapLand,
            topLeft = Offset(jeju.x - jejuWidth / 2, jeju.y - jejuWidth / 5),
            size = Size(jejuWidth, jejuWidth * 0.4f),
        )

        val roadWidth = (3f * camera.zoom).coerceIn(3f, 9f)
        roads.forEach { road -> drawPath(smoothPath(road.map { camera.toScreen(it, size) }), Palette.MapRoad, style = Stroke(roadWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)) }

        cities.filter { it.major || camera.zoom >= 1.6f }.forEach { city ->
            val p = camera.toScreen(city.location, size)
            if (p.x !in -100f..size.width + 100f || p.y !in -50f..size.height + 50f) return@forEach
            drawCircle(Palette.MapLabel, radius = 4f, center = p)
            val text = measurer.measure(city.name, labelStyle)
            drawText(text, topLeft = Offset(p.x + 10f, p.y - text.size.height / 2f))
        }
    }

    /** Rounds the corners of a polyline with quadratic curves through the segment midpoints. */
    private fun smoothPath(points: List<Offset>): Path = Path().apply {
        if (points.isEmpty()) return@apply
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.lastIndex) {
            val mid = (points[i] + points[i + 1]) / 2f
            quadraticBezierTo(points[i].x, points[i].y, mid.x, mid.y)
        }
        lineTo(points.last().x, points.last().y)
    }
}
