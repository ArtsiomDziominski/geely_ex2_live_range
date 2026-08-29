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
    private var lastSpeedKmh: Float? = null
    private var lastOdometerKm: Float? = null
    var odometerTrusted: Boolean = false
        private set

    fun restore(totalKm: Double, lastOdometerKm: Float? = null) {
        this.totalKm = totalKm.coerceAtLeast(0.0)
        this.lastOdometerKm = lastOdometerKm
        lastElapsedMs = null
        lastSpeedKmh = null
    }

    /**
     * @return kilometres added this tick (0 when parked, Acc Off, standstill, or gap).
     */
    fun onTick(
        elapsedMs: Long,
        speedKmh: Float?,
        odometerKm: Float?,
        parked: Boolean,
        accOff: Boolean,
    ): DistanceTick {
        val previousElapsed = lastElapsedMs
        val dtMs = if (previousElapsed != null) (elapsedMs - previousElapsed).coerceAtLeast(0L) else 0L
        val gap = previousElapsed != null && dtMs >= RangeConstants.GAP_ELAPSED_MS

        if (parked || accOff || gap) {
            lastElapsedMs = elapsedMs
            lastSpeedKmh = speedKmh
            if (odometerKm != null) lastOdometerKm = odometerKm
            return DistanceTick(deltaKm = 0.0, gap = gap, usedOdometer = false)
        }

        val speed = speedKmh
        val standing = speed == null || speed < RangeConstants.STANDSTILL_KMH
        var delta = 0.0
        var usedOdometer = false

        val previousOdo = lastOdometerKm
        if (odometerKm != null && previousOdo != null) {
            val odoDelta = (odometerKm - previousOdo).toDouble()
            val maxDelta = maxOdoDeltaKm(dtMs, speed)
            if (odoDelta > 0.0 && odoDelta <= maxDelta) {
                delta = odoDelta
                usedOdometer = true
                odometerTrusted = true
            }
        }

        if (delta == 0.0 && speed != null && speed >= RangeConstants.STANDSTILL_KMH && previousElapsed != null && dtMs > 0L) {
            val dtHours = dtMs / 3_600_000.0
            val previousSpeed = lastSpeedKmh ?: speed
            delta = ((previousSpeed + speed) / 2.0) * dtHours
        }

        if (standing) {
            delta = 0.0
            usedOdometer = false
        }

        lastElapsedMs = elapsedMs
        lastSpeedKmh = speedKmh
        if (odometerKm != null) lastOdometerKm = odometerKm
        if (delta > 0.0) totalKm += delta
        return DistanceTick(deltaKm = delta, gap = false, usedOdometer = usedOdometer)
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
