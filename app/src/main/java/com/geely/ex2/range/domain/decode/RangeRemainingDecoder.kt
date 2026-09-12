package com.geely.ex2.range.domain.decode

import com.geely.ex2.range.domain.model.RangeConstants

/**
 * AOSP specs `RANGE_REMAINING` as FLOAT/metres — but `PERF_ODOMETER` on this vehicle already
 * proves the HAL doesn't always follow its own spec (it answers Int, not the specced Float), so
 * blindly dividing by 1000 is not something we can just trust. Instead of assuming metres, pick
 * whichever scale actually lands in a plausible km figure: if the raw value already reads as a
 * sane range in km, use it as-is; only divide by 1000 when it's too large to be km directly
 * (i.e. it actually looks like metres). Self-corrects either way; only genuinely ambiguous right
 * at under ~1 km of true remaining range, where both readings look small.
 */
object RangeRemainingDecoder {
    fun decodeKm(raw: Float?, maxPlausibleKm: Double = RangeConstants.MAX_RANGE_KM): Float? {
        if (raw == null || !raw.isFinite() || raw < 0f) return null
        val maxKm = maxPlausibleKm.toFloat()
        if (raw <= maxKm) return raw
        val asMeters = raw / 1000f
        return asMeters.takeIf { it <= maxKm }
    }
}
