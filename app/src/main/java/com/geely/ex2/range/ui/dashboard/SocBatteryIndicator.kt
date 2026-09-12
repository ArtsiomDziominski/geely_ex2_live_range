package com.geely.ex2.range.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.ui.theme.RangeThemeColors

private const val SEGMENT_COUNT = 4

/**
 * Компактная горизонтальная шкала SOC.
 *
 * Сегментов по-прежнему четыре, а последний заполненный краснеет ниже резерва —
 * логика индикации сохранена, изменились только размеры и подача.
 */
@Composable
fun SocBatteryIndicator(
    socPercent: Float?,
    charging: Boolean,
    modifier: Modifier = Modifier,
    reserveSocPercent: Double = RangeConstants.RESERVE_SOC_PERCENT,
    height: Dp = 14.dp,
) {
    val extra = RangeThemeColors.extra
    val track = MaterialTheme.colorScheme.surfaceVariant
    val reserveMark = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val level = socPercent?.takeIf { it.isFinite() }?.coerceIn(0f, 100f)
    val reserve = reserveSocPercent.toFloat()
    val low = level != null && level < reserve

    val animatedLevel by animateFloatAsState(
        targetValue = level ?: 0f,
        animationSpec = tween(durationMillis = 280),
        label = "soc-level",
    )
    val fillColor by animateColorAsState(
        targetValue = when {
            level == null -> track
            charging -> extra.charging
            low -> extra.lowSoc
            else -> extra.charging
        },
        animationSpec = tween(durationMillis = 240),
        label = "soc-color",
    )

    val description = when {
        level == null -> "Заряд батареи: нет данных"
        charging -> "Заряд батареи ${DisplayFormat.socNumber(level)} процентов, идёт зарядка"
        low -> "Заряд батареи ${DisplayFormat.socNumber(level)} процентов, ниже резерва"
        else -> "Заряд батареи ${DisplayFormat.socNumber(level)} процентов"
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description },
    ) {
        val radius = size.height / 2f
        val tipW = size.height * 0.32f
        val gap = size.height * 0.22f
        val bodyW = (size.width - tipW - gap).coerceAtLeast(1f)
        val body = RoundRect(Rect(0f, 0f, bodyW, size.height), CornerRadius(radius, radius))
        val bodyPath = Path().apply { addRoundRect(body) }

        drawPath(bodyPath, color = track)

        if (animatedLevel > 0f) {
            val inset = size.height * 0.12f
            val innerLeft = inset
            val innerTop = inset
            val innerW = (bodyW - inset * 2f).coerceAtLeast(0f)
            val innerH = (size.height - inset * 2f).coerceAtLeast(0f)
            val segGap = innerH * 0.22f
            val segW = (innerW - segGap * (SEGMENT_COUNT - 1)) / SEGMENT_COUNT
            val filled = animatedLevel / 100f * SEGMENT_COUNT
            val lastFilledIndex = (filled - 1e-5f).toInt().coerceIn(0, SEGMENT_COUNT - 1)

            clipPath(bodyPath, ClipOp.Intersect) {
                for (i in 0 until SEGMENT_COUNT) {
                    val segFill = (filled - i).coerceIn(0f, 1f)
                    if (segFill <= 0f) continue
                    val x = innerLeft + i * (segW + segGap)
                    val segmentColor = if (i == lastFilledIndex && low) extra.lowSoc else fillColor
                    drawRoundRect(
                        color = segmentColor,
                        topLeft = Offset(x, innerTop),
                        size = Size(segW * segFill, innerH),
                        cornerRadius = CornerRadius(innerH * 0.35f, innerH * 0.35f),
                    )
                }
            }
        }

        // Отметка резерва (20 % по умолчанию).
        val reserveX = bodyW * (reserve / 100f)
        drawLine(
            color = reserveMark,
            start = Offset(reserveX, 0f),
            end = Offset(reserveX, size.height),
            strokeWidth = size.height * 0.14f,
        )

        drawRoundRect(
            color = track,
            topLeft = Offset(bodyW + gap, size.height * 0.28f),
            size = Size(tipW, size.height * 0.44f),
            cornerRadius = CornerRadius(tipW * 0.5f, tipW * 0.5f),
        )
    }
}
