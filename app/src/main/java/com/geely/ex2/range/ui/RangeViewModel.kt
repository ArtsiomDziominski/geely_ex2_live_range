package com.geely.ex2.range.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geely.ex2.range.app.RangeApplication
import com.geely.ex2.range.app.RangeUiState
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

    val settings: StateFlow<SettingsSnapshot> = uiState.screenState { it.settings }
    val dashboard: StateFlow<DashboardUiState> = uiState.screenState(::dashboardState)
    val trips: StateFlow<TripsUiState> = uiState.screenState(::tripsState)
    val help: StateFlow<HelpUiState> = uiState.screenState(::helpState)

    /**
     * Состояние одного экрана из общего. Начальное значение считается из текущего состояния, а не
     * пустое: первый кадр вкладки сразу с данными, без лишней перекомпоновки «скелет → данные».
     * StateFlow отбрасывает равные значения — тики без видимых изменений до экрана не доходят.
     */
    private fun <T> StateFlow<RangeUiState>.screenState(transform: (RangeUiState) -> T): StateFlow<T> =
        map { transform(it) }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = transform(value),
        )

    private var pendingOverlayEnable = false

    /** resetPeriod()/setUserCapacityKwh() заканчиваются синхронным poll() — блокирующие чтения
     *  VHAL через Binder, поэтому уходим с главного потока, как и в retry(). */
    fun resetPeriod() {
        viewModelScope.launch(Dispatchers.Default) {
            container.resetPeriod()
        }
    }

    /** Повторный опрос телеметрии по кнопке «Повторить» в состоянии ошибки. */
    fun retry() {
        viewModelScope.launch(Dispatchers.Default) {
            container.poll()
        }
    }

    fun setUserCapacityKwh(value: Double?) {
        viewModelScope.launch(Dispatchers.Default) {
            container.setUserCapacityKwh(value)
        }
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

private fun dashboardState(state: RangeUiState) = DashboardUiState(
    engine = state.engine,
    connectError = state.raw.connectError,
    carReady = state.raw.carReady,
)

private fun tripsState(state: RangeUiState) = TripsUiState(
    driveStats = state.driveStats,
    trips = state.trips,
    currentTrip = state.engine?.let(::currentTripView),
)

private fun helpState(state: RangeUiState) = HelpUiState(
    usableCapacityKwh = state.settings.usableCapacityKwh,
    engine = state.engine,
    raw = state.raw,
)

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
