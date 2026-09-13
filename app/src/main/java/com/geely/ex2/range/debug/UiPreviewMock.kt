package com.geely.ex2.range.debug

import android.os.SystemClock
import com.geely.ex2.range.data.vhal.TelemetryRead
import com.geely.ex2.range.domain.model.BufferPoint
import com.geely.ex2.range.domain.model.DriveStatsSnapshot
import com.geely.ex2.range.domain.model.EngineCheckpoint
import com.geely.ex2.range.domain.model.Gear
import com.geely.ex2.range.domain.model.PeriodSnapshot
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.RawPropertyLine
import com.geely.ex2.range.domain.model.RawTelemetry
import com.geely.ex2.range.domain.model.TelemetryTick
import com.geely.ex2.range.domain.model.TripRecord
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * TEMP UI preview data for emulator / sideload without VHAL.
 *
 * Mocks only live inputs the engine needs: SOC %, odometer km, outdoor °C
 * (plus supporting speed/gear so distance can accumulate). Period, trip,
 * windows and kWh are computed by [com.geely.ex2.range.domain.engine.RangeEngine].
 */
object UiPreviewMock {
    /** Flip to false to disable without deleting the file. */
    const val ENABLED = true

    enum class Scenario {
        DRIVING,
        CHARGING,
    }

    /** Switch scenario for layout checks. */
    val scenario: Scenario = Scenario.DRIVING

    private const val CAPACITY_KWH = 39.4
    private const val ODO_START_KM = 12_480.0

    /** See [seedBuffer]. */
    private val SEED_KM = RangeConstants.WINDOW_KM_5
    private const val SEED_STEP_KM = 0.25
    private const val SEED_CONSUMPTION_PERCENT_PER_KM = 0.38

    /** Empty records — live maxes grow from parked trips if you switch scenarios later. */
    val driveStats: DriveStatsSnapshot = DriveStatsSnapshot()

    /** Sample trip history for the Trips screen — spread across three days for filter testing. */
    val trips: List<TripRecord> = run {
        val now = System.currentTimeMillis()
        val hour = 60 * 60 * 1000L
        val day = 24 * hour
        listOf(
            TripRecord(
                finishedAtMs = now - 2 * hour,
                distanceKm = 18.4,
                socStartPercent = 82f,
                socEndPercent = 74f,
                socUsedPercent = 8.0,
                avgSpeedKmh = 47.0,
                tempStartC = 6f,
                tempEndC = 9f,
                avgTempC = 7.5,
            ),
            TripRecord(
                finishedAtMs = now - 6 * hour,
                distanceKm = 42.1,
                socStartPercent = 95f,
                socEndPercent = 78f,
                socUsedPercent = 17.0,
                avgSpeedKmh = 78.0,
                tempStartC = 3f,
                tempEndC = 5f,
                avgTempC = 4.0,
            ),
            TripRecord(
                finishedAtMs = now - day - 3 * hour,
                distanceKm = 9.7,
                socStartPercent = 60f,
                socEndPercent = 55f,
                socUsedPercent = 5.0,
                avgSpeedKmh = 32.0,
                tempStartC = -2f,
                tempEndC = 1f,
                avgTempC = -0.5,
            ),
            TripRecord(
                finishedAtMs = now - day - 8 * hour,
                distanceKm = 63.5,
                socStartPercent = 90f,
                socEndPercent = 62f,
                socUsedPercent = 28.0,
                avgSpeedKmh = 91.0,
                tempStartC = -5f,
                tempEndC = -3f,
                avgTempC = -4.0,
            ),
            TripRecord(
                finishedAtMs = now - 2 * day - 1 * hour,
                distanceKm = 5.2,
                socStartPercent = 40f,
                socEndPercent = 37f,
                socUsedPercent = 3.0,
                avgSpeedKmh = 24.0,
                tempStartC = 12f,
                tempEndC = 13f,
                avgTempC = 12.5,
            ),
            TripRecord(
                finishedAtMs = now - 2 * day - 5 * hour,
                distanceKm = 27.8,
                socStartPercent = 70f,
                socEndPercent = 58f,
                socUsedPercent = 12.0,
                avgSpeedKmh = 65.0,
                tempStartC = 10f,
                tempEndC = 14f,
                avgTempC = 12.0,
            ),
        )
    }

    private var sessionStartElapsedMs: Long = Long.MIN_VALUE
    private var lastTickElapsedMs: Long = Long.MIN_VALUE
    private var odometerKm: Double = ODO_START_KM

