package com.geely.ex2.range.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.WindowChartPoint
import com.geely.ex2.range.domain.model.WindowStatus
import com.geely.ex2.range.ui.theme.RangeThemeColors

/**
 * Native Compose sparkline for one range window (SOC vs km). No chart libraries.
 */
@Composable
fun WindowPaceChart(
    windowKm: Double,
    status: WindowStatus,
    points: List<WindowChartPoint>,
    remainingToFillKm: Double?,
    modifier: Modifier = Modifier,
) {
    val line = MaterialTheme.colorScheme.primary
    val fill = line.copy(alpha = 0.22f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val reserve = RangeThemeColors.extra.warning.copy(alpha = 0.7f)
    val progressTrack = MaterialTheme.colorScheme.surfaceVariant
    val progressFill = MaterialTheme.colorScheme.primary
    val coveredFraction = when {
        status != WindowStatus.NEED_MORE_KM -> 1f
        remainingToFillKm == null -> 0f
        else -> ((windowKm - remainingToFillKm) / windowKm).toFloat().coerceIn(0f, 1f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp),
    ) {
        val padL = 6.dp.toPx()
        val padR = 6.dp.toPx()
        val padT = 8.dp.toPx()
        val padB = 14.dp.toPx()
        val plotW = (size.width - padL - padR).coerceAtLeast(1f)
        val plotH = (size.height - padT - padB).coerceAtLeast(1f)
        val originX = padL
        val originY = padT + plotH

        drawLine(grid, Offset(originX, originY), Offset(originX + plotW, originY), strokeWidth = 1.5f)
        drawLine(
            grid.copy(alpha = 0.5f),
            Offset(originX, padT + plotH * 0.5f),
            Offset(originX + plotW, padT + plotH * 0.5f),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
        )

        if (points.size < 2) {
            drawLine(
                muted,
                Offset(originX, padT + plotH * 0.55f),
                Offset(originX + plotW * coveredFraction.coerceAtLeast(0.08f), padT + plotH * 0.55f),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
        } else {
            val minSoc = points.minOf { it.soc }
            val maxSoc = points.maxOf { it.soc }
            val span = (maxSoc - minSoc).coerceAtLeast(2f)
            val yMin = minSoc - span * 0.15f
            val yMax = maxSoc + span * 0.15f
            val yRange = (yMax - yMin).coerceAtLeast(1f)
            val xMax = windowKm.toFloat().coerceAtLeast(points.maxOf { it.km }.coerceAtLeast(0.1f))

            fun xOf(km: Float): Float = originX + (km / xMax).coerceIn(0f, 1f) * plotW
            fun yOf(soc: Float): Float = originY - ((soc - yMin) / yRange).coerceIn(0f, 1f) * plotH

            val reserveY = RangeConstants.RESERVE_SOC_PERCENT.toFloat()
            if (reserveY in yMin..yMax) {
                val y = yOf(reserveY)
                drawLine(
                    reserve,
                    Offset(originX, y),
                    Offset(originX + plotW, y),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                )
            }

            val linePath = Path()
            val areaPath = Path()
            points.forEachIndexed { index, point ->
                val x = xOf(point.km)
                val y = yOf(point.soc)
                if (index == 0) {
                    linePath.moveTo(x, y)
                    areaPath.moveTo(x, originY)
                    areaPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    areaPath.lineTo(x, y)
                }
            }
            val last = points.last()
            areaPath.lineTo(xOf(last.km), originY)
            areaPath.close()

            drawPath(areaPath, color = fill)
            drawPath(
                linePath,
                color = line,
                style = Stroke(
                    width = 3.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
            drawCircle(
                color = line,
                radius = 5f,
                center = Offset(xOf(last.km), yOf(last.soc)),
            )
        }

        val barH = 4.dp.toPx()
        val barY = size.height - barH
        drawRoundRect(
            color = progressTrack,
            topLeft = Offset(originX, barY),
            size = Size(plotW, barH),
            cornerRadius = CornerRadius(barH / 2f, barH / 2f),
        )
        if (coveredFraction > 0f) {
            drawRoundRect(
                color = progressFill,
                topLeft = Offset(originX, barY),
                size = Size(plotW * coveredFraction, barH),
                cornerRadius = CornerRadius(barH / 2f, barH / 2f),
            )
        }
    }
}
