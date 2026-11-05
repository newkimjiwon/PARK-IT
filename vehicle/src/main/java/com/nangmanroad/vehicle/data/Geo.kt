package com.nangmanroad.vehicle.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Seoul City Hall, used until the navigation service reports the car's position. */
val DefaultLocation = LatLng(37.5665, 126.9780)

private const val EARTH_RADIUS_KM = 6371.0

fun distanceKm(from: LatLng, to: LatLng): Double {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLat = lat2 - lat1
    val dLng = Math.toRadians(to.longitude - from.longitude)
    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
    return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
}

fun formatKm(km: Double): String = when {
    km < 1 -> "${(km * 1000).roundToInt()}m"
    km < 10 -> String.format("%.1fkm", km)
    else -> "${km.roundToInt()}km"
}

fun formatMeters(meters: Int): String = formatKm(meters / 1000.0)

fun formatDuration(seconds: Int): String {
    val minutes = (seconds + 59) / 60
    return if (minutes < 60) "${minutes}분" else "${minutes / 60}시간 ${minutes % 60}분"
}
