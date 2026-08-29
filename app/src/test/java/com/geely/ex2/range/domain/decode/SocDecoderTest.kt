package com.geely.ex2.range.domain.decode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SocDecoderTest {
    @Test
    fun oemPercentWinsUnrounded() {
        val value = SocDecoder.decodePercent(
            oemPercent = 64.3f,
            batteryLevel = 32000f,
            currentCapacityWh = 50000f,
            nominalCapacityWh = 51000f,
        )
        assertEquals(64.3f, value!!, 0.0001f)
    }

    @Test
    fun fallbackLevelNotTreatedAsPercentWhen0to100() {
        assertNull(
            SocDecoder.decodePercent(
                oemPercent = null,
                batteryLevel = 64f,
                currentCapacityWh = 50000f,
                nominalCapacityWh = 51000f,
            ),
        )
    }

    @Test
    fun fallbackWhDividedByCapacity() {
        val value = SocDecoder.decodePercent(
            oemPercent = null,
            batteryLevel = 25000f,
            currentCapacityWh = 50000f,
            nominalCapacityWh = null,
        )
        assertEquals(50f, value!!, 0.01f)
    }
}
