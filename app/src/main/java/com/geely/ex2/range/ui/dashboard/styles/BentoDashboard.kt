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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.ui.theme.StyleColors

/** Размер макета «Бенто» над доком разделов, в пикселях макета. */
internal const val BENTO_DESIGN_WIDTH = 1920f
internal const val BENTO_DESIGN_HEIGHT = 960f

private const val RESERVE_SHARE = 0.2f

/**
 * «Бенто»: плитки. Крупная акцентная плитка запаса хода, «дорога заряда» — путь до резерва
 * и до нуля одной полосой, и все три окна прогноза сразу, без переключателя.
 */
@Composable
internal fun BentoDashboard(
    ui: DashboardUi,
    colors: StyleColors,
    onResetPeriod: () -> Unit,
    onForecastInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, top = 28.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.weight(0.48f),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HeroTile(
                ui = ui,
                colors = colors,
                modifier = Modifier
                    .width(600.dp)
                    .fillMaxHeight(),
            )
            RoadTile(
                ui = ui,
                colors = colors,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SmallTile(Icons.Outlined.Speed, "Скорость", ui.speed, "км/ч", colors, Modifier.weight(1f))
                SmallTile(Icons.Outlined.DirectionsCar, "Передача", ui.gear, null, colors, Modifier.weight(1f))
            }
        }
        Row(
            modifier = Modifier.weight(0.52f),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ConsumptionTile(
                ui = ui,
                colors = colors,
                onResetPeriod = onResetPeriod,
                modifier = Modifier
                    .width(600.dp)
                    .fillMaxHeight(),
            )
            ForecastTile(
                ui = ui,
                colors = colors,
                onForecastInfo = onForecastInfo,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SmallTile(Icons.Outlined.Thermostat, "За бортом", ui.outsideTemp, "°C", colors, Modifier.weight(1f))
                SmallTile(
                    Icons.Outlined.Straighten,
                    "Одометр",
                    ui.odometer,
                    "км",
                    colors,
                    Modifier.weight(1f),
                    valueSize = 46,
                )
            }
        }
    }
}

@Composable
private fun HeroTile(ui: DashboardUi, colors: StyleColors, modifier: Modifier = Modifier) {
    val ink = colors.onAccent
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(40.dp))
            .background(colors.accent)
            .padding(horizontal = 40.dp, vertical = 34.dp)
            .semantics {
                contentDescription = "Запас хода ${ui.rangeKm} километров, заряд ${ui.soc} процентов"
            },
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Txt("Запас хода", 26, ink, weight = FontWeight.SemiBold)
            Box(
                Modifier
                    .border(1.5.dp, ink, CircleShape)
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Txt(ui.rangeCaption, 19, ink)
            }
        }
        Figure(
            value = ui.rangeKm,
            unit = if (ui.hasRange) "км" else null,
            size = 172,
            unitSize = 46,
            color = ink,
            unitColor = ink,
            weight = FontWeight.Black,
            gap = 16,
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Txt("${ui.soc} %", 40, ink, Modifier.alignByBaseline(), weight = FontWeight.Bold)
                Txt(ui.chargeStatus ?: "заряд · резерв 20 %", 20, ink, Modifier.alignByBaseline())
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ink.copy(alpha = 0.2f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(ui.socFraction)
                        .fillMaxHeight()
                        .background(ink, RoundedCornerShape(10.dp)),
                )
                // Прорезь на отметке резерва: видна и на залитой, и на пустой части полосы.
                Box(
                    Modifier
                        .align(fractionBias(RESERVE_SHARE))
                        .size(width = 4.dp, height = 20.dp)
                        .background(colors.accent),
                )
            }
        }
    }
}

/** Путь до резерва и до нуля одной полосой, с отметкой оценки головного устройства. */
@Composable
private fun RoadTile(ui: DashboardUi, colors: StyleColors, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(40.dp))
            .background(colors.panel)
            .padding(horizontal = 40.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt("Дорога заряда", 26, colors.text, weight = FontWeight.SemiBold)
                Txt("куда хватит при текущем темпе", 20, colors.muted)
            }
            if (ui.vehicleKm != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .size(width = 4.dp, height = 22.dp)
                            .background(colors.text, RoundedCornerShape(2.dp)),
                    )
                    Txt("оценка ГУ · ${ui.vehicleKm} км", 20, colors.text)
                }
            }
        }

        if (ui.forecastReady) {
            RoadBar(ui, colors)
        } else {
            val waiting = ui.windows.firstOrNull { it.status != null }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt(waiting?.status ?: "Прогноза пока нет", 30, colors.text, weight = FontWeight.SemiBold)
                Txt(waiting?.statusHint.orEmpty(), 21, colors.muted, maxLines = 2)
            }
        }
    }
}

