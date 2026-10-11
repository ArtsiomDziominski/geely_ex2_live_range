package com.geely.ex2.range.ui.dashboard.styles

import com.geely.ex2.range.domain.engine.EngineView
import com.geely.ex2.range.domain.model.ChargeState
import com.geely.ex2.range.domain.model.ConsumptionRates
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowStatus
import com.geely.ex2.range.ui.dashboard.VehicleDeltaTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardUiTest {
    @Test
    fun readyForecastFillsRangeReserveAndRoad() {
        val ui = dashboardUi(
            view(
                soc = 64.3f,
                vehicleKm = 236f,
                windows = listOf(ready(5.0, 231.0), ready(15.0, 252.0), ready(30.0, 261.0)),
            ),
        )
        assertEquals("248", ui.rangeKm)
        assertEquals("среднее по окнам", ui.rangeCaption)
        assertTrue(ui.forecastReady)
        // (64.3 − 20) / 64.3 пути до нуля приходится на заряд выше резерва.
        assertEquals("171", ui.toReserveKm)
        assertEquals(0.689f, ui.reserveFraction!!, 0.001f)
        assertEquals(236f / 248f, ui.vehicleRoadFraction!!, 0.001f)
        assertEquals("236", ui.vehicleKm)
    }

    @Test
    fun windowsShareOneBarScaleWithVehicleEstimate() {
        val ui = dashboardUi(
            view(vehicleKm = 236f, windows = listOf(ready(5.0, 231.0), ready(15.0, 252.0), ready(30.0, 261.0))),
        )
        val scale = 261.0 * 1.15
        assertEquals((231.0 / scale).toFloat(), ui.window(5.0).barFraction, 0.001f)
        assertEquals((261.0 / scale).toFloat(), ui.window(30.0).barFraction, 0.001f)
        assertEquals((236.0 / scale).toFloat(), ui.vehicleBarFraction!!, 0.001f)
        assertTrue(ui.windows.all { it.barFraction < 1f })
    }

    @Test
    fun readyWindowCarriesDeltaAgainstVehicleEstimate() {
        val ui = dashboardUi(view(vehicleKm = 236f, windows = listOf(ready(5.0, 231.0), ready(15.0, 252.0))))
        val below = ui.window(5.0)
        assertEquals("231", below.value)
        assertEquals("-2%", below.deltaLabel)
        assertEquals(VehicleDeltaTone.BELOW, below.deltaTone)
        val above = ui.window(15.0)
        assertEquals("+7%", above.deltaLabel)
        assertEquals(VehicleDeltaTone.ABOVE, above.deltaTone)
        assertNull(above.status)
    }

    @Test
    fun fillingWindowShowsProgressInsteadOfForecast() {
        val filling = RangeWindow(windowKm = 15.0, status = WindowStatus.NEED_MORE_KM, remainingToFillKm = 9.0)
        val window = dashboardUi(view(windows = listOf(ready(5.0, 231.0), filling))).window(15.0)
        assertFalse(window.ready)
        assertEquals("…", window.value)
        assertEquals("Ещё 9.0 км до окна", window.status)
        assertEquals(0.4f, window.barFraction, 0.001f)
        assertNull(window.reserveKm)
        assertNull(window.deltaLabel)
    }

    /** Окна, которого движок ещё не прислал, на экране всё равно три — недостающее помечено. */
    @Test
    fun missingWindowIsStillListed() {
        val ui = dashboardUi(view(windows = listOf(ready(5.0, 231.0))))
        assertEquals(listOf(5.0, 15.0, 30.0), ui.windows.map { it.windowKm })
        assertEquals("Ещё нет окна", ui.window(30.0).status)
        assertEquals("—", ui.window(30.0).value)
    }

    @Test
    fun withoutForecastRangeFallsBackToVehicleEstimate() {
        val ui = dashboardUi(view(vehicleKm = 80f, forecastKm = null, windows = emptyList()))
        assertEquals("80", ui.rangeKm)
        assertEquals("оценка ГУ", ui.rangeCaption)
        assertFalse(ui.forecastReady)
        assertNull(ui.toReserveKm)
        assertNull(ui.reserveFraction)
        assertNull(ui.vehicleRoadFraction)
    }

    @Test
    fun noDataAtAllIsDashes() {
        val ui = dashboardUi(view(soc = null, vehicleKm = null, forecastKm = null, windows = emptyList()))
        assertEquals("—", ui.soc)
        assertEquals(0f, ui.socFraction, 0f)
        assertEquals("—", ui.rangeKm)
        assertFalse(ui.hasRange)
        assertEquals("нет данных", ui.rangeCaption)
        assertNull(ui.vehicleKm)
    }

    @Test
    fun reservePassedLeavesNoDistanceToReserve() {
        val ui = dashboardUi(view(soc = 15f, windows = listOf(ready(5.0, 40.0))))
        assertTrue(ui.forecastReady)
        assertNull(ui.toReserveKm)
        assertNull(ui.reserveFraction)
    }

    @Test
    fun chargingMutesConsumptionAndNamesTheCharge() {
        val ui = dashboardUi(
            view(
                charging = true,
                charge = ChargeState(plugged = true, powerKw = 6.2f, minutesLeft = 80),
                windows = listOf(RangeWindow(windowKm = 5.0, status = WindowStatus.CHARGING)),
                forecastKm = null,
            ),
        )
        assertTrue(ui.charging)
        assertEquals("Зарядка AC · 6.2 кВт · ещё 1 ч 20 мин", ui.chargeStatus)
        assertEquals("—", ui.periodKwh)
        assertEquals("—", ui.tripKwh)
        assertEquals("Зарядка", ui.window(5.0).status)
    }

    @Test
    fun pluggedWithoutPowerIsReportedButNotCharging() {
        val ui = dashboardUi(view(charge = ChargeState(plugged = true, powerKw = 0f)))
        assertFalse(ui.charging)
        assertEquals("Кабель подключён · заряд не идёт", ui.chargeStatus)
        assertEquals("16.0", ui.periodKwh)
    }

    @Test
    fun parkedTripHidesItsRateButKeepsThePeriod() {
        val ui = dashboardUi(view(waitingForDrive = true))
        assertEquals("—", ui.tripKwh)
        assertEquals("16.0", ui.periodKwh)
        assertEquals("40", ui.periodPct)
        assertNull(ui.chargeStatus)
    }

    private fun ready(windowKm: Double, rangeKm: Double) = RangeWindow(
        windowKm = windowKm,
        status = WindowStatus.READY,
        rangeTo0Km = rangeKm,
        rangeToReserveKm = rangeKm * 0.689,
    )

    private fun view(
        soc: Float? = 64.3f,
        vehicleKm: Float? = 236f,
        windows: List<RangeWindow> = listOf(ready(5.0, 231.0), ready(15.0, 252.0), ready(30.0, 261.0)),
        forecastKm: Double? = windows
            .filter { it.status == WindowStatus.READY }
            .mapNotNull { it.rangeTo0Km }
            .takeIf { it.isNotEmpty() }
            ?.average(),
        charging: Boolean = false,
        charge: ChargeState? = null,
        waitingForDrive: Boolean = false,
    ): EngineView {
        val rates = ConsumptionRates(distanceKm = 10.0, socUsedPoints = 4.0, pctPer100 = 40.0, kWhPer100 = 16.0)
        return EngineView(
            socPercent = soc,
            socDeltaPoints = null,
            speedKmh = 72f,
            outsideTempC = 11f,
            gear = null,
            odometerKm = 12_480.6f,
            vehicleRangeRemainingKm = vehicleKm,
            parked = waitingForDrive,
            waitingForDrive = waitingForDrive,
            charging = charging,
            charge = charge,
            period = rates,
            trip = rates,
            tripIncomplete = false,
            tripAvgSpeedKmh = null,
            tripAvgTempC = null,
            tripSocStartPercent = null,
            tripSocEndPercent = null,
            tripTempStartC = null,
            tripTempEndC = null,
            windows = windows,
            forecastAverageKm = forecastKm,
            usableCapacityKwh = 40.0,
            capacityIsUserSet = true,
            odometerTrusted = true,
            bufferCoveredKm = 30.0,
            tripDurationMs = null,
            persistPeriod = false,
            persistBuffer = false,
        )
    }
}
