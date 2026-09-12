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

/** Порядок предпочтения окон для крупной цифры запаса хода. */
private val HERO_WINDOW_ORDER = listOf(
    RangeConstants.WINDOW_KM_15,
    RangeConstants.WINDOW_KM_30,
    RangeConstants.WINDOW_KM_5,
)

@Immutable
data class HeroRange(
    val km: Double?,
    val caption: String,
    val fromVehicle: Boolean,
)

/**
 * Крупный запас хода на главной.
 * Берём готовый прогноз окна (по умолчанию 15 км), иначе оценку ГУ, иначе «нет данных».
 */
fun heroRange(engine: EngineView?): HeroRange {
    if (engine == null) return HeroRange(null, "Запас хода — нет данных", false)
    val ready = engine.windows.filter { it.status == WindowStatus.READY && it.rangeTo0Km != null }
    val chosen = HERO_WINDOW_ORDER.firstNotNullOfOrNull { km ->
        ready.firstOrNull { abs(it.windowKm - km) < 1e-6 }
    }
    if (chosen != null) {
        return HeroRange(
            km = chosen.rangeTo0Km,
            caption = "Запас хода · окно ${DisplayFormat.windowKmLabel(chosen.windowKm)}",
            fromVehicle = false,
        )
    }
    val vehicle = engine.vehicleRangeRemainingKm?.takeIf { it.isFinite() && it > 0f }
    if (vehicle != null) {
        return HeroRange(km = vehicle.toDouble(), caption = "Запас хода · оценка ГУ", fromVehicle = true)
    }
    return HeroRange(null, "Запас хода — нет данных", false)
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

/** Направление уклона — словом, не только цветом. */
enum class InclineDirection(val label: String) {
    UP("Подъём"),
    DOWN("Спуск"),
    FLAT("Ровно"),
    UNKNOWN("Нет данных"),
}

fun inclineDirection(pitchDegrees: Float?): InclineDirection {
    if (pitchDegrees == null || !pitchDegrees.isFinite()) return InclineDirection.UNKNOWN
    return when {
        pitchDegrees > 0.5f -> InclineDirection.UP
        pitchDegrees < -0.5f -> InclineDirection.DOWN
        else -> InclineDirection.FLAT
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
