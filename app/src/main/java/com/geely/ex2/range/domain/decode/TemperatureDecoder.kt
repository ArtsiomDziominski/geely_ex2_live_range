package com.geely.ex2.range.domain.decode

object TemperatureDecoder {
    fun fromFlymeRaw(raw: Number): Float = (raw.toFloat() - 80f) / 2f

    fun outsideC(oemRaw: Number?, envOutsideC: Float?): Float? {
        if (oemRaw != null) {
            val decoded = fromFlymeRaw(oemRaw)
            if (decoded.isFinite()) return decoded
        }
        return envOutsideC?.takeIf { it.isFinite() }
    }
}
