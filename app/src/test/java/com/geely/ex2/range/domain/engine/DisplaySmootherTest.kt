package com.geely.ex2.range.domain.engine

import com.geely.ex2.range.domain.model.ConsumptionRates
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplaySmootherTest {
    private val tauMs = 30_000L

    @Test
    fun firstValuePassesThroughUnchanged() {
        val smoother = DisplaySmoother(tauMs)
        val shown = smoother.apply(view(rangeKm = 200.0, tripPct = 40.0), elapsedMs = 0)
        assertEquals(200.0, shown.windows.single().rangeTo0Km!!, 0.0)
        assertEquals(200.0, shown.forecastAverageKm!!, 0.0)
        assertEquals(40.0, shown.trip.pctPer100!!, 0.0)
    }

    @Test
    fun stepGlidesInsteadOfJumping() {
        val smoother = DisplaySmoother(tauMs)
        smoother.apply(view(rangeKm = 200.0), elapsedMs = 0)

        val afterOneSecond = smoother.apply(view(rangeKm = 230.0), elapsedMs = 1_000)
        assertEquals(201.0, afterOneSecond.forecastAverageKm!!, 0.1)
        assertEquals(201.0, afterOneSecond.windows.single().rangeTo0Km!!, 0.1)

        // За одну постоянную времени проходится ~63 % ступеньки.
        val afterTau = smoother.apply(view(rangeKm = 230.0), elapsedMs = 1_000 + tauMs)
        assertEquals(219.3, afterTau.forecastAverageKm!!, 0.3)
    }

    @Test
    fun convergesAndSnapsToTarget() {
        val smoother = DisplaySmoother(tauMs)
        smoother.apply(view(rangeKm = 200.0), elapsedMs = 0)
        var shown = smoother.apply(view(rangeKm = 230.0), elapsedMs = 1_000)
        var previous = shown.forecastAverageKm!!
        for (second in 2..600) {
            shown = smoother.apply(view(rangeKm = 230.0), elapsedMs = second * 1_000L)
            assertTrue(shown.forecastAverageKm!! >= previous)
            previous = shown.forecastAverageKm!!
        }
        assertEquals(230.0, shown.forecastAverageKm!!, 0.0)
        assertEquals(230.0, shown.windows.single().rangeTo0Km!!, 0.0)
    }

    /** После зарядки запас в разы больше — он должен встать сразу, а не ползти от старого. */
    @Test
    fun valueAppearsAtOnceAfterBeingAbsent() {
        val smoother = DisplaySmoother(tauMs)
        smoother.apply(view(rangeKm = 80.0), elapsedMs = 0)
        val charging = smoother.apply(view(rangeKm = null, status = WindowStatus.CHARGING), elapsedMs = 1_000)
        assertNull(charging.forecastAverageKm)
        assertNull(charging.windows.single().rangeTo0Km)

        val afterCharge = smoother.apply(view(rangeKm = 310.0), elapsedMs = 2_000)
        assertEquals(310.0, afterCharge.forecastAverageKm!!, 0.0)
        assertEquals(310.0, afterCharge.windows.single().rangeTo0Km!!, 0.0)
    }

    @Test
    fun reserveScalesWithSmoothedRange() {
        val smoother = DisplaySmoother(tauMs)
        smoother.apply(view(rangeKm = 200.0, reserveKm = 150.0), elapsedMs = 0)
        val shown = smoother.apply(view(rangeKm = 240.0, reserveKm = 180.0), elapsedMs = 1_000)
        val window = shown.windows.single()
        assertEquals(0.75, window.rangeToReserveKm!! / window.rangeTo0Km!!, 1e-9)
    }

    @Test
    fun kWhFollowsSmoothedPercentAndCapacity() {
        val smoother = DisplaySmoother(tauMs)
        smoother.apply(view(tripPct = 40.0, capacityKwh = 40.0), elapsedMs = 0)
        val shown = smoother.apply(view(tripPct = 50.0, capacityKwh = 40.0), elapsedMs = 1_000)
        val pct = shown.trip.pctPer100!!
        assertTrue(pct > 40.0 && pct < 41.0)
        assertEquals(pct * 0.4, shown.trip.kWhPer100!!, 1e-9)

        // Сменили ёмкость в «Справке» — кВт·ч пересчитываются сразу, без сглаживания.
        val recalculated = smoother.apply(view(tripPct = 50.0, capacityKwh = 20.0), elapsedMs = 1_000)
        assertEquals(pct, recalculated.trip.pctPer100!!, 1e-9)
        assertEquals(pct * 0.2, recalculated.trip.kWhPer100!!, 1e-9)
    }

    /** Новая поездка: расход сначала пропадает (0 км), потом начинается с нуля, а не с прошлой поездки. */
    @Test
    fun newTripDoesNotInheritPreviousRate() {
        val smoother = DisplaySmoother(tauMs)
        smoother.apply(view(tripPct = 40.0), elapsedMs = 0)
        assertNull(smoother.apply(view(tripPct = null), elapsedMs = 1_000).trip.pctPer100)
        assertEquals(12.0, smoother.apply(view(tripPct = 12.0), elapsedMs = 2_000).trip.pctPer100!!, 0.0)
    }

    private fun view(
        rangeKm: Double? = 200.0,
        reserveKm: Double? = rangeKm?.times(0.75),
        status: WindowStatus = WindowStatus.READY,
        tripPct: Double? = 40.0,
        capacityKwh: Double? = 40.0,
    ): EngineView {
        val rates = ConsumptionRates(
            distanceKm = 10.0,
            socUsedPoints = 4.0,
            pctPer100 = tripPct,
            kWhPer100 = if (tripPct != null && capacityKwh != null) capacityKwh * tripPct / 100.0 else null,
        )
        return EngineView(
            socPercent = 60f,
            socDeltaPoints = null,
            speedKmh = 50f,
            outsideTempC = 10f,
            gear = null,
            odometerKm = null,
            vehicleRangeRemainingKm = null,
            parked = false,
            waitingForDrive = false,
            charging = status == WindowStatus.CHARGING,
            charge = null,
            period = rates,
            trip = rates,
            tripIncomplete = false,
            tripAvgSpeedKmh = null,
            tripAvgTempC = null,
            tripSocStartPercent = null,
            tripSocEndPercent = null,
            tripTempStartC = null,
            tripTempEndC = null,
            windows = listOf(
                RangeWindow(
                    windowKm = 15.0,
                    status = status,
                    rangeTo0Km = rangeKm,
                    rangeToReserveKm = reserveKm,
                ),
            ),
            forecastAverageKm = rangeKm,
            usableCapacityKwh = capacityKwh,
            capacityIsUserSet = true,
            odometerTrusted = true,
            bufferCoveredKm = 20.0,
            tripDurationMs = null,
            persistPeriod = false,
            persistBuffer = false,
        )
    }
}
