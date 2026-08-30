package com.geely.ex2.range.ui.dashboard

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geely.ex2.range.app.RangeUiState
import com.geely.ex2.range.domain.engine.EngineView
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowStatus
import com.geely.ex2.range.ui.theme.RangeThemeColors

@Composable
fun DashboardScreen(
    state: RangeUiState,
    onResetPeriod: () -> Unit,
) {
    val engine = state.engine
    val charging = engine?.charging == true
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LiveHeader(
            engine = engine,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.28f),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .weight(0.28f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MetricCard(
                title = "Период",
                icon = Icons.Outlined.CalendarMonth,
                kwh = metricKwh(engine?.period?.kWhPer100, charging),
                pct = metricPct(engine?.period?.pctPer100, charging),
                trailing = {
                    TextButton(onClick = onResetPeriod) { Text("Сброс") }
                },
                modifier = Modifier
                    .weight(0.88f)
                    .fillMaxHeight(),
            )
            MetricCard(
                title = "Поездка",
                icon = Icons.Outlined.Route,
                hint = tripHint(engine),
                kwh = metricKwh(engine?.trip?.kWhPer100, charging || engine?.waitingForDrive == true),
                pct = metricPct(engine?.trip?.pctPer100, charging || engine?.waitingForDrive == true),
                muted = engine?.waitingForDrive == true,
                modifier = Modifier
                    .weight(0.88f)
                    .fillMaxHeight(),
            )
            InclinationCard(
                pitchDegrees = state.pitchDegrees,
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight(),
            )
        }
        ForecastCard(
            windows = engine?.windows.orEmpty(),
            charging = charging,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.44f),
        )
    }
}

@Composable
private fun LiveHeader(
    engine: EngineView?,
    modifier: Modifier = Modifier,
) {
    val charging = engine?.charging == true
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SocBatteryIndicator(
                socPercent = engine?.socPercent,
                charging = charging,
                deltaLabel = if (charging) DisplayFormat.socDeltaLabel(engine?.socDeltaPoints) else null,
                modifier = Modifier.weight(1.6f),
            )
            VerticalDivider(Modifier.fillMaxHeight(0.7f), color = MaterialTheme.colorScheme.outlineVariant)
            HeaderCell(
                modifier = Modifier.weight(1f),
                value = DisplayFormat.speedNumber(engine?.speedKmh),
                unit = "км/ч",
                caption = if (engine?.speedKmh == null) "нет скорости" else null,
            )
            VerticalDivider(Modifier.fillMaxHeight(0.7f), color = MaterialTheme.colorScheme.outlineVariant)
            HeaderCell(
                modifier = Modifier.weight(1f),
                value = DisplayFormat.tempNumber(engine?.outsideTempC),
                unit = "°C",
                caption = if (engine?.outsideTempC == null) "нет t°" else null,
            )
            VerticalDivider(Modifier.fillMaxHeight(0.7f), color = MaterialTheme.colorScheme.outlineVariant)
            HeaderCell(
                modifier = Modifier.weight(0.7f),
                value = engine?.gear?.label ?: "—",
            )
        }
    }
}

