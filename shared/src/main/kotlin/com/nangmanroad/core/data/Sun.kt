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
fun sunTimes(location: LatLng, date: LocalDate, zone: ZoneId = KoreaZone): SunTimes? {
    // Julian day of local midnight, then the day count since J2000 adjusted by longitude.
    val midnightUtc = date.atStartOfDay(zone).toInstant()
    val julianMidnight = midnightUtc.epochSecond / 86400.0 + 2440587.5
    val n = ceil(julianMidnight - 2451545.0 + 0.0008 - location.longitude / 360.0)
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
