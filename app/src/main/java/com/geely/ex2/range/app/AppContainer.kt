package com.geely.ex2.range.app

import android.content.Context
import com.geely.ex2.range.data.sensor.InclinationSensor
import com.geely.ex2.range.data.store.JsonStores
import com.geely.ex2.range.data.vhal.VehicleTelemetryReader
import com.geely.ex2.range.debug.UiPreviewMock
import com.geely.ex2.range.domain.engine.EngineView
import com.geely.ex2.range.domain.engine.RangeEngine
import com.geely.ex2.range.domain.model.AppThemeMode
import com.geely.ex2.range.domain.model.EngineCheckpoint
import com.geely.ex2.range.domain.model.PeriodSnapshot
import com.geely.ex2.range.domain.model.RawTelemetry
import com.geely.ex2.range.domain.model.SettingsSnapshot
import com.geely.ex2.range.overlay.RangeOverlayController
import com.geely.ex2.range.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RangeUiState(
    val engine: EngineView? = null,
    val pitchDegrees: Float? = null,
    val raw: RawTelemetry = RawTelemetry(carReady = false, connectError = null, lines = emptyList()),
    val settings: SettingsSnapshot = SettingsSnapshot(),
)

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val lock = Any()
    private val stores = JsonStores(appContext.filesDir)
    private val engine = RangeEngine()
    private val time = AndroidTimeSource()
    private val reader = VehicleTelemetryReader(appContext)
    private val inclination = InclinationSensor(appContext)
    private val overlay = RangeOverlayController(appContext) { x, y ->
        setOverlayPosition(x, y)
    }

    private val _uiState = MutableStateFlow(RangeUiState(settings = stores.loadSettings()))
    val uiState: StateFlow<RangeUiState> = _uiState.asStateFlow()

    private val mockActive = BuildConfig.UI_PREVIEW_MOCK && UiPreviewMock.ENABLED

    init {
        val settings = stores.loadSettings()
        val nowMs = time.wallClockMs()
        if (mockActive) {
            engine.restore(UiPreviewMock.initialCheckpoint(nowMs), settings)
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
        }
        _uiState.value = RangeUiState(settings = settings)
    }

    fun startReader() {
        inclination.start()
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
            if (!mockActive) {
                stores.saveSettings(settings)
            }
            _uiState.value = _uiState.value.copy(settings = settings)
            overlay.setSavedPosition(settings.overlayX, settings.overlayY)
            overlay.setEnabled(enabled)
            if (enabled) {
                syncOverlay(_uiState.value.engine)
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        synchronized(lock) {
            val settings = _uiState.value.settings.copy(themeMode = mode)
            if (!mockActive) {
                stores.saveSettings(settings)
            }
            _uiState.value = _uiState.value.copy(settings = settings)
            overlay.setThemeMode(mode)
        }
    }

    private fun setOverlayPosition(x: Int, y: Int) {
        synchronized(lock) {
            val settings = _uiState.value.settings.copy(overlayX = x, overlayY = y)
            if (!mockActive) {
                stores.saveSettings(settings)
            }
            _uiState.value = _uiState.value.copy(settings = settings)
        }
    }

    private fun publishState(
        view: EngineView?,
        pitchDegrees: Float?,
        raw: RawTelemetry,
    ) {
        val settings = _uiState.value.settings.copy(
            usableCapacityKwh = if (view?.capacityIsUserSet == true) {
                view.usableCapacityKwh
            } else {
                _uiState.value.settings.usableCapacityKwh
            },
        )
        _uiState.value = RangeUiState(
            engine = view,
            pitchDegrees = pitchDegrees,
            raw = raw,
            settings = settings,
        )
        syncOverlay(view)
    }

    private fun syncOverlay(view: EngineView?) {
        if (!_uiState.value.settings.overlayEnabled) return
        overlay.update(
            windows = view?.windows.orEmpty(),
            charging = view?.charging == true,
        )
    }

    fun poll() {
        if (mockActive) {
            val read = UiPreviewMock.read(time.wallClockMs())
            synchronized(lock) {
                val view = engine.onTick(read.tick)
                publishState(
                    view = view,
                    pitchDegrees = UiPreviewMock.pitchDegrees(time.wallClockMs()),
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
                stores.savePeriod(checkpoint.period)
            }
            if (view.persistBuffer) {
                stores.saveCheckpoint(checkpoint)
            }
            publishState(
                view = view,
                pitchDegrees = inclination.pitchDegrees(),
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
                stores.savePeriod(
                    PeriodSnapshot(
                        distanceKm = 0.0,
                        socUsedPoints = 0.0,
                        updatedAtMs = time.wallClockMs(),
                        parkSessionId = null,
                    ),
                )
                stores.saveCheckpoint(engine.checkpoint(time.wallClockMs()))
            }
            val current = _uiState.value
            _uiState.value = current.copy(engine = current.engine)
        }
        poll()
    }

    fun setUserCapacityKwh(value: Double?) {
        synchronized(lock) {
            engine.setUserCapacityKwh(value)
            val settings = _uiState.value.settings.copy(usableCapacityKwh = value)
            if (!mockActive) {
                stores.saveSettings(settings)
            }
            _uiState.value = _uiState.value.copy(settings = settings)
        }
        poll()
    }

    fun closeReader() {
        inclination.stop()
        if (!mockActive) {
            reader.close()
        }
    }
}
