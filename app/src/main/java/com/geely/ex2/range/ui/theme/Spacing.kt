package com.geely.ex2.range.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Единая шкала отступов приложения (Material 3, шаг 4 dp).
 * Используется вместо «магических» значений в разметке.
 */
@Immutable
object Spacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val s: Dp = 12.dp
    val m: Dp = 16.dp
    val l: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp

    /** Горизонтальные поля контента экрана. */
    val screenH: Dp = 16.dp

    /** Минимальная зона нажатия по гайдлайнам доступности. */
    val touchTarget: Dp = 48.dp
}
