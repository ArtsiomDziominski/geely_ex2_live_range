package com.geely.ex2.range.domain.engine

import com.geely.ex2.range.domain.model.EngineCheckpoint
import com.geely.ex2.range.domain.model.Gear
import com.geely.ex2.range.domain.model.PeriodSnapshot
import com.geely.ex2.range.domain.model.SettingsSnapshot
import com.geely.ex2.range.domain.model.TelemetryTick
import com.geely.ex2.range.domain.model.WindowStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RangeEngineTest {
    @Test
    fun periodPersistsAcrossRestoreAndResetLeavesBuffer() {
        val engine = RangeEngine()
        drive(engine, startMs = 0, seconds = 400, startSoc = 80f, kmh = 90f)
        park(engine, startMs = 400_000)
        val before = engine.checkpoint(500_000)
        assertTrue(before.period.distanceKm > 5.0)

        val restored = RangeEngine()
        restored.restore(before, SettingsSnapshot(49.0))
        restored.onTick(sample(510_000, 70f, 0f, Gear.PARK))
        val afterRestore = restored.checkpoint(510_000)
        assertEquals(before.period.distanceKm, afterRestore.period.distanceKm, 0.01)
        assertEquals(before.period.socUsedPoints, afterRestore.period.socUsedPoints, 0.05)

        val covered = before.buffer.last().cumulativeKm - before.buffer.first().cumulativeKm
        restored.resetPeriod(520_000)
        val afterReset = restored.checkpoint(520_000)
        assertEquals(0.0, afterReset.period.distanceKm, 0.0)
        val coveredAfter = afterReset.buffer.last().cumulativeKm - afterReset.buffer.first().cumulativeKm
        assertEquals(covered, coveredAfter, 0.05)
    }

    @Test
    fun reverseAfterParkDoesNotStartSecondTrip() {
        val engine = RangeEngine()
        park(engine, startMs = 0)
        engine.onTick(sample(3_000, 80f, 0f, Gear.REVERSE))
        val afterR = engine.onTick(sample(3_600, 80f, 2f, Gear.REVERSE))
        engine.onTick(sample(4_200, 80f, 6f, Gear.DRIVE))
        val afterD = engine.onTick(sample(4_800, 79.8f, 10f, Gear.DRIVE))
        assertTrue(afterR.trip.distanceKm >= 0.0)
        assertTrue(afterD.trip.distanceKm < 1.0)
        assertFalse(afterD.waitingForDrive)
    }

    @Test
    fun nextLeaveParkResetsTrip() {
        val engine = RangeEngine()
        park(engine, 0)
        drive(engine, startMs = 3_000, seconds = 120, startSoc = 80f, kmh = 72f)
        val first = engine.checkpoint(130_000).trip!!
        assertTrue(first.lastDistanceKm > 1.0)
        park(engine, startMs = 130_000)
        engine.onTick(sample(135_000, 78f, 0f, Gear.DRIVE))
        val second = engine.onTick(sample(136_000, 78f, 5f, Gear.DRIVE))
        assertTrue(second.trip.distanceKm < first.lastDistanceKm / 2)
    }

    @Test
    fun parkingClimateSocDropIsNotAddedToPeriod() {
        val engine = RangeEngine()
        park(engine, 0)
        engine.onTick(sample(3_000, 80f, 0f, Gear.DRIVE))
        drive(engine, startMs = 3_500, seconds = 40, startSoc = 80f, kmh = 90f)
        val afterDrive = engine.checkpoint(50_000).period.socUsedPoints
        park(engine, startMs = 50_000)
        engine.onTick(sample(55_000, 70f, 0f, Gear.PARK))
        engine.onTick(sample(80_000, 70f, 0f, Gear.PARK))
        val afterPark = engine.checkpoint(80_000).period.socUsedPoints
        assertEquals(afterDrive, afterPark, 0.05)
    }

    @Test
    fun standingInDriveStillCountsSocDrop() {
        val engine = RangeEngine()
        park(engine, 0)
        engine.onTick(sample(3_000, 80f, 0f, Gear.DRIVE, odometerKm = 10f))
        engine.onTick(sample(4_000, 80f, 0f, Gear.DRIVE, odometerKm = 10f))
        val afterStand = engine.onTick(sample(5_000, 79.5f, 0f, Gear.DRIVE, odometerKm = 10f))
        assertEquals(0.0, afterStand.period.distanceKm, 0.0)
        assertEquals(0.5, afterStand.period.socUsedPoints, 0.05)
    }

    @Test
    fun chargingDoesNotProduceInfiniteRange() {
        val engine = RangeEngine()
        drive(engine, startMs = 0, seconds = 300, startSoc = 50f, kmh = 72f)
        val charging = engine.onTick(sample(301_000, 60f, 0f, Gear.PARK, charging = true))
        assertTrue(charging.windows.all { it.status == WindowStatus.CHARGING || it.rangeTo0Km == null })
        charging.windows.forEach { window ->
            assertTrue(window.rangeTo0Km == null || window.rangeTo0Km!!.isFinite())
        }
    }

    @Test
    fun socStaysFractionalInBuffer() {
        val engine = RangeEngine()
        engine.onTick(sample(0, 64.3f, 36f, Gear.DRIVE))
        engine.onTick(sample(1_000, 64.3f, 36f, Gear.DRIVE))
        val view = engine.onTick(sample(2_000, 64.3f, 36f, Gear.DRIVE))
        assertEquals(64.3f, view.socPercent!!, 0.0f)
        val point = engine.checkpoint(2_000).buffer.last()
        assertEquals(64.3f, point.socPercent, 0.0f)
    }

    @Test
    fun chargeSessionResetsWindowBufferOnLeavePark() {
        val engine = RangeEngine()
        drive(engine, startMs = 0, seconds = 400, startSoc = 80f, kmh = 90f)
        park(engine, startMs = 400_000)

        var t = 405_000L
        var soc = 60f
        repeat(4) {
            engine.onTick(sample(t, soc, 0f, Gear.PARK, charging = true))
            soc += 5f
            t += 1_000L
        }

        engine.onTick(sample(t, soc, 0f, Gear.DRIVE, odometerKm = 10f))
        t += 1_000L
        val afterDrive = engine.onTick(sample(t, soc - 0.2f, 20f, Gear.DRIVE, odometerKm = 10.05f))

        assertTrue(afterDrive.windows.all { it.status == WindowStatus.NEED_MORE_KM })
        afterDrive.windows.forEach { window ->
            assertEquals(window.windowKm, window.remainingToFillKm!!, 0.05)
        }
    }

    private fun drive(
        engine: RangeEngine,
        startMs: Long,
        seconds: Int,
        startSoc: Float,
        kmh: Float,
    ) {
        var soc = startSoc
        val kmPerSecond = kmh / 3600f
        val socPerKm = 1f
        repeat(seconds + 1) { index ->
            val t = startMs + index * 1_000L
            engine.onTick(sample(t, soc, kmh, Gear.DRIVE, odometerKm = kmPerSecond * index))
            soc -= socPerKm * kmPerSecond
        }
    }

    private fun park(engine: RangeEngine, startMs: Long) {
        engine.onTick(sample(startMs, 80f, 0f, Gear.PARK))
        engine.onTick(sample(startMs + 500, 80f, 0f, Gear.PARK))
        engine.onTick(sample(startMs + 2_600, 80f, 0f, Gear.PARK))
    }

    private fun sample(
        tMs: Long,
        soc: Float,
        speed: Float,
        gear: Gear,
        odometerKm: Float? = null,
        charging: Boolean = false,
    ): TelemetryTick {
        return TelemetryTick(
            elapsedRealtimeMs = tMs,
            wallClockMs = tMs,
            socPercent = soc,
            speedKmh = speed,
            odometerKm = odometerKm,
            gear = gear,
            outsideTempC = 8f,
            pepsPowerMode = 1,
            currentCapacityWh = null,
            nominalCapacityWh = 51_000f,
            chargingLikelyHint = charging,
        )
    }
}
