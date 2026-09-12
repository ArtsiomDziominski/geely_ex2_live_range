package com.geely.ex2.range.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.WindowChartPoint
import com.geely.ex2.range.ui.theme.RangeThemeColors
import com.geely.ex2.range.ui.theme.Spacing
import kotlin.math.abs

/**
 * Спарклайн одного окна прогноза: SOC, скорость и температура по километрам.
 *
 * Нажатие по графику выбирает ближайшую точку и показывает её значения
 * в карточке над графиком; повторное нажатие снимает выбор.
 */
@Composable
fun WindowPaceChart(
    windowKm: Double,
    points: List<WindowChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    /**
     * Верхняя граница оси X. Пока окно не набрано, каллер передаёт пройденное
     * расстояние — иначе данные сжимаются в узкую полоску у левого края.
     */
    xMaxKm: Double = windowKm,
) {
    val extra = RangeThemeColors.extra
    val grid = MaterialTheme.colorScheme.outlineVariant
    val crosshair = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val surface = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current

    val showSpeed = points.any { it.speedKmh != null }
    val showTemp = points.any { it.outsideTempC != null }
    val socRange = remember(points) { socYRange(points) }
    val reserveVisible = RangeConstants.RESERVE_SOC_PERCENT.toFloat() in socRange.first..socRange.second

    var selectedIndex by remember(points.size, windowKm) { mutableStateOf<Int?>(null) }
    val selected = selectedIndex?.let { points.getOrNull(it) }

    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
            label = "chart-header",
        ) { point ->
            if (point == null) {
                ChartLegend(showSpeed = showSpeed, showTemp = showTemp, reserveVisible = reserveVisible)
            } else {
                SelectedPointCard(point)
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .semantics {
                    contentDescription = "График окна " + DisplayFormat.windowKmLabel(windowKm) +
                        ": SOC, скорость и температура. Нажмите, чтобы выбрать точку."
                }
                .pointerInput(points, windowKm) {
                    detectTapGestures { tap ->
                        if (points.size < 2) {
                            selectedIndex = null
                            return@detectTapGestures
                        }
                        val padH = with(density) { 8.dp.toPx() }
                        val plotW = (size.width - padH * 2).coerceAtLeast(1f)
                        val xMax = xMaxKm.toFloat().coerceAtLeast(0.1f)
                        val kmAtTap = ((tap.x - padH) / plotW).coerceIn(0f, 1f) * xMax
                        val nearest = points.indices.minByOrNull { abs(points[it].km - kmAtTap) }
                        selectedIndex = if (nearest != null && nearest == selectedIndex) null else nearest
                    }
                },
        ) {
            val padH = 8.dp.toPx()
            val padT = 10.dp.toPx()
            val padB = 10.dp.toPx()
            val plotW = (size.width - padH * 2).coerceAtLeast(1f)
            val plotH = (size.height - padT - padB).coerceAtLeast(1f)
            val originX = padH
            val originY = padT + plotH

            drawLine(
                grid,
                Offset(originX, originY),
                Offset(originX + plotW, originY),
                strokeWidth = 1.dp.toPx(),
            )

            if (points.size < 2) return@Canvas

            val xMax = xMaxKm.toFloat().coerceAtLeast(0.1f)
            fun xOf(km: Float): Float = originX + (km / xMax).coerceIn(0f, 1f) * plotW

            val speedRange = speedYRange(points)
            val tempRange = tempYRange(points)
            fun ySoc(value: Float): Float = normalizeY(value, socRange, originY, plotH)
            fun ySpeed(value: Float): Float = normalizeY(value, speedRange, originY, plotH)
            fun yTemp(value: Float): Float = normalizeY(value, tempRange, originY, plotH)

            if (reserveVisible) {
                val y = ySoc(RangeConstants.RESERVE_SOC_PERCENT.toFloat())
                drawLine(
                    extra.warning,
                    Offset(originX, y),
                    Offset(originX + plotW, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(6.dp.toPx(), 5.dp.toPx()),
                        0f,
                    ),
                )
            }

            drawSocArea(points, ::xOf, ::ySoc, originY, extra.chartSoc)

            if (showSpeed) {
                drawSeriesLine(points, ::xOf, { it.speedKmh?.let(::ySpeed) }, extra.chartSpeed, 2.5.dp.toPx())
            }
            if (showTemp) {
                drawSeriesLine(points, ::xOf, { it.outsideTempC?.let(::yTemp) }, extra.chartTemp, 2.5.dp.toPx())
            }
            drawSeriesLine(points, ::xOf, { ySoc(it.soc) }, extra.chartSoc, 3.5.dp.toPx())

            val focus = selected ?: points.last()
            val focusX = xOf(focus.km)
            if (selected != null) {
                drawLine(
                    crosshair,
                    Offset(focusX, padT),
                    Offset(focusX, originY),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
            val dotRadius = if (selected != null) 5.dp.toPx() else 4.dp.toPx()
            focus.speedKmh?.let {
                drawFocusDot(Offset(focusX, ySpeed(it)), extra.chartSpeed, surface, dotRadius, selected != null)
            }
            focus.outsideTempC?.let {
                drawFocusDot(Offset(focusX, yTemp(it)), extra.chartTemp, surface, dotRadius, selected != null)
            }
            drawFocusDot(Offset(focusX, ySoc(focus.soc)), extra.chartSoc, surface, dotRadius, selected != null)
        }
    }
}

@Composable
private fun ChartLegend(
    showSpeed: Boolean,
    showTemp: Boolean,
    reserveVisible: Boolean,
) {
    val extra = RangeThemeColors.extra
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        LegendDot("SOC", extra.chartSoc)
        if (showSpeed) LegendDot("Скорость", extra.chartSpeed)
        if (showTemp) LegendDot("t°", extra.chartTemp)
        if (reserveVisible) LegendDot("20%", extra.warning)
        Spacer(Modifier.weight(1f))
        Text(
            "нажмите на график",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(Spacing.xxs))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectedPointCard(point: WindowChartPoint) {
    val extra = RangeThemeColors.extra
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
            .padding(horizontal = Spacing.s, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SelectedValue("Расстояние", DisplayFormat.km(point.km.toDouble()), MaterialTheme.colorScheme.onSurface)
        SelectedValue(
            "Скорость",
            point.speedKmh?.let { DisplayFormat.speedNumber(it) + " км/ч" } ?: "— км/ч",
            extra.chartSpeed,
        )
        SelectedValue("SOC", DisplayFormat.socPercent(point.soc), extra.chartSoc)
        SelectedValue(
            "t°",
            point.outsideTempC?.let { DisplayFormat.tempNumber(it) + " °C" } ?: "— °C",
            extra.chartTemp,
        )
    }
}

@Composable
private fun SelectedValue(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

private fun DrawScope.drawFocusDot(
    center: Offset,
    color: Color,
    ring: Color,
    radius: Float,
    selected: Boolean,
) {
    if (selected) {
        drawCircle(color = ring, radius = radius * 1.5f, center = center)
    }
    drawCircle(color = color, radius = radius, center = center)
}

private fun normalizeY(value: Float, range: Pair<Float, Float>, originY: Float, plotH: Float): Float {
    val (yMin, yMax) = range
    val span = (yMax - yMin).coerceAtLeast(1f)
    return originY - ((value - yMin) / span).coerceIn(0f, 1f) * plotH
}

private fun DrawScope.drawSeriesLine(
    points: List<WindowChartPoint>,
    xOf: (Float) -> Float,
    yOf: (WindowChartPoint) -> Float?,
    color: Color,
    strokeWidth: Float,
) {
    val path = Path()
    var started = false
    var drew = false
    points.forEach { point ->
        val y = yOf(point)
        if (y == null) {
            started = false
            return@forEach
        }
        val x = xOf(point.km)
        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
            drew = true
        }
    }
    if (!drew) return
    drawPath(
        path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/** Мягкая заливка под линией SOC — помогает считывать главную серию. */
private fun DrawScope.drawSocArea(
    points: List<WindowChartPoint>,
    xOf: (Float) -> Float,
    yOf: (Float) -> Float,
    baselineY: Float,
    color: Color,
) {
    if (points.size < 2) return
    val path = Path()
    path.moveTo(xOf(points.first().km), baselineY)
    points.forEach { point -> path.lineTo(xOf(point.km), yOf(point.soc)) }
    path.lineTo(xOf(points.last().km), baselineY)
    path.close()
    drawPath(
        path,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.02f)),
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

private fun tempYRange(points: List<WindowChartPoint>): Pair<Float, Float> {
    val temps = points.mapNotNull { it.outsideTempC?.takeIf { temp -> temp.isFinite() } }
    if (temps.isEmpty()) return -10f to 30f
    val minTemp = temps.min()
    val maxTemp = temps.max()
    val span = (maxTemp - minTemp).coerceAtLeast(4f)
    return (minTemp - span * 0.2f) to (maxTemp + span * 0.2f)
}
