package com.geely.ex2.range.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.OverlayEdge
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowStatus
import com.geely.ex2.range.ui.dashboard.windowFilledFraction
import com.geely.ex2.range.ui.theme.RangeThemeColors
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val PanelColor = Color.Black
private val TrackColor = Color.White.copy(alpha = 0.18f)
private val MutedText = Color.White.copy(alpha = 0.6f)

// Тонкая обводка: на светлом фоне её не видно, а на тёмном интерфейсе ГУ без неё
// чёрная панель теряет силуэт и остаются «висящие» в воздухе кольца.
private val OutlineColor = Color.White.copy(alpha = 0.12f)
private val OutlineWidth = 1.dp

private val PanelWidth = 96.dp
private val PanelCorner = 46.dp
private val PanelFlare = 40.dp
private val GaugeSize = 58.dp
private val GaugeStroke = 6.dp

private val HandleWidth = 40.dp
private val HandleBodyHeight = 76.dp
private val HandleCorner = 18.dp
private val HandleFlare = 22.dp

private val DetailsWidth = 300.dp
private val DetailsCorner = 22.dp
private val PointerWidth = 12.dp
private val PointerHeight = 26.dp
private val DetailsGap = 6.dp

/** Сколько висит карточка подробностей, если её не закрыли сами. */
private const val DETAILS_AUTO_HIDE_MS = 8_000L

/**
 * Виджет поверх окон: чёрная панель, прижатая к краю экрана, — по кольцу на окно 5 / 15 / 30 км
 * и прогноз под ним. Нажатие на кольцо открывает рядом карточку подробностей, стрелка внизу
 * прячет панель в язычок у края, нажатие на язычок возвращает её. Перетаскивание — в
 * [OverlayRootLayout], сюда доходят только нажатия.
 */
@Composable
fun RangeOverlayContent(
    windows: List<RangeWindow>,
    charging: Boolean,
    vehicleRangeRemainingKm: Float?,
    edge: OverlayEdge,
    collapsed: Boolean,
    detailsWindowKm: Double?,
    onWindowClick: (Double) -> Unit,
    onDismissDetails: () -> Unit,
    onCollapsedChange: (Boolean) -> Unit,
    onOpenApp: () -> Unit,
) {
    if (collapsed) {
        OverlayHandle(edge = edge, onClick = { onCollapsedChange(false) })
    } else {
        ExpandedOverlay(
            windows = windows,
            charging = charging,
            vehicleRangeRemainingKm = vehicleRangeRemainingKm,
            edge = edge,
            detailsWindowKm = detailsWindowKm,
            onWindowClick = onWindowClick,
            onDismissDetails = onDismissDetails,
            onCollapse = { onCollapsedChange(true) },
            onOpenApp = onOpenApp,
        )
    }
}

