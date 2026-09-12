package com.geely.ex2.range.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val Numeric = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/**
 * Стили крупных приборных чисел.
 * Всё в sp — масштаб шрифта системы поддерживается.
 */
// Все размеры ниже — базовые значения ×1.2 (единый шаг увеличения шрифта по всему приложению).
@Immutable
object RangeTextStyles {
    /** SOC — самый крупный показатель главной. */
    val socValue = Numeric.copy(fontSize = 62.sp, lineHeight = 67.sp)

    /** SOC на низком экране (телефон в ландшафте). */
    val socValueCompact = Numeric.copy(fontSize = 48.sp, lineHeight = 53.sp)

    /** Запас хода — второй по важности. */
    val rangeValue = Numeric.copy(fontSize = 43.sp, lineHeight = 48.sp)

    /** Запас хода на низком экране. */
    val rangeValueCompact = Numeric.copy(fontSize = 34.sp, lineHeight = 38.sp)

    /** Прогноз в блоке Forecast. */
    val forecastValue = Numeric.copy(fontSize = 41.sp, lineHeight = 46.sp)

    /** Скорость / передача. */
    val metricValue = Numeric.copy(fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold)

    /** Расход, вторичные числа карточек. */
    val statValue = Numeric.copy(fontSize = 31.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold)

    /** Единицы измерения рядом с крупным числом. */
    val unit = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)

    /** Подписи под значениями. */
    val caption = TextStyle(fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.Normal)
}

val RangeTypography = Typography(
    headlineSmall = TextStyle(fontSize = 29.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 19.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium),
)
