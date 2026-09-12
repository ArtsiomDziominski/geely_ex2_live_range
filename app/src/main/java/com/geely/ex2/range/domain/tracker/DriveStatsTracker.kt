package com.geely.ex2.range.domain.tracker

import com.geely.ex2.range.domain.model.DriveStatsSnapshot
import com.geely.ex2.range.domain.model.DriveStatsView
import com.geely.ex2.range.domain.model.RangeConstants
import kotlin.math.max

/**
 * Max trip (leave P until confirmed P) and max km between charges.
 * Mutates RAM only; caller persists [snapshot] when [dirty] after park / charge edge.
 */
class DriveStatsTracker {
    private var maxTripKm: Double = 0.0
    private var maxChargeCycleKm: Double = 0.0
    private var openChargeCycleKm: Double = 0.0
    private var chargingSession: Boolean = false
    private var chargeTrueCount: Int = 0

    var dirty: Boolean = false
        private set

    fun restore(snapshot: DriveStatsSnapshot) {
        maxTripKm = finiteKm(snapshot.maxTripKm)
        maxChargeCycleKm = finiteKm(snapshot.maxChargeCycleKm)
        openChargeCycleKm = finiteKm(snapshot.openChargeCycleKm)
        chargingSession = snapshot.chargingSession
        chargeTrueCount = if (snapshot.chargingSession) RangeConstants.CHARGE_CONFIRM_TICKS else 0
        dirty = false
    }

    fun snapshot(): DriveStatsSnapshot {
        return DriveStatsSnapshot(
            maxTripKm = maxTripKm,
            maxChargeCycleKm = maxChargeCycleKm,
            openChargeCycleKm = openChargeCycleKm,
            chargingSession = chargingSession,
        )
    }

    fun markClean() {
        dirty = false
    }

    fun onParked(tripKm: Double) {
        val km = finiteKm(tripKm)
        if (km < RangeConstants.MIN_COUNTED_TRIP_KM) return
        if (km > maxTripKm) {
            maxTripKm = km
            dirty = true
        }
        if (chargingSession) return
        openChargeCycleKm += km
        dirty = true
        if (openChargeCycleKm > maxChargeCycleKm) {
            maxChargeCycleKm = openChargeCycleKm
        }
    }

    fun onCharging(charging: Boolean, parked: Boolean = true) {
        if (!parked) {
            chargeTrueCount = 0
            if (!chargingSession) return
            chargingSession = false
            openChargeCycleKm = 0.0
            dirty = true
            return
        }
        if (charging && chargeTrueCount < RangeConstants.CHARGE_CONFIRM_TICKS) {
            chargeTrueCount++
        }
        if (chargingSession) return
        if (chargeTrueCount < RangeConstants.CHARGE_CONFIRM_TICKS) return
        chargingSession = true
        if (openChargeCycleKm > maxChargeCycleKm) {
            maxChargeCycleKm = openChargeCycleKm
        }
        dirty = true
    }

    fun displayed(tripKm: Double, tripLive: Boolean): DriveStatsView {
        val km = finiteKm(tripKm)
        val liveAdd = if (tripLive) km else 0.0
        val cycle = if (chargingSession) {
            openChargeCycleKm
        } else {
            openChargeCycleKm + liveAdd
        }
        return DriveStatsView(
            maxTripKm = max(maxTripKm, if (tripLive) km else 0.0),
            maxChargeCycleKm = max(maxChargeCycleKm, cycle),
            currentTripKm = km,
            currentChargeCycleKm = cycle,
        )
    }

    private fun finiteKm(value: Double): Double {
        return if (value.isFinite() && value > 0.0) value else 0.0
    }
}
