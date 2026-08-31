package com.geely.ex2.range.debug

import android.os.SystemClock
import com.geely.ex2.range.data.vhal.TelemetryRead
import com.geely.ex2.range.domain.model.BufferPoint
import com.geely.ex2.range.domain.model.EngineCheckpoint
import com.geely.ex2.range.domain.model.Gear
import com.geely.ex2.range.domain.model.PeriodSnapshot
import com.geely.ex2.range.domain.model.RawPropertyLine
import com.geely.ex2.range.domain.model.RawTelemetry
import com.geely.ex2.range.domain.model.TelemetryTick
import com.geely.ex2.range.domain.model.TripSnapshot
import kotlin.math.sin

/**
 * TEMP UI preview data for emulator / sideload without VHAL.
 * Delete this file and [AppContainer] hooks when mock is no longer needed.
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

    private const val TOTAL_KM = 142.8
    private const val TRIP_KM0 = 107.3
    private const val PERIOD_KM = 218.0
    private const val PERIOD_SOC = 28.4
    private const val CAPACITY_KWH = 39.4

    fun initialCheckpoint(nowMs: Long): EngineCheckpoint {
        return EngineCheckpoint(
            period = PeriodSnapshot(
                distanceKm = PERIOD_KM,
                socUsedPoints = PERIOD_SOC,
                updatedAtMs = nowMs,
                parkSessionId = if (scenario == Scenario.CHARGING) nowMs else null,
            ),
            buffer = buildBuffer(TOTAL_KM),
            trip = when (scenario) {
                Scenario.DRIVING -> TripSnapshot(
                    active = true,
                    soc0 = 71.2f,
                    km0 = TRIP_KM0,
                    startedAtMs = nowMs - 3_600_000L,
                    lastDistanceKm = TOTAL_KM - TRIP_KM0,
                    lastSocUsedPoints = 7.02,
                )
                Scenario.CHARGING -> null
            },
            totalKm = TOTAL_KM,
            lastDrivingSoc = if (scenario == Scenario.CHARGING) null else currentSoc(nowMs),
        )
    }

    fun pitchDegrees(nowMs: Long): Float {
        return 3.2f + sin(nowMs / 3_500.0).toFloat() * 2.8f
    }

    fun read(nowMs: Long): TelemetryRead {
        val soc = currentSoc(nowMs)
        val speed = when (scenario) {
            Scenario.DRIVING -> 52f + sin(nowMs / 2_800.0).toFloat() * 5f
            Scenario.CHARGING -> 0f
        }
        val gear = when (scenario) {
            Scenario.DRIVING -> Gear.DRIVE
            Scenario.CHARGING -> Gear.PARK
        }
        val tick = TelemetryTick(
            elapsedRealtimeMs = SystemClock.elapsedRealtime(),
            wallClockMs = nowMs,
            socPercent = soc,
            speedKmh = speed,
            odometerKm = TOTAL_KM.toFloat() + sin(nowMs / 5_000.0).toFloat() * 0.02f,
            gear = gear,
            outsideTempC = 4f + sin(nowMs / 12_000.0).toFloat() * 1.5f,
            pepsPowerMode = 1,
            currentCapacityWh = (CAPACITY_KWH * 1000.0 * soc / 100.0).toFloat(),
            nominalCapacityWh = (CAPACITY_KWH * 1000).toFloat(),
            chargingLikelyHint = scenario == Scenario.CHARGING,
        )
        return TelemetryRead(tick = tick, raw = rawTelemetry(soc, speed, gear))
    }

    private fun currentSoc(nowMs: Long): Float {
        return when (scenario) {
            Scenario.DRIVING -> {
                val base = 64.18f - ((nowMs % 120_000L) / 120_000f) * 0.35f
                base + sin(nowMs / 4_000.0).toFloat() * 0.08f
            }
            Scenario.CHARGING -> {
                71.2f + ((nowMs % 180_000L) / 180_000f) * 0.45f
            }
        }
    }

    private fun rawTelemetry(soc: Float, speed: Float, gear: Gear): RawTelemetry {
        return RawTelemetry(
            carReady = true,
            connectError = null,
            lines = listOf(
                line("OEM SOC", "0x2140A6ED", ok = true, raw = "float", decoded = "${soc}%"),
                line("PERF_VEHICLE_SPEED", "0x11600207", ok = true, raw = "m/s×3.6", decoded = "${speed.toInt()} km/h"),
                line("CURRENT_GEAR", "0x11400401", ok = true, raw = gear.vhalValue.toString(), decoded = gear.label),
                line("OUTSIDE_TEMP", "0x2140A377", ok = true, raw = "raw", decoded = "+4 °C"),
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

    private fun buildBuffer(totalKm: Double): List<BufferPoint> {
        val startSoc = 79.5f
        val endSoc = 64.18f
        val points = 48
        val baseElapsed = SystemClock.elapsedRealtime() - 3_600_000L
        return List(points) { index ->
            val t = index.toDouble() / (points - 1)
            val km = totalKm * t
            val trend = startSoc + (endSoc - startSoc) * t.toFloat()
            val wiggle = sin(km * 0.65).toFloat() * 0.55f
            val speed = (58f + sin(km * 0.75).toFloat() * 28f + sin(km * 2.3).toFloat() * 12f)
                .coerceIn(0f, 120f)
            BufferPoint(
                elapsedRealtimeMs = baseElapsed + (t * 3_600_000).toLong(),
                wallClockMs = 0L,
                cumulativeKm = km,
                socPercent = (trend + wiggle).coerceIn(20f, 100f),
                speedKmh = speed,
                chargingLikely = false,
                gap = false,
            )
        }
    }
}
