package com.geely.ex2.range.domain.tracker

import com.geely.ex2.range.domain.model.Gear
import com.geely.ex2.range.domain.model.RangeConstants

sealed class GearEvent {
    data object ConfirmedPark : GearEvent()
    data object LeftPark : GearEvent()
    data object StartedDriving : GearEvent()
}

class GearMachine(
    private val debounceMs: Long = RangeConstants.GEAR_DEBOUNCE_MS,
    private val stableParkMs: Long = RangeConstants.STABLE_PARK_MS,
) {
    var stableGear: Gear? = null
        private set

    var parkConfirmed: Boolean = false
        private set

    var periodSavedThisPark: Boolean = false
        private set

    private var pendingGear: Gear? = null
    private var pendingSinceMs: Long = 0L
    private var parkSinceMs: Long? = null

    val displayedGear: Gear?
        get() = stableGear ?: pendingGear

    fun markPeriodSaved() {
        periodSavedThisPark = true
    }

    fun shouldPersistPeriod(): Boolean = parkConfirmed && !periodSavedThisPark

    fun restore(parked: Boolean, driving: Boolean) {
        if (parked) {
            parkConfirmed = true
            stableGear = Gear.PARK
            pendingGear = Gear.PARK
        } else if (driving) {
            parkConfirmed = false
            stableGear = Gear.DRIVE
            pendingGear = Gear.DRIVE
        }
    }

    fun tick(nowMs: Long, rawGear: Gear?): List<GearEvent> {
        val events = mutableListOf<GearEvent>()
        val previousStable = stableGear

        if (rawGear != null) {
            if (rawGear != pendingGear) {
                pendingGear = rawGear
                pendingSinceMs = nowMs
            }
            if (nowMs - pendingSinceMs >= debounceMs && stableGear != pendingGear) {
                stableGear = pendingGear
            }
        }

        val gear = stableGear
        if (gear == Gear.PARK) {
            if (parkSinceMs == null) parkSinceMs = nowMs
            val parkedFor = nowMs - (parkSinceMs ?: nowMs)
            if (!parkConfirmed && parkedFor >= stableParkMs) {
                parkConfirmed = true
                events += GearEvent.ConfirmedPark
            }
        } else if (gear != null) {
            parkSinceMs = null
            if (parkConfirmed) {
                parkConfirmed = false
                periodSavedThisPark = false
                events += GearEvent.LeftPark
            } else if (previousStable == null) {
                events += GearEvent.StartedDriving
            }
        }
        return events
    }
}
