package com.geely.ex2.range.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.geely.ex2.range.domain.model.AppStyle

/**
 * Палитра одного оформления ([AppStyle]) в одной теме. Из неё же собирается цветовая схема
 * Material для остальных экранов — см. [RangeTheme].
 */
@Immutable
data class StyleColors(
    val background: Color,
    /** Нижний правый цвет фона. Отличается от [background] только там, где фон с переходом. */
    val backgroundEnd: Color,
    val panel: Color,
    /** Вложенная плашка внутри [panel]. */
    val inset: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    /** Акцент для заливок: кольцо шкалы, выбранный сегмент, активный пункт меню. */
    val accent: Color,
    val onAccent: Color,
    /** Акцент для текста и линий по фону — в светлых темах темнее [accent], чтобы читался. */
    val accentText: Color,
    val reserve: Color,
    val track: Color,
    /** Линия заряда на графике окна. По умолчанию акцент; задаётся отдельно, где он совпал бы с t°. */
    val chartSoc: Color = accentText,
)

/** Палитра оформления; null у [AppStyle.CLASSIC] — он живёт на базовой схеме Material. */
fun styleColors(style: AppStyle, dark: Boolean): StyleColors? = when (style) {
    AppStyle.CLASSIC -> null
    AppStyle.COCKPIT -> if (dark) CockpitDark else CockpitLight
    AppStyle.NORDIC -> if (dark) NordicDark else NordicLight
    AppStyle.BENTO -> if (dark) BentoDark else BentoLight
    AppStyle.NATIVE -> if (dark) NativeDark else NativeLight
}

val LocalStyleColors = staticCompositionLocalOf<StyleColors?> { null }

private val CockpitDark = StyleColors(
    background = Color(0xFF0A0D10),
    backgroundEnd = Color(0xFF0A0D10),
    panel = Color(0xFF11161B),
    inset = Color(0xFF0A0D10),
    line = Color(0xFF212A33),
    text = Color(0xFFEAF0F5),
    muted = Color(0xFF9BA7B3),
    accent = Color(0xFF5CC8FF),
    onAccent = Color(0xFF04121D),
    accentText = Color(0xFF5CC8FF),
    reserve = Color(0xFFFFB454),
    track = Color(0xFF1C252E),
)

private val CockpitLight = StyleColors(
    background = Color(0xFFEEF1F4),
    backgroundEnd = Color(0xFFEEF1F4),
    panel = Color(0xFFFFFFFF),
    inset = Color(0xFFF1F4F7),
    line = Color(0xFFD5DBE2),
    text = Color(0xFF0E141A),
    muted = Color(0xFF55616D),
    accent = Color(0xFF0A84D6),
    onAccent = Color(0xFFFFFFFF),
    accentText = Color(0xFF0669B0),
    reserve = Color(0xFFC06A00),
    track = Color(0xFFDCE2E9),
)

private val NordicLight = StyleColors(
    background = Color(0xFFECEEF0),
    backgroundEnd = Color(0xFFECEEF0),
    panel = Color(0xFFF6F7F8),
    inset = Color(0xFFE2E5E8),
    line = Color(0xFFC5CAD0),
    text = Color(0xFF0E1114),
    muted = Color(0xFF4B545D),
    accent = Color(0xFFE8590C),
    onAccent = Color(0xFFFFFFFF),
    accentText = Color(0xFFA33B08),
    reserve = Color(0xFFE8590C),
    track = Color(0xFFC5CAD0),
    chartSoc = Color(0xFF0E1114),
)

private val NordicDark = StyleColors(
    background = Color(0xFF0F1113),
    backgroundEnd = Color(0xFF0F1113),
    panel = Color(0xFF16191C),
    inset = Color(0xFF1D2125),
    line = Color(0xFF2B3036),
    text = Color(0xFFECEEF0),
    muted = Color(0xFFA0A8B0),
    accent = Color(0xFFFF7A33),
    onAccent = Color(0xFF1A0E06),
    accentText = Color(0xFFFF8A4A),
    reserve = Color(0xFFFF7A33),
    track = Color(0xFF2B3036),
    chartSoc = Color(0xFFECEEF0),
)

private val BentoDark = StyleColors(
    background = Color(0xFF0D0F0E),
    backgroundEnd = Color(0xFF0D0F0E),
    panel = Color(0xFF181B19),
    inset = Color(0xFF222622),
    line = Color(0xFF2C322D),
    text = Color(0xFFF1F4EE),
    muted = Color(0xFFA3AB9F),
    accent = Color(0xFFC8F03C),
    onAccent = Color(0xFF10130A),
    accentText = Color(0xFFC8F03C),
    reserve = Color(0xFFFFAA33),
    track = Color(0xFF262B27),
)

private val BentoLight = StyleColors(
    background = Color(0xFFE9ECE6),
    backgroundEnd = Color(0xFFE9ECE6),
    panel = Color(0xFFFFFFFF),
    inset = Color(0xFFF1F3EE),
    line = Color(0xFFD9DDD4),
    text = Color(0xFF10130A),
    muted = Color(0xFF565D52),
    accent = Color(0xFFC8F03C),
    onAccent = Color(0xFF10130A),
    accentText = Color(0xFF4A6B00),
    reserve = Color(0xFFD97A00),
    track = Color(0xFFE3E7DF),
)

private val NativeDark = StyleColors(
    background = Color(0xFF223044),
    backgroundEnd = Color(0xFF101822),
    panel = Color(0xFF0F1622),
    inset = Color(0xFF1B2636),
    line = Color(0xFF2A384C),
    text = Color(0xFFF2F6FA),
    muted = Color(0xFF9AA8B8),
    accent = Color(0xFF20B8F2),
    onAccent = Color(0xFF04121D),
    accentText = Color(0xFF20B8F2),
    reserve = Color(0xFFFFB454),
    track = Color(0xFF1C2838),
)

private val NativeLight = StyleColors(
    background = Color(0xFFF4F6FA),
    backgroundEnd = Color(0xFFE4E9F0),
    panel = Color(0xFFFFFFFF),
    inset = Color(0xFFEDF1F6),
    line = Color(0xFFD3DAE3),
    text = Color(0xFF101820),
    muted = Color(0xFF5A6675),
    accent = Color(0xFF0A8FDC),
    onAccent = Color(0xFFFFFFFF),
    accentText = Color(0xFF0774B5),
    reserve = Color(0xFFC06A00),
    track = Color(0xFFDFE5EC),
)
