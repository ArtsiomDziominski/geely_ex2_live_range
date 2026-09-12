package com.geely.ex2.range.app

import android.content.Context
import com.geely.ex2.range.data.store.JsonStores
import com.geely.ex2.range.data.store.SharedJsonStore
import com.geely.ex2.range.data.vhal.VehicleTelemetryReader
import com.geely.ex2.range.debug.UiPreviewMock
import com.geely.ex2.range.domain.engine.EngineView
import com.geely.ex2.range.domain.engine.RangeEngine
import com.geely.ex2.range.domain.model.AppThemeMode
import com.geely.ex2.range.domain.model.DriveStatsView
import com.geely.ex2.range.domain.model.EngineCheckpoint
import com.geely.ex2.range.domain.model.PeriodSnapshot
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.RawTelemetry
import com.geely.ex2.range.domain.model.SettingsSnapshot
import com.geely.ex2.range.domain.model.TripRecord
import com.geely.ex2.range.domain.tracker.DriveStatsTracker
import com.geely.ex2.range.overlay.RangeOverlayController
import com.geely.ex2.range.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RangeUiState(
    val engine: EngineView? = null,
    val raw: RawTelemetry = RawTelemetry(carReady = false, connectError = null, lines = emptyList()),
    val settings: SettingsSnapshot = SettingsSnapshot(),
    val driveStats: DriveStatsView = DriveStatsView(),
    val trips: List<TripRecord> = emptyList(),
)

