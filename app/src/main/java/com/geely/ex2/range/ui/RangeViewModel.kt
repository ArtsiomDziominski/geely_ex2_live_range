package com.geely.ex2.range.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geely.ex2.range.app.RangeApplication
import com.geely.ex2.range.domain.engine.EngineView
import com.geely.ex2.range.domain.model.ActiveTripView
import com.geely.ex2.range.domain.model.AppThemeMode
import com.geely.ex2.range.domain.model.DriveStatsView
import com.geely.ex2.range.domain.model.RawTelemetry
import com.geely.ex2.range.domain.model.SettingsSnapshot
import com.geely.ex2.range.domain.model.TripRecord

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val engine: EngineView? = null,
    /** Текст ошибки чтения телеметрии — нужен UI для состояния ошибки. */
    val connectError: String? = null,
    val carReady: Boolean = false,
)

data class TripsUiState(
    val driveStats: DriveStatsView = DriveStatsView(),
    val trips: List<TripRecord> = emptyList(),
    /** Non-null while a trip is open (left P, not parked yet) — shown above the history. */
    val currentTrip: ActiveTripView? = null,
)

data class HelpUiState(
    val usableCapacityKwh: Double? = null,
    val engine: EngineView? = null,
    val raw: RawTelemetry = RawTelemetry(carReady = false, connectError = null, lines = emptyList()),
)

class RangeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as RangeApplication).container
    val uiState = container.uiState

    val settings: StateFlow<SettingsSnapshot> = uiState
        .map { it.settings }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = uiState.value.settings,
        )

    val dashboard: StateFlow<DashboardUiState> = uiState
        .map {
            DashboardUiState(
                engine = it.engine,
                connectError = it.raw.connectError,
                carReady = it.raw.carReady,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(),
        )

    val trips: StateFlow<TripsUiState> = uiState
        .map {
            TripsUiState(
                driveStats = it.driveStats,
                trips = it.trips,
                currentTrip = it.engine?.let(::currentTripView),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TripsUiState(),
        )

    val help: StateFlow<HelpUiState> = uiState
        .map {
            HelpUiState(
                usableCapacityKwh = it.settings.usableCapacityKwh,
                engine = it.engine,
                raw = it.raw,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HelpUiState(),
        )

    private var pendingOverlayEnable = false

    fun resetPeriod() {
        container.resetPeriod()
    }

    /** Повторный опрос телеметрии по кнопке «Повторить» в состоянии ошибки. */
    fun retry() {
        viewModelScope.launch(Dispatchers.Default) {
            container.poll()
        }
    }

    fun setUserCapacityKwh(value: Double?) {
        container.setUserCapacityKwh(value)
    }

    fun canDrawOverlays(): Boolean = container.canDrawOverlays()

    fun setOverlayEnabled(enabled: Boolean): Boolean {
        if (enabled && !container.canDrawOverlays()) {
            pendingOverlayEnable = true
            return false
        }
        pendingOverlayEnable = false
        container.setOverlayEnabled(enabled)
        return true
    }

    fun onOverlayPermissionResult() {
        if (pendingOverlayEnable && container.canDrawOverlays()) {
            pendingOverlayEnable = false
            container.setOverlayEnabled(true)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        container.setThemeMode(mode)
    }

    fun clearPendingOverlayEnable() {
        pendingOverlayEnable = false
    }

    fun deleteTrip(trip: TripRecord) {
        container.deleteTrip(trip)
    }

    fun clearTrips() {
        container.clearTrips()
    }
}

/** Non-null only while actually driving (left P, not parked) — the trip not yet saved to history. */
private fun currentTripView(view: EngineView): ActiveTripView? {
    if (view.waitingForDrive) return null
    return ActiveTripView(
        distanceKm = view.trip.distanceKm,
        socStartPercent = view.tripSocStartPercent,
        socNowPercent = view.tripSocEndPercent,
        socUsedPercent = view.trip.socUsedPoints,
        avgSpeedKmh = view.tripAvgSpeedKmh,
        tempStartC = view.tripTempStartC,
        tempNowC = view.tripTempEndC,
        avgTempC = view.tripAvgTempC,
    )
}
