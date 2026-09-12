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
    val pepsPowerMode: Int?,
    val currentCapacityWh: Float?,
    val nominalCapacityWh: Float?,
    val chargingLikelyHint: Boolean = false,
    /** Vehicle's own remaining-range estimate (RANGE_REMAINING), км — справочно, окна 5/15/30 не подменяет. */
    val vehicleRangeRemainingKm: Float? = null,
)

data class BufferPoint(
    val elapsedRealtimeMs: Long,
    val wallClockMs: Long,
    val cumulativeKm: Double,
    val socPercent: Float,
    val speedKmh: Float? = null,
    val outsideTempC: Float? = null,
    val chargingLikely: Boolean,
    val gap: Boolean,
    /**
     * Running total of SOC actually spent while driving, up to this point — only ever grows;
     * a charge's SOC jump never feeds into it. Lets a 5/15/30 km window keep computing a rate
     * across a charge stop instead of needing to reset and refill from empty.
     */
    val consumedSocPoints: Double = 0.0,
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
    val speedSumKmh: Double = 0.0,
    val speedSamples: Int = 0,
    val tempSumC: Double = 0.0,
    val tempSamples: Int = 0,
    /** t° воздуха на первом тике поездки. */
    val tempStartC: Float? = null,
    /** t° воздуха на последнем тике поездки — обновляется каждый тик, застывает на P. */
    val tempEndC: Float? = null,
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
    /** Mirrors [BufferPoint.consumedSocPoints]'s running total, restored so a restart mid-drive
     *  doesn't glitch it back to 0. */
    val bufferSocConsumed: Double = 0.0,
    val lastBufferDrivingSoc: Float? = null,
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

/** Sample for a window sparkline; [km] is distance within the window (0…windowKm). */
data class WindowChartPoint(
    val km: Float,
    val soc: Float,
    val speedKmh: Float? = null,
    val outsideTempC: Float? = null,
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

/** Persisted drive records — tiny JSON, written only on park / charge edge. */
data class DriveStatsSnapshot(
    val maxChargeCycleKm: Double = 0.0,
    val maxChargeCycleSocUsedPercent: Double = 0.0,
    val maxChargeCycleAvgSpeedKmh: Double? = null,
    val maxChargeCycleAvgTempC: Double? = null,
    val openChargeCycleKm: Double = 0.0,
    val openChargeCycleSocUsedPercent: Double = 0.0,
    val openChargeCycleSpeedWeightedSum: Double = 0.0,
    val openChargeCycleSpeedWeightKm: Double = 0.0,
    val openChargeCycleTempWeightedSum: Double = 0.0,
    val openChargeCycleTempWeightKm: Double = 0.0,
    val chargingSession: Boolean = false,
)

/** Records for the Trips screen: breakdown of the max charge-to-charge cycle. */
data class DriveStatsView(
    val maxChargeCycleKm: Double = 0.0,
    val maxChargeCycleSocUsedPercent: Double = 0.0,
    val maxChargeCycleAvgSpeedKmh: Double? = null,
    val maxChargeCycleAvgTempC: Double? = null,
)

/** One completed trip (P → P), saved for the Trips history list. */
data class TripRecord(
    val finishedAtMs: Long,
    val distanceKm: Double,
    val socStartPercent: Float?,
    val socEndPercent: Float?,
    val socUsedPercent: Double,
    val avgSpeedKmh: Double?,
    val tempStartC: Float?,
    val tempEndC: Float?,
    val avgTempC: Double?,
)

/** Trip in progress (left P, not parked yet) — same shape as [TripRecord], values as of now. */
data class ActiveTripView(
    val distanceKm: Double,
    val socStartPercent: Float?,
    val socNowPercent: Float?,
    val socUsedPercent: Double,
    val avgSpeedKmh: Double?,
    val tempStartC: Float?,
    val tempNowC: Float?,
    val avgTempC: Double?,
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
    /**
     * Как часто пересериализуем и пишем на диск весь буфер окон (до ~35 км истории, тысячи
     * точек на медленной езде). Больше интервал — меньше нагрузка на CPU/диск во время движения
     * ценой чуть большей потери буфера при аварийном перезапуске.
     */
    const val CHECKPOINT_INTERVAL_MS = 15_000L
    const val SOC_ODOMETER_POLL_MS = 10_000L
    const val OUTSIDE_TEMP_POLL_MS = 200_000L
    const val WH_PER_KWH = 1000.0
    /** Trips shorter than this are noise (parking shuffle) and do not update records. */
    const val MIN_COUNTED_TRIP_KM = 0.1
    /** SOC-up ticks while parked before a charge cycle is closed (flag is intermittent). */
    const val CHARGE_CONFIRM_TICKS = 3
    /** Trip history cap — oldest trips are dropped once the saved log passes this length. */
    const val MAX_SAVED_TRIPS = 200
    /** Geely EX2 HU reference canvas for UI scale (1920×1040 under typical chrome). */
    const val HU_CONTENT_WIDTH_PX = 1920
    const val HU_CONTENT_HEIGHT_PX = 1040
}
