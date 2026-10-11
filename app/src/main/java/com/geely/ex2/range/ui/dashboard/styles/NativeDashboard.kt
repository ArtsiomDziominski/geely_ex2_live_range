package com.geely.ex2.range.ui.dashboard.styles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.ui.theme.RangeThemeColors
import com.geely.ex2.range.ui.theme.StyleColors
import kotlin.math.roundToInt

/** Размер макета «Штатный» без левого меню разделов, в пикселях макета. */
internal const val NATIVE_DESIGN_WIDTH = 1560f
internal const val NATIVE_DESIGN_HEIGHT = 1080f

private const val BATTERY_CELLS = 20
private const val RESERVE_CELLS = 4
private val Plate = RoundedCornerShape(6.dp)

/**
 * «Штатный»: в духе экранов настроек головного устройства — плоский сегментный переключатель
 * окна, строки-плашки «подпись — значение», справа запас хода, батарея ячейками и график.
 */
@Composable
internal fun NativeDashboard(
    ui: DashboardUi,
    colors: StyleColors,
    selectedKm: Double,
    onSelectWindow: (Double) -> Unit,
    onResetPeriod: () -> Unit,
    onForecastInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = ui.window(selectedKm)
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 64.dp, top = 56.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(64.dp),
    ) {
        Column(
            modifier = Modifier
                .width(720.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Txt("Прогноз запаса хода · окно усреднения", 21, colors.muted)
                    StyleIconButton(
                        icon = Icons.Outlined.Info,
                        description = "Что означает прогноз и график",
                        onClick = onForecastInfo,
                        size = 52,
                        tint = colors.text,
                        container = colors.panel,
                        shape = Plate,
                    )
                }
                WindowSegments(ui, selected, colors, onSelectWindow)
                ForecastRows(ui, selected, colors)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Txt("Расход", 21, colors.muted)
                ConsumptionRow("За всё время", "с последнего сброса", ui.periodKwh, ui.periodPct, colors, onResetPeriod)
                ConsumptionRow("Поездка", ui.tripHint, ui.tripKwh, ui.tripPct, colors, null)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Txt("Сейчас", 21, colors.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NowTile("Скорость", ui.speed, "км/ч", colors, Modifier.weight(1f))
                    NowTile("За бортом", ui.outsideTemp, "°C", colors, Modifier.weight(1f))
                    NowTile("Передача", ui.gear, null, colors, Modifier.weight(0.8f))
                    NowTile("Одометр", ui.odometer, "км", colors, Modifier.weight(1.5f))
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Txt("Запас хода", 22, colors.muted)
                Figure(
                    value = ui.rangeKm,
                    unit = if (ui.hasRange) "км" else null,
                    size = 136,
                    unitSize = 46,
                    color = colors.text,
                    unitColor = colors.muted,
                    weight = FontWeight.Bold,
                    gap = 16,
                    prefix = if (ui.hasRange) "≈" else null,
                )
                Txt(ui.rangeCaption, 21, colors.muted)
            }
            Battery(ui, colors)
            StyleChart(selected, height = 200)
        }
    }
}