/** Панель у края и, если открыта, карточка подробностей со стороны экрана. */
@Composable
private fun ExpandedOverlay(
    windows: List<RangeWindow>,
    charging: Boolean,
    vehicleRangeRemainingKm: Float?,
    edge: OverlayEdge,
    detailsWindowKm: Double?,
    onWindowClick: (Double) -> Unit,
    onDismissDetails: () -> Unit,
    onCollapse: () -> Unit,
    onOpenApp: () -> Unit,
) {
    // Центры колец по вертикали — к ним карточка подробностей поворачивает свой уголок.
    // Оба значения — от корня окна; разница и есть высота кольца на панели.
    val gaugeCenters = remember { mutableStateMapOf<Double, Float>() }
    var overlayTop by remember { mutableFloatStateOf(0f) }
    Layout(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            overlayTop = coordinates.positionInRoot().y
        },
        content = {
            SidePanel(
                windows = windows,
                charging = charging,
                vehicleRangeRemainingKm = vehicleRangeRemainingKm,
                edge = edge,
                detailsWindowKm = detailsWindowKm,
                onWindowClick = onWindowClick,
                onGaugePlaced = { windowKm, centerY ->
                    if (gaugeCenters[windowKm] != centerY) gaugeCenters[windowKm] = centerY
                },
                onCollapse = onCollapse,
            )
            if (detailsWindowKm != null) {
                WindowDetails(
                    windowKm = detailsWindowKm,
                    window = windows.findWindow(detailsWindowKm),
                    charging = charging,
                    vehicleRangeRemainingKm = vehicleRangeRemainingKm,
                    edge = edge,
                    anchorY = (gaugeCenters[detailsWindowKm] ?: overlayTop) - overlayTop,
                    onOpenApp = onOpenApp,
                    onDismiss = onDismissDetails,
                )
            }
        },
    ) { measurables, constraints ->
        val panel = measurables[0].measure(Constraints())
        // Подробностям — место, что осталось рядом с панелью: на узком экране карточка ужмётся,
        // а не вылезет за окно.
        val detailsMaxWidth = if (constraints.hasBoundedWidth) {
            (constraints.maxWidth - panel.width).coerceAtLeast(0)
        } else {
            Constraints.Infinity
        }
        val details = measurables.getOrNull(1)?.measure(
            Constraints(maxWidth = detailsMaxWidth, maxHeight = panel.height),
        )
        val width = panel.width + (details?.width ?: 0)
        layout(width, panel.height) {
            if (edge == OverlayEdge.RIGHT) {
                details?.place(0, 0)
                panel.place(width - panel.width, 0)
            } else {
                panel.place(0, 0)
                details?.place(panel.width, 0)
            }
        }
    }

    if (detailsWindowKm != null) {
        LaunchedEffect(detailsWindowKm) {
            delay(DETAILS_AUTO_HIDE_MS)
            onDismissDetails()
        }
    }
}

@Composable
private fun SidePanel(
    windows: List<RangeWindow>,
    charging: Boolean,
    vehicleRangeRemainingKm: Float?,
    edge: OverlayEdge,
    detailsWindowKm: Double?,
    onWindowClick: (Double) -> Unit,
    onGaugePlaced: (windowKm: Double, centerY: Float) -> Unit,
    onCollapse: () -> Unit,
) {
    val shape = EdgeDockShape(edge, PanelCorner, PanelFlare)
    Column(
        modifier = Modifier
            .width(PanelWidth)
            .background(PanelColor, shape)
            .border(OutlineWidth, OutlineColor, shape)
            .padding(horizontal = 8.dp, vertical = PanelFlare + 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RangeConstants.RANGE_WINDOWS_KM.forEach { windowKm ->
            WindowGauge(
                windowKm = windowKm,
                state = gaugeState(windows.findWindow(windowKm), charging, vehicleRangeRemainingKm),
                selected = detailsWindowKm == windowKm,
                onClick = { onWindowClick(windowKm) },
                onRingPlaced = { centerY -> onGaugePlaced(windowKm, centerY) },
            )
        }
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 36.dp)
                .clip(CircleShape)
                .clickable(onClickLabel = "Спрятать виджет", onClick = onCollapse),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (edge == OverlayEdge.RIGHT) Icons.Rounded.ChevronRight else Icons.Rounded.ChevronLeft,
                contentDescription = "Спрятать виджет",
                tint = MutedText,
            )
        }
    }
}

