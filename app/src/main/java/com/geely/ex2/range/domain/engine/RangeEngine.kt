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
    val socDeltaPoints: Float?,
    val speedKmh: Float?,
    val outsideTempC: Float?,
    val gear: Gear?,
    val odometerKm: Float?,
    val vehicleRangeRemainingKm: Float?,
    val parked: Boolean,
    val waitingForDrive: Boolean,
    val charging: Boolean,
    val period: ConsumptionRates,
    val trip: ConsumptionRates,
    val tripIncomplete: Boolean,
    val tripAvgSpeedKmh: Double?,
    val tripAvgTempC: Double?,
    val tripSocStartPercent: Float?,
    val tripSocEndPercent: Float?,
    val tripTempStartC: Float?,
    val tripTempEndC: Float?,
    val windows: List<RangeWindow>,
    val usableCapacityKwh: Double?,
    val capacityIsUserSet: Boolean,
    val odometerTrusted: Boolean,
    val bufferCoveredKm: Double,
    val tripDurationMs: Long?,
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

    /** Drives [BufferPoint.consumedSocPoints] — see there for why it exists. */
    private var bufferSocConsumed: Double = 0.0
    private var lastBufferDrivingSoc: Float? = null

    fun restore(checkpoint: EngineCheckpoint, settings: SettingsSnapshot) {
        userCapacityKwh = settings.usableCapacityKwh
        periodDistanceKm = checkpoint.period.distanceKm
        periodSocUsed = checkpoint.period.socUsedPoints
        periodUpdatedAtMs = checkpoint.period.updatedAtMs
        parkSessionId = checkpoint.period.parkSessionId
        lastDrivingSoc = checkpoint.lastDrivingSoc
        bufferSocConsumed = checkpoint.bufferSocConsumed
        lastBufferDrivingSoc = checkpoint.lastBufferDrivingSoc
        distance.restore(checkpoint.totalKm)
        buffer.restore(checkpoint.buffer)
        trip = checkpoint.trip
        gearMachine.restore(
            parked = checkpoint.trip?.active != true && checkpoint.period.parkSessionId != null,
            driving = checkpoint.trip?.active == true,
        )
        // Incomplete only after mid-drive restart with real progress — not a fresh trip open at 0 km.
        tripIncomplete = checkpoint.trip?.active == true &&
            (checkpoint.trip.lastDistanceKm > 0.01 || checkpoint.buffer.isNotEmpty())
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
            bufferSocConsumed = bufferSocConsumed,
            lastBufferDrivingSoc = lastBufferDrivingSoc,
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
        accumulateBufferConsumption(tick, parked)
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

        val socDelta = if (tick.socPercent != null && lastSoc != null) {
            tick.socPercent - lastSoc!!
        } else {
            null
        }
        lastSoc = tick.socPercent
        val capacity = SocDecoder.usableCapacityKwh(userCapacityKwh, vehicleNominalWh)
        val periodRates = rates(periodDistanceKm, periodSocUsed, capacity)
        val tripRates = tripRates(capacity)
        val tripSnapshot = trip
        val tripAvgSpeedKmh = tripSnapshot?.takeIf { it.speedSamples > 0 }
            ?.let { it.speedSumKmh / it.speedSamples }
        val tripAvgTempC = tripSnapshot?.takeIf { it.tempSamples > 0 }
            ?.let { it.tempSumC / it.tempSamples }
        val tripSocEndPercent = tripSnapshot?.soc0?.let { soc0 ->
            (soc0 - tripSnapshot.lastSocUsedPoints).toFloat()
        }
        val windows = if (charging) {
            RangeConstants.RANGE_WINDOWS_KM.map { km ->
                RangeWindow(windowKm = km, status = WindowStatus.CHARGING)
            }
        } else {
            RangeWindows.estimateAll(buffer, tick.socPercent)
        }
        return EngineView(
            socPercent = tick.socPercent,
            socDeltaPoints = socDelta,
            speedKmh = tick.speedKmh,
            outsideTempC = tick.outsideTempC,
            gear = gearMachine.displayedGear,
            odometerKm = tick.odometerKm,
            vehicleRangeRemainingKm = tick.vehicleRangeRemainingKm,
            parked = parked,
            waitingForDrive = parked || (trip?.active != true),
            charging = charging,
            period = periodRates,
            trip = tripRates,
            tripIncomplete = tripIncomplete && trip?.active == true,
            tripAvgSpeedKmh = tripAvgSpeedKmh,
            tripAvgTempC = tripAvgTempC,
            tripSocStartPercent = tripSnapshot?.soc0,
            tripSocEndPercent = tripSocEndPercent,
            tripTempStartC = tripSnapshot?.tempStartC,
            tripTempEndC = tripSnapshot?.tempEndC,
            windows = windows,
            usableCapacityKwh = capacity,
            capacityIsUserSet = userCapacityKwh != null,
            odometerTrusted = distance.odometerTrusted,
            bufferCoveredKm = buffer.coveredKm(),
            tripDurationMs = trip?.let { (tick.wallClockMs - it.startedAtMs).coerceAtLeast(0L) },
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
                    // A charge (or just sitting) between here and the next drive must not be
                    // read as consumption once driving resumes — see accumulateBufferConsumption.
                    lastBufferDrivingSoc = null
                }
                GearEvent.LeftPark, GearEvent.StartedDriving -> {
                    tripIncomplete = false
                    val startTemp = tick.outsideTempC?.takeIf { it.isFinite() }
                    trip = TripSnapshot(
                        active = true,
                        soc0 = tick.socPercent,
                        km0 = distance.totalKm,
                        startedAtMs = tick.wallClockMs,
                        lastDistanceKm = 0.0,
                        lastSocUsedPoints = 0.0,
                        tempStartC = startTemp,
                        tempEndC = startTemp,
                    )
                    lastDrivingSoc = tick.socPercent
                }
            }
        }
    }

    private fun accumulatePeriod(tick: TelemetryTick, parked: Boolean, deltaKm: Double) {
        if (parked) return
        if (deltaKm > 0.0) {
            periodDistanceKm += deltaKm
        }
        val soc = tick.socPercent ?: return
        val previous = lastDrivingSoc
        if (previous != null) {
            val drop = (previous - soc).toDouble()
            if (drop > 0.0) {
                periodSocUsed += drop
            }
        }
        lastDrivingSoc = soc
    }

    /**
     * Drives [BufferPoint.consumedSocPoints]: only ever grows, only while actually driving, and
     * only on a real SOC drop — a charge's rise (or regen's) never feeds it. [lastBufferDrivingSoc]
     * resets to null on [GearEvent.ConfirmedPark] so the first tick after a charge (or any parked
     * stretch) isn't compared against a stale pre-park reading.
     */
    private fun accumulateBufferConsumption(tick: TelemetryTick, parked: Boolean) {
        if (parked) return
        val soc = tick.socPercent ?: return
        val previous = lastBufferDrivingSoc
        if (previous != null) {
            val drop = (previous - soc).toDouble()
            if (drop > 0.0) {
                bufferSocConsumed += drop
            }
        }
        lastBufferDrivingSoc = soc
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
        val speed = tick.speedKmh?.takeIf { it.isFinite() && it >= 0f }
        val temp = tick.outsideTempC?.takeIf { it.isFinite() }
        trip = current.copy(
            lastDistanceKm = distanceKm,
            lastSocUsedPoints = socUsed,
            speedSumKmh = current.speedSumKmh + (speed?.toDouble() ?: 0.0),
            speedSamples = current.speedSamples + if (speed != null) 1 else 0,
            tempSumC = current.tempSumC + (temp?.toDouble() ?: 0.0),
            tempSamples = current.tempSamples + if (temp != null) 1 else 0,
            tempEndC = temp ?: current.tempEndC,
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
                speedKmh = tick.speedKmh?.takeIf { it.isFinite() && it >= 0f },
                outsideTempC = tick.outsideTempC?.takeIf { it.isFinite() },
                chargingLikely = charging,
                gap = gap,
                consumedSocPoints = bufferSocConsumed,
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
