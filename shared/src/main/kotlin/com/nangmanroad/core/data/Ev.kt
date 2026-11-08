package com.nangmanroad.core.data

import kotlin.math.roundToInt

/** What the car's battery says about a trip to a place, before anyone looks for a charger. */
enum class Reach(val label: String) {
    ROUND_TRIP("왕복 가능"),
    ONE_WAY("편도만 · 도착 후 충전"),
    SHORT("충전 후 출발"),
}

/** Mixed-road economy of a mid-size EV in km per kWh; conservative so the verdict errs on caution. */
const val DEFAULT_KM_PER_KWH = 5.0

/** Straight-line distance padded for real roads, same factor the sun plans use. */
private const val ROAD_FACTOR = 1.3

/** Never plan down to zero: keep this share of the pack for detours, heating and the unknown. */
private const val RESERVE_SHARE = 0.15

/** Kilometres left in the pack at [percent] charge, with the reserve already taken out. */
fun usableRangeKm(percent: Int, capacityKwh: Double, kmPerKwh: Double = DEFAULT_KM_PER_KWH): Double =
    (capacityKwh * percent / 100.0 * kmPerKwh * (1 - RESERVE_SHARE)).coerceAtLeast(0.0)

/** Whether [usableRangeKm] covers the road distance there and back, there only, or neither. */
fun reach(straightKm: Double, usableRangeKm: Double): Reach {
    val road = straightKm * ROAD_FACTOR
    return when {
        usableRangeKm >= road * 2 -> Reach.ROUND_TRIP
        usableRangeKm >= road -> Reach.ONE_WAY
        else -> Reach.SHORT
    }
}

/** "배터리 68% · 약 260km": the number the verdict rests on, rounded to something a glance can hold. */
fun rangeLabel(percent: Int, usableRangeKm: Double): String = "배터리 $percent% · 약 ${(usableRangeKm / 10).roundToInt() * 10}km"
