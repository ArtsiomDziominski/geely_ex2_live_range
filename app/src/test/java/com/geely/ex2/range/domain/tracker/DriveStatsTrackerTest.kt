package com.geely.ex2.range.domain.tracker

import com.geely.ex2.range.domain.model.DriveStatsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveStatsTrackerTest {
    @Test
    fun parkAccumulatesOpenCycleAndUpdatesMaxImmediately() {
        val tracker = DriveStatsTracker()
        tracker.onParked(10.0, tripSocUsedPercent = 5.0, tripAvgSpeedKmh = 60.0, tripAvgTempC = 10.0)
        tracker.onParked(50.0, tripSocUsedPercent = 20.0, tripAvgSpeedKmh = 80.0, tripAvgTempC = 20.0)
        val snap = tracker.snapshot()
        assertEquals(60.0, snap.openChargeCycleKm, 1e-9)
        assertEquals(60.0, snap.maxChargeCycleKm, 1e-9)
        assertEquals(25.0, snap.maxChargeCycleSocUsedPercent, 1e-9)
        // weighted by trip km: (60*10 + 80*50) / 60 = 76.666...
        assertEquals(76.67, snap.maxChargeCycleAvgSpeedKmh!!, 0.01)
        assertEquals(18.33, snap.maxChargeCycleAvgTempC!!, 0.01)
        assertTrue(tracker.dirty)
    }

    @Test
    fun tinyTripIsIgnored() {
        val tracker = DriveStatsTracker()
        tracker.onParked(0.04, tripSocUsedPercent = 1.0, tripAvgSpeedKmh = 30.0, tripAvgTempC = 15.0)
        assertEquals(0.0, tracker.snapshot().openChargeCycleKm, 1e-9)
        assertFalse(tracker.dirty)
    }

    @Test
    fun missingSpeedOrTempIsExcludedFromAverage() {
        val tracker = DriveStatsTracker()
        tracker.onParked(10.0, tripSocUsedPercent = 5.0, tripAvgSpeedKmh = null, tripAvgTempC = null)
        val snap = tracker.snapshot()
        assertNull(snap.maxChargeCycleAvgSpeedKmh)
        assertNull(snap.maxChargeCycleAvgTempC)
    }

    @Test
    fun chargingFinalizesCycleThenResetOnUnplug() {
        val tracker = DriveStatsTracker()
        tracker.onParked(20.0, tripSocUsedPercent = 10.0, tripAvgSpeedKmh = 50.0, tripAvgTempC = 5.0)
        tracker.onParked(30.0, tripSocUsedPercent = 15.0, tripAvgSpeedKmh = 70.0, tripAvgTempC = 15.0)
        repeat(2) { tracker.onCharging(charging = true, parked = true) }
        assertFalse(tracker.snapshot().chargingSession)
        tracker.onCharging(charging = true, parked = true)
        val confirmed = tracker.snapshot()
        assertEquals(50.0, confirmed.maxChargeCycleKm, 1e-9)
        assertEquals(25.0, confirmed.maxChargeCycleSocUsedPercent, 1e-9)
        assertEquals(62.0, confirmed.maxChargeCycleAvgSpeedKmh!!, 0.01)
        assertEquals(11.0, confirmed.maxChargeCycleAvgTempC!!, 0.01)
        assertTrue(confirmed.chargingSession)
        tracker.markClean()
        tracker.onCharging(charging = false, parked = true)
        assertFalse(tracker.dirty)
        assertTrue(tracker.snapshot().chargingSession)
        tracker.onCharging(charging = false, parked = false)
        assertEquals(0.0, tracker.snapshot().openChargeCycleKm, 1e-9)
        assertFalse(tracker.snapshot().chargingSession)
        tracker.onParked(10.0, tripSocUsedPercent = 4.0, tripAvgSpeedKmh = 40.0, tripAvgTempC = 8.0)
        assertEquals(50.0, tracker.snapshot().maxChargeCycleKm, 1e-9)
        assertEquals(10.0, tracker.snapshot().openChargeCycleKm, 1e-9)
    }

    @Test
    fun displayedReturnsPersistedRecordsOnly() {
        val tracker = DriveStatsTracker()
        tracker.restore(
            DriveStatsSnapshot(
                maxChargeCycleKm = 100.0,
                maxChargeCycleSocUsedPercent = 50.0,
                maxChargeCycleAvgSpeedKmh = 65.0,
                maxChargeCycleAvgTempC = 12.0,
                openChargeCycleKm = 80.0,
            ),
        )
        val view = tracker.displayed()
        assertEquals(100.0, view.maxChargeCycleKm, 1e-9)
        assertEquals(50.0, view.maxChargeCycleSocUsedPercent, 1e-9)
        assertEquals(65.0, view.maxChargeCycleAvgSpeedKmh!!, 1e-9)
        assertEquals(12.0, view.maxChargeCycleAvgTempC!!, 1e-9)
        assertFalse(tracker.dirty)
        assertEquals(80.0, tracker.snapshot().openChargeCycleKm, 1e-9)
    }

    @Test
    fun restoreDoesNotMarkDirty() {
        val tracker = DriveStatsTracker()
        tracker.restore(
            DriveStatsSnapshot(
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
