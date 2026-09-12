package com.geely.ex2.range.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.DriveStatsView
import com.geely.ex2.range.ui.StatsUiState
import com.geely.ex2.range.ui.components.SectionCard
import com.geely.ex2.range.ui.layout.LocalRangeLayout
import com.geely.ex2.range.ui.theme.Spacing

/**
 * Подробная статистика: рекорд макс. цикла между зарядками. Экран прокручивается,
 * высоты — по содержимому.
 */
@Composable
fun StatsScreen(
    state: StatsUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
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
            ChargeCycleRecordCard(records)
        }
    }
}

/** Разбор макс. пробега между двумя зарядками: км, потраченный заряд, средняя скорость, средняя t° воздуха. */
@Composable
private fun ChargeCycleRecordCard(records: DriveStatsView) {
    SectionCard(
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
