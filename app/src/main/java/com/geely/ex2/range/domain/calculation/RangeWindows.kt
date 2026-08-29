package com.geely.ex2.range.domain.calculation

import com.geely.ex2.range.domain.buffer.SampleRingBuffer
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowStatus

object RangeWindows {
    fun estimateAll(
        buffer: SampleRingBuffer,
        socNow: Float?,
        minSocStep: Double = RangeConstants.MIN_SOC_STEP,
        reserveSoc: Double = RangeConstants.RESERVE_SOC_PERCENT,
        windowKm: DoubleArray = RangeConstants.RANGE_WINDOWS_KM,
    ): List<RangeWindow> {
        return windowKm.map { estimate(buffer, it, socNow, minSocStep, reserveSoc) }
    }

    fun estimate(
        buffer: SampleRingBuffer,
        windowKm: Double,
        socNow: Float?,
        minSocStep: Double = RangeConstants.MIN_SOC_STEP,
        reserveSoc: Double = RangeConstants.RESERVE_SOC_PERCENT,
    ): RangeWindow {
        if (socNow == null || !socNow.isFinite()) {
            return RangeWindow(windowKm, WindowStatus.INVALID)
        }
        val last = buffer.last() ?: return RangeWindow(
            windowKm = windowKm,
            status = WindowStatus.NEED_MORE_KM,
            remainingToFillKm = windowKm,
        )
        val covered = last.cumulativeKm - (buffer.snapshot().firstOrNull()?.cumulativeKm ?: last.cumulativeKm)
        if (covered + 1e-6 < windowKm) {
            return RangeWindow(
                windowKm = windowKm,
                status = WindowStatus.NEED_MORE_KM,
                remainingToFillKm = (windowKm - covered).coerceAtLeast(0.0),
            )
        }
        val targetKm = last.cumulativeKm - windowKm
        val slice = buffer.sliceFrom(targetKm)
        if (slice.any { it.chargingLikely }) {
            return RangeWindow(windowKm, WindowStatus.CHARGING)
        }
        if (slice.any { it.gap }) {
            return RangeWindow(windowKm, WindowStatus.GAP)
        }
        val socThen = buffer.socAt(targetKm) ?: return RangeWindow(windowKm, WindowStatus.INVALID)
        val deltaSoc = (socThen - socNow).toDouble()
        if (deltaSoc <= 0.0) {
            return RangeWindow(
                windowKm = windowKm,
                status = if (deltaSoc < 0.0) WindowStatus.CHARGING else WindowStatus.SOC_UNCHANGED,
                deltaSocPoints = deltaSoc,
            )
        }
        if (deltaSoc < minSocStep) {
            return RangeWindow(
                windowKm = windowKm,
                status = WindowStatus.SOC_UNCHANGED,
                deltaSocPoints = deltaSoc,
            )
        }
        val rangeTo0 = socNow * windowKm / deltaSoc
        if (!rangeTo0.isFinite() || rangeTo0 < 0.0 || rangeTo0 > RangeConstants.MAX_RANGE_KM) {
            return RangeWindow(windowKm, WindowStatus.INVALID, deltaSocPoints = deltaSoc)
        }
        val rangeToReserve = if (socNow > reserveSoc) {
            (socNow - reserveSoc) * windowKm / deltaSoc
        } else {
            0.0
        }
        return RangeWindow(
            windowKm = windowKm,
            status = WindowStatus.READY,
            rangeTo0Km = rangeTo0,
            rangeToReserveKm = rangeToReserve.coerceAtLeast(0.0),
            deltaSocPoints = deltaSoc,
        )
    }
}