@Composable
private fun RoadBar(ui: DashboardUi, colors: StyleColors) {
    val reserveFraction = ui.reserveFraction ?: 0f
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Txt("сейчас · ${ui.soc} %", 20, colors.muted)
        Box(
            Modifier
                .fillMaxWidth()
                .height(68.dp),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (reserveFraction > 0f) {
                    Box(
                        Modifier
                            .weight(reserveFraction)
                            .fillMaxHeight()
                            .background(colors.accent, RoundedCornerShape(26.dp, 6.dp, 6.dp, 26.dp)),
                    )
                }
                if (reserveFraction < 1f) {
                    Box(
                        Modifier
                            .weight(1f - reserveFraction)
                            .fillMaxHeight()
                            .background(
                                colors.reserve,
                                if (reserveFraction > 0f) {
                                    RoundedCornerShape(6.dp, 26.dp, 26.dp, 6.dp)
                                } else {
                                    RoundedCornerShape(26.dp)
                                },
                            ),
                    )
                }
            }
            if (ui.vehicleRoadFraction != null) {
                Box(
                    Modifier
                        .align(fractionBias(ui.vehicleRoadFraction))
                        .size(width = 10.dp, height = 68.dp)
                        .background(colors.panel),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(width = 4.dp, height = 68.dp)
                            .background(colors.text, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RoadLegend(
                swatch = colors.accent,
                value = if (ui.toReserveKm != null) "${ui.toReserveKm} км" else "0 км",
                caption = if (ui.toReserveKm != null) "до резерва 20 %" else "резерв пройден",
                colors = colors,
            )
            RoadLegend(
                swatch = colors.reserve,
                value = "${ui.rangeKm} км",
                caption = "до 0 %",
                colors = colors,
                alignEnd = true,
            )
        }
    }
}

@Composable
private fun RoadLegend(
    swatch: Color,
    value: String,
    caption: String,
    colors: StyleColors,
    alignEnd: Boolean = false,
) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(16.dp)
                    .background(swatch, CircleShape),
            )
            Txt(value, 38, colors.text, weight = FontWeight.Bold)
        }
        Txt(caption, 19, colors.muted)
    }
}

@Composable
private fun SmallTile(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String?,
    colors: StyleColors,
    modifier: Modifier = Modifier,
    valueSize: Int = 68,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(colors.panel)
            .padding(horizontal = 32.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = colors.muted, modifier = Modifier.size(28.dp))
            Txt(label, 22, colors.muted)
        }
        Figure(value, unit, valueSize, 24, colors.text, colors.muted, weight = FontWeight.Bold, gap = 12)
    }
}

@Composable
private fun ConsumptionTile(
    ui: DashboardUi,
    colors: StyleColors,
    onResetPeriod: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(40.dp))
            .background(colors.panel)
            .padding(horizontal = 40.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Txt("Расход", 26, colors.text, weight = FontWeight.SemiBold)
            StyleIconButton(
                icon = Icons.Outlined.RestartAlt,
                description = "Сбросить статистику за всё время",
                onClick = onResetPeriod,
                size = 64,
                tint = colors.text,
                container = colors.track,
            )
        }
        ConsumptionLine("Всего", ui.periodKwh, ui.periodPct, colors)
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line),
        )
        ConsumptionLine("Поездка · ${ui.tripHint}", ui.tripKwh, ui.tripPct, colors)
    }
}

@Composable
private fun ConsumptionLine(label: String, kwh: String, pct: String, colors: StyleColors) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(label, 20, colors.muted)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Txt(kwh, 76, colors.text, weight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt("кВт·ч / 100 км", 22, colors.muted)
                Txt("$pct % / 100 км", 24, colors.text)
            }
        }
    }
}

@Composable
private fun ForecastTile(
    ui: DashboardUi,
    colors: StyleColors,
    onForecastInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(40.dp))
            .background(colors.panel)
            .padding(horizontal = 32.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt("Прогноз по окнам", 26, colors.text, weight = FontWeight.SemiBold)
                Txt("до 0 % по темпу последних километров", 20, colors.muted)
            }
            StyleIconButton(
                icon = Icons.Outlined.Info,
                description = "Что означает прогноз",
                onClick = onForecastInfo,
                size = 64,
                tint = colors.text,
                container = colors.track,
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ui.windows.forEach { window ->
                WindowCard(
                    window = window,
                    colors = colors,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun WindowCard(window: WindowUi, colors: StyleColors, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(colors.inset)
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Txt(window.label, 24, colors.text, Modifier.alignByBaseline(), weight = FontWeight.SemiBold)
            if (window.deltaLabel != null) {
                Txt(
                    "к ГУ ${window.deltaLabel}",
                    18,
                    deltaColor(window.deltaTone, colors.muted),
                    Modifier.alignByBaseline(),
                )
            }
        }
        Figure(
            value = window.value,
            unit = if (window.ready) "км" else null,
            size = 60,
            unitSize = 22,
            color = colors.text,
            unitColor = colors.muted,
            weight = FontWeight.Bold,
        )
        if (window.chart.size >= 2) {
            Sparkline(
                points = window.chart,
                color = if (window.ready) colors.accentText else colors.muted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            )
        } else {
            Spacer(Modifier.height(56.dp))
        }
        Txt(
            if (window.ready) "до 20 % · ${window.reserveKm} км" else window.status.orEmpty(),
            20,
            colors.muted,
        )
    }
}
