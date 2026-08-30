package com.geely.ex2.range.domain.model

enum class Gear(val vhalValue: Int, val label: String) {
    PARK(4, "P"),
    REVERSE(2, "R"),
    NEUTRAL(1, "N"),
    DRIVE(8, "D"),
}

data class TelemetryTick(
    val elapsedRealtimeMs: Long,
    val wallClockMs: Long,
    val socPercent: Float?,
    val speedKmh: Float?,
    val odometerKm: Float?,
    val gear: Gear?,
    val outsideTempC: Float?,
    val cabinTempC: Float?,
    val pepsPowerMode: Int?,
    val currentCapacityWh: Float?,
    val nominalCapacityWh: Float?,
    val chargingLikelyHint: Boolean = false,
)

data class BufferPoint(
    val elapsedRealtimeMs: Long,
    val wallClockMs: Long,
    val cumulativeKm: Double,
    val socPercent: Float,
    val chargingLikely: Boolean,
    val gap: Boolean,
)

data class PeriodSnapshot(
    val distanceKm: Double,
    val socUsedPoints: Double,
    val updatedAtMs: Long,
    val parkSessionId: Long?,
)

data class TripSnapshot(
    val active: Boolean,
    val soc0: Float?,
    val km0: Double,
    val startedAtMs: Long,
    val lastDistanceKm: Double,
    val lastSocUsedPoints: Double,
)

enum class AppThemeMode(val storageKey: String, val label: String) {
    SYSTEM("system", "Система"),
    LIGHT("light", "Светлая"),
    DARK("dark", "Тёмная"),
    ;

    companion object {
        fun fromStorageKey(key: String?): AppThemeMode {
            return entries.find { it.storageKey == key } ?: SYSTEM
        }
    }
}

data class SettingsSnapshot(
    val usableCapacityKwh: Double? = RangeConstants.EX2_DEFAULT_USABLE_CAPACITY_KWH,
    val reserveSocPercent: Double = RangeConstants.RESERVE_SOC_PERCENT,
    val overlayEnabled: Boolean = false,
    val overlayX: Int? = null,
    val overlayY: Int? = null,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
)

data class EngineCheckpoint(
    val period: PeriodSnapshot,
    val buffer: List<BufferPoint>,
    val trip: TripSnapshot?,
    val totalKm: Double,
    val lastDrivingSoc: Float?,
)

enum class WindowStatus {
    READY,
    NEED_MORE_KM,
    SOC_UNCHANGED,
    CHARGING,
    GAP,
    INVALID,
}

data class RangeWindow(
    val windowKm: Double,
    val status: WindowStatus,
    val remainingToFillKm: Double? = null,
    val rangeTo0Km: Double? = null,
    val rangeToReserveKm: Double? = null,
    val deltaSocPoints: Double? = null,
    val chart: List<WindowChartPoint> = emptyList(),
)

/** SOC sample for a window sparkline; [km] is distance within the window (0…windowKm). */
data class WindowChartPoint(
    val km: Float,
    val soc: Float,
)

data class ConsumptionRates(
    val distanceKm: Double,
    val socUsedPoints: Double,
    val pctPer100: Double?,
    val kWhPer100: Double?,
)

data class RawPropertyLine(
    val name: String,
    val propertyHex: String,
    val ok: Boolean,
    val rawText: String,
    val decodedText: String? = null,
)

data class RawTelemetry(
    val carReady: Boolean,
    val connectError: String?,
    val lines: List<RawPropertyLine>,
)

object RangeConstants {
    const val WINDOW_KM_5 = 5.0
    const val WINDOW_KM_15 = 15.0
    const val WINDOW_KM_30 = 30.0
    val RANGE_WINDOWS_KM: DoubleArray = doubleArrayOf(WINDOW_KM_5, WINDOW_KM_15, WINDOW_KM_30)

    const val BUFFER_KEEP_KM = 35.0
    const val MIN_SOC_STEP = 0.5
    const val RESERVE_SOC_PERCENT = 20.0
    /** Geely EX2 — полезная ёмкость батареи, кВт·ч (проверено на авто). */
    const val EX2_DEFAULT_USABLE_CAPACITY_KWH = 39.4
    const val GEAR_DEBOUNCE_MS = 500L
    const val STABLE_PARK_MS = 2_000L
    const val STANDSTILL_KMH = 0.5
    const val GAP_ELAPSED_MS = 30_000L
    const val PEPS_ACC_OFF = 0
    const val MAX_RANGE_KM = 800.0
    const val CHECKPOINT_INTERVAL_MS = 5_000L
    const val WH_PER_KWH = 1000.0
}
