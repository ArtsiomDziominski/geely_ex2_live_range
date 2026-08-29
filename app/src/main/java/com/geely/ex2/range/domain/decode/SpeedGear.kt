package com.geely.ex2.range.domain.decode

import com.geely.ex2.range.domain.model.Gear
import kotlin.math.abs

object SpeedDecoder {
    /** EX2 VHAL speed is already km/h. Do not apply Tools SpeedNormalizer (+1). */
    fun rawKmh(raw: Float?): Float? {
        if (raw == null || !raw.isFinite()) return null
        return abs(raw)
    }
}

object GearDecoder {
    fun fromVhal(value: Int?): Gear? {
        if (value == null) return null
        return Gear.entries.firstOrNull { it.vhalValue == value }
    }
}
