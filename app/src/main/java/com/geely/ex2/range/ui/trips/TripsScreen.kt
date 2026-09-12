package com.geely.ex2.range.ui.trips

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.ActiveTripView
import com.geely.ex2.range.domain.model.DriveStatsView
import com.geely.ex2.range.domain.model.TripRecord
import com.geely.ex2.range.ui.TripsUiState
import com.geely.ex2.range.ui.components.SectionCard
import com.geely.ex2.range.ui.layout.LocalRangeLayout
import com.geely.ex2.range.ui.theme.RangeTextStyles
import com.geely.ex2.range.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Поездки: рекорд макс. цикла между зарядками, текущая поездка (если едем),
 * фильтр по дате и журнал поездок (самые свежие первыми). Экран прокручивается,
 * высоты — по содержимому.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    state: TripsUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onDeleteTrip: (TripRecord) -> Unit = {},
    onClearTrips: () -> Unit = {},
) {
    val layout = LocalRangeLayout.current
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var pendingDeleteTrip by remember { mutableStateOf<TripRecord?>(null) }

    val filteredTrips = remember(state.trips, selectedDateMillis) {
        val sorted = state.trips.sortedByDescending { it.finishedAtMs }
        val filterDate = selectedDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
        }
        if (filterDate == null) {
            sorted
        } else {
            sorted.filter { trip -> tripLocalDate(trip.finishedAtMs) == filterDate }
        }
    }

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
            ChargeCycleRecordCard(state.driveStats)
        }

        state.currentTrip?.let { current ->
            item(key = "current") {
                CurrentTripCard(current)
            }
        }

        item(key = "filter") {
            TripsFilterRow(
                selectedDateMillis = selectedDateMillis,
                onOpenPicker = { showDatePicker = true },
                onClear = { selectedDateMillis = null },
                onClearAll = { showClearAllConfirm = true },
                hasTrips = state.trips.isNotEmpty(),
            )
        }

        if (filteredTrips.isEmpty()) {
            item(key = "empty") {
                Text(
                    if (state.trips.isEmpty()) {
                        "Поездок ещё нет — едем, первая появится тут после парковки"
                    } else {
                        "На эту дату поездок нет"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.s),
                )
            }
        } else {
            items(filteredTrips, key = { "${it.finishedAtMs}-${it.distanceKm}" }) { trip ->
                TripCard(trip, onDelete = { pendingDeleteTrip = trip })
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("Ок") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Удалить всю историю поездок?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearAllConfirm = false
                    onClearTrips()
                }) { Text("Удалить всё") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) { Text("Отмена") }
            },
        )
    }

    pendingDeleteTrip?.let { trip ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTrip = null },
            title = { Text("Удалить эту поездку?") },
            text = { Text("${tripDateTimeLabel(trip.finishedAtMs)} · ${DisplayFormat.km(trip.distanceKm)}") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteTrip = null
                    onDeleteTrip(trip)
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTrip = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun TripsFilterRow(
    selectedDateMillis: Long?,
    onOpenPicker: () -> Unit,
    onClear: () -> Unit,
    onClearAll: () -> Unit,
    hasTrips: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onOpenPicker) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xxs))
            Text(selectedDateMillis?.let { filterDateLabel(it) } ?: "Выбрать дату")
        }
        if (selectedDateMillis != null) {
            TextButton(onClick = onClear) { Text("Сбросить") }
        }
        Spacer(Modifier.weight(1f))
        if (hasTrips) {
            IconButton(onClick = onClearAll) {
                Icon(
                    Icons.Outlined.DeleteSweep,
                    contentDescription = "Удалить всю историю",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
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
    ) {
        StatGrid(
            listOf(
                "Пройдено" to DisplayFormat.km(records.maxChargeCycleKm),
                "Заряд потрачен" to DisplayFormat.socPoints(records.maxChargeCycleSocUsedPercent),
                "Средняя скорость" to (records.maxChargeCycleAvgSpeedKmh?.let { DisplayFormat.speedKmh(it.toFloat()) } ?: "—"),
                "Средняя t°" to (records.maxChargeCycleAvgTempC?.let { DisplayFormat.tempC(it.toFloat()) } ?: "—"),
            ),
        )
    }
}

/** Поездка в процессе — данные обновляются на лету, ещё не сохранена в историю. */
@Composable
private fun CurrentTripCard(trip: ActiveTripView) {
    SectionCard(
        title = "В пути сейчас",
        icon = Icons.Outlined.Route,
        subtitle = "поездка ещё не завершена",
    ) {
        StatGrid(
            listOf(
                "Пройдено" to DisplayFormat.km(trip.distanceKm),
                "t° старта" to DisplayFormat.tempC(trip.tempStartC),
                "t° сейчас" to DisplayFormat.tempC(trip.tempNowC),
                "Средняя t°" to (trip.avgTempC?.let { DisplayFormat.tempC(it.toFloat()) } ?: "—"),
                "Средняя скорость" to (trip.avgSpeedKmh?.let { DisplayFormat.speedKmh(it.toFloat()) } ?: "—"),
                "Заряд на старте" to DisplayFormat.socPercent(trip.socStartPercent),
                "Заряд сейчас" to DisplayFormat.socPercent(trip.socNowPercent),
                "Заряд потрачен" to DisplayFormat.socPoints(trip.socUsedPercent),
            ),
        )
    }
}

/** Одна сохранённая поездка (P → P), на всю ширину, данные — сеткой вместо стопки строк. */
@Composable
private fun TripCard(trip: TripRecord, onDelete: () -> Unit) {
    SectionCard(
        title = tripDateTimeLabel(trip.finishedAtMs),
        icon = Icons.Outlined.Route,
        action = {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Удалить поездку",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        StatGrid(
            listOf(
                "Пройдено" to DisplayFormat.km(trip.distanceKm),
                "t° старта" to DisplayFormat.tempC(trip.tempStartC),
                "t° финиша" to DisplayFormat.tempC(trip.tempEndC),
                "Средняя t°" to (trip.avgTempC?.let { DisplayFormat.tempC(it.toFloat()) } ?: "—"),
                "Средняя скорость" to (trip.avgSpeedKmh?.let { DisplayFormat.speedKmh(it.toFloat()) } ?: "—"),
                "Заряд на старте" to DisplayFormat.socPercent(trip.socStartPercent),
                "Заряд на финише" to DisplayFormat.socPercent(trip.socEndPercent),
                "Заряд потрачен" to DisplayFormat.socPoints(trip.socUsedPercent),
            ),
        )
    }
}

/**
 * Плотная сетка «подпись сверху / значение снизу» вместо строк на всю ширину — по 4 ячейки
 * в ряд (2 в компактную ширину), с разделителями. Число элементов должно делиться на число
 * колонок нацело, иначе последний ряд будет короче остальных.
 */
@Composable
private fun StatGrid(stats: List<Pair<String, String>>) {
    val columns = if (LocalRangeLayout.current.isCompact) 2 else 4
    Column {
        stats.chunked(columns).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                row.forEachIndexed { index, (label, value) ->
                    if (index > 0) {
                        VerticalDivider(Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    StatCell(label, value, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = Spacing.xs, horizontal = Spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = RangeTextStyles.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xxs))
        Text(value, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
    }
}

private val filterDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("ru"))
private val tripDateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("ru"))

/** Дата, выбранная в календаре, — DatePicker отдаёт полночь UTC для выбранного дня. */
private fun filterDateLabel(utcMillis: Long): String {
    return Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate().format(filterDateFormatter)
}

/** Момент завершения поездки — в часовом поясе устройства. */
private fun tripLocalDate(finishedAtMs: Long) =
    Instant.ofEpochMilli(finishedAtMs).atZone(ZoneId.systemDefault()).toLocalDate()

private fun tripDateTimeLabel(finishedAtMs: Long): String {
    return Instant.ofEpochMilli(finishedAtMs).atZone(ZoneId.systemDefault()).format(tripDateTimeFormatter)
}
