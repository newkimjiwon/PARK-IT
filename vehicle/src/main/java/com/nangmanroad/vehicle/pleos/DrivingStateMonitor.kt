package com.nangmanroad.vehicle.pleos

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
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
            if (initialized) vehicle.release()
        }.onFailure { Log.w(TAG, "Vehicle release failed", it) }
        registered = false
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
    }.onFailure { Log.w(TAG, "Vehicle SDK not ready", it) }.isSuccess

    private fun VehicleGear?.toDrivingState(): DrivingState = when (this) {
        VehicleGear.GEAR_PARK -> DrivingState.PARKED
        null, VehicleGear.GEAR_UNKNOWN -> DrivingState.UNKNOWN
        else -> DrivingState.DRIVING
    }

    private companion object {
        const val TAG = "DrivingStateMonitor"
        const val CONNECT_ATTEMPTS = 5
        const val CONNECT_RETRY_MS = 1_000L
    }
}
