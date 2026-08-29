package com.geely.ex2.range.domain.engine

import com.geely.ex2.range.domain.buffer.SampleRingBuffer
import com.geely.ex2.range.domain.calculation.Consumption
import com.geely.ex2.range.domain.calculation.DistanceAccumulator
import com.geely.ex2.range.domain.calculation.RangeWindows
import com.geely.ex2.range.domain.decode.SocDecoder
import com.geely.ex2.range.domain.model.BufferPoint
import com.geely.ex2.range.domain.model.ConsumptionRates
import com.geely.ex2.range.domain.model.EngineCheckpoint
import com.geely.ex2.range.domain.model.Gear
import com.geely.ex2.range.domain.model.PeriodSnapshot
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.SettingsSnapshot
import com.geely.ex2.range.domain.model.WindowStatus
import com.geely.ex2.range.domain.model.TelemetryTick
import com.geely.ex2.range.domain.model.TripSnapshot
import com.geely.ex2.range.domain.tracker.GearEvent
import com.geely.ex2.range.domain.tracker.GearMachine

data class EngineView(
    val socPercent: Float?,
    val speedKmh: Float?,
    val outsideTempC: Float?,
    val cabinTempC: Float?,
    val gear: Gear?,
    val parked: Boolean,
    val waitingForDrive: Boolean,
    val period: ConsumptionRates,
    val trip: ConsumptionRates,
    val tripIncomplete: Boolean,
    val windows: List<RangeWindow>,
    val usableCapacityKwh: Double?,
    val capacityIsUserSet: Boolean,
    val odometerTrusted: Boolean,
    val bufferCoveredKm: Double,
    val persistPeriod: Boolean,
    val persistBuffer: Boolean,
)

class RangeEngine {
    private val gearMachine = GearMachine()
    private val distance = DistanceAccumulator()
    private val buffer = SampleRingBuffer()

    private var periodDistanceKm: Double = 0.0
    private var periodSocUsed: Double = 0.0
    private var periodUpdatedAtMs: Long = 0L
    private var parkSessionId: Long? = null

    private var trip: TripSnapshot? = null
    private var lastDrivingSoc: Float? = null
    private var lastSoc: Float? = null
    private var userCapacityKwh: Double? = null
    private var vehicleNominalWh: Float? = null
    private var lastCheckpointElapsedMs: Long = Long.MIN_VALUE
    private var tripIncomplete: Boolean = false

    fun restore(checkpoint: EngineCheckpoint, settings: SettingsSnapshot) {
        userCapacityKwh = settings.usableCapacityKwh
        periodDistanceKm = checkpoint.period.distanceKm
        periodSocUsed = checkpoint.period.socUsedPoints
        periodUpdatedAtMs = checkpoint.period.updatedAtMs
        parkSessionId = checkpoint.period.parkSessionId
        lastDrivingSoc = checkpoint.lastDrivingSoc
        distance.restore(checkpoint.totalKm)
        buffer.restore(checkpoint.buffer)
        trip = checkpoint.trip
        gearMachine.restore(
            parked = checkpoint.trip?.active != true && checkpoint.period.parkSessionId != null,
            driving = checkpoint.trip?.active == true,
        )
        if (checkpoint.trip?.active == true) {
            tripIncomplete = true
        }
    }

    fun resetPeriod(nowMs: Long) {
        periodDistanceKm = 0.0
        periodSocUsed = 0.0
        periodUpdatedAtMs = nowMs
        parkSessionId = null
    }

    fun setUserCapacityKwh(value: Double?) {
        userCapacityKwh = value?.takeIf { it.isFinite() && it > 0.0 }
    }

    fun checkpoint(nowMs: Long): EngineCheckpoint {
        return EngineCheckpoint(
            period = PeriodSnapshot(
                distanceKm = periodDistanceKm,
                socUsedPoints = periodSocUsed,
                updatedAtMs = nowMs,
                parkSessionId = parkSessionId,
            ),
            buffer = buffer.snapshot(),
            trip = trip,
            totalKm = distance.totalKm,
            lastDrivingSoc = lastDrivingSoc,
        )
    }

