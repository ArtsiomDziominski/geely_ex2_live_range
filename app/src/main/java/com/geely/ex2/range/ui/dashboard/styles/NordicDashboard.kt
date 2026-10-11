package com.geely.ex2.range.ui.dashboard.styles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.ui.theme.RangeThemeColors
import com.geely.ex2.range.ui.theme.StyleColors

/** Размер макета «Нордик» под строкой вкладок, в пикселях макета. */
internal const val NORDIC_DESIGN_WIDTH = 1920f
internal const val NORDIC_DESIGN_HEIGHT = 984f

private const val RESERVE_SHARE = 0.2f

/**
 * «Нордик»: без карточек, всё держится на размере чисел и тонких линиях. Слева запас хода
 * и заряд, справа три окна прогноза колонками, график и расход.
 */
@Composable
internal fun NordicDashboard(
    ui: DashboardUi,
    colors: StyleColors,
    selectedKm: Double,
    onSelectWindow: (Double) -> Unit,
    onResetPeriod: () -> Unit,
    onForecastInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 56.dp, end = 56.dp, top = 44.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            RangeBlock(ui, colors)
            ChargeBlock(ui, colors)
            Row(Modifier.height(96.dp)) {
                Metric("Скорость", ui.speed, "км/ч", colors, Modifier.weight(1f), first = true)
                Metric("За бортом", ui.outsideTemp, "°C", colors, Modifier.weight(1f))
                Metric("Передача", ui.gear, null, colors, Modifier.weight(0.8f))
                Metric("Одометр", ui.odometer, "км", colors, Modifier.weight(1.6f))
            }
        }
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(colors.line),
        )
        ForecastColumn(
            ui = ui,
            colors = colors,
            selectedKm = selectedKm,
            onSelectWindow = onSelectWindow,
            onResetPeriod = onResetPeriod,
            onForecastInfo = onForecastInfo,
            modifier = Modifier
                .width(820.dp)
                .fillMaxHeight()
                .padding(start = 56.dp, end = 56.dp, top = 44.dp, bottom = 48.dp),
        )
    }
}

