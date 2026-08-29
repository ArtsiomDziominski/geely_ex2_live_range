package com.geely.ex2.range.domain.decode

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureDecoderTest {
    @Test
    fun flymeFormula() {
        assertEquals(8f, TemperatureDecoder.fromFlymeRaw(96), 0.01f)
        assertEquals(-10f, TemperatureDecoder.fromFlymeRaw(60), 0.01f)
    }

    @Test
    fun oemPreferredOverEnv() {
        assertEquals(8f, TemperatureDecoder.outsideC(96, 99f)!!, 0.01f)
    }

    @Test
    fun envFallback() {
        assertEquals(12.5f, TemperatureDecoder.outsideC(null, 12.5f)!!, 0.01f)
    }
}

class SpeedDecoderTest {
    @Test
    fun rawAbsWithoutPlusOne() {
        assertEquals(72f, SpeedDecoder.rawKmh(-72f)!!, 0.0f)
        assertEquals(10f, SpeedDecoder.rawKmh(10f)!!, 0.0f)
    }
}

class GearDecoderTest {
    @Test
    fun ex2Values() {
        assertEquals("P", GearDecoder.fromVhal(4)?.label)
        assertEquals("R", GearDecoder.fromVhal(2)?.label)
        assertEquals("N", GearDecoder.fromVhal(1)?.label)
        assertEquals("D", GearDecoder.fromVhal(8)?.label)
    }
}
