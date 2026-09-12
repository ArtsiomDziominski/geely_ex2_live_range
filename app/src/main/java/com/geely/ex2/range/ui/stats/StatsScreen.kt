package com.geely.ex2.range.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.R
import com.geely.ex2.range.domain.calculation.Consumption
import com.geely.ex2.range.domain.engine.EngineView
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.ConsumptionRates
import com.geely.ex2.range.domain.model.DriveStatsView
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowStatus
import com.geely.ex2.range.ui.StatsUiState
import com.geely.ex2.range.ui.components.SectionCard
import com.geely.ex2.range.ui.layout.LocalRangeLayout
import com.geely.ex2.range.ui.theme.RangeTextStyles
import com.geely.ex2.range.ui.theme.Spacing

/**
 * Подробная статистика: рекорды, расход за период и поездку, текущие значения
 * и разбор окон прогноза. Экран прокручивается, высоты — по содержимому.
 */
@Composable
fun StatsScreen(
    state: StatsUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val engine = state.engine
    val charging = engine?.charging == true
    val records = state.driveStats
    val layout = LocalRangeLayout.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = layout.screenPadding,
            end = layout.screenPadding,
            top = Spacing.s + contentPadding.calculateTopPadding(),
            bottom = Spacing.m + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(layout.sectionGap),
    ) {
        item(key = "records") {
            RecordBlocks(records = records, compact = layout.isCompact, gap = layout.sectionGap)
        }

        item(key = "consumption") {
            ConsumptionBlocks(engine = engine, charging = charging, compact = layout.isCompact, gap = layout.sectionGap)
        }

        item(key = "windows") {
            SectionCard(
                title = "Окна прогноза",
                icon = Icons.AutoMirrored.Outlined.ShowChart,
                subtitle = "темп последних 5 / 15 / 30 км",
            ) {
                val windows = engine?.windows.orEmpty()
                if (windows.isEmpty()) {
                    Text(
                        "Ещё нет окна — едем, буфер копится",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (layout.isCompact) {
                    windows.forEachIndexed { index, window ->
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        WindowStatsBlock(window, charging)
                    }
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    ) {
                        windows.forEachIndexed { index, window ->
                            if (index > 0) {
                                VerticalDivider(
                                    Modifier.fillMaxHeight(),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                WindowStatsBlock(window, charging)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Два блока рекордов: макс. поездка с P до P, и разбор макс. пробега между двумя зарядками
 * (км, потраченный заряд, средняя скорость, средняя t° воздуха за этот цикл).
 */
@Composable
private fun RecordBlocks(
    records: DriveStatsView,
    compact: Boolean,
    gap: Dp,
) {
    val trip = @Composable { modifier: Modifier ->
        SectionCard(
            modifier = modifier,
            title = "Макс. поездка",
            icon = Icons.Outlined.Route,
            subtitle = "с выезда с P до возврата на P",
        ) {
            Text(DisplayFormat.km(records.maxTripKm), style = RangeTextStyles.statValue)
        }
    }
    val chargeCycle = @Composable { modifier: Modifier ->
        SectionCard(
            modifier = modifier,
            title = "Макс. поездка между зарядками",
            icon = Icons.Outlined.EmojiEvents,
            subtitle = "от зарядки до следующей зарядки",
            contentGap = Spacing.xxs,
        ) {
            StatRow("Пройдено", DisplayFormat.km(records.maxChargeCycleKm))
            StatRow("Заряд потрачен", DisplayFormat.socPoints(records.maxChargeCycleSocUsedPercent))
            StatRow("Средняя скорость", records.maxChargeCycleAvgSpeedKmh?.let { DisplayFormat.speedKmh(it.toFloat()) } ?: "—")
            StatRow("Средняя t° воздуха", records.maxChargeCycleAvgTempC?.let { DisplayFormat.tempC(it.toFloat()) } ?: "—")
        }
    }

    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            trip(Modifier.fillMaxWidth())
            chargeCycle(Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            trip(Modifier.weight(1f))
            chargeCycle(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ConsumptionBlocks(
    engine: EngineView?,
    charging: Boolean,
    compact: Boolean,
    gap: Dp,
) {
    val period = @Composable { modifier: Modifier ->
        SectionCard(
            modifier = modifier,
            title = "Расход за все время",
            iconPainter = painterResource(R.drawable.directions_car_24),
            subtitle = "с последнего сброса",
            contentGap = Spacing.xxs,
        ) {
            ConsumptionRows(engine?.period, engine?.usableCapacityKwh, charging)
        }
    }
    val trip = @Composable { modifier: Modifier ->
        SectionCard(
            modifier = modifier,
            title = "Расход за текущую поездку",
            icon = Icons.Outlined.Route,
            subtitle = tripHint(engine),
            contentGap = Spacing.xxs,
        ) {
            StatRow("Время", if (charging) "—" else DisplayFormat.duration(engine?.tripDurationMs))
            ConsumptionRows(
                rates = engine?.trip,
                capacityKwh = engine?.usableCapacityKwh,
                placeholder = charging || engine?.waitingForDrive == true,
            )
        }
    }
    val now = @Composable { modifier: Modifier ->
        SectionCard(
            modifier = modifier,
            title = "Сейчас",
            icon = Icons.Outlined.Speed,
            subtitle = if (engine?.odometerTrusted == true) "одометр доверен" else "одометр не доверен",
            contentGap = Spacing.xxs,
        ) {
            StatRow("SOC", DisplayFormat.socPercent(engine?.socPercent))
            StatRow("Скорость", DisplayFormat.speedKmh(engine?.speedKmh))
            StatRow("t° улицы", DisplayFormat.tempC(engine?.outsideTempC))
            StatRow("Одометр", DisplayFormat.odometerKm(engine?.odometerKm) ?: "—")
            StatRow("Буфер", DisplayFormat.km(engine?.bufferCoveredKm))
            StatRow(
                "Ёмкость C",
                engine?.usableCapacityKwh?.let { DisplayFormat.energyKwh(it) } ?: "—",
            )
        }
    }

    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            period(Modifier.fillMaxWidth())
            trip(Modifier.fillMaxWidth())
            now(Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            period(Modifier.weight(1f))
            trip(Modifier.weight(1f))
            now(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ConsumptionRows(
    rates: ConsumptionRates?,
    capacityKwh: Double?,
    placeholder: Boolean,
) {
    val energy = if (placeholder || rates == null) {
        null
    } else {
        Consumption.energyKwh(rates.socUsedPoints, capacityKwh)
    }
    StatRow("Пройдено", if (placeholder) "—" else DisplayFormat.km(rates?.distanceKm))
    StatRow("SOC потрачено", if (placeholder) "—" else DisplayFormat.socPoints(rates?.socUsedPoints))
    StatRow("Энергия", if (placeholder) "—" else DisplayFormat.energyKwh(energy))
    StatRow("кВт·ч / 100 км", if (placeholder) "—" else DisplayFormat.kWhPer100(rates?.kWhPer100))
    StatRow("% / 100 км", if (placeholder) "—" else DisplayFormat.pctPer100(rates?.pctPer100))
}

@Composable
private fun WindowStatsBlock(window: RangeWindow, charging: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            DisplayFormat.windowKmLabel(window.windowKm),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Spacing.xxs))
        when {
            charging || window.status == WindowStatus.CHARGING -> StatRow("Статус", "зарядка")
            window.status == WindowStatus.NEED_MORE_KM -> {
                val left = window.remainingToFillKm ?: window.windowKm
                StatRow("Статус", "ещё ${DisplayFormat.km(left)}")
            }
            window.status == WindowStatus.READY -> {
                StatRow("ΔSOC", DisplayFormat.socPoints(window.deltaSocPoints))
                StatRow("до 0%", DisplayFormat.km(window.rangeTo0Km, digits = 0))
                StatRow("до 20%", DisplayFormat.km(window.rangeToReserveKm, digits = 0))
                StatRow("средняя скорость", avgSpeedLabel(window))
            }
            window.status == WindowStatus.SOC_UNCHANGED -> StatRow("Статус", "SOC не изменился")
            window.status == WindowStatus.GAP -> StatRow("Статус", "разрыв в окне")
            window.status == WindowStatus.INVALID -> StatRow("Статус", "мало данных")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

private fun tripHint(engine: EngineView?): String {
    return when {
        engine == null -> "нет данных"
        engine.waitingForDrive && engine.trip.distanceKm <= 0.0 -> "ждём выезд"
        engine.waitingForDrive -> "последняя поездка"
        engine.tripIncomplete -> "поездка неполная"
        else -> "с выезда"
    }
}

private fun avgSpeedLabel(window: RangeWindow): String {
    val speeds = window.chart.mapNotNull { point ->
        point.speedKmh?.takeIf { it.isFinite() && it >= 0f }
    }
    if (speeds.size < 2) return "—"
    return DisplayFormat.speedKmh(speeds.average().toFloat())
}
