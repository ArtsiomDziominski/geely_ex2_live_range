package com.geely.ex2.range.domain.calculation

import com.geely.ex2.range.domain.model.RangeConstants

object Consumption {
    fun pctPer100(distanceKm: Double, socUsedPoints: Double): Double? {
        if (!distanceKm.isFinite() || !socUsedPoints.isFinite()) return null
        if (distanceKm <= 0.0 || socUsedPoints < 0.0) return null
        return socUsedPoints / distanceKm * 100.0
    }

    fun energyKwh(socUsedPoints: Double, capacityKwh: Double?): Double? {
        if (capacityKwh == null || !capacityKwh.isFinite() || capacityKwh <= 0.0) return null
        if (!socUsedPoints.isFinite() || socUsedPoints < 0.0) return null
        return capacityKwh * socUsedPoints / 100.0
    }

    fun kWhPer100(distanceKm: Double, socUsedPoints: Double, capacityKwh: Double?): Double? {
        val energy = energyKwh(socUsedPoints, capacityKwh) ?: return null
        if (!distanceKm.isFinite() || distanceKm <= 0.0) return null
        return energy / distanceKm * 100.0
    }
}

class DistanceAccumulator {
    var totalKm: Double = 0.0
        private set

    private var lastElapsedMs: Long? = null
    private var lastOdometerKm: Float? = null
    private var lastOdometerElapsedMs: Long? = null
    var odometerTrusted: Boolean = false
        private set

    fun restore(totalKm: Double, lastOdometerKm: Float? = null) {
        this.totalKm = totalKm.coerceAtLeast(0.0)
        this.lastOdometerKm = lastOdometerKm
        lastElapsedMs = null
        lastOdometerElapsedMs = null
    }

    /**
     * @return kilometres added this tick from odometer only (0 when parked, Acc Off, or gap).
     */
    fun onTick(
        elapsedMs: Long,
        speedKmh: Float?,
        odometerKm: Float?,
        parked: Boolean,
        accOff: Boolean,
    ): DistanceTick {
        val previousElapsed = lastElapsedMs
        val pollDtMs = if (previousElapsed != null) (elapsedMs - previousElapsed).coerceAtLeast(0L) else 0L
        val gap = previousElapsed != null && pollDtMs >= RangeConstants.GAP_ELAPSED_MS

        lastElapsedMs = elapsedMs

        if (parked || accOff || gap) {
            syncOdometer(odometerKm, elapsedMs)
            return DistanceTick(deltaKm = 0.0, gap = gap, usedOdometer = false)
        }

        if (odometerKm == null) {
            return DistanceTick(deltaKm = 0.0, gap = false, usedOdometer = false)
        }

        val previousOdo = lastOdometerKm
        val odoDtMs = lastOdometerElapsedMs?.let { (elapsedMs - it).coerceAtLeast(0L) } ?: 0L
        lastOdometerKm = odometerKm
        lastOdometerElapsedMs = elapsedMs
        if (previousOdo == null) {
            return DistanceTick(deltaKm = 0.0, gap = false, usedOdometer = false)
        }

        val odoDelta = (odometerKm - previousOdo).toDouble()
        if (odoDelta <= 0.0) {
            return DistanceTick(deltaKm = 0.0, gap = false, usedOdometer = false)
        }

        val maxDelta = maxOdoDeltaKm(odoDtMs, speedKmh)
        if (odoDelta > maxDelta) {
            return DistanceTick(deltaKm = 0.0, gap = false, usedOdometer = false)
        }

        odometerTrusted = true
        totalKm += odoDelta
        return DistanceTick(deltaKm = odoDelta, gap = false, usedOdometer = true)
    }

    private fun syncOdometer(odometerKm: Float?, elapsedMs: Long) {
        if (odometerKm == null) return
        lastOdometerKm = odometerKm
        lastOdometerElapsedMs = elapsedMs
    }

    private fun maxOdoDeltaKm(dtMs: Long, speedKmh: Float?): Double {
        val dtHours = (dtMs.coerceAtLeast(250L)) / 3_600_000.0
        val speed = (speedKmh ?: 160f).coerceAtLeast(30f)
        return (speed * 2.0 * dtHours).coerceAtLeast(0.05)
    }
}

data class DistanceTick(
    val deltaKm: Double,
    val gap: Boolean,
    val usedOdometer: Boolean,
)
