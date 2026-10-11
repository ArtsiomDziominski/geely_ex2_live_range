package com.geely.ex2.range.ui.dashboard.styles

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.geely.ex2.range.domain.model.WindowChartPoint
import com.geely.ex2.range.ui.dashboard.VehicleDeltaTone
import com.geely.ex2.range.ui.dashboard.WindowPaceChart
import com.geely.ex2.range.ui.theme.RangeThemeColors
import kotlin.math.min

/** Узкое начертание системного шрифта — для приборных чисел «Кокпита». */
internal val CondensedFamily: FontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))

/** Плотность приложения снаружи [FitToDesign] — для вставок, свёрстанных в обычных dp/sp. */
internal val LocalAppDensity = staticCompositionLocalOf { Density(1f) }

/**
 * Раскладки оформлений свёрстаны под экран ГУ в «пикселях макета» и не прокручиваются.
 * Здесь плотность подбирается так, чтобы макет [designWidth]×[designHeight] целиком помещался
 * в отведённое место: 1 dp внутри — это один пиксель макета. Лишнее место по второй оси
 * раскладки добирают весами. Системный масштаб шрифта внутри не действует: текст растёт вместе
 * с окном, иначе вёрстка без прокрутки обрезалась бы.
 */
@Composable
internal fun FitToDesign(
    designWidth: Float,
    designHeight: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val appDensity = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val scale = min(constraints.maxWidth / designWidth, constraints.maxHeight / designHeight)
        val fitted = remember(scale) { Density(density = scale, fontScale = 1f) }
        CompositionLocalProvider(
            LocalDensity provides fitted,
            LocalAppDensity provides appDensity,
        ) {
            content()
        }
    }
}

private val TightLines = TextStyle(
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** Строка текста в единицах макета. */
@Composable
internal fun Txt(
    text: String,
    size: Int,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Medium,
    family: FontFamily? = null,
    caps: Boolean = false,
    align: TextAlign? = null,
    maxLines: Int = 1,
) {
    Text(
        text = if (caps) text.uppercase() else text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        fontFamily = family,
        letterSpacing = if (caps) 0.1.em else 0.em,
        textAlign = align,
        lineHeight = (size * 1.18f).sp,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        overflow = TextOverflow.Ellipsis,
        style = TightLines,
    )
}

/** Крупное число с единицей на одной базовой линии. */
@Composable
internal fun Figure(
    value: String,
    unit: String?,
    size: Int,
    unitSize: Int,
    color: Color,
    unitColor: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.SemiBold,
    family: FontFamily? = null,
    gap: Int = 10,
    prefix: String? = null,
    unitWeight: FontWeight = FontWeight.Medium,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(gap.dp)) {
        if (prefix != null) {
            Txt(prefix, unitSize, unitColor, Modifier.alignByBaseline(), weight = unitWeight, family = family)
        }
        Txt(value, size, color, Modifier.alignByBaseline(), weight = weight, family = family)
        if (unit != null) {
            Txt(unit, unitSize, unitColor, Modifier.alignByBaseline(), weight = unitWeight)
        }
    }
}

@Composable
internal fun StyleIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    size: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    container: Color = Color.Transparent,
    outline: Color? = null,
    shape: Shape = CircleShape,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(shape)
            .background(container)
            .then(if (outline != null) Modifier.border(1.5.dp, outline, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size((size * 0.44f).dp))
    }
}

/**
 * График выбранного окна — тот же, что в классическом оформлении, с легендой и выбором точки.
 * Он свёрстан в обычных dp/sp, поэтому рисуется в плотности приложения; [height] — высота
 * самого полотна в единицах макета.
 */
@Composable
internal fun StyleChart(window: WindowUi, height: Int, modifier: Modifier = Modifier) {
    if (window.chart.size < 2) return
    val appDensity = LocalAppDensity.current
    val heightPx = with(LocalDensity.current) { height.dp.toPx() }
    CompositionLocalProvider(LocalDensity provides appDensity) {
        WindowPaceChart(
            windowKm = window.windowKm,
            points = window.chart,
            modifier = modifier,
            height = with(appDensity) { heightPx.toDp() },
            xMaxKm = window.chartMaxKm,
        )
    }
}

/** Линия SOC по километрам окна без осей и подписей — для карточек, где важна только форма. */
@Composable
internal fun Sparkline(points: List<WindowChartPoint>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val minKm = points.minOf { it.km }
        val maxKm = points.maxOf { it.km }
        val minSoc = points.minOf { it.soc }
        val maxSoc = points.maxOf { it.soc }
        val kmSpan = (maxKm - minKm).coerceAtLeast(0.01f)
        // Почти ровный SOC не растягиваем на всю высоту — иначе шум выглядит как обрыв.
        val socSpan = (maxSoc - minSoc).coerceAtLeast(0.5f)
        val stroke = 3.dp.toPx()
        val plotHeight = size.height - stroke
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = (point.km - minKm) / kmSpan * size.width
            val y = stroke / 2 + (maxSoc - point.soc) / socSpan * plotHeight
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Блок вместо прогноза, когда окно не готово: что с ним и что делать. */
@Composable
internal fun WindowStatusNote(
    window: WindowUi,
    titleColor: Color,
    hintColor: Color,
    modifier: Modifier = Modifier,
    titleSize: Int = 30,
    hintSize: Int = 21,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Txt(window.status.orEmpty(), titleSize, titleColor, weight = FontWeight.SemiBold, align = TextAlign.Center)
        Txt(window.statusHint.orEmpty(), hintSize, hintColor, align = TextAlign.Center, maxLines = 2)
    }
}

/** Цвет расхождения с оценкой ГУ — те же три тона, что в классическом блоке прогноза. */
@Composable
internal fun deltaColor(tone: VehicleDeltaTone?, fallback: Color): Color {
    val extra = RangeThemeColors.extra
    return when (tone) {
        VehicleDeltaTone.ABOVE -> extra.success
        VehicleDeltaTone.BELOW -> extra.warning
        VehicleDeltaTone.FAR_BELOW -> extra.danger
        null -> fallback
    }
}

/** Выравнивание отметки на полосе: [fraction] её длины слева направо, по высоте — по центру. */
internal fun fractionBias(fraction: Float): Alignment = Alignment { size, space, _ ->
    IntOffset(
        x = ((space.width - size.width) * fraction.coerceIn(0f, 1f)).toInt(),
        y = (space.height - size.height) / 2,
    )
}
