package com.geely.ex2.range.domain.decode

/**
 * OEM SOC (`0x2140A6ED`) is a float 0–100 and is the only trusted percent source.
 * AOSP `EV_BATTERY_LEVEL` is energy (Wh) on EX2 — never treat 0–100 as percent.
 */
object SocDecoder {
    fun decodePercent(
        oemPercent: Float?,
        batteryLevel: Float?,
        currentCapacityWh: Float?,
        nominalCapacityWh: Float?,
    ): Float? {
        oemPercent?.let { value ->
            if (value.isFinite() && value in 0f..100f) {
                return value
            }
        }

        val level = batteryLevel ?: return null
        if (!level.isFinite() || level <= 100f) {
            return null
        }
        val capacityWh = currentCapacityWh?.takeIf { it.isFinite() && it > 0f }
            ?: nominalCapacityWh?.takeIf { it.isFinite() && it > 0f }
            ?: return null
        val percent = level / capacityWh * 100f
        return if (percent.isFinite()) percent.coerceIn(0f, 100f) else null
    }

    fun usableCapacityKwh(
        userKwh: Double?,
        nominalCapacityWh: Float?,
    ): Double? {
        userKwh?.let { value ->
            if (value.isFinite() && value > 0.0) return value
        }
        val wh = nominalCapacityWh?.takeIf { it.isFinite() && it > 0f } ?: return null
        return wh / 1000.0
    }
}
