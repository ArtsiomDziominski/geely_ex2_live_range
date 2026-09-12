package com.geely.ex2.range.ui.dashboard

import androidx.compose.runtime.Immutable
import com.geely.ex2.range.domain.engine.EngineView
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowStatus
import kotlin.math.abs

/**
 * Презентационные правила главного экрана.
 *
 * Здесь нет расчётов: только выбор того, какое из уже посчитанных движком
 * значений показать крупно и какой подписью его сопроводить.
 */

@Immutable
data class HeroRange(
    val km: Double?,
    val caption: String,
    /**
     * Оценка головного устройства — показывается отдельной строкой под крупным числом со
     * сравнением (см. [com.geely.ex2.range.ui.dashboard.RangeHero]). Null, если сравнивать не с
     * чем: либо ГУ не даёт своей оценки, либо [km] сам и есть эта оценка (нечего сравнивать с собой).
     */
    val vehicleKm: Float?,
)

/**
 * Крупный запас хода на главной — среднее по всем готовым окнам (5/15/30 км разом, не одно
 * предпочтительное), иначе оценка ГУ, иначе «нет данных».
 */
fun heroRange(engine: EngineView?): HeroRange {
    if (engine == null) return HeroRange(null, "Запас хода — нет данных", null)
    val ready = engine.windows
        .filter { it.status == WindowStatus.READY }
        .mapNotNull { it.rangeTo0Km }
    val vehicle = engine.vehicleRangeRemainingKm?.takeIf { it.isFinite() && it > 0f }
    if (ready.isNotEmpty()) {
        return HeroRange(
            km = ready.average(),
            caption = "Запас хода · среднее по окнам",
            vehicleKm = vehicle,
        )
    }
    if (vehicle != null) {
        return HeroRange(km = vehicle.toDouble(), caption = "Запас хода · оценка ГУ", vehicleKm = null)
    }
    return HeroRange(null, "Запас хода — нет данных", null)
}

/** Подсказка под расходом текущей поездки — состояния движка сохранены дословно. */
fun tripHint(engine: EngineView?): String {
    return when {
        engine == null -> "Нет данных"
        engine.charging -> "Зарядка"
        engine.waitingForDrive && engine.trip.distanceKm <= 0.0 -> "Ждём выезд"
        engine.waitingForDrive -> "Последняя поездка"
        engine.tripIncomplete -> "Поездка неполная"
        else -> "С выезда · ${DisplayFormat.km(engine.trip.distanceKm)}"
    }
}

/** Насколько прогноз близок к оценке головного устройства. */
enum class VehicleDeltaTone { CLOSE, FAR }

/** До 10 % расхождения считаем нормальным разбросом методик. */
private const val VEHICLE_DELTA_CLOSE_PERCENT = 10.0

fun vehicleDeltaTone(percent: Double?): VehicleDeltaTone {
    if (percent == null || !percent.isFinite()) return VehicleDeltaTone.CLOSE
    return if (abs(percent) <= VEHICLE_DELTA_CLOSE_PERCENT) VehicleDeltaTone.CLOSE else VehicleDeltaTone.FAR
}

/** Доля набранного окна для индикатора прогресса. */
fun windowFilledFraction(window: RangeWindow): Float {
    val remaining = window.remainingToFillKm ?: return 1f
    if (window.windowKm <= 0.0) return 0f
    return ((window.windowKm - remaining) / window.windowKm).toFloat().coerceIn(0f, 1f)
}
