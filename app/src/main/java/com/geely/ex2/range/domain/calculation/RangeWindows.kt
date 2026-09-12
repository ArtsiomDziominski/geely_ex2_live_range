package com.geely.ex2.range.domain.calculation

import com.geely.ex2.range.domain.buffer.SampleRingBuffer
import com.geely.ex2.range.domain.model.BufferPoint
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowChartPoint
import com.geely.ex2.range.domain.model.WindowStatus

object RangeWindows {
    private const val MAX_CHART_POINTS = 32

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
        val first = buffer.snapshot().firstOrNull() ?: last
        val covered = last.cumulativeKm - first.cumulativeKm
        if (covered + 1e-6 < windowKm) {
            return RangeWindow(
                windowKm = windowKm,
                status = WindowStatus.NEED_MORE_KM,
                remainingToFillKm = (windowKm - covered).coerceAtLeast(0.0),
                chart = mapChart(buffer.snapshot(), first.cumulativeKm, windowKm),
            )
        }
        val targetKm = last.cumulativeKm - windowKm
        val slice = buffer.sliceFrom(targetKm)
        val chart = mapChart(slice, targetKm, windowKm)
        if (slice.any { it.chargingLikely }) {
            return RangeWindow(windowKm, WindowStatus.CHARGING, chart = chart)
        }
        if (slice.any { it.gap }) {
            return RangeWindow(windowKm, WindowStatus.GAP, chart = chart)
        }
        val socThen = buffer.socAt(targetKm) ?: return RangeWindow(
            windowKm,
            WindowStatus.INVALID,
            chart = chart,
        )
        val deltaSoc = (socThen - socNow).toDouble()
        if (deltaSoc <= 0.0) {
            return RangeWindow(
                windowKm = windowKm,
                status = if (deltaSoc < 0.0) WindowStatus.CHARGING else WindowStatus.SOC_UNCHANGED,
                deltaSocPoints = deltaSoc,
                chart = chart,
            )
        }
        if (deltaSoc < minSocStep) {
            return RangeWindow(
                windowKm = windowKm,
                status = WindowStatus.SOC_UNCHANGED,
                deltaSocPoints = deltaSoc,
                chart = chart,
            )
        }
        val rangeTo0 = socNow * windowKm / deltaSoc
        if (!rangeTo0.isFinite() || rangeTo0 < 0.0 || rangeTo0 > RangeConstants.MAX_RANGE_KM) {
            return RangeWindow(
                windowKm,
                WindowStatus.INVALID,
                deltaSocPoints = deltaSoc,
                chart = chart,
            )
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
            chart = chart,
        )
    }

    private fun mapChart(
        points: List<BufferPoint>,
        startKm: Double,
        windowKm: Double,
    ): List<WindowChartPoint> {
        if (points.isEmpty()) return emptyList()
        var lastSpeed: Float? = null
        var lastTemp: Float? = null
        val mapped = points.map { point ->
            val speed = point.speedKmh?.takeIf { it.isFinite() && it >= 0f } ?: lastSpeed
            val temp = point.outsideTempC?.takeIf { it.isFinite() } ?: lastTemp
            if (speed != null) lastSpeed = speed
            if (temp != null) lastTemp = temp
            WindowChartPoint(
                km = ((point.cumulativeKm - startKm).coerceIn(0.0, windowKm)).toFloat(),
                soc = point.socPercent,
                speedKmh = speed,
                outsideTempC = temp,
            )
        }
        return downsample(mapped)
    }

    private fun downsample(points: List<WindowChartPoint>): List<WindowChartPoint> {
        if (points.size <= MAX_CHART_POINTS) return points
        val lastIndex = points.lastIndex
        return List(MAX_CHART_POINTS) { i ->
            val index = (i.toDouble() / (MAX_CHART_POINTS - 1) * lastIndex).toInt()
            points[index]
        }
    }
}
