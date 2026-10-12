package com.geely.ex2.range.domain.decode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargingDecoderTest {
    @Test
    fun plugFunctionWinsOverObc() {
        assertEquals(true, ChargingDecoder.plugged(ChargingDecoder.PLUG_STATE_CONNECTED, 0))
        assertEquals(false, ChargingDecoder.plugged(ChargingDecoder.PLUG_STATE_DISCONNECTED, 1))
    }

    @Test
    fun plugFallsBackToObcWhenFunctionUnanswered() {
        assertEquals(true, ChargingDecoder.plugged(0, 1))
        assertEquals(false, ChargingDecoder.plugged(null, 0))
        assertNull(ChargingDecoder.plugged(null, null))
    }

    @Test
    fun powerPrefersFunctionAndScalesProperty() {
        assertEquals(6.3f, ChargingDecoder.powerKw(6.3f, 62)!!, 0.001f)
        assertEquals(6.2f, ChargingDecoder.powerKw(null, 62)!!, 0.001f)
        // Неотвеченная функция приходит нулём — берём свойство.
        assertEquals(6.2f, ChargingDecoder.powerKw(0f, 62)!!, 0.001f)
        assertEquals(0f, ChargingDecoder.powerKw(0f, 0)!!, 0f)
        assertNull(ChargingDecoder.powerKw(null, null))
    }

    @Test
    fun implausiblePowerIsIgnored() {
        assertNull(ChargingDecoder.powerKw(Float.NaN, null))
        assertNull(ChargingDecoder.powerKw(-3f, null))
        assertEquals(6.2f, ChargingDecoder.powerKw(9_999f, 62)!!, 0.001f)
    }

    @Test
    fun minutesTreatNoEstimateAsMissing() {
        assertEquals(80, ChargingDecoder.minutesLeft(80.4f, 1800))
        assertEquals(95, ChargingDecoder.minutesLeft(0f, 95))
        assertNull(ChargingDecoder.minutesLeft(0f, 1800))
        assertNull(ChargingDecoder.minutesLeft(null, 0))
    }

    @Test
    fun decodePicksLargerOfAcAndDc() {
        val ac = ChargingDecoder.decode(plugged = true, acKw = 6.2f, dcKw = 0f, minutesLeft = 80)
        assertFalse(ac.isDc)
        assertEquals(6.2f, ac.powerKw!!, 0.001f)
        assertTrue(ac.energyFlowing)
        assertEquals(80, ac.minutesLeft)

        val dc = ChargingDecoder.decode(plugged = true, acKw = 0f, dcKw = 49.5f, minutesLeft = 20)
        assertTrue(dc.isDc)
        assertEquals(49.5f, dc.powerKw!!, 0.001f)
    }

    @Test
    fun stalePowerWithoutCableIsNotCharging() {
        // Снято с ГУ на стоянке: кабеля нет, а функция DC всё ещё отдаёт 34,5 кВт прошлой сессии.
        val stale = ChargingDecoder.decode(plugged = false, acKw = 0f, dcKw = 34.5f, minutesLeft = null)
        assertFalse(stale.energyFlowing)
        assertFalse(stale.isDc)
        assertEquals(0f, stale.powerKw!!, 0f)
    }

    @Test
    fun pluggedWithoutPowerIsNotCharging() {
        val idle = ChargingDecoder.decode(plugged = true, acKw = 0.1f, dcKw = null, minutesLeft = 30)
        assertFalse(idle.energyFlowing)
        assertNull(idle.minutesLeft)

        val unknown = ChargingDecoder.decode(plugged = null, acKw = null, dcKw = null, minutesLeft = null)
        assertNull(unknown.powerKw)
        assertFalse(unknown.energyFlowing)
    }
}
