package com.nangmanroad.vehicle.pleos

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The traction battery as the Vehicle SDK reports it; null until the service answers. */
data class EvBatteryStatus(val percent: Int, val capacityKwh: Double)

enum class DrivingState {
    /** No answer from the Vehicle service yet, e.g. on a phone or a plain AAOS image. */
    UNKNOWN,
    PARKED,
    DRIVING,
}

/**
 * Watches the gear selector through the Pleos Vehicle SDK. Anything other than P counts as
 * driving, so the app falls back to the glanceable screen as soon as the car can move.
 */
class DrivingStateMonitor(context: Context, private val scope: CoroutineScope) {

    private val vehicle = Vehicle(context.applicationContext)
    private val _state = MutableStateFlow(DrivingState.UNKNOWN)
    val state: StateFlow<DrivingState> = _state.asStateFlow()

    private val _battery = MutableStateFlow<EvBatteryStatus?>(null)
    val battery: StateFlow<EvBatteryStatus?> = _battery.asStateFlow()
    private var batteryPercent: Int? = null
    private var batteryCapacityKwh: Double? = null
    private var batteryRegistered = false

    private var initialized = false
    private var registered = false
    private var connectJob: Job? = null

    private val listener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            _state.value = gear.toDrivingState()
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "Gear updates failed", e)
        }
    }

    private val batteryListener = object : EvBatteryStateOfChargeListener {
        override fun onEvBatteryStateOfChargeUpdated(percent: Int?) {
            batteryPercent = percent
            publishBattery()
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "Battery updates failed", e)
        }
    }

    private fun publishBattery() {
        val percent = batteryPercent ?: return
        val capacity = batteryCapacityKwh ?: return
        _battery.value = EvBatteryStatus(percent, capacity)
    }

    fun start() {
        if (connectJob?.isActive == true || registered) return
        connectJob = scope.launch {
            // The Vehicle service binds asynchronously; retry a few times while it comes up.
            repeat(CONNECT_ATTEMPTS) { attempt ->
                if (tryRegister()) return@launch
                delay(CONNECT_RETRY_MS * (attempt + 1))
            }
            Log.w(TAG, "Vehicle service unavailable; driving state stays UNKNOWN")
        }
    }

    fun stop() {
        connectJob?.cancel()
        runCatching {
            if (registered) vehicle.getGear().unregisterGearSelection(listener)
            if (batteryRegistered) vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(batteryListener)
            if (initialized) vehicle.release()
        }.onFailure { Log.w(TAG, "Vehicle release failed", it) }
        registered = false
        batteryRegistered = false
        initialized = false
    }

    private fun tryRegister(): Boolean = runCatching {
        if (!initialized) {
            vehicle.initialize()
            initialized = true
        }
        val gear = vehicle.getGear()
        gear.getCurrentGearSelectionState(
            { current -> _state.value = current.toDrivingState() },
            { error -> Log.w(TAG, "Reading the gear failed", error) },
        )
        gear.registerGearSelection(listener)
        registered = true
        // The battery is optional: a car without an EV pack simply keeps the range verdict hidden.
        runCatching {
            val ev = vehicle.getEvBattery()
            ev.getEvBatteryCapacity(
                { capacity -> batteryCapacityKwh = capacity?.toDouble()?.asKwh() ?: DEFAULT_PACK_KWH; publishBattery() },
                // Some builds guard the capacity behind another permission; a typical pack keeps the verdict useful.
                { Log.w(TAG, "Battery capacity failed, assuming ${DEFAULT_PACK_KWH} kWh", it); batteryCapacityKwh = DEFAULT_PACK_KWH; publishBattery() },
            )
            ev.getEvBatteryStateOfCharge({ percent -> batteryPercent = percent; publishBattery() }, { Log.w(TAG, "Battery charge failed", it) })
            ev.registerEvBatteryStateOfCharge(batteryListener)
            batteryRegistered = true
        }.onFailure { Log.w(TAG, "EV battery unavailable", it) }
    }.onFailure { Log.w(TAG, "Vehicle SDK not ready", it) }.isSuccess

    /** The service reports the pack in Wh on the emulator (77400) and kWh in the docs; take either. */
    private fun Double.asKwh(): Double = if (this > 1_000) this / 1_000 else this

    private fun VehicleGear?.toDrivingState(): DrivingState = when (this) {
        VehicleGear.GEAR_PARK -> DrivingState.PARKED
        null, VehicleGear.GEAR_UNKNOWN -> DrivingState.UNKNOWN
        else -> DrivingState.DRIVING
    }

    private companion object {
        const val TAG = "DrivingStateMonitor"
        const val CONNECT_ATTEMPTS = 5
        const val CONNECT_RETRY_MS = 1_000L
        /** Ioniq 5 long-range pack, the car the Pleos emulator models. */
        const val DEFAULT_PACK_KWH = 77.4
    }
}
