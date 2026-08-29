package com.geely.ex2.range.domain.format

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayFormatTest {
    @Test
    fun socKeepsFractionAndDoesNotForceInt() {
        assertEquals("64.3%", DisplayFormat.socPercent(64.3f))
        assertEquals("64%", DisplayFormat.socPercent(64f))
        assertEquals("нет SOC", DisplayFormat.socPercent(null))
    }
}
