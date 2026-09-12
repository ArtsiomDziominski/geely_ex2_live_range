package com.geely.ex2.range.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Классы ширины окна по Material 3 window size classes. */
enum class RangeWidthClass {
    /** Телефон в портрете (< 600 dp). */
    COMPACT,

    /** Телефон в ландшафте / небольшой планшет (600…839 dp). */
    MEDIUM,

    /** Планшет и головное устройство авто (>= 840 dp). */
    EXPANDED,
}

/**
 * Адаптивные параметры разметки. Высоты блоков нигде не фиксируются в процентах —
 * от класса окна зависят только колоночность, отступы и размер графика.
 */
@Immutable
data class RangeLayoutSpec(
    val widthClass: RangeWidthClass,
    val widthDp: Int,
    val heightDp: Int,
    val isLandscape: Boolean,
    /** Экран низкий (телефон в ландшафте) — прячем необязательный «воздух». */
    val isShort: Boolean,
    /** Навигация сбоку (rail) вместо нижней панели. */
    val useNavigationRail: Boolean,
    /** Главная в две колонки. */
    val twoPane: Boolean,
    val screenPadding: Dp,
    val sectionGap: Dp,
    val chartHeight: Dp,
) {
    val isCompact: Boolean get() = widthClass == RangeWidthClass.COMPACT
}

private val DefaultLayoutSpec = RangeLayoutSpec(
    widthClass = RangeWidthClass.COMPACT,
    widthDp = 411,
    heightDp = 891,
    isLandscape = false,
    isShort = false,
    useNavigationRail = false,
    twoPane = false,
    screenPadding = 16.dp,
    sectionGap = 12.dp,
    chartHeight = 180.dp,
)

val LocalRangeLayout = staticCompositionLocalOf { DefaultLayoutSpec }

/**
 * Очень широкие окна — это головное устройство авто и планшеты: экран большой,
 * но смотрят на него с расстояния вытянутой руки. Плотность слегка увеличивается,
 * чтобы одна и та же вёрстка в dp читалась физически крупнее.
 */
private const val LARGE_SCREEN_MIN_WIDTH_DP = 1200
private const val LARGE_SCREEN_UI_SCALE = 1.2f

@Composable
fun ProvideRangeLayout(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val uiScale = if (configuration.screenWidthDp >= LARGE_SCREEN_MIN_WIDTH_DP) {
        LARGE_SCREEN_UI_SCALE
    } else {
        1f
    }
    val widthDp = (configuration.screenWidthDp / uiScale).toInt()
    val heightDp = (configuration.screenHeightDp / uiScale).toInt()
    val spec = remember(widthDp, heightDp) {
        val widthClass = when {
            widthDp < 600 -> RangeWidthClass.COMPACT
            widthDp < 840 -> RangeWidthClass.MEDIUM
            else -> RangeWidthClass.EXPANDED
        }
        val landscape = widthDp > heightDp
        val short = heightDp < 480
        RangeLayoutSpec(
            widthClass = widthClass,
            widthDp = widthDp,
            heightDp = heightDp,
            isLandscape = landscape,
            isShort = short,
            useNavigationRail = widthClass == RangeWidthClass.EXPANDED,
            twoPane = widthClass == RangeWidthClass.EXPANDED,
            screenPadding = when (widthClass) {
                RangeWidthClass.COMPACT -> 16.dp
                RangeWidthClass.MEDIUM -> 20.dp
                RangeWidthClass.EXPANDED -> 24.dp
            },
            sectionGap = if (widthClass == RangeWidthClass.COMPACT) 12.dp else 16.dp,
            chartHeight = when {
                short -> 150.dp
                widthClass == RangeWidthClass.COMPACT -> 180.dp
                else -> 220.dp
            },
        )
    }
    val scaledDensity = remember(density, uiScale) {
        if (uiScale == 1f) density else Density(density.density * uiScale, density.fontScale)
    }
    CompositionLocalProvider(
        LocalRangeLayout provides spec,
        LocalDensity provides scaledDensity,
        content = content,
    )
}