@Composable
private fun HeaderCell(
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    value: String,
    unit: String? = null,
    caption: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    captionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val valueFontSize = MaterialTheme.typography.headlineSmall.fontSize * 3
    val unitFontSize = MaterialTheme.typography.titleLarge.fontSize * 1.5f
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                value,
                fontSize = valueFontSize,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
            )
            if (unit != null) {
                Text(
                    unit,
                    modifier = Modifier.padding(start = 8.dp, top = 16.dp),
                    fontSize = unitFontSize,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (caption != null) {
            Text(caption, color = captionColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    icon: ImageVector,
    kwh: String,
    pct: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    muted: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val valueColor = if (muted) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "/100 км",
                    modifier = Modifier.padding(start = 6.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
                if (hint != null) {
                    Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                trailing?.invoke()
            }
            Spacer(Modifier.height(16.dp))
            val metricFontSize = MaterialTheme.typography.headlineSmall.fontSize * 3
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = metricLine(kwh, pct),
                    color = valueColor,
                    fontSize = metricFontSize,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun InclinationCard(
    pitchDegrees: Float?,
    modifier: Modifier = Modifier,
) {
    val valueColor = MaterialTheme.colorScheme.onSurface
    val valueFontSize = MaterialTheme.typography.headlineSmall.fontSize * 3
    val hasValue = pitchDegrees != null && pitchDegrees.isFinite()
    val pitchLabel = if (hasValue) DisplayFormat.pitchDegrees(pitchDegrees) else "—"
    val slopeHint = when {
        !hasValue -> "нет данных"
        pitchDegrees!! > 0.5f -> "подъём"
        pitchDegrees < -0.5f -> "спуск"
        else -> "ровно"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Terrain,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Наклон",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    pitchLabel,
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor,
                )
                Text(
                    "°",
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                    fontSize = MaterialTheme.typography.titleLarge.fontSize * 1.5f,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                slopeHint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ForecastCard(
    windows: List<RangeWindow>,
    charging: Boolean,
    modifier: Modifier = Modifier,
) {
    val extra = RangeThemeColors.extra
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Outlined.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Остаток при текущем темпе",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (charging) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = extra.warning,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "SOC растёт — прогноз по расходу недоступен",
                        color = extra.warning,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (windows.isEmpty()) {
                Text(
                    "ещё нет окна — едем, буфер копится",
                    modifier = Modifier.padding(top = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    Modifier.fillMaxSize().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    windows.forEachIndexed { index, window ->
                        if (index > 0) {
                            VerticalDivider(
                                Modifier.fillMaxHeight(0.7f),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                        WindowColumn(window, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun WindowColumn(window: RangeWindow, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            DisplayFormat.windowKmLabel(window.windowKm),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(6.dp))
        WindowPaceChart(
            windowKm = window.windowKm,
            status = window.status,
            points = window.chart,
            remainingToFillKm = window.remainingToFillKm,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        when (window.status) {
            WindowStatus.READY -> {
                Text(
                    DisplayFormat.km(window.rangeTo0Km, digits = 0),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
                val reserve = window.rangeToReserveKm ?: 0.0
                val reserveText = if (reserve <= 0.0) {
                    "до 20%: резерв"
                } else {
                    "до 20%: ${DisplayFormat.km(reserve, digits = 0)}"
                }
                Text("($reserveText)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            WindowStatus.NEED_MORE_KM -> {
                val left = window.remainingToFillKm ?: window.windowKm
                Text(
                    "ещё ${DisplayFormat.km(left)} до окна",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            WindowStatus.SOC_UNCHANGED -> StatusText("SOC не изменился")
            WindowStatus.CHARGING -> StatusText("зарядка")
            WindowStatus.GAP -> StatusText("разрыв в окне")
            WindowStatus.INVALID -> StatusText("мало данных")
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
    )
}

private fun metricLine(kwh: String, pct: String): String = "$kwh ($pct)"

private fun metricKwh(value: Double?, placeholder: Boolean): String {
    if (placeholder) return "--- кВт·ч"
    return "${DisplayFormat.kWhPer100Number(value)} кВт·ч"
}

private fun metricPct(value: Double?, placeholder: Boolean): String {
    if (placeholder) return "--- %"
    return "${DisplayFormat.pctPer100Number(value)} %"
}

private fun tripHint(engine: EngineView?): String {
    return when {
        engine == null -> "нет данных"
        engine.waitingForDrive && engine.trip.distanceKm <= 0.0 -> "ждём выезд"
        engine.waitingForDrive -> "последняя поездка"
        engine.tripIncomplete -> "поездка неполная"
        else -> "с выезда · ${DisplayFormat.km(engine.trip.distanceKm)}"
    }
}
