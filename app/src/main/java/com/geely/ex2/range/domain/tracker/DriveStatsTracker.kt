package com.geely.ex2.range.domain.tracker

import com.geely.ex2.range.domain.model.DriveStatsSnapshot
import com.geely.ex2.range.domain.model.DriveStatsView
import com.geely.ex2.range.domain.model.RangeConstants

/**
 * Two records for the Stats screen:
 *  - max trip: leave P until confirmed P.
 *  - max charge cycle: km / SOC used / avg speed / avg outside temp between two charges
 *    (a cycle can span several trips if the car isn't charged after every one).
 * Mutates RAM only; caller persists [snapshot] when [dirty] after a park / charge edge.
 */
class DriveStatsTracker {
    private var maxTripKm: Double = 0.0

    private var maxChargeCycleKm: Double = 0.0
    private var maxChargeCycleSocUsedPercent: Double = 0.0
    private var maxChargeCycleAvgSpeedKmh: Double? = null
    private var maxChargeCycleAvgTempC: Double? = null

    private var openChargeCycleKm: Double = 0.0
    private var openChargeCycleSocUsedPercent: Double = 0.0
    private var openSpeedWeightedSum: Double = 0.0
    private var openSpeedWeightKm: Double = 0.0
    private var openTempWeightedSum: Double = 0.0
    private var openTempWeightKm: Double = 0.0

    private var chargingSession: Boolean = false
    private var chargeTrueCount: Int = 0

    var dirty: Boolean = false
        private set

    fun restore(snapshot: DriveStatsSnapshot) {
        maxTripKm = finiteKm(snapshot.maxTripKm)
        maxChargeCycleKm = finiteKm(snapshot.maxChargeCycleKm)
        maxChargeCycleSocUsedPercent = finitePositive(snapshot.maxChargeCycleSocUsedPercent)
        maxChargeCycleAvgSpeedKmh = snapshot.maxChargeCycleAvgSpeedKmh?.takeIf { it.isFinite() }
        maxChargeCycleAvgTempC = snapshot.maxChargeCycleAvgTempC?.takeIf { it.isFinite() }
        openChargeCycleKm = finiteKm(snapshot.openChargeCycleKm)
        openChargeCycleSocUsedPercent = finitePositive(snapshot.openChargeCycleSocUsedPercent)
        openSpeedWeightedSum = snapshot.openChargeCycleSpeedWeightedSum.takeIf { it.isFinite() } ?: 0.0
        openSpeedWeightKm = finiteKm(snapshot.openChargeCycleSpeedWeightKm)
        openTempWeightedSum = snapshot.openChargeCycleTempWeightedSum.takeIf { it.isFinite() } ?: 0.0
        openTempWeightKm = finiteKm(snapshot.openChargeCycleTempWeightKm)
        chargingSession = snapshot.chargingSession
        chargeTrueCount = if (snapshot.chargingSession) RangeConstants.CHARGE_CONFIRM_TICKS else 0
        dirty = false
    }

    fun snapshot(): DriveStatsSnapshot {
        return DriveStatsSnapshot(
            maxTripKm = maxTripKm,
            maxChargeCycleKm = maxChargeCycleKm,
            maxChargeCycleSocUsedPercent = maxChargeCycleSocUsedPercent,
            maxChargeCycleAvgSpeedKmh = maxChargeCycleAvgSpeedKmh,
            maxChargeCycleAvgTempC = maxChargeCycleAvgTempC,
            openChargeCycleKm = openChargeCycleKm,
            openChargeCycleSocUsedPercent = openChargeCycleSocUsedPercent,
            openChargeCycleSpeedWeightedSum = openSpeedWeightedSum,
            openChargeCycleSpeedWeightKm = openSpeedWeightKm,
            openChargeCycleTempWeightedSum = openTempWeightedSum,
            openChargeCycleTempWeightKm = openTempWeightKm,
            chargingSession = chargingSession,
        )
    }

    fun markClean() {
        dirty = false
    }

    /** Called once a trip ends (confirmed park) with that trip's own summary. */
    fun onParked(
        tripKm: Double,
        tripSocUsedPercent: Double,
        tripAvgSpeedKmh: Double?,
        tripAvgTempC: Double?,
    ) {
        val km = finiteKm(tripKm)
        if (km < RangeConstants.MIN_COUNTED_TRIP_KM) return
        if (km > maxTripKm) {
            maxTripKm = km
            dirty = true
        }
        if (chargingSession) return
        openChargeCycleKm += km
        openChargeCycleSocUsedPercent += finitePositive(tripSocUsedPercent)
        if (tripAvgSpeedKmh != null && tripAvgSpeedKmh.isFinite()) {
            openSpeedWeightedSum += tripAvgSpeedKmh * km
            openSpeedWeightKm += km
        }
        if (tripAvgTempC != null && tripAvgTempC.isFinite()) {
            openTempWeightedSum += tripAvgTempC * km
            openTempWeightKm += km
        }
        dirty = true
        recordCycleIfNewMax()
    }

    fun onCharging(charging: Boolean, parked: Boolean = true) {
        if (!parked) {
            chargeTrueCount = 0
            if (!chargingSession) return
            chargingSession = false
            openChargeCycleKm = 0.0
            openChargeCycleSocUsedPercent = 0.0
            openSpeedWeightedSum = 0.0
            openSpeedWeightKm = 0.0
            openTempWeightedSum = 0.0
            openTempWeightKm = 0.0
            dirty = true
            return
        }
        if (charging && chargeTrueCount < RangeConstants.CHARGE_CONFIRM_TICKS) {
            chargeTrueCount++
        }
        if (chargingSession) return
        if (chargeTrueCount < RangeConstants.CHARGE_CONFIRM_TICKS) return
        chargingSession = true
        recordCycleIfNewMax()
        dirty = true
    }

    fun displayed(): DriveStatsView {
        return DriveStatsView(
            maxTripKm = maxTripKm,
            maxChargeCycleKm = maxChargeCycleKm,
            maxChargeCycleSocUsedPercent = maxChargeCycleSocUsedPercent,
            maxChargeCycleAvgSpeedKmh = maxChargeCycleAvgSpeedKmh,
            maxChargeCycleAvgTempC = maxChargeCycleAvgTempC,
        )
    }

    private fun recordCycleIfNewMax() {
        if (openChargeCycleKm <= maxChargeCycleKm) return
        maxChargeCycleKm = openChargeCycleKm
        maxChargeCycleSocUsedPercent = openChargeCycleSocUsedPercent
        maxChargeCycleAvgSpeedKmh = if (openSpeedWeightKm > 0.0) openSpeedWeightedSum / openSpeedWeightKm else null
        maxChargeCycleAvgTempC = if (openTempWeightKm > 0.0) openTempWeightedSum / openTempWeightKm else null
    }

    private fun finiteKm(value: Double): Double {
        return if (value.isFinite() && value > 0.0) value else 0.0
    }

    private fun finitePositive(value: Double): Double {
        return if (value.isFinite() && value > 0.0) value else 0.0
    }
}
