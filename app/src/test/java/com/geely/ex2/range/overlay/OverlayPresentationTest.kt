package com.geely.ex2.range.overlay

import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayPresentationTest {
    private val ready = RangeWindow(
        windowKm = 15.0,
        status = WindowStatus.READY,
        rangeTo0Km = 212.4,
        rangeToReserveKm = 160.0,
    )

    @Test
    fun readyWindowIsFullRingWithForecast() {
        assertEquals(GaugeState(1f, GaugeTone.READY, "212"), gaugeState(ready, charging = false, null))
    }

    @Test
    fun readyWindowToneFollowsVehicleEstimate() {
        assertEquals(GaugeTone.CLOSE, gaugeState(ready, charging = false, 220f).tone)
        assertEquals(GaugeTone.FAR, gaugeState(ready, charging = false, 300f).tone)
    }

    @Test
    fun fillingWindowShowsCollectedShare() {
        val filling = RangeWindow(windowKm = 30.0, status = WindowStatus.NEED_MORE_KM, remainingToFillKm = 7.5)
        assertEquals(GaugeState(0.75f, GaugeTone.FILLING, "…"), gaugeState(filling, charging = false, null))
    }

    @Test
    fun chargingOverridesReadyForecast() {
        assertEquals(GaugeState(1f, GaugeTone.CHARGING, "—"), gaugeState(ready, charging = true, 220f))
    }

    @Test
    fun missingOrBrokenWindowHasNoForecast() {
        assertEquals(GaugeState(0f, GaugeTone.NONE, "—"), gaugeState(null, charging = false, null))
        val gap = RangeWindow(windowKm = 5.0, status = WindowStatus.GAP)
        assertEquals(GaugeState(0f, GaugeTone.NONE, "!"), gaugeState(gap, charging = false, null))
    }
}
