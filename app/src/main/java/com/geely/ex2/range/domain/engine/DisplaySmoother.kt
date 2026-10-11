package com.geely.ex2.range.domain.engine

import com.geely.ex2.range.domain.model.ConsumptionRates
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowStatus
import kotlin.math.abs
import kotlin.math.exp

/**
 * Сглаживает то, что видит водитель: прогноз км по окнам, их среднее и расход на 100 км.
 *
 * SOC и одометр приходят ступеньками (опрос раз в 10 с, шаг SOC 0,1 %), поэтому на каждом
 * пересчёте сырые числа прыгают, а среднее по окнам ещё и скачет, когда окно добирается или
 * выпадает. Здесь число идёт к новому значению плавно — экспоненциально, с постоянной времени
 * [tauMs]. Значение, которого только что не было (окно не готово, зарядка, новая поездка),
 * появляется сразу, без разгона от старого.
 *
 * Только для показа: журнал поездок, рекорды и чекпойнт берут несглаженный [EngineView].
 */
class DisplaySmoother(
    private val tauMs: Long = RangeConstants.DISPLAY_SMOOTHING_TAU_MS,
) {
    private var lastElapsedMs: Long? = null
    private val windowRangeKm = HashMap<Double, Double>()
    private var forecastAverageKm: Double? = null
    private var periodPctPer100: Double? = null
    private var tripPctPer100: Double? = null

    fun apply(view: EngineView, elapsedMs: Long): EngineView {
        val weight = weight(elapsedMs)
        val windows = view.windows.map { smoothWindow(it, weight) }
        forecastAverageKm = blend(forecastAverageKm, view.forecastAverageKm, weight)
        periodPctPer100 = blend(periodPctPer100, view.period.pctPer100, weight)
        tripPctPer100 = blend(tripPctPer100, view.trip.pctPer100, weight)
        return view.copy(
            windows = windows,
            forecastAverageKm = forecastAverageKm,
            period = view.period.withRate(periodPctPer100, view.usableCapacityKwh),
            trip = view.trip.withRate(tripPctPer100, view.usableCapacityKwh),
        )
    }

    /** Доля пути к новому значению за прошедшее с прошлого вызова время. */
    private fun weight(elapsedMs: Long): Double {
        val previous = lastElapsedMs
        lastElapsedMs = elapsedMs
        if (previous == null || tauMs <= 0L) return 1.0
        val dtMs = (elapsedMs - previous).coerceAtLeast(0L)
        return 1.0 - exp(-dtMs.toDouble() / tauMs)
    }

    private fun blend(previous: Double?, target: Double?, weight: Double): Double? {
        if (target == null || !target.isFinite()) return null
        if (previous == null) return target
        val next = previous + (target - previous) * weight
        return if (abs(target - next) < SNAP_EPSILON) target else next
    }

    private fun smoothWindow(window: RangeWindow, weight: Double): RangeWindow {
        val raw = window.rangeTo0Km?.takeIf { window.status == WindowStatus.READY }
        val smoothed = blend(windowRangeKm[window.windowKm], raw, weight)
        if (raw == null || smoothed == null) {
            windowRangeKm.remove(window.windowKm)
            return window
        }
        windowRangeKm[window.windowKm] = smoothed
        // «До резерва» — та же оценка, умноженная на долю SOC выше резерва: масштабируется вместе с ней.
        val scale = if (raw > 0.0) smoothed / raw else 1.0
        return window.copy(
            rangeTo0Km = smoothed,
            rangeToReserveKm = window.rangeToReserveKm?.times(scale),
        )
    }

    /** кВт·ч/100 км — это %/100 км, умноженные на ёмкость, поэтому пересчитывается из сглаженного %. */
    private fun ConsumptionRates.withRate(pctPer100: Double?, capacityKwh: Double?): ConsumptionRates {
        if (pctPer100 == null) return this
        return copy(
            pctPer100 = pctPer100,
            kWhPer100 = if (kWhPer100 == null || capacityKwh == null) kWhPer100 else capacityKwh * pctPer100 / 100.0,
        )
    }

    private companion object {
        /** Ближе этого к цели (км или %/100 км) — уже не видно на экране, встаём точно на значение. */
        const val SNAP_EPSILON = 0.05
    }
}
