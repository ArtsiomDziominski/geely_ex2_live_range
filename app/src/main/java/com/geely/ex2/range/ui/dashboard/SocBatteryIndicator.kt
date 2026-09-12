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

private const val SEGMENT_COUNT = 5

/**
 * Компактная горизонтальная шкала SOC.
 *
 * Пять сегментов по 20 % каждый. Цвет всей шкалы: оранжевый ≤ [warningSocPercent],
 * красный < [criticalSocPercent], иначе зелёный (или цвет зарядки во время зарядки).
 */
@Composable
fun SocBatteryIndicator(
    socPercent: Float?,
    charging: Boolean,
    modifier: Modifier = Modifier,
    warningSocPercent: Double = RangeConstants.RESERVE_SOC_PERCENT,
    criticalSocPercent: Double = 10.0,
    height: Dp = 14.dp,
) {
    val extra = RangeThemeColors.extra
    val track = MaterialTheme.colorScheme.surfaceVariant
    val level = socPercent?.takeIf { it.isFinite() }?.coerceIn(0f, 100f)
    val warning = warningSocPercent.toFloat()
    val critical = criticalSocPercent.toFloat()
    val isLow = level != null && level <= warning
    val isCritical = level != null && level < critical

    val animatedLevel by animateFloatAsState(
        targetValue = level ?: 0f,
        animationSpec = tween(durationMillis = 280),
        label = "soc-level",
    )
    val fillColor by animateColorAsState(
        targetValue = when {
            level == null -> track
            charging -> extra.charging
            isCritical -> extra.lowSoc
            isLow -> extra.warning
            else -> extra.charging
        },
        animationSpec = tween(durationMillis = 240),
        label = "soc-color",
    )

    val description = when {
        level == null -> "Заряд батареи: нет данных"
        charging -> "Заряд батареи ${DisplayFormat.socNumber(level)} процентов, идёт зарядка"
        isCritical -> "Заряд батареи ${DisplayFormat.socNumber(level)} процентов, критично низкий"
        isLow -> "Заряд батареи ${DisplayFormat.socNumber(level)} процентов, ниже резерва"
        else -> "Заряд батареи ${DisplayFormat.socNumber(level)} процентов"
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description },
    ) {
        val radius = size.height / 2f
        val bodyW = size.width
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

            clipPath(bodyPath, ClipOp.Intersect) {
                for (i in 0 until SEGMENT_COUNT) {
                    val segFill = (filled - i).coerceIn(0f, 1f)
                    if (segFill <= 0f) continue
                    val x = innerLeft + i * (segW + segGap)
                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(x, innerTop),
                        size = Size(segW * segFill, innerH),
                        cornerRadius = CornerRadius(innerH * 0.35f, innerH * 0.35f),
                    )
                }
            }
        }
    }
}
