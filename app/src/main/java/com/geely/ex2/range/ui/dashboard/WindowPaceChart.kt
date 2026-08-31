package com.geely.ex2.range.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.WindowChartPoint
import com.geely.ex2.range.domain.model.WindowStatus
import com.geely.ex2.range.ui.theme.RangeThemeColors

/**
 * Native Compose sparkline for one range window: SOC and speed vs km on one plot.
 */
@Composable
fun WindowPaceChart(
    windowKm: Double,
    status: WindowStatus,
    points: List<WindowChartPoint>,
    remainingToFillKm: Double?,
    modifier: Modifier = Modifier,
) {
    val socLine = MaterialTheme.colorScheme.primary
    val speedLine = RangeThemeColors.extra.lowSoc
    val grid = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val reserve = RangeThemeColors.extra.warning
    val showSpeed = points.any { it.speedKmh != null }
    val coveredFraction = when {
        status != WindowStatus.NEED_MORE_KM -> 1f
        remainingToFillKm == null -> 0f
        else -> ((windowKm - remainingToFillKm) / windowKm).toFloat().coerceIn(0f, 1f)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val padL = 6.dp.toPx()
        val padR = 6.dp.toPx()
        val padT = 8.dp.toPx()
        val padB = 8.dp.toPx()
        val plotW = (size.width - padL - padR).coerceAtLeast(1f)
        val plotH = (size.height - padT - padB).coerceAtLeast(1f)
        val originX = padL
        val originY = padT + plotH

        drawLine(grid, Offset(originX, originY), Offset(originX + plotW, originY), strokeWidth = 1.5f)

        if (points.size < 2) {
            drawLine(
                muted,
                Offset(originX, padT + plotH * 0.55f),
                Offset(originX + plotW * coveredFraction.coerceAtLeast(0.08f), padT + plotH * 0.55f),
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )
        } else {
            val xMax = windowKm.toFloat().coerceAtLeast(points.maxOf { it.km }.coerceAtLeast(0.1f))
            fun xOf(km: Float): Float = originX + (km / xMax).coerceIn(0f, 1f) * plotW

            val socRange = socYRange(points)
            fun ySoc(value: Float): Float = normalizeY(value, socRange, originY, plotH)

            val speedRange = speedYRange(points)
            fun ySpeed(value: Float): Float = normalizeY(value, speedRange, originY, plotH)

            val reserveY = RangeConstants.RESERVE_SOC_PERCENT.toFloat()
            if (reserveY in socRange.first..socRange.second) {
                drawLine(
                    reserve,
                    Offset(originX, ySoc(reserveY)),
                    Offset(originX + plotW, ySoc(reserveY)),
                    strokeWidth = 3f,
                )
            }

            if (showSpeed) {
                drawSeriesLine(
                    points = points,
                    xOf = ::xOf,
                    yOf = { ySpeed(it.speedKmh ?: 0f) },
                    color = speedLine,
                    strokeWidth = 5f,
                )
            }
            drawSeriesLine(
                points = points,
                xOf = ::xOf,
                yOf = { ySoc(it.soc) },
                color = socLine,
                strokeWidth = 7f,
            )

            val last = points.last()
            drawCircle(color = socLine, radius = 8f, center = Offset(xOf(last.km), ySoc(last.soc)))
            if (last.speedKmh != null) {
                drawCircle(
                    color = speedLine,
                    radius = 7f,
                    center = Offset(xOf(last.km), ySpeed(last.speedKmh)),
                )
            }
        }
    }
}

private fun normalizeY(value: Float, range: Pair<Float, Float>, originY: Float, plotH: Float): Float {
    val (yMin, yMax) = range
    val span = (yMax - yMin).coerceAtLeast(1f)
    return originY - ((value - yMin) / span).coerceIn(0f, 1f) * plotH
}

private fun DrawScope.drawSeriesLine(
    points: List<WindowChartPoint>,
    xOf: (Float) -> Float,
    yOf: (WindowChartPoint) -> Float,
    color: Color,
    strokeWidth: Float,
) {
    val path = Path()
    points.forEachIndexed { index, point ->
        val x = xOf(point.km)
        val y = yOf(point)
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

private fun socYRange(points: List<WindowChartPoint>): Pair<Float, Float> {
    if (points.isEmpty()) return 0f to 100f
    val minSoc = points.minOf { it.soc }
    val maxSoc = points.maxOf { it.soc }
    val span = (maxSoc - minSoc).coerceAtLeast(2f)
    return (minSoc - span * 0.15f) to (maxSoc + span * 0.15f)
}

private fun speedYRange(points: List<WindowChartPoint>): Pair<Float, Float> {
    if (points.isEmpty()) return 0f to 100f
    val speeds = points.mapNotNull { it.speedKmh?.takeIf { speed -> speed.isFinite() && speed >= 0f } }
    if (speeds.isEmpty()) return 0f to 100f
    val maxSpeed = speeds.maxOrNull() ?: 0f
    val span = maxSpeed.coerceAtLeast(20f)
    return 0f to (span * 1.15f)
}
