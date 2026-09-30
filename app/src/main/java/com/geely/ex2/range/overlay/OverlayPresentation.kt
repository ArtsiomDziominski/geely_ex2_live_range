package com.geely.ex2.range.overlay

import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowStatus
import com.geely.ex2.range.ui.dashboard.VehicleDeltaTone
import com.geely.ex2.range.ui.dashboard.vehicleDeltaTone
import com.geely.ex2.range.ui.dashboard.windowFilledFraction

/** Цвет кольца окна на панели виджета. */
internal enum class GaugeTone {
    /** Прогноз готов и расходится с оценкой ГУ не больше обычного. */
    CLOSE,

    /** Прогноз готов, но заметно расходится с оценкой ГУ. */
    FAR,

    /** Прогноз готов, оценки ГУ нет — сравнивать не с чем. */
    READY,

    /** Окно ещё набирается. */
    FILLING,

    CHARGING,

    /** Прогноза нет: SOC не менялся, разрыв телеметрии, мало данных. */
    NONE,
}

/**
 * Кольцо окна 5 / 15 / 30 км: [progress] — насколько окно набрано (готовое — полное кольцо),
 * [value] — прогноз до 0 % в км либо знак статуса, как в прежнем виджете.
 */
internal data class GaugeState(
    val progress: Float,
    val tone: GaugeTone,
    val value: String,
)

internal fun gaugeState(
    window: RangeWindow?,
    charging: Boolean,
    vehicleRangeRemainingKm: Float?,
): GaugeState {
    if (charging || window?.status == WindowStatus.CHARGING) {
        return GaugeState(progress = 1f, tone = GaugeTone.CHARGING, value = "—")
    }
    if (window == null) return GaugeState(progress = 0f, tone = GaugeTone.NONE, value = "—")
    return when (window.status) {
        WindowStatus.READY -> {
            val delta = DisplayFormat.rangeDeltaPercent(window.rangeTo0Km, vehicleRangeRemainingKm)
            val tone = when {
                delta == null -> GaugeTone.READY
                vehicleDeltaTone(delta) == VehicleDeltaTone.CLOSE -> GaugeTone.CLOSE
                else -> GaugeTone.FAR
            }
            GaugeState(progress = 1f, tone = tone, value = DisplayFormat.kmNumber(window.rangeTo0Km))
        }
        WindowStatus.NEED_MORE_KM -> GaugeState(
            progress = windowFilledFraction(window),
            tone = GaugeTone.FILLING,
            value = "…",
        )
        WindowStatus.SOC_UNCHANGED -> GaugeState(progress = 0f, tone = GaugeTone.NONE, value = "—")
        WindowStatus.GAP -> GaugeState(progress = 0f, tone = GaugeTone.NONE, value = "!")
        WindowStatus.INVALID -> GaugeState(progress = 0f, tone = GaugeTone.NONE, value = "?")
        WindowStatus.CHARGING -> GaugeState(progress = 1f, tone = GaugeTone.CHARGING, value = "—")
    }
}
