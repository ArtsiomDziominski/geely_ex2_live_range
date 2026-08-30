package com.geely.ex2.range.domain.format

import java.util.Locale
import kotlin.math.abs

object DisplayFormat {
    fun socPercent(value: Float?): String {
        if (value == null || !value.isFinite()) return "нет SOC"
        return "${socNumber(value)}%"
    }

    fun socNumber(value: Float?): String {
        if (value == null || !value.isFinite()) return "—"
        return String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }

    fun socDeltaLabel(delta: Float?): String? {
        if (delta == null || !delta.isFinite() || abs(delta) < 0.05f) return null
        val arrow = if (delta > 0f) "↑" else "↓"
        return "SOC ($arrow${socNumber(abs(delta))}%)"
    }

    fun speedKmh(value: Float?): String {
        if (value == null || !value.isFinite()) return "нет скорости"
        return "${speedNumber(value)} км/ч"
    }

    fun speedNumber(value: Float?): String {
        if (value == null || !value.isFinite()) return "—"
        return String.format(Locale.US, "%.0f", value)
    }

    fun tempC(value: Float?): String {
        if (value == null || !value.isFinite()) return "нет t°"
        return "${tempNumber(value)} °C"
    }

    fun tempNumber(value: Float?): String {
        if (value == null || !value.isFinite()) return "—"
        val sign = if (value > 0f) "+" else ""
        return String.format(Locale.US, "%s%.0f", sign, value)
    }

    fun km(value: Double?, digits: Int = 1): String {
        if (value == null || !value.isFinite()) return "—"
        return String.format(Locale.US, "%.${digits}f км", value)
    }

    fun pctPer100(value: Double?): String {
        if (value == null || !value.isFinite()) return "—"
        return String.format(Locale.US, "%.0f %%/100 км", value)
    }

    fun kWhPer100(value: Double?): String {
        if (value == null || !value.isFinite()) return "—"
        return String.format(Locale.US, "%.1f кВт·ч/100 км", value)
    }

    fun windowKmLabel(windowKm: Double): String {
        return String.format(Locale.US, "%.0f км", windowKm)
    }
}
