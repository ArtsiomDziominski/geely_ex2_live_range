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
     * Среднее по готовым окнам прогноза — есть всегда, когда хоть одно окно готово (равно [km] в
     * этом случае, так как прогноз и есть крупное число). Null, если ни одно окно ещё не готово.
     * Используется для [kmUntilLowSoc].
     */
    val forecastAverageKm: Double?,
)

/**
 * Крупный запас хода на главной — среднее по всем готовым окнам прогноза (одно окно — само его
 * значение, несколько — среднее между ними), иначе оценка головного устройства, иначе «нет данных».
 */
fun heroRange(engine: EngineView?): HeroRange {
    if (engine == null) return HeroRange(null, "Запас хода — нет данных", null)
    val ready = engine.windows
        .filter { it.status == WindowStatus.READY }
        .mapNotNull { it.rangeTo0Km }
    if (ready.isNotEmpty()) {
        val average = ready.average()
        return HeroRange(km = average, caption = "Запас хода · среднее по окнам", forecastAverageKm = average)
    }
    val vehicle = engine.vehicleRangeRemainingKm?.takeIf { it.isFinite() && it > 0f }
    if (vehicle != null) {
        return HeroRange(km = vehicle.toDouble(), caption = "Запас хода · оценка ГУ", forecastAverageKm = null)
    }
    return HeroRange(null, "Запас хода — нет данных", null)
}

/**
 * Сколько км хода останется, когда заряд дойдёт до предупреждающего порога
 * ([RangeConstants.RESERVE_SOC_PERCENT] — после него индикатор батареи желтеет/краснеет).
 * Считаем линейно от среднего по прогнозу ([HeroRange.forecastAverageKm], а не от оценки ГУ —
 * это наш собственный расчёт км на % SOC, поэтому и порог по SOC масштабируем через него)
 * и текущего SOC. Null, если прогноз ещё не готов или заряд уже на пороге/ниже.
 */
fun kmUntilLowSoc(hero: HeroRange, socPercent: Float?): Double? {
    val km = hero.forecastAverageKm?.takeIf { it.isFinite() && it > 0.0 } ?: return null
    val soc = socPercent?.takeIf { it.isFinite() }?.toDouble() ?: return null
    val threshold = RangeConstants.RESERVE_SOC_PERCENT
    if (soc <= threshold) return null
    return km * (soc - threshold) / soc
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
