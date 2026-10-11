package com.geely.ex2.range.ui.dashboard.styles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.ui.theme.RangeThemeColors
import com.geely.ex2.range.ui.theme.StyleColors

/** Размер макета «Кокпит» без боковой панели разделов, в пикселях макета. */
internal const val COCKPIT_DESIGN_WIDTH = 1796f
internal const val COCKPIT_DESIGN_HEIGHT = 1080f

private const val GAUGE_START_ANGLE = 135f
private const val GAUGE_SWEEP_ANGLE = 270f
private const val RESERVE_SHARE = 0.2f

/**
 * «Кокпит»: слева круговая шкала заряда с запасом хода внутри и приборные показатели,
 * справа — окна прогноза полосами на общей шкале с отметкой оценки ГУ.
 */
@Composable
internal fun CockpitDashboard(
    ui: DashboardUi,
    colors: StyleColors,
    selectedKm: Double,
    onSelectWindow: (Double) -> Unit,
    onResetPeriod: () -> Unit,
    onForecastInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 44.dp, vertical = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Txt(
                    "Dynamic Range",
                    34,
                    colors.text,
                    Modifier.alignByBaseline(),
                    weight = FontWeight.SemiBold,
                    family = CondensedFamily,
                    caps = true,
                )
                Txt("Geely EX2", 20, colors.muted, Modifier.alignByBaseline())
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(48.dp),
            ) {
                Gauge(ui, colors, Modifier.size(580.dp))
                Column(Modifier.weight(1f)) {
                    Readout("Скорость", ui.speed, "км/ч", colors)
                    Divider(colors)
                    Readout("За бортом", ui.outsideTemp, "°C", colors)
                    Divider(colors)
                    Readout("Передача", ui.gear, null, colors)
                    Divider(colors)
                    Readout("Одометр", ui.odometer, "км", colors, size = 64)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Consumption(
                    title = "Расход · всего",
                    hint = null,
                    kwh = ui.periodKwh,
                    pct = ui.periodPct,
                    colors = colors,
                    onReset = onResetPeriod,
                    modifier = Modifier.weight(1f),
                )
                Consumption(
                    title = "Поездка",
                    hint = ui.tripHint,
                    kwh = ui.tripKwh,
                    pct = ui.tripPct,
                    colors = colors,
                    onReset = null,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        ForecastPanel(
            ui = ui,
            colors = colors,
            selectedKm = selectedKm,
            onSelectWindow = onSelectWindow,
            onForecastInfo = onForecastInfo,
            modifier = Modifier
                .width(680.dp)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun Gauge(ui: DashboardUi, colors: StyleColors, modifier: Modifier = Modifier) {
    val charging = RangeThemeColors.extra.charging
    val fill = if (ui.charging) charging else colors.accent
    Box(
        modifier = modifier.semantics {
            contentDescription = "Заряд ${ui.soc} процентов, запас хода ${ui.rangeKm} километров"
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Тонкое внешнее кольцо показывает, где на шкале резерв 20 %.
            val outer = 7.dp.toPx()
            val outerSize = Size(size.width - outer, size.height - outer)
            val outerOffset = Offset(outer / 2, outer / 2)
            drawArc(colors.track, GAUGE_START_ANGLE, GAUGE_SWEEP_ANGLE, false, outerOffset, outerSize, style = Stroke(outer))
            drawArc(
                colors.reserve,
                GAUGE_START_ANGLE,
                GAUGE_SWEEP_ANGLE * RESERVE_SHARE,
                false,
                outerOffset,
                outerSize,
                style = Stroke(outer),
            )

            val ring = 40.dp.toPx()
            val inset = 24.dp.toPx() + ring / 2
            val ringSize = Size(size.width - inset * 2, size.height - inset * 2)
            val ringOffset = Offset(inset, inset)
            drawArc(colors.track, GAUGE_START_ANGLE, GAUGE_SWEEP_ANGLE, false, ringOffset, ringSize, style = Stroke(ring))
            if (ui.socFraction > 0f) {
                drawArc(
                    fill,
                    GAUGE_START_ANGLE,
                    GAUGE_SWEEP_ANGLE * ui.socFraction,
                    false,
                    ringOffset,
                    ringSize,
                    style = Stroke(ring),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Txt("Запас хода", 20, colors.muted, weight = FontWeight.SemiBold, caps = true)
            Figure(
                value = ui.rangeKm,
                unit = if (ui.hasRange) "км" else null,
                size = 190,
                unitSize = 48,
                color = colors.text,
                unitColor = colors.muted,
                weight = FontWeight.Bold,
                family = CondensedFamily,
                gap = 12,
                prefix = if (ui.hasRange) "≈" else null,
            )
            Txt(ui.rangeCaption, 20, colors.muted)
            Figure(
                value = ui.soc,
                unit = "% заряд",
                size = 60,
                unitSize = 30,
                color = if (ui.charging) charging else colors.accentText,
                unitColor = colors.muted,
                modifier = Modifier.padding(top = 10.dp),
                family = CondensedFamily,
                gap = 8,
            )
        }
        GaugeFootnote(ui, colors, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

/** В разрыве шкалы снизу: состояние зарядки либо сколько осталось до резерва. */
@Composable
private fun GaugeFootnote(ui: DashboardUi, colors: StyleColors, modifier: Modifier = Modifier) {
    val charging = RangeThemeColors.extra.charging
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            ui.chargeStatus != null -> {
                val tint = if (ui.charging) charging else colors.muted
                Icon(Icons.Outlined.Bolt, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                Txt(ui.chargeStatus, 21, tint)
            }
            ui.toReserveKm != null -> {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(colors.reserve, CircleShape),
                )
                Txt("≈ ${ui.toReserveKm} км до 20 %", 22, colors.text)
            }
        }
    }
}

@Composable
private fun Readout(label: String, value: String, unit: String?, colors: StyleColors, size: Int = 76) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Txt(label, 18, colors.muted, weight = FontWeight.SemiBold, caps = true)
        Figure(value, unit, size, 28, colors.text, colors.muted, family = CondensedFamily)
    }
}

@Composable
private fun Divider(colors: StyleColors) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.line),
    )
}

@Composable
private fun Consumption(
    title: String,
    hint: String?,
    kwh: String,
    pct: String,
    colors: StyleColors,
    onReset: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(colors.panel)
            .padding(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Txt(title, 18, colors.muted, weight = FontWeight.SemiBold, caps = true)
            if (onReset != null) {
                StyleIconButton(
                    icon = Icons.Outlined.RestartAlt,
                    description = "Сбросить статистику за всё время",
                    onClick = onReset,
                    size = 60,
                    tint = colors.text,
                    outline = colors.line,
                )
            } else if (hint != null) {
                Txt(hint, 20, colors.muted)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Txt(kwh, 88, colors.text, weight = FontWeight.SemiBold, family = CondensedFamily)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt("кВт·ч / 100 км", 23, colors.muted)
                Txt("$pct % / 100 км", 26, colors.text)
            }
        }
    }
}

@Composable
private fun ForecastPanel(
    ui: DashboardUi,
    colors: StyleColors,
    selectedKm: Double,
    onSelectWindow: (Double) -> Unit,
    onForecastInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = ui.window(selectedKm)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(colors.panel)
            .padding(horizontal = 32.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt("Прогноз запаса хода", 30, colors.text, weight = FontWeight.SemiBold)
                Txt("по темпу последних километров", 20, colors.muted)
            }
            StyleIconButton(
                icon = Icons.Outlined.Info,
                description = "Что означает прогноз и график",
                onClick = onForecastInfo,
                size = 64,
                tint = colors.text,
                outline = colors.line,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ui.windows.forEach { window ->
                WindowRow(
                    window = window,
                    selected = window.windowKm == selected.windowKm,
                    vehicleFraction = ui.vehicleBarFraction,
                    colors = colors,
                    onClick = { onSelectWindow(window.windowKm) },
                )
            }
            if (ui.vehicleKm != null) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .size(width = 3.dp, height = 20.dp)
                            .background(colors.reserve, RoundedCornerShape(2.dp)),
                    )
                    Txt("оценка головного устройства · ${ui.vehicleKm} км", 19, colors.muted)
                }
            }
        }

        StyleChart(selected, height = 170)

        if (selected.ready) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResultTile("До 0 %", "≈ ${selected.value}", "км", colors.accentText, colors, Modifier.weight(1f))
                    ResultTile(
                        "До резерва 20 %",
                        "≈ ${selected.reserveKm}",
                        "км",
                        colors.text,
                        colors,
                        Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResultTile("Оценка ГУ", ui.vehicleKm ?: "—", "км", colors.text, colors, Modifier.weight(1f))
                    ResultTile(
                        "Расхождение",
                        selected.deltaLabel ?: "—",
                        null,
                        deltaColor(selected.deltaTone, colors.text),
                        colors,
                        Modifier.weight(1f),
                    )
                }
            }
        } else {
            WindowStatusNote(
                window = selected,
                titleColor = colors.text,
                hintColor = colors.muted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.inset)
                    .padding(24.dp),
            )
        }
    }
}

