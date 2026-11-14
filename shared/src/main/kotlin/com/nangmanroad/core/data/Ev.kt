package com.nangmanroad.core.data

import kotlin.math.roundToInt

/** What the car's battery says about a trip to a place, before anyone looks for a charger. */
enum class Reach(val label: String) {
    ROUND_TRIP("왕복 가능"),
    ONE_WAY("도착 후 충전 필요"),
    SHORT("출발 전 충전 필요"),
}

/** Mixed-road economy of a mid-size EV in km per kWh; conservative so the verdict errs on caution. */
const val DEFAULT_KM_PER_KWH = 5.0

/** Straight-line distance padded for real roads, same factor the sun plans use. */
private const val ROAD_FACTOR = 1.3

fun estimatedRoadKm(straightKm: Double): Double = straightKm * ROAD_FACTOR

/** Never plan down to zero: keep this share of the pack for detours, heating and the unknown. */
private const val RESERVE_SHARE = 0.15

/** Kilometres left in the pack at [percent] charge, with the reserve already taken out. */
fun usableRangeKm(percent: Int, capacityKwh: Double, kmPerKwh: Double = DEFAULT_KM_PER_KWH): Double =
    (capacityKwh * percent / 100.0 * kmPerKwh * (1 - RESERVE_SHARE)).coerceAtLeast(0.0)

/** Whether [usableRangeKm] covers the road distance there and back, there only, or neither. */
fun reach(straightKm: Double, usableRangeKm: Double): Reach =
    reachForRoadDistance(estimatedRoadKm(straightKm), usableRangeKm)

fun reachForRoadDistance(roadKm: Double, usableRangeKm: Double): Reach = when {
    usableRangeKm >= roadKm * 2 -> Reach.ROUND_TRIP
    usableRangeKm >= roadKm -> Reach.ONE_WAY
    else -> Reach.SHORT
}

/** "배터리 68% · 약 260km": the number the verdict rests on, rounded to something a glance can hold. */
fun rangeLabel(percent: Int, usableRangeKm: Double): String = "배터리 $percent% · 약 ${(usableRangeKm / 10).roundToInt() * 10}km"

/** Only the places the pack can reach and come back from, when the pack is known; everything otherwise. */
fun List<Spot>.withinRoundTrip(here: LatLng, usableRangeKm: Double?): List<Spot> =
    if (usableRangeKm == null) this else filter { reach(distanceKm(here, it.location), usableRangeKm) == Reach.ROUND_TRIP }

/** Below this much charge on arrival the night is a gamble; below the lower line it is a tow. */
const val ARRIVAL_COMFORT_PERCENT = 30
const val ARRIVAL_LOW_PERCENT = 15

/**
 * Charge left on arrival after [roadKm] of actual route (no road factor: the navigation already
 * measured the road), clamped at zero. The same question Tesla's navigation answers in one number.
 */
fun arrivalPercent(percent: Int, capacityKwh: Double, roadKm: Double, kmPerKwh: Double = DEFAULT_KM_PER_KWH): Int =
    (percent - roadKm / kmPerKwh / capacityKwh * 100).roundToInt().coerceIn(0, 100)

/** "도착 시 배터리 약 48%", with a word of warning once it gets low. */
fun arrivalLabel(arrivalPercent: Int): String = when {
    arrivalPercent < ARRIVAL_LOW_PERCENT -> "도착 시 배터리 약 $arrivalPercent% · 도중 충전 필요"
    arrivalPercent < ARRIVAL_COMFORT_PERCENT -> "도착 시 배터리 약 $arrivalPercent% · 돌아올 땐 충전"
    else -> "도착 시 배터리 약 $arrivalPercent%"
}

/**
 * The single head-unit filter: places the car can round-trip on its battery (when it knows the
 * pack) and where the night is not banned. One toggle instead of a row of chips to read while parked.
 */
fun List<Spot>.goableNow(here: LatLng, usableRangeKm: Double?): List<Spot> =
    withinRoundTrip(here, usableRangeKm).filter { it.carCamping != CarCamping.BANNED }
