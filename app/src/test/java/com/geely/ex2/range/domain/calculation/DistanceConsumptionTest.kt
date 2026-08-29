package com.geely.ex2.range.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DistanceAccumulatorTest {
    @Test
    fun trapezoidUsesRawSpeedWithoutPlusOne() {
        val acc = DistanceAccumulator()
        acc.onTick(0, speedKmh = 72f, odometerKm = null, parked = false, accOff = false)
        val tick = acc.onTick(1_000, speedKmh = 72f, odometerKm = null, parked = false, accOff = false)
        assertEquals(72.0 / 3600.0, tick.deltaKm, 1e-6)
        assertFalse(tick.usedOdometer)
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
    fun odometerPreferredWhenSane() {
        val acc = DistanceAccumulator()
        acc.onTick(0, 36f, 10f, parked = false, accOff = false)
        val tick = acc.onTick(1_000, 36f, 10.01f, parked = false, accOff = false)
        assertEquals(0.01, tick.deltaKm, 1e-6)
        assertEquals(true, tick.usedOdometer)
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