/** Окно прогноза строкой: полоса на общей шкале и отметка оценки ГУ на ней. */
@Composable
private fun WindowRow(
    window: WindowUi,
    selected: Boolean,
    vehicleFraction: Float?,
    colors: StyleColors,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(shape)
            .background(if (selected) colors.accent.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, if (selected) colors.accent else Color.Transparent, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Txt(
            window.label,
            26,
            if (selected) colors.text else colors.muted,
            Modifier.width(84.dp),
            weight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(34.dp),
        ) {
            val bar = RoundedCornerShape(8.dp)
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(colors.track, bar),
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(window.barFraction)
                    .height(16.dp)
                    .background(if (selected && window.ready) colors.accent else colors.muted.copy(alpha = 0.55f), bar),
            )
            if (vehicleFraction != null) {
                Box(
                    Modifier
                        .align(fractionBias(vehicleFraction))
                        .size(width = 3.dp, height = 34.dp)
                        .background(colors.reserve, RoundedCornerShape(2.dp)),
                )
            }
        }
        Row(
            modifier = Modifier.width(128.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Figure(
                value = window.value,
                unit = if (window.ready) "км" else null,
                size = 40,
                unitSize = 22,
                color = colors.text,
                unitColor = colors.muted,
                family = CondensedFamily,
                gap = 8,
            )
        }
    }
}

@Composable
private fun ResultTile(
    label: String,
    value: String,
    unit: String?,
    valueColor: Color,
    colors: StyleColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.inset)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Txt(label, 17, colors.muted, weight = FontWeight.SemiBold, caps = true)
        Figure(value, unit, 52, 26, valueColor, colors.muted, family = CondensedFamily)
    }
}