@Composable
private fun RangeBlock(ui: DashboardUi, colors: StyleColors) {
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Txt("Запас хода", 26, colors.muted, weight = FontWeight.Normal)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Figure(
                value = ui.rangeKm,
                unit = if (ui.hasRange) "км" else null,
                size = 340,
                unitSize = 72,
                color = colors.text,
                unitColor = colors.muted,
                weight = FontWeight.Light,
                gap = 22,
                unitWeight = FontWeight.Normal,
            )
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (ui.toReserveKm != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier
                                .size(14.dp)
                                .background(colors.accent),
                        )
                        Txt("≈ ${ui.toReserveKm} км", 44, colors.text)
                    }
                    Txt("до резерва 20 %", 22, colors.muted, weight = FontWeight.Normal)
                }
                Txt(ui.rangeCaption, 20, colors.muted, Modifier.padding(top = 10.dp), weight = FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun ChargeBlock(ui: DashboardUi, colors: StyleColors) {
    val charging = RangeThemeColors.extra.charging
    Column(
        modifier = Modifier.semantics { contentDescription = "Заряд батареи ${ui.soc} процентов" },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            Figure(
                value = ui.soc,
                unit = "%",
                size = 104,
                unitSize = 44,
                color = colors.text,
                unitColor = colors.muted,
                modifier = Modifier.alignByBaseline(),
                weight = FontWeight.Normal,
                gap = 14,
                unitWeight = FontWeight.Normal,
            )
            Txt(
                ui.chargeStatus ?: "заряд батареи",
                22,
                if (ui.charging) charging else colors.muted,
                Modifier.alignByBaseline(),
                weight = FontWeight.Normal,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(32.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(colors.track),
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(ui.socFraction)
                    .height(14.dp)
                    .background(if (ui.charging) charging else colors.text),
            )
            Box(
                Modifier
                    .align(fractionBias(RESERVE_SHARE))
                    .size(width = 4.dp, height = 32.dp)
                    .background(colors.accent),
            )
        }
        Box(Modifier.fillMaxWidth()) {
            Txt("0", 20, colors.muted, Modifier.align(Alignment.CenterStart), weight = FontWeight.Normal)
            Row(Modifier.align(Alignment.CenterStart)) {
                Spacer(Modifier.fillMaxWidth(RESERVE_SHARE))
                Txt("резерв 20 %", 20, colors.accentText, Modifier.padding(start = 14.dp))
            }
            Txt("100 %", 20, colors.muted, Modifier.align(Alignment.CenterEnd), weight = FontWeight.Normal)
        }
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    unit: String?,
    colors: StyleColors,
    modifier: Modifier = Modifier,
    first: Boolean = false,
) {
    Row(modifier.fillMaxHeight()) {
        if (!first) {
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(colors.line),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = if (first) 0.dp else 28.dp, end = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Txt(label, 20, colors.muted, weight = FontWeight.Normal)
            Figure(
                value,
                unit,
                60,
                24,
                colors.text,
                colors.muted,
                weight = FontWeight.Normal,
                unitWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun ForecastColumn(
    ui: DashboardUi,
    colors: StyleColors,
    selectedKm: Double,
    onSelectWindow: (Double) -> Unit,
    onResetPeriod: () -> Unit,
    onForecastInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = ui.window(selectedKm)
    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt("Прогноз", 36, colors.text)
                Txt("по темпу последних километров", 20, colors.muted, weight = FontWeight.Normal)
            }
            StyleIconButton(
                icon = Icons.Outlined.QuestionMark,
                description = "Что означает прогноз и график",
                onClick = onForecastInfo,
                size = 64,
                tint = colors.text,
                outline = colors.text,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ui.windows.forEach { window ->
                WindowColumn(
                    window = window,
                    selected = window.windowKm == selected.windowKm,
                    colors = colors,
                    onClick = { onSelectWindow(window.windowKm) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (selected.chart.size >= 2) {
            StyleChart(selected, height = 150)
        } else {
            WindowStatusNote(
                window = selected,
                titleColor = colors.text,
                hintColor = colors.muted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            )
        }

        Column {
            Rule(colors)
            ConsumptionRow("Расход всего", null, ui.periodKwh, ui.periodPct, colors, onResetPeriod)
            Rule(colors)
            ConsumptionRow("Поездка", ui.tripHint, ui.tripKwh, ui.tripPct, colors, null)
            Rule(colors)
        }
    }
}

/** Окно прогноза колонкой: линия сверху — выбранное окно отмечено акцентом. */
@Composable
private fun WindowColumn(
    window: WindowUi,
    selected: Boolean,
    colors: StyleColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = if (selected) colors.text else colors.muted
    Column(
        modifier = modifier.selectable(selected = selected, role = Role.Tab, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(if (selected) colors.accent else colors.line),
        )
        Txt(
            window.label,
            24,
            ink,
            Modifier.padding(top = 8.dp),
            weight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Figure(
            value = window.value,
            unit = if (window.ready) "км" else null,
            size = 92,
            unitSize = 26,
            color = ink,
            unitColor = colors.muted,
            weight = if (selected) FontWeight.Normal else FontWeight.Light,
            gap = 8,
            unitWeight = FontWeight.Normal,
        )
        Txt(
            if (window.ready) "до 20 % · ${window.reserveKm} км" else window.status.orEmpty(),
            20,
            ink,
            weight = FontWeight.Normal,
        )
        Txt(
            if (window.deltaLabel != null) "к оценке ГУ ${window.deltaLabel}" else " ",
            20,
            if (selected) deltaColor(window.deltaTone, colors.muted) else colors.muted,
            weight = FontWeight.Normal,
        )
    }
}

@Composable
private fun ConsumptionRow(
    title: String,
    hint: String?,
    kwh: String,
    pct: String,
    colors: StyleColors,
    onReset: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Txt(title, 24, colors.text, weight = FontWeight.Normal)
            if (hint != null) {
                Txt(hint, 20, colors.muted, weight = FontWeight.Normal)
            }
        }
        Txt(kwh, 56, colors.text, weight = FontWeight.Normal)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Txt("кВт·ч / 100 км", 20, colors.muted, weight = FontWeight.Normal)
            Txt("$pct % / 100 км", 22, colors.text, weight = FontWeight.Normal)
        }
        Box(Modifier.size(60.dp)) {
            if (onReset != null) {
                StyleIconButton(
                    icon = Icons.Outlined.RestartAlt,
                    description = "Сбросить статистику за всё время",
                    onClick = onReset,
                    size = 60,
                    tint = colors.text,
                    outline = colors.text,
                )
            }
        }
    }
}

@Composable
private fun Rule(colors: StyleColors) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.line),
    )
}
