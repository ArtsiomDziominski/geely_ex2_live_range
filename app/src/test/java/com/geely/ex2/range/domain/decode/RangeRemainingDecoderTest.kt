package com.geely.ex2.range.domain.decode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RangeRemainingDecoderTest {
    @Test
    fun valueAlreadyPlausibleAsKmIsUsedAsIs() {
        // A real reading of "300 km left" must not be shrunk 1000x just because AOSP specs metres.
        assertEquals(300f, RangeRemainingDecoder.decodeKm(300f)!!, 0.001f)
    }

    @Test
    fun smallRemainingRangeIsUsedAsIs() {
        // Near-empty battery: 15 km left must not become 0.015 km.
        assertEquals(15f, RangeRemainingDecoder.decodeKm(15f)!!, 0.001f)
    }

    @Test
    fun valueTooLargeForKmIsTreatedAsMetres() {
        // 300 km truly encoded in metres, per the AOSP spec.
        assertEquals(300f, RangeRemainingDecoder.decodeKm(300_000f)!!, 0.001f)
    }

    @Test
    fun implausibleEitherWayReturnsNull() {
        assertNull(RangeRemainingDecoder.decodeKm(5_000_000f))
    }

    @Test
    fun negativeOrNonFiniteIsRejected() {
        assertNull(RangeRemainingDecoder.decodeKm(-1f))
        assertNull(RangeRemainingDecoder.decodeKm(Float.NaN))
        assertNull(RangeRemainingDecoder.decodeKm(null))
    }

    @Test
    fun zeroIsValidRange() {
        assertEquals(0f, RangeRemainingDecoder.decodeKm(0f)!!, 0.001f)
    }
}
