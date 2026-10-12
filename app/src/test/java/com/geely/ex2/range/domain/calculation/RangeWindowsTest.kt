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
    fun unfilledWindowEstimatesFromCoveredDistance() {
        val buffer = buffer(0.0 to 80f, 5.0 to 75f)
        val five = RangeWindows.estimate(buffer, 5.0, socNow = 75f)
        val fifteen = RangeWindows.estimate(buffer, 15.0, socNow = 75f)
        assertEquals(WindowStatus.READY, five.status)
        assertEquals(75.0, five.rangeTo0Km!!, 0.2)
        // 15 км ещё не набрано — тот же темп по уже пройденным 5 км.
        assertEquals(WindowStatus.READY, fifteen.status)
        assertEquals(75.0, fifteen.rangeTo0Km!!, 0.2)
        assertEquals(55.0, fifteen.rangeToReserveKm!!, 0.2)
        assertNull(fifteen.remainingToFillKm)
    }

    @Test
    fun firstEstimateNeedsMinimumDistance() {
        // На месте с климатом SOC падает без километров — по 300 м прогноз не строим.
        val short = RangeWindows.estimate(buffer(0.0 to 80f, 0.3 to 79f), 5.0, socNow = 79f)
        assertEquals(WindowStatus.NEED_MORE_KM, short.status)
        assertEquals(0.7, short.remainingToFillKm!!, 1e-6)
        assertNull(short.rangeTo0Km)

        val enough = RangeWindows.estimate(buffer(0.0 to 80f, 1.0 to 79f), 5.0, socNow = 79f)
        assertEquals(WindowStatus.READY, enough.status)
        assertEquals(79.0, enough.rangeTo0Km!!, 0.2)
    }

    @Test
    fun emptyBufferWaitsForFirstEstimate() {
        val window = RangeWindows.estimate(SampleRingBuffer(), 5.0, socNow = 80f)
        assertEquals(WindowStatus.NEED_MORE_KM, window.status)
        assertEquals(1.0, window.remainingToFillKm!!, 0.0)
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

    /** Точка с пометкой зарядки внутри окна его не глушит: расход считается по consumedSocPoints. */
    @Test
    fun chargeFlaggedPointDoesNotInvalidateWindow() {
        val buffer = SampleRingBuffer()
        buffer.add(point(0.0, 50f, consumedSocPoints = 0.0))
        buffer.add(point(3.0, 47f, charging = true, consumedSocPoints = 3.0))
        buffer.add(point(6.0, 44f, consumedSocPoints = 6.0))
        val window = RangeWindows.estimate(buffer, 5.0, socNow = 44f)
        assertEquals(WindowStatus.READY, window.status)
        assertEquals(5.0, window.deltaSocPoints!!, 0.05)
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
        assertEquals(8f, window.chart.first().outsideTempC)
        assertEquals(54f, window.chart.first().speedKmh)
    }

    /** A charge's SOC jump inside the window must not cancel out the real driving consumption. */
    @Test
    fun deltaIgnoresChargeJumpWithinWindow() {
        // 0→5 km driving (80→70), then a charge at km 5 (70→95, parked so km doesn't move),
        // then 5→8 km driving again (95→92).
        val buffer = buffer(0.0 to 80f, 5.0 to 70f, 5.0 to 95f, 8.0 to 92f)
        val window = RangeWindows.estimate(buffer, 3.0, socNow = 92f)
        assertEquals(WindowStatus.READY, window.status)
        assertEquals(3.0, window.deltaSocPoints!!, 0.05)
    }

    private fun buffer(vararg kmSoc: Pair<Double, Float>): SampleRingBuffer {
        val buffer = SampleRingBuffer()
        var consumed = 0.0
        var previousSoc: Float? = null
        kmSoc.forEach { (km, soc) ->
            val previous = previousSoc
            if (previous != null) {
                val drop = (previous - soc).toDouble()
                if (drop > 0.0) consumed += drop
            }
            previousSoc = soc
            buffer.add(point(km, soc, consumedSocPoints = consumed))
        }
        return buffer
    }

    private fun point(
        km: Double,
        soc: Float,
        charging: Boolean = false,
        consumedSocPoints: Double = 0.0,
    ): BufferPoint {
        return BufferPoint(
            elapsedRealtimeMs = (km * 100_000).toLong(),
            wallClockMs = 0L,
            cumulativeKm = km,
            socPercent = soc,
            speedKmh = 54f,
            outsideTempC = 8f,
            chargingLikely = charging,
            gap = false,
            consumedSocPoints = consumedSocPoints,
        )
    }
}