/** Выбор окна усреднения — плоский сегментный переключатель, как в настройках ГУ. */
@Composable
private fun WindowSegments(
    ui: DashboardUi,
    selected: WindowUi,
    colors: StyleColors,
    onSelectWindow: (Double) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(Plate)
            .background(colors.panel),
    ) {
        ui.windows.forEach { window ->
            val active = window.windowKm == selected.windowKm
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(Plate)
                    .background(if (active) colors.accent else Color.Transparent)
                    .selectable(
                        selected = active,
                        role = Role.Tab,
                        onClick = { onSelectWindow(window.windowKm) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Txt(
                    window.label,
                    25,
                    if (active) colors.onAccent else colors.text,
                    weight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ForecastRows(ui: DashboardUi, selected: WindowUi, colors: StyleColors) {
    if (!selected.ready) {
        WindowStatusNote(
            window = selected,
            titleColor = colors.text,
            hintColor = colors.muted,
            modifier = Modifier
                .fillMaxWidth()
                .height(312.dp)
                .clip(Plate)
                .background(colors.panel)
                .padding(24.dp),
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ValueRow("До 0 %", "≈ ${selected.value}", "км", colors.accentText, colors)
        ValueRow("До резерва 20 %", "≈ ${selected.reserveKm}", "км", colors.text, colors)
        ValueRow("Оценка головного устройства", ui.vehicleKm ?: "—", "км", colors.text, colors)
        ValueRow(
            "Расхождение с ГУ",
            selected.deltaLabel ?: "—",
            null,
            deltaColor(selected.deltaTone, colors.text),
            colors,
        )
    }
}

@Composable
private fun ValueRow(label: String, value: String, unit: String?, valueColor: Color, colors: StyleColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(Plate)
            .background(colors.panel)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Txt(label, 25, colors.text, weight = FontWeight.SemiBold)
        Figure(value, unit, 32, 21, valueColor, colors.muted, weight = FontWeight.Bold, gap = 8)
    }
}

@Composable
private fun ConsumptionRow(
    title: String,
    hint: String,
    kwh: String,
    pct: String,
    colors: StyleColors,
    onReset: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(Plate)
            .background(colors.panel)
            .padding(start = 24.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Txt(title, 25, colors.text, weight = FontWeight.SemiBold)
            Txt(hint, 19, colors.muted)
        }
        Figure(kwh, "кВт·ч / 100 км", 36, 19, colors.text, colors.muted, weight = FontWeight.Bold, gap = 8)
        Txt("$pct %", 24, colors.text, Modifier.width(74.dp), weight = FontWeight.SemiBold)
        Box(Modifier.width(56.dp)) {
            if (onReset != null) {
                StyleIconButton(
                    icon = Icons.Outlined.RestartAlt,
                    description = "Сбросить статистику за всё время",
                    onClick = onReset,
                    size = 56,
                    tint = colors.text,
                    container = colors.inset,
                    shape = Plate,
                )
            }
        }
    }
}

@Composable
private fun NowTile(
    label: String,
    value: String,
    unit: String?,
    colors: StyleColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(Plate)
            .background(colors.panel)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Txt(label, 19, colors.muted)
        Figure(value, unit, 32, 19, colors.text, colors.muted, weight = FontWeight.Bold, gap = 8)
    }
}

/** Заряд ячейками: первые четыре — резерв 20 %, подчёркнут снизу. */
@Composable
private fun Battery(ui: DashboardUi, colors: StyleColors) {
    val charging = RangeThemeColors.extra.charging
    val filled = (ui.socFraction * BATTERY_CELLS).roundToInt()
    val fill = if (ui.charging) charging else colors.accent
    Column(
        modifier = Modifier.semantics { contentDescription = "Заряд батареи ${ui.soc} процентов" },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt(ui.chargeStatus ?: "Заряд", 20, if (ui.charging) charging else colors.muted)
                Txt("${ui.soc} %", 46, colors.text, weight = FontWeight.Bold)
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Txt("До резерва 20 %", 20, colors.muted)
                Txt(
                    if (ui.toReserveKm != null) "≈ ${ui.toReserveKm} км" else "—",
                    46,
                    colors.text,
                    weight = FontWeight.Bold,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(colors.panel, RoundedCornerShape(10.dp))
                .border(2.dp, colors.line, RoundedCornerShape(10.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(BATTERY_CELLS) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (index < filled) fill else colors.track, RoundedCornerShape(3.dp)),
                )
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .weight(RESERVE_CELLS.toFloat())
                    .height(4.dp)
                    .background(colors.reserve, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.weight((BATTERY_CELLS - RESERVE_CELLS).toFloat()))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Txt("резерв 20 %", 20, colors.reserve)
            if (ui.vehicleKm != null) {
                Txt("оценка ГУ · ${ui.vehicleKm} км", 20, colors.muted)
            }
        }
    }
}
