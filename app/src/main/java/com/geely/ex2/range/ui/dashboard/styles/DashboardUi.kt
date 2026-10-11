package com.geely.ex2.range.ui.dashboard.styles

import androidx.compose.runtime.Immutable
import com.geely.ex2.range.domain.engine.EngineView
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowChartPoint
import com.geely.ex2.range.domain.model.WindowStatus
import com.geely.ex2.range.ui.components.NO_VALUE
import com.geely.ex2.range.ui.dashboard.VehicleDeltaTone
import com.geely.ex2.range.ui.dashboard.chargingLabel
import com.geely.ex2.range.ui.dashboard.heroRange
import com.geely.ex2.range.ui.dashboard.kmUntilLowSoc
import com.geely.ex2.range.ui.dashboard.pluggedIdle
import com.geely.ex2.range.ui.dashboard.tripHint
import com.geely.ex2.range.ui.dashboard.vehicleDeltaTone
import com.geely.ex2.range.ui.dashboard.windowFilledFraction
import kotlin.math.abs

/**
 * Всё, что показывает главная, уже в виде готовых подписей и долей — чтобы четыре оформления
 * различались только раскладкой и не повторяли правила «что показать, когда данных нет».
 * Расчётов здесь нет: значения берутся из [EngineView] как есть.
 */
@Immutable
data class DashboardUi(
    val soc: String,
    /** Заряд 0…1 для шкал; 0, если SOC нет. */
    val socFraction: Float,
    val rangeKm: String,
    val hasRange: Boolean,
    /** Откуда крупное число: «среднее по окнам», «оценка ГУ» или «нет данных». */
    val rangeCaption: String,
    /** Наш прогноз готов (а не подменён оценкой ГУ) — есть что раскладывать на «дороге заряда». */
    val forecastReady: Boolean,
    val toReserveKm: String?,
    /** Доля пути до резерва от пути до 0 %; null — прогноза нет или резерв уже пройден. */
    val reserveFraction: Float?,
    val speed: String,
    val outsideTemp: String,
    val gear: String,
    val odometer: String,
    val periodKwh: String,
    val periodPct: String,
    val tripKwh: String,
    val tripPct: String,
    val tripHint: String,
    val vehicleKm: String?,
    /** Оценка ГУ в долях нашего прогноза (0…1) — отметка на «дороге заряда». */
    val vehicleRoadFraction: Float?,
    /** Оценка ГУ на общей шкале полос окон. */
    val vehicleBarFraction: Float?,
    val charging: Boolean,
    /** «Зарядка AC · 6.2 кВт · …» или «Кабель подключён · заряд не идёт»; null — ни того, ни другого. */
    val chargeStatus: String?,
    val windows: List<WindowUi>,
) {
    fun window(windowKm: Double): WindowUi {
        return windows.firstOrNull { abs(it.windowKm - windowKm) < 1e-6 } ?: windows.first()
    }
}

@Immutable
data class WindowUi(
    val windowKm: Double,
    val label: String,
    val ready: Boolean,
    /** Прогноз до 0 % у готового окна, иначе короткий знак: «…» — набирается, «—» — нет. */
    val value: String,
    val reserveKm: String?,
    val deltaLabel: String?,
    val deltaTone: VehicleDeltaTone?,
    /** Что с окном, когда прогноза нет: «Ещё 3.2 км до окна», «Зарядка»… Null у готового. */
    val status: String?,
    val statusHint: String?,
    /** Длина полосы окна на общей шкале; у набирающегося — доля набранного. */
    val barFraction: Float,
    val chart: List<WindowChartPoint>,
    val chartMaxKm: Double,
)

