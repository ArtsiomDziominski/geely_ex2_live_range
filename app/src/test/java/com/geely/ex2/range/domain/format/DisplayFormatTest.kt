package com.geely.ex2.range.domain.format

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayFormatTest {
    @Test
    fun socKeepsFractionAndDoesNotForceInt() {
        assertEquals("64.3%", DisplayFormat.socPercent(64.3f))
        assertEquals("64%", DisplayFormat.socPercent(64f))
        assertEquals("нет SOC", DisplayFormat.socPercent(null))
        assertEquals("SOC (↑0.2%)", DisplayFormat.socDeltaLabel(0.2f))
    }

    @Test
    fun pitchZeroHasNoSign() {
        assertEquals("0", DisplayFormat.pitchDegrees(0f))
        assertEquals("0", DisplayFormat.pitchDegrees(0.4f))
        assertEquals("0", DisplayFormat.pitchDegrees(-0.4f))
        assertEquals("+3", DisplayFormat.pitchDegrees(3.9f))
        assertEquals("-2", DisplayFormat.pitchDegrees(-2.9f))
    }
}