    /**
     * Cold start: empty period. For DRIVING, buffer is pre-seeded with [SEED_KM] km of synthetic
     * “already driven” history (see [seedBuffer]) so the 5 km forecast window is READY right
     * away instead of only after several real minutes of the mock accumulating it itself.
     *
     * Trip is left closed (like CHARGING) rather than pre-opened: with a non-empty buffer, an
     * already-active trip would make [com.geely.ex2.range.domain.engine.RangeEngine.restore]
     * flag it as a restart mid-drive (“Поездка неполная”). The real gear/trip machinery opens a
     * fresh trip within one debounce tick (500 ms) of the first DRIVE reading — imperceptible
     * here — so this only trades a moment.
     */
    fun initialCheckpoint(nowMs: Long): EngineCheckpoint {
        resetSession()
        val startSoc = mockSocPercent(0.0)
        return when (scenario) {
            Scenario.DRIVING -> {
                val seed = seedBuffer(startSoc)
                EngineCheckpoint(
                    period = PeriodSnapshot(
                        distanceKm = 0.0,
                        socUsedPoints = 0.0,
                        updatedAtMs = nowMs,
                        parkSessionId = null,
                    ),
                    buffer = seed.points,
                    trip = null,
                    totalKm = 0.0,
                    lastDrivingSoc = null,
                    bufferSocConsumed = seed.consumedSocPoints,
                    lastBufferDrivingSoc = seed.lastSocPercent,
                )
            }
            Scenario.CHARGING -> EngineCheckpoint(
                period = PeriodSnapshot(
                    distanceKm = 0.0,
                    socUsedPoints = 0.0,
                    updatedAtMs = nowMs,
                    parkSessionId = nowMs,
                ),
                buffer = emptyList(),
                trip = null,
                totalKm = 0.0,
                lastDrivingSoc = null,
            )
        }
    }

    private class SeedBuffer(
        val points: List<BufferPoint>,
        val consumedSocPoints: Double,
        val lastSocPercent: Float,
    )

    /**
     * [SEED_KM] km of synthetic buffer history ending exactly “now” (cumulativeKm 0, SOC
     * [startSoc]) so [com.geely.ex2.range.domain.calculation.RangeWindows] sees the 5 km window
     * as already covered. Uses negative cumulativeKm for the past — the mock’s own live distance
     * still starts counting from 0, unaffected.
     */
    private fun seedBuffer(startSoc: Float): SeedBuffer {
        val steps = (SEED_KM / SEED_STEP_KM).roundToInt().coerceAtLeast(1)
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        val stepMs = 15_000L
        val points = ArrayList<BufferPoint>(steps + 1)
        for (i in 0..steps) {
            val kmAgo = SEED_KM - i * SEED_STEP_KM
            val consumed = (SEED_KM - kmAgo) * SEED_CONSUMPTION_PERCENT_PER_KM
            val soc = (startSoc + kmAgo * SEED_CONSUMPTION_PERCENT_PER_KM).coerceAtMost(100.0).toFloat()
            points += BufferPoint(
                elapsedRealtimeMs = nowElapsed - (steps - i) * stepMs,
                wallClockMs = nowWall - (steps - i) * stepMs,
                cumulativeKm = -kmAgo,
                socPercent = soc,
                speedKmh = 65f,
                outsideTempC = 8f,
                chargingLikely = false,
                gap = false,
                consumedSocPoints = consumed,
            )
        }
        return SeedBuffer(points, points.last().consumedSocPoints, points.last().socPercent)
    }

    fun read(nowMs: Long): TelemetryRead {
        val elapsed = SystemClock.elapsedRealtime()
        ensureSession(elapsed)
        val dtMs = if (lastTickElapsedMs == Long.MIN_VALUE) {
            0L
        } else {
            (elapsed - lastTickElapsedMs).coerceAtLeast(0L)
        }
        lastTickElapsedMs = elapsed

        val t = sessionSeconds(elapsed)
        val speed = mockSpeedKmh(t)
        if (scenario == Scenario.DRIVING && dtMs > 0L) {
            odometerKm += speed * (dtMs / 3_600_000.0)
        }

        val soc = mockSocPercent(t)
        val temp = mockOutsideTempC(t)
        val gear = when (scenario) {
            Scenario.DRIVING -> Gear.DRIVE
            Scenario.CHARGING -> Gear.PARK
        }
        val tick = TelemetryTick(
            elapsedRealtimeMs = elapsed,
            wallClockMs = nowMs,
            socPercent = soc,
            speedKmh = if (scenario == Scenario.DRIVING) speed else 0f,
            odometerKm = odometerKm.toFloat(),
            gear = gear,
            outsideTempC = temp,
            pepsPowerMode = 1,
            currentCapacityWh = (CAPACITY_KWH * 1000.0 * soc / 100.0).toFloat(),
            nominalCapacityWh = (CAPACITY_KWH * 1000).toFloat(),
            chargingLikelyHint = scenario == Scenario.CHARGING,
            vehicleRangeRemainingKm = (soc * 3.5).toFloat(),
        )
        return TelemetryRead(tick = tick, raw = rawTelemetry(soc, speed, gear, temp))
    }

