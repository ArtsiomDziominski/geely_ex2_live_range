package com.geely.ex2.range.domain.buffer

import com.geely.ex2.range.domain.model.BufferPoint
import com.geely.ex2.range.domain.model.RangeConstants

class SampleRingBuffer(
    private val keepKm: Double = RangeConstants.BUFFER_KEEP_KM,
) {
    private val points = ArrayDeque<BufferPoint>()

    fun restore(restored: List<BufferPoint>) {
        points.clear()
        points.addAll(restored)
        trim()
    }

    fun snapshot(): List<BufferPoint> = points.toList()

    fun isEmpty(): Boolean = points.isEmpty()

    fun clear() {
        points.clear()
    }

    fun last(): BufferPoint? = points.lastOrNull()

    fun coveredKm(): Double {
        val first = points.firstOrNull() ?: return 0.0
        val last = points.lastOrNull() ?: return 0.0
        return (last.cumulativeKm - first.cumulativeKm).coerceAtLeast(0.0)
    }

    fun add(point: BufferPoint) {
        val previous = points.lastOrNull()
        if (previous != null &&
            previous.cumulativeKm == point.cumulativeKm &&
            previous.socPercent == point.socPercent &&
            previous.chargingLikely == point.chargingLikely &&
            !point.gap
        ) {
            return
        }
        points.addLast(point)
        trim()
    }

    fun socAt(targetKm: Double): Float? {
        if (points.isEmpty()) return null
        if (targetKm <= points.first().cumulativeKm) return points.first().socPercent
        if (targetKm >= points.last().cumulativeKm) return points.last().socPercent
        for (index in 1 until points.size) {
            val left = points[index - 1]
            val right = points[index]
            if (targetKm in left.cumulativeKm..right.cumulativeKm) {
                val span = right.cumulativeKm - left.cumulativeKm
                if (span <= 1e-9) return left.socPercent
                val t = (targetKm - left.cumulativeKm) / span
                return (left.socPercent + (right.socPercent - left.socPercent) * t).toFloat()
            }
        }
        return null
    }

    /** Same interpolation as [socAt], over [BufferPoint.consumedSocPoints] instead of raw SOC. */
    fun consumedAt(targetKm: Double): Double? {
        if (points.isEmpty()) return null
        if (targetKm <= points.first().cumulativeKm) return points.first().consumedSocPoints
        if (targetKm >= points.last().cumulativeKm) return points.last().consumedSocPoints
        for (index in 1 until points.size) {
            val left = points[index - 1]
            val right = points[index]
            if (targetKm in left.cumulativeKm..right.cumulativeKm) {
                val span = right.cumulativeKm - left.cumulativeKm
                if (span <= 1e-9) return left.consumedSocPoints
                val t = (targetKm - left.cumulativeKm) / span
                return left.consumedSocPoints + (right.consumedSocPoints - left.consumedSocPoints) * t
            }
        }
        return null
    }

    fun sliceFrom(targetKm: Double): List<BufferPoint> {
        if (points.isEmpty()) return emptyList()
        val start = points.indexOfFirst { it.cumulativeKm >= targetKm }.let { index ->
            if (index <= 0) 0 else index - 1
        }
        return points.drop(start)
    }

    private fun trim() {
        while (points.size >= 2 && (points.last().cumulativeKm - points.first().cumulativeKm) > keepKm) {
            points.removeFirst()
        }
        while (points.size > 20_000) {
            points.removeFirst()
        }
    }
}
