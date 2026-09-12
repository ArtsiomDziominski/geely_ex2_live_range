package com.geely.ex2.range.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DistanceAccumulatorTest {
    @Test
    fun noOdometerNoDistance() {
        val acc = DistanceAccumulator()
        acc.onTick(0, speedKmh = 72f, odometerKm = null, parked = false, accOff = false)
        val tick = acc.onTick(1_000, speedKmh = 72f, odometerKm = null, parked = false, accOff = false)
        assertEquals(0.0, tick.deltaKm, 0.0)
        assertFalse(tick.usedOdometer)
        assertEquals(0.0, acc.totalKm, 0.0)
    }

    @Test
    fun parkedDoesNotGrow() {
        val acc = DistanceAccumulator()
        acc.onTick(0, 72f, null, parked = true, accOff = false)
        val tick = acc.onTick(3_600_000, 72f, null, parked = true, accOff = false)
        assertEquals(0.0, tick.deltaKm, 0.0)
        assertEquals(0.0, acc.totalKm, 0.0)
    }

    @Test
    fun odometerUnchangedAddsNothing() {
        val acc = DistanceAccumulator()
        acc.onTick(0, 36f, 10f, parked = false, accOff = false)
        val tick = acc.onTick(1_000, 36f, 10f, parked = false, accOff = false)
        assertEquals(0.0, tick.deltaKm, 0.0)
        assertEquals(0.0, acc.totalKm, 0.0)
    }

    @Test
    fun odometerPreferredWhenSane() {
        val acc = DistanceAccumulator()
        acc.onTick(0, 36f, 10f, parked = false, accOff = false)
        val tick = acc.onTick(1_000, 36f, 10.01f, parked = false, accOff = false)
        assertEquals(0.01, tick.deltaKm, 1e-6)
        assertEquals(true, tick.usedOdometer)
    }

    @Test
    fun missingOdometerDoesNotResetBaseline() {
        val acc = DistanceAccumulator()
        acc.onTick(0, 72f, 10f, parked = false, accOff = false)
        acc.onTick(1_000, 72f, null, parked = false, accOff = false)
        acc.onTick(2_000, 72f, null, parked = false, accOff = false)
        val tick = acc.onTick(10_000, 72f, 10.18f, parked = false, accOff = false)
        assertEquals(0.18, tick.deltaKm, 1e-6)
        assertEquals(true, tick.usedOdometer)
        assertEquals(0.18, acc.totalKm, 1e-6)
    }

    @Test
    fun implausibleOdometerJumpIsIgnored() {
        val acc = DistanceAccumulator()
        acc.onTick(0, 36f, 10f, parked = false, accOff = false)
        val tick = acc.onTick(1_000, 36f, 12f, parked = false, accOff = false)
        assertEquals(0.0, tick.deltaKm, 0.0)
        assertFalse(tick.usedOdometer)
        assertEquals(0.0, acc.totalKm, 0.0)
    }

    @Test
    fun cachedOdometerJumpAfterTenSecondsIsAccepted() {
        val acc = DistanceAccumulator()
        acc.onTick(0, 50f, 100f, parked = false, accOff = false)
        for (second in 1..9) {
            val tick = acc.onTick(second * 1_000L, 50f, 100f, parked = false, accOff = false)
            assertEquals(0.0, tick.deltaKm, 0.0)
        }
        // ~0.14 km in 10 s at 50 km/h — matches VHAL cache poll every 10 s
        val tick = acc.onTick(10_000, 50f, 100.14f, parked = false, accOff = false)
        assertEquals(0.14, tick.deltaKm, 1e-6)
        assertEquals(true, tick.usedOdometer)
        assertEquals(0.14, acc.totalKm, 1e-6)
    }
}

class ConsumptionTest {
    @Test
    fun percentPointsNotPercentOfStart() {
        assertEquals(50.0, Consumption.pctPer100(distanceKm = 20.0, socUsedPoints = 10.0)!!, 0.01)
        assertEquals(12.5, Consumption.kWhPer100(20.0, 10.0, 25.0)!!, 0.01)
    }

    @Test
    fun noDistanceNoRate() {
        assertEquals(null, Consumption.pctPer100(0.0, 5.0))
    }
}
