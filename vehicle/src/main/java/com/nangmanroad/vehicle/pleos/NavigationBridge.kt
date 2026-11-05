package com.nangmanroad.vehicle.pleos

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import com.nangmanroad.vehicle.data.LatLng
import com.nangmanroad.vehicle.data.Spot
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveDestination(
    val name: String,
    val remainMeters: Int,
    val remainSeconds: Int,
)

sealed interface NaviEvent {
    data class RouteStarted(val name: String) : NaviEvent
    data class Failed(val reason: String) : NaviEvent
    data object Arrived : NaviEvent
}

/** Hands destinations to the car's navigation app through the Pleos NaviHelper SDK. */
class NavigationBridge(context: Context) {

    private val navi = NaviHelper(context.applicationContext)

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _location = MutableStateFlow<LatLng?>(null)
    val location: StateFlow<LatLng?> = _location.asStateFlow()

    private val _destination = MutableStateFlow<ActiveDestination?>(null)
    val destination: StateFlow<ActiveDestination?> = _destination.asStateFlow()

    private val _events = MutableSharedFlow<NaviEvent>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<NaviEvent> = _events.asSharedFlow()

    private var started = false

    private val listener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            _location.value = LatLng(currentLocationInfo.latitude, currentLocationInfo.longitude)
        }

        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            _destination.value = destinationInfo.name.takeIf { it.isNotBlank() }?.let {
                ActiveDestination(it, destinationInfo.remainDistance, destinationInfo.remainTime)
            }
        }

        override fun onDestinationChanged() = refresh()

        override fun onRoutingCompleted() = refresh()

        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            refresh()
        }

        override fun onRouteCancelled() {
            _destination.value = null
        }

        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            _destination.value = null
            _events.tryEmit(NaviEvent.Arrived)
        }

        override fun onError(naviErrorCode: NaviErrorCode) {
            _events.tryEmit(NaviEvent.Failed(naviErrorCode.toMessage()))
        }
    }

    fun start() {
        if (started) return
        started = true
        runCatching {
            navi.initialize(
                onConnected = {
                    navi.addListener(listener)
                    _connected.value = true
                    navi.getCurrentLocationInfo()
                    refresh()
                },
                onFailed = { error ->
                    Log.w(TAG, "NaviHelper connection failed", error)
                    _connected.value = false
                },
            )
        }.onFailure {
            Log.w(TAG, "NaviHelper unavailable", it)
            started = false
        }
    }

    fun stop() {
        if (!started) return
        runCatching {
            navi.removeListener(listener)
            navi.release()
        }.onFailure { Log.w(TAG, "NaviHelper release failed", it) }
        started = false
        _connected.value = false
    }

    /** Returns false when the navigation service is not reachable, so the caller can say so. */
    fun requestRoute(spot: Spot): Boolean {
        if (!_connected.value) return false
        return runCatching {
            navi.requestRoute(
                RouteInfo(
                    latitude = spot.location.latitude,
                    longitude = spot.location.longitude,
                    displayLatitude = null,
                    displayLongitude = null,
                    poiName = spot.title,
                    poiId = "",
                    poiSubId = "0",
                    address = spot.address,
                ),
            )
            _events.tryEmit(NaviEvent.RouteStarted(spot.title))
        }.onFailure {
            Log.w(TAG, "requestRoute failed", it)
            _events.tryEmit(NaviEvent.Failed("내비게이션에 목적지를 보내지 못했어요"))
        }.isSuccess
    }

    /** Asks for the remaining distance and time again; the answer arrives in onDestinationInfo. */
    fun refresh() {
        if (_connected.value) runCatching { navi.getDestinationInfo() }
    }

    private fun NaviErrorCode.toMessage(): String = when (this) {
        NaviErrorCode.CAUSE_LOW_BATTERY -> "배터리가 부족해 경로를 찾지 못했어요"
        NaviErrorCode.CAUSE_SEARCH_ROUTE_OVER_DISTANCE -> "목적지가 너무 멀어 경로를 찾지 못했어요"
        NaviErrorCode.CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE -> "목적지가 너무 가까워요"
        else -> "경로를 찾지 못했어요"
    }

    private companion object {
        const val TAG = "NavigationBridge"
    }
}
