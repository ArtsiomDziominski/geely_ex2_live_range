package com.geely.ex2.range.domain.tracker

import com.geely.ex2.range.domain.model.DriveStatsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveStatsTrackerTest {
    @Test
    fun parkUpdatesMaxTripAndOpenCycle() {
        val tracker = DriveStatsTracker()
        tracker.onParked(10.0)
        tracker.onParked(50.0)
        val snap = tracker.snapshot()
        assertEquals(50.0, snap.maxTripKm, 1e-9)
        assertEquals(60.0, snap.openChargeCycleKm, 1e-9)
        assertEquals(60.0, snap.maxChargeCycleKm, 1e-9)
        assertTrue(tracker.dirty)
    }

    @Test
    fun tinyTripIsIgnored() {
        val tracker = DriveStatsTracker()
        tracker.onParked(0.04)
        assertEquals(0.0, tracker.snapshot().maxTripKm, 1e-9)
        assertFalse(tracker.dirty)
    }

    @Test
    fun chargingFinalizesCycleThenResetOnUnplug() {
        val tracker = DriveStatsTracker()
        tracker.onParked(20.0)
        tracker.onParked(30.0)
        repeat(2) { tracker.onCharging(charging = true, parked = true) }
        assertFalse(tracker.snapshot().chargingSession)
        tracker.onCharging(charging = true, parked = true)
        assertEquals(50.0, tracker.snapshot().maxChargeCycleKm, 1e-9)
        assertTrue(tracker.snapshot().chargingSession)
        tracker.markClean()
        tracker.onCharging(charging = false, parked = true)
        assertFalse(tracker.dirty)
        assertTrue(tracker.snapshot().chargingSession)
        tracker.onCharging(charging = false, parked = false)
        assertEquals(0.0, tracker.snapshot().openChargeCycleKm, 1e-9)
        assertFalse(tracker.snapshot().chargingSession)
        tracker.onParked(10.0)
        assertEquals(50.0, tracker.snapshot().maxChargeCycleKm, 1e-9)
        assertEquals(10.0, tracker.snapshot().openChargeCycleKm, 1e-9)
    }

    @Test
    fun liveDisplayAddsOpenTripWithoutWriting() {
        val tracker = DriveStatsTracker()
        tracker.restore(
            DriveStatsSnapshot(
                maxTripKm = 40.0,
                maxChargeCycleKm = 100.0,
                openChargeCycleKm = 80.0,
            ),
        )
        val view = tracker.displayed(tripKm = 25.0, tripLive = true)
        assertEquals(40.0, view.maxTripKm, 1e-9)
        assertEquals(105.0, view.maxChargeCycleKm, 1e-9)
        assertEquals(25.0, view.currentTripKm, 1e-9)
        assertEquals(105.0, view.currentChargeCycleKm, 1e-9)
        assertFalse(tracker.dirty)
        assertEquals(80.0, tracker.snapshot().openChargeCycleKm, 1e-9)
    }

    @Test
    fun parkedDisplayDoesNotDoubleCountLastTrip() {
        val tracker = DriveStatsTracker()
        tracker.onParked(12.0)
        tracker.markClean()
        val view = tracker.displayed(tripKm = 12.0, tripLive = false)
        assertEquals(12.0, view.maxTripKm, 1e-9)
        assertEquals(12.0, view.maxChargeCycleKm, 1e-9)
        assertEquals(12.0, view.currentTripKm, 1e-9)
        assertEquals(12.0, view.currentChargeCycleKm, 1e-9)
        assertFalse(tracker.dirty)
    }

    @Test
    fun restoreDoesNotMarkDirty() {
        val tracker = DriveStatsTracker()
        tracker.restore(
            DriveStatsSnapshot(
                maxTripKm = 87.4,
                maxChargeCycleKm = 318.2,
                openChargeCycleKm = 142.0,
                chargingSession = true,
            ),
        )
        assertFalse(tracker.dirty)
        tracker.onCharging(charging = true, parked = true)
        assertFalse(tracker.dirty)
    }
}