/** Кольцо окна: внутри — размер окна, под ним — прогноз до 0 % SOC. */
@Composable
private fun WindowGauge(
    windowKm: Double,
    state: GaugeState,
    selected: Boolean,
    onClick: () -> Unit,
    onRingPlaced: (centerY: Float) -> Unit,
) {
    val color = gaugeColor(state.tone)
    val progress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = 400),
        label = "gauge-progress",
    )
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp)
            .clearAndSetSemantics { contentDescription = gaugeDescription(windowKm, state) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(GaugeSize)
                .onGloballyPositioned { coordinates ->
                    onRingPlaced(coordinates.positionInRoot().y + coordinates.size.height / 2f)
                }
                .drawBehind { drawGauge(progress, color) },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    DisplayFormat.kmNumber(windowKm),
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    "км",
                    color = MutedText,
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        if (state.tone == GaugeTone.CHARGING) {
            Icon(
                Icons.Rounded.Bolt,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp),
            )
        } else {
            Text(
                state.value,
                color = Color.White,
                fontSize = 23.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

private fun DrawScope.drawGauge(progress: Float, color: Color) {
    val stroke = GaugeStroke.toPx()
    val topLeft = Offset(stroke / 2f, stroke / 2f)
    val arcSize = Size(size.width - stroke, size.height - stroke)
    drawArc(
        color = TrackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = stroke),
    )
    if (progress > 0f) {
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

/** Спрятанная панель — язычок у края; нажатие возвращает панель. */
@Composable
private fun OverlayHandle(edge: OverlayEdge, onClick: () -> Unit) {
    val shape = EdgeDockShape(edge, HandleCorner, HandleFlare)
    Box(
        modifier = Modifier
            .size(width = HandleWidth, height = HandleBodyHeight + HandleFlare * 2)
            .clip(shape)
            .background(PanelColor)
            .border(OutlineWidth, OutlineColor, shape)
            .clickable(onClickLabel = "Показать виджет", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (edge == OverlayEdge.RIGHT) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight,
            contentDescription = "Показать виджет",
            tint = Color.White,
        )
    }
}

/**
 * Карточка подробностей по окну. Высотой она с панель: сама карточка стоит напротив кольца,
 * а уголок смотрит точно на него. Касание по пустому месту рядом с карточкой её закрывает,
 * по самой карточке — открывает приложение.
 */
@Composable
private fun WindowDetails(
    windowKm: Double,
    window: RangeWindow?,
    charging: Boolean,
    vehicleRangeRemainingKm: Float?,
    edge: OverlayEdge,
    anchorY: Float,
    onOpenApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dismiss by rememberUpdatedState(onDismiss)
    val pointsRight = edge == OverlayEdge.RIGHT
    Layout(
        content = {
            DetailsCard(
                windowKm = windowKm,
                window = window,
                charging = charging,
                vehicleRangeRemainingKm = vehicleRangeRemainingKm,
                onOpenApp = onOpenApp,
            )
            Box(
                Modifier
                    .size(PointerWidth, PointerHeight)
                    .background(PanelColor, PointerShape(pointsRight)),
            )
        },
        modifier = Modifier.pointerInput(Unit) { detectTapGestures { dismiss() } },
    ) { measurables, constraints ->
        val height = constraints.maxHeight
        val gap = DetailsGap.roundToPx()
        val pointer = measurables[1].measure(Constraints())
        val cardWidth = if (constraints.hasBoundedWidth) {
            min(DetailsWidth.roundToPx(), constraints.maxWidth - pointer.width - gap).coerceAtLeast(0)
        } else {
            DetailsWidth.roundToPx()
        }
        val card = measurables[0].measure(Constraints(maxWidth = cardWidth, maxHeight = height))
        // Уголок заходит на карточку, закрывая её обводку, — между ними нет шва.
        val overlap = OutlineWidth.roundToPx()
        val width = card.width + pointer.width - overlap + gap
        val cardTop = (anchorY - card.height / 2f).roundToInt()
            .coerceIn(0, max(0, height - card.height))
        val inset = DetailsCorner.roundToPx()
        val pointerTop = (anchorY - pointer.height / 2f).roundToInt()
            .coerceIn(cardTop + inset, max(cardTop + inset, cardTop + card.height - inset - pointer.height))
        layout(width, height) {
            if (pointsRight) {
                card.place(0, cardTop)
                pointer.place(card.width - overlap, pointerTop)
            } else {
                pointer.place(gap, pointerTop)
                card.place(gap + pointer.width - overlap, cardTop)
            }
        }
    }
}

@Composable
private fun DetailsCard(
    windowKm: Double,
    window: RangeWindow?,
    charging: Boolean,
    vehicleRangeRemainingKm: Float?,
    onOpenApp: () -> Unit,
) {
    val state = gaugeState(window, charging, vehicleRangeRemainingKm)
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DetailsCorner))
            .background(PanelColor)
            .border(OutlineWidth, OutlineColor, RoundedCornerShape(DetailsCorner))
            .clickable(onClickLabel = "Открыть приложение", onClick = onOpenApp)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(gaugeColor(state.tone), CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Окно " + DisplayFormat.windowKmLabel(windowKm),
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(12.dp))
        when {
            state.tone == GaugeTone.CHARGING -> DetailsStatus(
                title = "Зарядка",
                description = "Прогноз вернётся, когда машина поедет.",
            )
            window == null -> DetailsStatus(
                title = "Нет данных",
                description = "Окно наберётся в движении.",
            )
            window.status == WindowStatus.READY -> ReadyDetails(window, vehicleRangeRemainingKm)
            window.status == WindowStatus.NEED_MORE_KM -> FillingDetails(window, accent)
            window.status == WindowStatus.SOC_UNCHANGED -> DetailsStatus(
                title = "SOC не изменился",
                description = "Расход на этом участке слишком мал для оценки.",
            )
            window.status == WindowStatus.GAP -> DetailsStatus(
                title = "Разрыв в окне",
                description = "Телеметрия прерывалась — окно наберётся заново.",
            )
            else -> DetailsStatus(
                title = "Мало данных",
                description = "Недостаточно данных для оценки по окну.",
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
            color = Color.White.copy(alpha = 0.12f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Открыть приложение",
                color = accent,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ReadyDetails(window: RangeWindow, vehicleRangeRemainingKm: Float?) {
    val reserve = window.rangeToReserveKm ?: 0.0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailsRow(label = "До 0%", value = "≈ " + DisplayFormat.km(window.rangeTo0Km, digits = 0))
        DetailsRow(
            label = "До " + RangeConstants.RESERVE_SOC_PERCENT.toInt() + "%",
            value = if (reserve <= 0.0) "резерв пройден" else "≈ " + DisplayFormat.km(reserve, digits = 0),
        )
        val vehicleKm = vehicleRangeRemainingKm?.takeIf { it.isFinite() && it > 0f }
        if (vehicleKm != null) {
            val delta = DisplayFormat.rangeDeltaPercent(window.rangeTo0Km, vehicleKm)
            DetailsRow(
                label = "Оценка ГУ",
                value = DisplayFormat.km(vehicleKm.toDouble(), digits = 0),
                badge = DisplayFormat.rangeDeltaLabel(delta),
                badgeColor = gaugeColor(gaugeState(window, charging = false, vehicleKm).tone),
            )
        }
    }
}

@Composable
private fun FillingDetails(window: RangeWindow, accent: Color) {
    val filled = windowFilledFraction(window)
    val left = window.remainingToFillKm ?: window.windowKm
    Column {
        DetailsRow(
            label = "Набираем окно",
            value = DisplayFormat.kmNumber(window.windowKm - left, digits = 1) + " из " +
                DisplayFormat.windowKmLabel(window.windowKm),
        )
        Box(
            Modifier
                .padding(vertical = 10.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(TrackColor),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(filled)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(accent),
            )
        }
        Text(
            "Прогноз появится через " + DisplayFormat.km(left),
            color = MutedText,
            fontSize = 14.sp,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun DetailsRow(
    label: String,
    value: String,
    badge: String? = null,
    badgeColor: Color = Color.Unspecified,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = MutedText,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            color = Color.White,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (badge != null) {
            Text(
                badge,
                color = badgeColor,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun DetailsStatus(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            color = Color.White,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            description,
            color = MutedText,
            fontSize = 14.sp,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun gaugeColor(tone: GaugeTone): Color {
    val extra = RangeThemeColors.extra
    return when (tone) {
        GaugeTone.CLOSE -> extra.success
        GaugeTone.FAR -> extra.warning
        GaugeTone.READY, GaugeTone.FILLING -> MaterialTheme.colorScheme.primary
        GaugeTone.CHARGING -> extra.charging
        GaugeTone.NONE -> TrackColor
    }
}

private fun gaugeDescription(windowKm: Double, state: GaugeState): String {
    val window = "Окно " + DisplayFormat.windowKmLabel(windowKm)
    return when (state.tone) {
        GaugeTone.CLOSE, GaugeTone.FAR, GaugeTone.READY -> "$window: прогноз ${state.value} км"
        GaugeTone.FILLING -> "$window: набирается"
        GaugeTone.CHARGING -> "$window: зарядка"
        GaugeTone.NONE -> "$window: прогноза нет"
    }
}

private fun List<RangeWindow>.findWindow(windowKm: Double): RangeWindow? =
    firstOrNull { abs(it.windowKm - windowKm) < 1e-6 }
