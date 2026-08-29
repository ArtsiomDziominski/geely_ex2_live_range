package com.geely.ex2.range.domain.format

import java.util.Locale

object DisplayFormat {
    fun socPercent(value: Float?): String {
        if (value == null || !value.isFinite()) return "нет SOC"
        val two = String.format(Locale.US, "%.2f", value)
        val body = two.trimEnd('0').trimEnd('.')
        return "$body%"
    }

    fun speedKmh(value: Float?): String {
        if (value == null || !value.isFinite()) return "нет скорости"
        return String.format(Locale.US, "%.0f км/ч", value)
    }

    fun tempC(value: Float?): String {
        if (value == null || !value.isFinite()) return "нет t°"
        val sign = if (value > 0f) "+" else ""
        return String.format(Locale.US, "%s%.0f °C", sign, value)
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
