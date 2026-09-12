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

    /** Короткая подпись дельты SOC для чипа зарядки: «↑ 1.2%». */
    fun socDeltaShort(delta: Float?): String? {
        if (delta == null || !delta.isFinite() || abs(delta) < 0.05f) return null
        val arrow = if (delta > 0f) "↑" else "↓"
        return "$arrow ${socNumber(abs(delta))}%"
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

    fun odometerKm(value: Float?): String? {
        if (value == null || !value.isFinite()) return null
        return String.format(Locale.US, "%.1f км", value)
    }

    /** Одометр без единицы — единица выводится отдельным стилем. */
    fun odometerNumber(value: Float?): String? {
        if (value == null || !value.isFinite()) return null
        return String.format(Locale.US, "%.1f", value)
    }

    /** Километры без единицы — для крупных приборных чисел. */
    fun kmNumber(value: Double?, digits: Int = 0): String {
        if (value == null || !value.isFinite()) return "—"
        return String.format(Locale.US, "%.${digits}f", value)
    }

    fun km(value: Double?, digits: Int = 1): String {
        if (value == null || !value.isFinite()) return "—"
        return String.format(Locale.US, "%.${digits}f км", value)
    }

    fun pctPer100(value: Double?): String {
        if (value == null || !value.isFinite()) return "—"
        return String.format(Locale.US, "%.0f %%/100 км", value)
    }

    fun pctPer100Number(value: Double?): String {
        if (value == null || !value.isFinite()) return "—"
        return String.format(Locale.US, "%.0f", value)
    }

    fun kWhPer100(value: Double?): String {
        if (value == null || !value.isFinite()) return "—"
        return String.format(Locale.US, "%.1f кВт·ч/100 км", value)
    }

    fun kWhPer100Number(value: Double?): String {
        if (value == null || !value.isFinite()) return "—"
        return String.format(Locale.US, "%.1f", value)
    }

    fun pitchDegrees(value: Float?): String {
        if (value == null || !value.isFinite()) return "—"
        val whole = value.toInt()
        val sign = when {
            whole > 0 -> "+"
            whole < 0 -> "-"
            else -> ""
        }
        return String.format(Locale.US, "%s%d", sign, kotlin.math.abs(whole))
    }

    fun energyKwh(value: Double?): String {
        if (value == null || !value.isFinite()) return "—"
        return String.format(Locale.US, "%.1f кВт·ч", value)
    }

    fun socPoints(value: Double?): String {
        if (value == null || !value.isFinite()) return "—"
        return String.format(Locale.US, "%.1f п.п.", value)
    }

    fun duration(ms: Long?): String {
        if (ms == null || ms < 0L) return "—"
        val totalMin = ms / 60_000L
        val hours = totalMin / 60L
        val minutes = totalMin % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d ч %d мин", hours, minutes)
        } else {
            String.format(Locale.US, "%d мин", minutes)
        }
    }

    fun windowKmLabel(windowKm: Double): String {
        return String.format(Locale.US, "%.0f км", windowKm)
    }

    /** Our own window prediction vs the vehicle's own remaining-range estimate, in %. */
    fun rangeDeltaPercent(predictedKm: Double?, vehicleKm: Float?): Double? {
        if (predictedKm == null || !predictedKm.isFinite()) return null
        if (vehicleKm == null || !vehicleKm.isFinite() || vehicleKm <= 0f) return null
        return (predictedKm - vehicleKm) / vehicleKm * 100.0
    }

    fun rangeDeltaLabel(percent: Double?): String? {
        if (percent == null || !percent.isFinite()) return null
        return String.format(Locale.US, "%+.0f%%", percent)
    }
}