    private fun resetSession() {
        sessionStartElapsedMs = Long.MIN_VALUE
        lastTickElapsedMs = Long.MIN_VALUE
        odometerKm = ODO_START_KM
    }

    private fun ensureSession(elapsedMs: Long) {
        if (sessionStartElapsedMs == Long.MIN_VALUE) {
            sessionStartElapsedMs = elapsedMs
            lastTickElapsedMs = elapsedMs
            odometerKm = ODO_START_KM
        }
    }

    private fun sessionSeconds(elapsedMs: Long = SystemClock.elapsedRealtime()): Double {
        ensureSession(elapsedMs)
        return (elapsedMs - sessionStartElapsedMs).coerceAtLeast(0L) / 1000.0
    }

    /** Smooth speed 25…115 km/h — also drives odometer growth. */
    private fun mockSpeedKmh(tSec: Double): Float {
        val wave = 70.0 + 35.0 * sin(tSec / 40.0) + 10.0 * sin(tSec / 13.0)
        return wave.toFloat().coerceIn(25f, 115f)
    }

    /**
     * Smooth SOC across a wide band (~18…95 %).
     * Driving: slow drain + long sine. Charging: slow rise + sine.
     */
    private fun mockSocPercent(tSec: Double): Float {
        return when (scenario) {
            Scenario.DRIVING -> {
                val cycle = 22.0 * 60.0
                val phase = (tSec % cycle) / cycle
                val drain = 92.0 - phase * 70.0
                val wobble = sin(tSec / 55.0) * 4.5 + sin(tSec / 19.0) * 1.8
                (drain + wobble).toFloat().coerceIn(18f, 95f)
            }
            Scenario.CHARGING -> {
                val cycle = 18.0 * 60.0
                val phase = (tSec % cycle) / cycle
                val rise = 22.0 + phase * 72.0
                val wobble = sin(tSec / 48.0) * 2.0
                (rise + wobble).toFloat().coerceIn(18f, 98f)
            }
        }
    }

    /** Smooth outdoor temperature ≈ −18…+36 °C. */
    private fun mockOutsideTempC(tSec: Double): Float {
        val base = 8.0 + 22.0 * sin(2.0 * PI * tSec / (16.0 * 60.0))
        val detail = 4.0 * sin(tSec / 37.0) + 1.5 * sin(tSec / 9.0)
        return (base + detail).toFloat().coerceIn(-18f, 36f)
    }

    private fun rawTelemetry(soc: Float, speed: Float, gear: Gear, temp: Float): RawTelemetry {
        return RawTelemetry(
            carReady = true,
            connectError = null,
            lines = listOf(
                line("OEM SOC", "0x2140A6ED", ok = true, raw = "mock", decoded = String.format("%.2f %%", soc)),
                line("PERF_VEHICLE_SPEED", "0x11600207", ok = true, raw = "mock", decoded = "${speed.toInt()} km/h"),
                line("PERF_ODOMETER", "0x11600204", ok = true, raw = "mock", decoded = String.format("%.3f км", odometerKm)),
                line("CURRENT_GEAR", "0x11400401", ok = true, raw = gear.vhalValue.toString(), decoded = gear.label),
                line("OUTSIDE_TEMP", "0x2140A377", ok = true, raw = "mock", decoded = String.format("%+.1f °C", temp)),
                line("INFO_EV_BATTERY_CAPACITY", "0x11600106", ok = true, raw = "Wh", decoded = "$CAPACITY_KWH kWh"),
            ),
        )
    }

    private fun line(
        name: String,
        hex: String,
        ok: Boolean,
        raw: String,
        decoded: String,
    ): RawPropertyLine {
        return RawPropertyLine(
            name = name,
            propertyHex = hex,
            ok = ok,
            rawText = raw,
            decodedText = decoded,
        )
    }
}