class AppContainer(
    context: Context,
    private val persistScope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val lock = Any()
    private val stores = JsonStores(appContext.filesDir, SharedJsonStore(appContext))
    private val engine = RangeEngine()
    private val driveStats = DriveStatsTracker()
    private val time = AndroidTimeSource()
    private val reader = VehicleTelemetryReader(appContext)
    private val overlay = RangeOverlayController(appContext) { x, y ->
        setOverlayPosition(x, y)
    }

    private val _uiState = MutableStateFlow(RangeUiState(settings = stores.loadSettings()))
    val uiState: StateFlow<RangeUiState> = _uiState.asStateFlow()

    private val mockActive = BuildConfig.UI_PREVIEW_MOCK && UiPreviewMock.ENABLED
    private var lastOverlaySnapshot: OverlaySnapshot? = null
    private var trips: List<TripRecord> = emptyList()

    init {
        val settings = stores.loadSettings()
        val nowMs = time.wallClockMs()
        if (mockActive) {
            engine.restore(UiPreviewMock.initialCheckpoint(nowMs), settings)
            driveStats.restore(UiPreviewMock.driveStats)
            trips = UiPreviewMock.trips
        } else {
            val period = stores.loadPeriod()
            val checkpoint = stores.loadCheckpoint()
            val restored = (checkpoint ?: EngineCheckpoint(
                period = period,
                buffer = emptyList(),
                trip = null,
                totalKm = 0.0,
                lastDrivingSoc = null,
            )).copy(period = period)
            engine.restore(restored, settings)
            driveStats.restore(stores.loadDriveStats())
            trips = stores.loadTrips()
        }
        _uiState.value = RangeUiState(
            settings = settings,
            driveStats = driveStats.displayed(),
            trips = trips,
        )
    }

    fun startReader() {
        if (mockActive) {
            _uiState.value = _uiState.value.copy(
                raw = UiPreviewMock.read(time.wallClockMs()).raw,
            )
            poll()
            return
        }
        val raw = reader.connect()
        reader.subscribeLive()
        _uiState.value = _uiState.value.copy(raw = raw)
    }

    fun attachOverlay() {
        val settings = _uiState.value.settings
        overlay.setThemeMode(settings.themeMode)
        overlay.setSavedPosition(settings.overlayX, settings.overlayY)
        overlay.attach()
        if (settings.overlayEnabled && overlay.canDraw()) {
            overlay.setEnabled(true)
            syncOverlay(_uiState.value.engine)
        }
    }

    fun releaseOverlay() {
        overlay.release()
    }

    fun canDrawOverlays(): Boolean = overlay.canDraw()

    fun setOverlayEnabled(enabled: Boolean) {
        synchronized(lock) {
            val settings = _uiState.value.settings.copy(overlayEnabled = enabled)
            persistSettings(settings)
            _uiState.value = _uiState.value.copy(settings = settings)
            overlay.setSavedPosition(settings.overlayX, settings.overlayY)
            overlay.setEnabled(enabled)
            if (enabled) {
                syncOverlay(_uiState.value.engine, force = true)
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        synchronized(lock) {
            val settings = _uiState.value.settings.copy(themeMode = mode)
            persistSettings(settings)
            _uiState.value = _uiState.value.copy(settings = settings)
            overlay.setThemeMode(mode)
        }
    }

    private fun setOverlayPosition(x: Int, y: Int) {
        synchronized(lock) {
            val settings = _uiState.value.settings.copy(overlayX = x, overlayY = y)
            persistSettings(settings)
            _uiState.value = _uiState.value.copy(settings = settings)
        }
    }

    private fun persistSettings(settings: SettingsSnapshot) {
        if (mockActive) return
        persistScope.launch { stores.saveSettings(settings) }
    }

    private fun publishState(
        view: EngineView?,
        raw: RawTelemetry,
    ) {
        val previous = _uiState.value
        val settings = if (view?.capacityIsUserSet == true &&
            view.usableCapacityKwh != previous.settings.usableCapacityKwh
        ) {
            previous.settings.copy(usableCapacityKwh = view.usableCapacityKwh)
        } else {
            previous.settings
        }
        val statsView = driveStats.displayed()
        _uiState.value = RangeUiState(
            engine = view,
            raw = raw,
            settings = settings,
            driveStats = statsView,
            trips = trips,
        )
        syncOverlay(view)
    }

    private fun syncOverlay(view: EngineView?, force: Boolean = false) {
        if (!_uiState.value.settings.overlayEnabled) return
        val charging = view?.charging == true
        val snapshot = OverlaySnapshot(
            charging = charging,
            vehicleRangeRemainingKm = view?.vehicleRangeRemainingKm,
            windows = view?.windows.orEmpty().map { window ->
                OverlayWindowSnapshot(
                    windowKm = window.windowKm,
                    status = window.status,
                    rangeTo0Km = window.rangeTo0Km,
                )
            },
        )
        if (!force && snapshot == lastOverlaySnapshot) return
        lastOverlaySnapshot = snapshot
        overlay.update(
            windows = view?.windows.orEmpty(),
            charging = charging,
            vehicleRangeRemainingKm = view?.vehicleRangeRemainingKm,
        )
    }

    fun poll() {
        if (mockActive) {
            val read = UiPreviewMock.read(time.wallClockMs())
            synchronized(lock) {
                val view = engine.onTick(read.tick)
                publishState(
                    view = view,
                    raw = read.raw,
                )
            }
            return
        }
        val read = try {
            reader.read(time.wallClockMs())
        } catch (t: Throwable) {
            _uiState.value = _uiState.value.copy(
                raw = RawTelemetry(
                    carReady = false,
                    connectError = t.message ?: t.javaClass.simpleName,
                    lines = emptyList(),
                ),
            )
            return
        }
        synchronized(lock) {
            val view = engine.onTick(read.tick)
            val checkpoint = engine.checkpoint(read.tick.wallClockMs)
            if (view.persistPeriod) {
                val period = checkpoint.period
                driveStats.onParked(
                    tripKm = view.trip.distanceKm,
                    tripSocUsedPercent = view.trip.socUsedPoints,
                    tripAvgSpeedKmh = view.tripAvgSpeedKmh,
                    tripAvgTempC = view.tripAvgTempC,
                )
                if (view.trip.distanceKm >= RangeConstants.MIN_COUNTED_TRIP_KM) {
                    val record = TripRecord(
                        finishedAtMs = read.tick.wallClockMs,
                        distanceKm = view.trip.distanceKm,
                        socStartPercent = view.tripSocStartPercent,
                        socEndPercent = view.tripSocEndPercent,
                        socUsedPercent = view.trip.socUsedPoints,
                        avgSpeedKmh = view.tripAvgSpeedKmh,
                        tempStartC = view.tripTempStartC,
                        tempEndC = view.tripTempEndC,
                        avgTempC = view.tripAvgTempC,
                    )
                    trips = (trips + record).takeLast(RangeConstants.MAX_SAVED_TRIPS)
                    if (!mockActive) {
                        persistScope.launch { stores.saveTrips(trips) }
                    }
                }
                if (!mockActive) {
                    persistScope.launch { stores.savePeriod(period) }
                }
            }
            driveStats.onCharging(charging = view.charging, parked = view.parked)
            if (driveStats.dirty) {
                val snapshot = driveStats.snapshot()
                driveStats.markClean()
                if (!mockActive) {
                    persistScope.launch { stores.saveDriveStats(snapshot) }
                }
            }
            if (view.persistBuffer) {
                val savedCheckpoint = checkpoint
                if (!mockActive) {
                    persistScope.launch { stores.saveCheckpoint(savedCheckpoint) }
                }
            }
            publishState(
                view = view,
                raw = read.raw,
            )
        }
        if (read.raw.carReady) {
            reader.subscribeLive()
        }
    }

    fun resetPeriod() {
        synchronized(lock) {
            engine.resetPeriod(time.wallClockMs())
            if (!mockActive) {
                val period = PeriodSnapshot(
                    distanceKm = 0.0,
                    socUsedPoints = 0.0,
                    updatedAtMs = time.wallClockMs(),
                    parkSessionId = null,
                )
                val checkpoint = engine.checkpoint(time.wallClockMs())
                persistScope.launch {
                    stores.savePeriod(period)
                    stores.saveCheckpoint(checkpoint)
                }
            }
            val current = _uiState.value
            _uiState.value = current.copy(engine = current.engine)
        }
        poll()
    }

    fun deleteTrip(trip: TripRecord) {
        synchronized(lock) {
            trips = trips.filterNot { it.finishedAtMs == trip.finishedAtMs && it.distanceKm == trip.distanceKm }
            if (!mockActive) {
                persistScope.launch { stores.saveTrips(trips) }
            }
            _uiState.value = _uiState.value.copy(trips = trips)
        }
    }

    fun clearTrips() {
        synchronized(lock) {
            trips = emptyList()
            if (!mockActive) {
                persistScope.launch { stores.clearTrips() }
            }
            _uiState.value = _uiState.value.copy(trips = trips)
        }
    }

    fun setUserCapacityKwh(value: Double?) {
        synchronized(lock) {
            engine.setUserCapacityKwh(value)
            val settings = _uiState.value.settings.copy(usableCapacityKwh = value)
            persistSettings(settings)
            _uiState.value = _uiState.value.copy(settings = settings)
        }
        poll()
    }

    fun closeReader() {
        if (!mockActive) {
            reader.close()
        }
    }
}

private data class OverlayWindowSnapshot(
    val windowKm: Double,
    val status: com.geely.ex2.range.domain.model.WindowStatus,
    val rangeTo0Km: Double?,
)

private data class OverlaySnapshot(
    val charging: Boolean,
    val vehicleRangeRemainingKm: Float?,
    val windows: List<OverlayWindowSnapshot>,
)