fun dashboardUi(engine: EngineView): DashboardUi {
    val hero = heroRange(engine)
    val soc = engine.socPercent?.takeIf { it.isFinite() }
    val vehicleKm = engine.vehicleRangeRemainingKm?.takeIf { it.isFinite() && it > 0f }
    val forecastKm = hero.forecastAverageKm?.takeIf { it.isFinite() && it > 0.0 }
    val toReserve = kmUntilLowSoc(hero, soc)

    // Общая шкала полос: самое длинное из готовых окон и оценки ГУ, с запасом справа.
    val readyKm = engine.windows
        .filter { it.status == WindowStatus.READY }
        .mapNotNull { it.rangeTo0Km }
    val barScaleKm = (readyKm + listOfNotNull(vehicleKm?.toDouble())).maxOrNull()?.times(BAR_HEADROOM)

    val trip = engine.trip
    val tripMuted = engine.charging || engine.waitingForDrive
    return DashboardUi(
        soc = DisplayFormat.socNumber(soc),
        socFraction = soc?.div(100f)?.coerceIn(0f, 1f) ?: 0f,
        rangeKm = hero.km?.let { DisplayFormat.kmNumber(it) } ?: NO_VALUE,
        hasRange = hero.km != null,
        rangeCaption = when {
            forecastKm != null -> "среднее по окнам"
            hero.km != null -> "оценка ГУ"
            else -> "нет данных"
        },
        forecastReady = forecastKm != null,
        toReserveKm = toReserve?.let { DisplayFormat.kmNumber(it) },
        reserveFraction = if (toReserve != null && forecastKm != null) {
            (toReserve / forecastKm).toFloat().coerceIn(0f, 1f)
        } else {
            null
        },
        speed = DisplayFormat.speedNumber(engine.speedKmh),
        outsideTemp = DisplayFormat.tempNumber(engine.outsideTempC),
        gear = engine.gear?.label ?: NO_VALUE,
        odometer = DisplayFormat.odometerNumber(engine.odometerKm) ?: NO_VALUE,
        periodKwh = if (engine.charging) NO_VALUE else DisplayFormat.kWhPer100Number(engine.period.kWhPer100),
        periodPct = if (engine.charging) NO_VALUE else DisplayFormat.pctPer100Number(engine.period.pctPer100),
        tripKwh = if (tripMuted) NO_VALUE else DisplayFormat.kWhPer100Number(trip.kWhPer100),
        tripPct = if (tripMuted) NO_VALUE else DisplayFormat.pctPer100Number(trip.pctPer100),
        tripHint = tripHint(engine),
        vehicleKm = vehicleKm?.let { DisplayFormat.kmNumber(it.toDouble()) },
        vehicleRoadFraction = if (vehicleKm != null && forecastKm != null) {
            (vehicleKm / forecastKm).toFloat().coerceIn(0f, 1f)
        } else {
            null
        },
        vehicleBarFraction = if (vehicleKm != null && barScaleKm != null) {
            (vehicleKm / barScaleKm).toFloat().coerceIn(0f, 1f)
        } else {
            null
        },
        charging = engine.charging,
        chargeStatus = when {
            engine.charging -> chargingLabel(engine)
            pluggedIdle(engine) -> "Кабель подключён · заряд не идёт"
            else -> null
        },
        windows = RangeConstants.RANGE_WINDOWS_KM.map { km ->
            windowUi(
                window = engine.windows.firstOrNull { abs(it.windowKm - km) < 1e-6 },
                windowKm = km,
                vehicleKm = vehicleKm,
                barScaleKm = barScaleKm,
            )
        },
    )
}

private fun windowUi(
    window: RangeWindow?,
    windowKm: Double,
    vehicleKm: Float?,
    barScaleKm: Double?,
): WindowUi {
    val label = DisplayFormat.windowKmLabel(windowKm)
    if (window == null) {
        return WindowUi(
            windowKm = windowKm,
            label = label,
            ready = false,
            value = NO_VALUE,
            reserveKm = null,
            deltaLabel = null,
            deltaTone = null,
            status = "Ещё нет окна",
            statusHint = "Едем — буфер копится.",
            barFraction = 0f,
            chart = emptyList(),
            chartMaxKm = windowKm,
        )
    }
    val rangeKm = window.rangeTo0Km?.takeIf { window.status == WindowStatus.READY && it.isFinite() }
    val ready = rangeKm != null
    val delta = if (ready) DisplayFormat.rangeDeltaPercent(rangeKm, vehicleKm) else null
    val filling = window.status == WindowStatus.NEED_MORE_KM
    val (status, hint) = windowStatus(window)
    return WindowUi(
        windowKm = windowKm,
        label = label,
        ready = ready,
        value = when {
            ready -> DisplayFormat.kmNumber(rangeKm)
            filling -> "…"
            else -> NO_VALUE
        },
        reserveKm = if (ready) DisplayFormat.kmNumber(window.rangeToReserveKm ?: 0.0) else null,
        deltaLabel = DisplayFormat.rangeDeltaLabel(delta),
        deltaTone = delta?.let(::vehicleDeltaTone),
        status = status,
        statusHint = hint,
        barFraction = when {
            rangeKm != null && barScaleKm != null -> (rangeKm / barScaleKm).toFloat().coerceIn(0f, 1f)
            filling -> windowFilledFraction(window)
            else -> 0f
        },
        chart = window.chart,
        chartMaxKm = if (filling && window.chart.isNotEmpty()) {
            window.chart.maxOf { it.km }.toDouble()
        } else {
            windowKm
        },
    )
}

/** Формулировки статусов — те же, что в классическом блоке прогноза. */
private fun windowStatus(window: RangeWindow): Pair<String?, String?> = when (window.status) {
    WindowStatus.READY -> null to null
    WindowStatus.NEED_MORE_KM ->
        ("Ещё " + DisplayFormat.km(window.remainingToFillKm ?: window.windowKm) + " до окна") to
            "Продолжайте движение, чтобы накопить данные для прогноза."
    WindowStatus.SOC_UNCHANGED -> "SOC не изменился" to "Расход на этом участке слишком мал для оценки."
    WindowStatus.CHARGING -> "Зарядка" to "Прогноз возобновится, когда автомобиль поедет."
    WindowStatus.GAP -> "Разрыв в окне" to "Телеметрия прерывалась — окно наберётся заново."
    WindowStatus.INVALID -> "Мало данных" to "Недостаточно данных для оценки по окну."
}

/** Запас справа от самой длинной полосы, чтобы она не упиралась в край шкалы. */
private const val BAR_HEADROOM = 1.15
