package com.geely.ex2.range.domain.decode

import com.geely.ex2.range.domain.model.ChargeState
import com.geely.ex2.range.domain.model.RangeConstants

/**
 * Зарядка на EX2 читается двумя путями: функция Flyme (значение уже в кВт / минутах) и сырое
 * свойство VHAL (×0,1 кВт, минуты). Функция в приоритете, свойство — запасной вариант.
 * Неотвеченное чтение машина часто отдаёт нулём, а не ошибкой, поэтому 0 от функции не мешает
 * взять ненулевое значение свойства.
 */
object ChargingDecoder {
    /** Значения функции CHARGE_FUNC_CHARGING_PLUG_STATE (id + 19 / id + 18). */
    const val PLUG_STATE_CONNECTED = 0x24130213
    const val PLUG_STATE_DISCONNECTED = 0x24130212

    /** OBC_STS_CC: 1 — кабель вставлен. */
    const val OBC_CC_CONNECTED = 1

    /** CHANGING_BMSH_FinishChgTime: 1800 — у машины нет оценки. */
    const val MINUTES_NO_ESTIMATE = 1800

    fun plugged(plugFunction: Int?, obcCc: Int?): Boolean? {
        return when (plugFunction) {
            PLUG_STATE_CONNECTED -> true
            PLUG_STATE_DISCONNECTED -> false
            else -> obcCc?.let { it == OBC_CC_CONNECTED }
        }
    }

    /** [functionKw] — функция Flyme, кВт; [propertyRaw] — свойство VHAL, ×0,1 кВт. */
    fun powerKw(functionKw: Float?, propertyRaw: Int?): Float? {
        val fromFunction = functionKw?.takeIf(::plausiblePower)
        val fromProperty = propertyRaw?.let { it / 10f }?.takeIf(::plausiblePower)
        return when {
            fromFunction != null && fromFunction > 0f -> fromFunction
            fromProperty != null && fromProperty > 0f -> fromProperty
            else -> fromFunction ?: fromProperty
        }
    }

    fun minutesLeft(functionMinutes: Float?, propertyMinutes: Int?): Int? {
        val fromFunction = functionMinutes
            ?.takeIf { it.isFinite() && it > 0f && it < MINUTES_NO_ESTIMATE }
            ?.toInt()
            ?.takeIf { it > 0 }
        return fromFunction ?: propertyMinutes?.takeIf { it in 1 until MINUTES_NO_ESTIMATE }
    }

    fun decode(
        plugged: Boolean?,
        acKw: Float?,
        dcKw: Float?,
        minutesLeft: Int?,
    ): ChargeState {
        val isDc = (dcKw ?: 0f) > (acKw ?: 0f)
        val power = if (isDc) dcKw else acKw ?: dcKw
        val state = ChargeState(plugged = plugged, powerKw = power, isDc = isDc)
        // Оценка времени имеет смысл только пока энергия идёт — иначе это остаток прошлой сессии.
        return if (state.energyFlowing) state.copy(minutesLeft = minutesLeft) else state
    }

    private fun plausiblePower(kw: Float): Boolean {
        return kw.isFinite() && kw >= 0f && kw <= RangeConstants.CHARGING_MAX_POWER_KW
    }
}
