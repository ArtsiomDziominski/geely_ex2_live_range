package com.geely.ex2.range.domain.calculation

import com.geely.ex2.range.domain.buffer.SampleRingBuffer
import com.geely.ex2.range.domain.model.BufferPoint
import com.geely.ex2.range.domain.model.WindowStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RangeWindowsTest {
    @Test
    fun windowFiveReadyAtFiveKmFifteenStillFilling() {
        val buffer = buffer(0.0 to 80f, 5.0 to 75f)
        val five = RangeWindows.estimate(buffer, 5.0, socNow = 75f)
        val fifteen = RangeWindows.estimate(buffer, 15.0, socNow = 75f)
        assertEquals(WindowStatus.READY, five.status)
        assertEquals(75.0, five.rangeTo0Km!!, 0.2)
        assertEquals(WindowStatus.NEED_MORE_KM, fifteen.status)
        assertTrue(fifteen.remainingToFillKm!! > 9.0)
    }

    @Test
    fun interpolatesSocOnWindowBoundary() {
        val buffer = buffer(0.0 to 80f, 10.0 to 70f)
        val window = RangeWindows.estimate(buffer, 5.0, socNow = 70f)
        assertEquals(WindowStatus.READY, window.status)
        assertEquals(5.0, window.deltaSocPoints!!, 0.05)
        assertEquals(70.0, window.rangeTo0Km!!, 0.2)
    }

    @Test
    fun unchangedSocHasNoForecast() {
        val buffer = buffer(0.0 to 70f, 8.0 to 70f)
        val window = RangeWindows.estimate(buffer, 5.0, socNow = 70f)
        assertEquals(WindowStatus.SOC_UNCHANGED, window.status)
        assertNull(window.rangeTo0Km)
    }

    @Test
    fun chargingInvalidatesWindow() {
        val buffer = SampleRingBuffer()
        buffer.add(point(0.0, 50f, charging = false))
        buffer.add(point(6.0, 52f, charging = true))
        val window = RangeWindows.estimate(buffer, 5.0, socNow = 52f)
        assertEquals(WindowStatus.CHARGING, window.status)
    }

    @Test
    fun reserveIsZeroWhenSocAtOrBelow20() {
        val buffer = buffer(0.0 to 24f, 5.0 to 20f)
        val window = RangeWindows.estimate(buffer, 5.0, socNow = 20f)
        assertEquals(WindowStatus.READY, window.status)
        assertEquals(0.0, window.rangeToReserveKm!!, 0.0)
    }

    @Test
    fun readyWindowIncludesChartSamples() {
        val buffer = buffer(0.0 to 80f, 2.5 to 77f, 5.0 to 75f)
        val window = RangeWindows.estimate(buffer, 5.0, socNow = 75f)
        assertEquals(WindowStatus.READY, window.status)
        assertTrue(window.chart.size >= 2)
        assertEquals(0f, window.chart.first().km, 0.05f)
        assertEquals(5f, window.chart.last().km, 0.05f)
    }

    private fun buffer(vararg kmSoc: Pair<Double, Float>): SampleRingBuffer {
        val buffer = SampleRingBuffer()
        kmSoc.forEach { (km, soc) -> buffer.add(point(km, soc)) }
        return buffer
    }

    private fun point(km: Double, soc: Float, charging: Boolean = false): BufferPoint {
        return BufferPoint(
            elapsedRealtimeMs = (km * 100_000).toLong(),
            wallClockMs = 0L,
            cumulativeKm = km,
            socPercent = soc,
            chargingLikely = charging,
            gap = false,
        )
    }
}