    fun onTick(tick: TelemetryTick): EngineView {
        vehicleNominalWh = tick.nominalCapacityWh ?: vehicleNominalWh
        val events = gearMachine.tick(tick.elapsedRealtimeMs, tick.gear)
        val parked = gearMachine.parkConfirmed
        val accOff = tick.pepsPowerMode == RangeConstants.PEPS_ACC_OFF
        val distanceTick = distance.onTick(
            elapsedMs = tick.elapsedRealtimeMs,
            speedKmh = tick.speedKmh,
            odometerKm = tick.odometerKm,
            parked = parked,
            accOff = accOff,
        )

        val charging = detectCharging(tick, parked)
        applyGearEvents(events, tick)
        accumulatePeriod(tick, parked, distanceTick.deltaKm)
        updateLiveTrip(tick)
        appendBuffer(tick, charging, distanceTick.gap)

        val persistPeriod = gearMachine.shouldPersistPeriod()
        if (persistPeriod) {
            parkSessionId = tick.elapsedRealtimeMs
            periodUpdatedAtMs = tick.wallClockMs
            gearMachine.markPeriodSaved()
        }

        val persistBuffer = persistPeriod ||
            lastCheckpointElapsedMs == Long.MIN_VALUE ||
            tick.elapsedRealtimeMs - lastCheckpointElapsedMs >= RangeConstants.CHECKPOINT_INTERVAL_MS
        if (persistBuffer) {
            lastCheckpointElapsedMs = tick.elapsedRealtimeMs
        }

        lastSoc = tick.socPercent
        val capacity = SocDecoder.usableCapacityKwh(userCapacityKwh, vehicleNominalWh)
        val periodRates = rates(periodDistanceKm, periodSocUsed, capacity)
        val tripRates = tripRates(capacity)
        val windows = if (charging) {
            RangeConstants.RANGE_WINDOWS_KM.map { km ->
                RangeWindow(windowKm = km, status = WindowStatus.CHARGING)
            }
        } else {
            RangeWindows.estimateAll(buffer, tick.socPercent)
        }
        return EngineView(
            socPercent = tick.socPercent,
            speedKmh = tick.speedKmh,
            outsideTempC = tick.outsideTempC,
            cabinTempC = tick.cabinTempC,
            gear = gearMachine.displayedGear,
            parked = parked,
            waitingForDrive = parked || (trip?.active != true),
            period = periodRates,
            trip = tripRates,
            tripIncomplete = tripIncomplete && trip?.active == true,
            windows = windows,
            usableCapacityKwh = capacity,
            capacityIsUserSet = userCapacityKwh != null,
            odometerTrusted = distance.odometerTrusted,
            bufferCoveredKm = buffer.coveredKm(),
            persistPeriod = persistPeriod,
            persistBuffer = persistBuffer,
        )
    }

    private fun applyGearEvents(events: List<GearEvent>, tick: TelemetryTick) {
        for (event in events) {
            when (event) {
                GearEvent.ConfirmedPark -> {
                    val current = trip
                    if (current != null) {
                        trip = current.copy(active = false)
                    }
                    lastDrivingSoc = null
                }
                GearEvent.LeftPark, GearEvent.StartedDriving -> {
                    tripIncomplete = false
                    trip = TripSnapshot(
                        active = true,
                        soc0 = tick.socPercent,
                        km0 = distance.totalKm,
                        startedAtMs = tick.wallClockMs,
                        lastDistanceKm = 0.0,
                        lastSocUsedPoints = 0.0,
                    )
                    lastDrivingSoc = tick.socPercent
                }
            }
        }
    }

    private fun accumulatePeriod(tick: TelemetryTick, parked: Boolean, deltaKm: Double) {
        if (parked || deltaKm <= 0.0) return
        val soc = tick.socPercent
        if (soc != null) {
            val previous = lastDrivingSoc
            if (previous != null) {
                val drop = (previous - soc).toDouble()
                if (drop > 0.0) {
                    periodSocUsed += drop
                }
            }
            lastDrivingSoc = soc
        }
        periodDistanceKm += deltaKm
    }

    private fun updateLiveTrip(tick: TelemetryTick) {
        val current = trip ?: return
        if (!current.active) return
        val distanceKm = (distance.totalKm - current.km0).coerceAtLeast(0.0)
        val socUsed = if (current.soc0 != null && tick.socPercent != null) {
            (current.soc0 - tick.socPercent).toDouble()
        } else {
            current.lastSocUsedPoints
        }
        trip = current.copy(
            lastDistanceKm = distanceKm,
            lastSocUsedPoints = socUsed,
        )
    }

    private fun tripRates(capacity: Double?): ConsumptionRates {
        val current = trip
        if (current == null) {
            return ConsumptionRates(0.0, 0.0, null, null)
        }
        val socUsed = current.lastSocUsedPoints
        return rates(current.lastDistanceKm, socUsed, capacity)
    }

    private fun rates(distanceKm: Double, socUsed: Double, capacity: Double?): ConsumptionRates {
        val usableSoc = if (socUsed < 0.0) 0.0 else socUsed
        val pct = if (socUsed < 0.0) null else Consumption.pctPer100(distanceKm, usableSoc)
        val kwh = if (socUsed < 0.0) null else Consumption.kWhPer100(distanceKm, usableSoc, capacity)
        return ConsumptionRates(
            distanceKm = distanceKm,
            socUsedPoints = socUsed,
            pctPer100 = pct,
            kWhPer100 = kwh,
        )
    }

    private fun appendBuffer(tick: TelemetryTick, charging: Boolean, gap: Boolean) {
        val soc = tick.socPercent ?: return
        if (gearMachine.parkConfirmed) return
        buffer.add(
            BufferPoint(
                elapsedRealtimeMs = tick.elapsedRealtimeMs,
                wallClockMs = tick.wallClockMs,
                cumulativeKm = distance.totalKm,
                socPercent = soc,
                chargingLikely = charging,
                gap = gap,
            ),
        )
    }

    private fun detectCharging(tick: TelemetryTick, parked: Boolean): Boolean {
        if (tick.chargingLikelyHint) return true
        val soc = tick.socPercent ?: return false
        val previous = lastSoc ?: return false
        val rose = soc - previous > 0.15f
        val standing = tick.speedKmh == null || tick.speedKmh < 1f
        return rose && (parked || standing)
    }
}
