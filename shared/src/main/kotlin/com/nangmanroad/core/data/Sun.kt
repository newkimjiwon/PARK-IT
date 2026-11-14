package com.nangmanroad.core.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** The clock the apps live on: Korean local time. */
val KoreaZone: ZoneId = ZoneId.of("Asia/Seoul")

/** Sunrise and sunset as epoch milliseconds for one calendar day at one place. */
data class SunTimes(val sunrise: Long, val sunset: Long)

/** Which moment of the sun a kind of place is about, so cards can show just that one. */
enum class SunMoment(val label: String) { SUNRISE("일출"), SUNSET("일몰") }

/** Sunset places and star places both wait for the sun to go; morning-mist water waits for it to come. */
val Category.sunMoment: SunMoment?
    get() = when (this) {
        Category.SEA_SUNSET, Category.STARS -> SunMoment.SUNSET
        Category.LAKE_RIVER -> SunMoment.SUNRISE
        Category.FOREST, Category.DRIVE, Category.QUIET -> null
    }

fun SunTimes.at(moment: SunMoment): Long = when (moment) {
    SunMoment.SUNRISE -> sunrise
    SunMoment.SUNSET -> sunset
}

/**
 * Sunrise and sunset for [date] at [location], from the standard sunrise equation (NOAA/Wikipedia
 * form, accurate to a couple of minutes) with the usual −0.833° for refraction and the sun's disc.
 * Runs on the device with no network, which is what a head unit in a valley needs. Null above the
 * polar circles when the sun never rises or sets that day.
 */
fun sunTimes(location: LatLng, date: LocalDate): SunTimes? {
    // Julian day number of the civil date (1970-01-01 is 2440588), then the day count since J2000
    // nudged by longitude so the transit lands on this date's local noon rather than the day before.
    val julianDayNumber = date.toEpochDay() + 2440588.0
    val n = ceil(julianDayNumber - 2451545.0 + 0.0008 - location.longitude / 360.0)
    val meanSolarTime = n - location.longitude / 360.0
    val meanAnomaly = (357.5291 + 0.98560028 * meanSolarTime).mod(360.0)
    val m = Math.toRadians(meanAnomaly)
    val center = 1.9148 * sin(m) + 0.0200 * sin(2 * m) + 0.0003 * sin(3 * m)
    val eclipticLongitude = (meanAnomaly + center + 180.0 + 102.9372).mod(360.0)
    val lambda = Math.toRadians(eclipticLongitude)
    val transit = 2451545.0 + meanSolarTime + 0.0053 * sin(m) - 0.0069 * sin(2 * lambda)
    val declination = asin(sin(lambda) * sin(Math.toRadians(23.4397)))
    val latitude = Math.toRadians(location.latitude)
    val cosHourAngle = (sin(Math.toRadians(-0.833)) - sin(latitude) * sin(declination)) / (cos(latitude) * cos(declination))
    if (cosHourAngle < -1.0 || cosHourAngle > 1.0) return null
    val hourAngle = Math.toDegrees(acos(cosHourAngle))
    fun toMillis(julian: Double): Long = ((julian - 2440587.5) * 86400_000L).toLong()
    return SunTimes(sunrise = toMillis(transit - hourAngle / 360.0), sunset = toMillis(transit + hourAngle / 360.0))
}

/** Today's sun times at [location] on the Korean clock. */
fun sunTimesToday(location: LatLng, now: Long = System.currentTimeMillis()): SunTimes? =
    sunTimes(location, Instant.ofEpochMilli(now).atZone(KoreaZone).toLocalDate())

private val clockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREA)

/** "오후 5:23" on the Korean clock. */
fun formatClock(epochMillis: Long): String = clockFormat.format(Instant.ofEpochMilli(epochMillis).atZone(KoreaZone))

/** Average speed used for an estimate before the navigation route is available. */
private const val AVERAGE_KMH = 60.0

fun estimatedDriveSeconds(roadKm: Double): Int =
    (roadKm / AVERAGE_KMH * 3600).roundToInt()

/** Minutes to be parked and settled before the sun does its thing. */
private const val SETTLE_MINUTES = 15

/**
 * Whether leaving now gets the car to [spot] before its sun moment today: the question a saved
 * sunset place has to answer on the head unit before anyone starts the engine. When today is
 * already lost, [tomorrowLatestDeparture] says when to leave tomorrow instead.
 */
data class SunPlan(
    val moment: SunMoment,
    val sunAt: Long,
    val arrival: Long,
    val minutesToSpare: Int,
    /** Leave by then to be settled [SETTLE_MINUTES] before the sun; past already when too late. */
    val latestDeparture: Long,
    val tomorrowLatestDeparture: Long?,
    /** True when the drive time came from the navigation route rather than a straight-line guess. */
    val fromRoute: Boolean = false,
) {
    val makesIt: Boolean get() = minutesToSpare >= 1

    /** "지금 출발하면 일몰 40분 전 도착" or "오늘 일몰은 늦었어요 · 오후 5:20". */
    fun verdict(): String = when {
        makesIt -> "지금 출발하면 ${moment.label} ${formatDuration(minutesToSpare * 60)} 전 도착"
        else -> "오늘 ${moment.label}은 늦었어요 · ${formatClock(sunAt)}"
    }

    /** "늦어도 오후 1:30 출발", or tomorrow's when today is gone; null only when nothing can be said. */
    fun departureHint(): String? = when {
        makesIt && minutesToSpare > SETTLE_MINUTES -> "늦어도 ${formatClock(latestDeparture)} 출발"
        !makesIt && tomorrowLatestDeparture != null -> "내일은 늦어도 ${formatClock(tomorrowLatestDeparture)} 출발"
        else -> null
    }
}

/**
 * [driveSeconds] overrides the straight-line estimate, e.g. with the navigation route's remaining
 * time when the car is already heading there.
 */
fun sunPlan(spot: Spot, here: LatLng, now: Long = System.currentTimeMillis(), driveSeconds: Int? = null): SunPlan? {
    val moment = spot.category.sunMoment ?: return null
    val today = Instant.ofEpochMilli(now).atZone(KoreaZone).toLocalDate()
    val sunAt = sunTimes(spot.location, today)?.at(moment) ?: return null
    val drive = driveSeconds ?: estimatedDriveSeconds(estimatedRoadKm(distanceKm(here, spot.location)))
    val driveMillis = drive * 1000L
    val arrival = now + driveMillis
    val settle = SETTLE_MINUTES * 60_000L
    val tomorrowSunAt = sunTimes(spot.location, today.plusDays(1))?.at(moment)
    return SunPlan(
        moment = moment,
        sunAt = sunAt,
        arrival = arrival,
        minutesToSpare = ((sunAt - arrival) / 60_000L).toInt(),
        latestDeparture = sunAt - driveMillis - settle,
        tomorrowLatestDeparture = tomorrowSunAt?.let { it - driveMillis - settle },
        fromRoute = driveSeconds != null,
    )
}
