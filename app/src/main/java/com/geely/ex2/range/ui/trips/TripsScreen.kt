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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Thermostat
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.ActiveTripView
import com.geely.ex2.range.domain.model.DriveStatsView
import com.geely.ex2.range.domain.model.TripRecord
import com.geely.ex2.range.ui.TripsUiState
import com.geely.ex2.range.ui.components.SectionCard
import com.geely.ex2.range.ui.layout.LocalRangeLayout
import com.geely.ex2.range.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val PAGE_SIZE = 10

/**
 * Поездки: рекорд макс. цикла между зарядками, текущая поездка (если едем),
 * фильтр по дате, история поездок (самые свежие первыми, по 10 на страницу) и
 * инфо-модалка с расшифровкой значков. Экран прокручивается, высоты — по содержимому.
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
    var showLegend by remember { mutableStateOf(false) }
    var pendingDeleteTrip by remember { mutableStateOf<TripRecord?>(null) }
    var page by remember { mutableStateOf(0) }

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
    // Новая дата фильтра или изменившийся список — начинаем со страницы 1.
    LaunchedEffect(selectedDateMillis, filteredTrips.size) { page = 0 }

    val totalPages = ((filteredTrips.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
    val safePage = page.coerceIn(0, totalPages - 1)
    val pagedTrips = remember(filteredTrips, safePage) {
        filteredTrips.drop(safePage * PAGE_SIZE).take(PAGE_SIZE)
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
                onShowLegend = { showLegend = true },
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
            items(pagedTrips, key = { "${it.finishedAtMs}-${it.distanceKm}" }) { trip ->
                TripCard(trip, onDelete = { pendingDeleteTrip = trip })
            }
            if (totalPages > 1) {
                item(key = "pager") {
                    TripsPager(
                        page = safePage,
                        totalPages = totalPages,
                        onPrev = { page = (safePage - 1).coerceAtLeast(0) },
                        onNext = { page = (safePage + 1).coerceAtMost(totalPages - 1) },
                    )
                }
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

    if (showLegend) {
        IconLegendDialog(onDismiss = { showLegend = false })
    }
}

@Composable
private fun TripsFilterRow(
    selectedDateMillis: Long?,
    onOpenPicker: () -> Unit,
    onClear: () -> Unit,
    onClearAll: () -> Unit,
    onShowLegend: () -> Unit,
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
        IconButton(onClick = onShowLegend) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "Что означают значки на этой странице",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TripsPager(
    page: Int,
    totalPages: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev, enabled = page > 0) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Предыдущая страница")
        }
        Text(
            "Страница ${page + 1} из $totalPages",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.s),
        )
        IconButton(onClick = onNext, enabled = page < totalPages - 1) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Следующая страница")
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
                StatEntry(Icons.Outlined.Straighten, DisplayFormat.km(records.maxChargeCycleKm), "Пройдено: ${DisplayFormat.km(records.maxChargeCycleKm)}"),
                StatEntry(
                    Icons.Outlined.Bolt,
                    DisplayFormat.socPoints(records.maxChargeCycleSocUsedPercent),
                    "Заряд потрачен: ${DisplayFormat.socPoints(records.maxChargeCycleSocUsedPercent)}",
                ),
                StatEntry(
                    Icons.Outlined.Speed,
                    records.maxChargeCycleAvgSpeedKmh?.let { DisplayFormat.speedKmh(it.toFloat()) } ?: "—",
                    "Средняя скорость: ${records.maxChargeCycleAvgSpeedKmh?.let { DisplayFormat.speedKmh(it.toFloat()) } ?: "нет данных"}",
                ),
                StatEntry(
                    Icons.Outlined.Thermostat,
                    records.maxChargeCycleAvgTempC?.let { DisplayFormat.tempC(it.toFloat()) } ?: "—",
                    "Средняя температура воздуха: ${records.maxChargeCycleAvgTempC?.let { DisplayFormat.tempC(it.toFloat()) } ?: "нет данных"}",
                ),
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
                StatEntry(Icons.Outlined.Straighten, DisplayFormat.km(trip.distanceKm), "Пройдено: ${DisplayFormat.km(trip.distanceKm)}"),
                StatEntry(
                    Icons.Outlined.Speed,
                    trip.avgSpeedKmh?.let { DisplayFormat.speedKmh(it.toFloat()) } ?: "—",
                    "Средняя скорость: ${trip.avgSpeedKmh?.let { DisplayFormat.speedKmh(it.toFloat()) } ?: "нет данных"}",
                ),
                StatEntry(
                    Icons.Outlined.Bolt,
                    socRangeLabel(trip.socStartPercent, trip.socNowPercent, trip.socUsedPercent),
                    "Заряд: старт ${DisplayFormat.socPercent(trip.socStartPercent)}, сейчас ${DisplayFormat.socPercent(trip.socNowPercent)}, потрачено ${DisplayFormat.socPoints(trip.socUsedPercent)}",
                ),
                StatEntry(
                    Icons.Outlined.Thermostat,
                    tempRangeLabel(trip.tempStartC, trip.tempNowC, trip.avgTempC),
                    "Температура: старт ${DisplayFormat.tempC(trip.tempStartC)}, сейчас ${DisplayFormat.tempC(trip.tempNowC)}",
                ),
            ),
        )
    }
}

/** Одна сохранённая поездка (P → P), на всю ширину, данные — сеткой значков вместо подписей. */
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
                StatEntry(Icons.Outlined.Straighten, DisplayFormat.km(trip.distanceKm), "Пройдено: ${DisplayFormat.km(trip.distanceKm)}"),
                StatEntry(
                    Icons.Outlined.Speed,
                    trip.avgSpeedKmh?.let { DisplayFormat.speedKmh(it.toFloat()) } ?: "—",
                    "Средняя скорость: ${trip.avgSpeedKmh?.let { DisplayFormat.speedKmh(it.toFloat()) } ?: "нет данных"}",
                ),
                StatEntry(
                    Icons.Outlined.Bolt,
                    socRangeLabel(trip.socStartPercent, trip.socEndPercent, trip.socUsedPercent),
                    "Заряд: старт ${DisplayFormat.socPercent(trip.socStartPercent)}, финиш ${DisplayFormat.socPercent(trip.socEndPercent)}, потрачено ${DisplayFormat.socPoints(trip.socUsedPercent)}",
                ),
                StatEntry(
                    Icons.Outlined.Thermostat,
                    tempRangeLabel(trip.tempStartC, trip.tempEndC, trip.avgTempC),
                    "Температура: старт ${DisplayFormat.tempC(trip.tempStartC)}, финиш ${DisplayFormat.tempC(trip.tempEndC)}",
                ),
            ),
        )
    }
}

/** Одна ячейка сетки: значок + значение, без подписи — расшифровка значков в [IconLegendDialog]. */
private data class StatEntry(
    val icon: ImageVector,
    val value: String,
    val description: String,
)

/**
 * Плотная сетка «значок сверху / значение снизу», без текстовых подписей — по 4 ячейки
 * в ряд (2 в компактную ширину), с разделителями. Число элементов должно делиться на число
 * колонок нацело, иначе последний ряд будет короче остальных.
 */
@Composable
private fun StatGrid(stats: List<StatEntry>) {
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
                row.forEachIndexed { index, entry ->
                    if (index > 0) {
                        VerticalDivider(Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    StatCell(entry, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCell(entry: StatEntry, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(vertical = Spacing.xs, horizontal = Spacing.xxs)
            .clearAndSetSemantics { contentDescription = entry.description },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            entry.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(Spacing.xxs))
        Text(
            entry.value,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

/** «80% - 20% (60%)»: старт, финиш/сейчас, и сколько заряда потрачено суммарно. */
private fun socRangeLabel(startPercent: Float?, endPercent: Float?, usedPercent: Double): String {
    val start = startPercent?.let { "${DisplayFormat.socNumber(it)}%" } ?: "—"
    val end = endPercent?.let { "${DisplayFormat.socNumber(it)}%" } ?: "—"
    return "$start - $end (${DisplayFormat.socPoints(usedPercent)})"
}

/** «6° - 9° (7°)»: старт, финиш/сейчас, и средняя за поездку. */
private fun tempRangeLabel(startC: Float?, endC: Float?, avgC: Double?): String {
    val start = startC?.let { "${DisplayFormat.tempNumber(it)}°" } ?: "—"
    val end = endC?.let { "${DisplayFormat.tempNumber(it)}°" } ?: "—"
    val avg = avgC?.let { "${DisplayFormat.tempNumber(it.toFloat())}°" }
    return if (avg != null) "$start - $end ($avg)" else "$start - $end"
}

/** Расшифровка значков карточек поездок — на весь экран, вызывается кнопкой ⓘ в фильтре. */
@Composable
private fun IconLegendDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Значки в карточках поездок") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                LegendRow(Icons.Outlined.Straighten, "Пройдено расстояние, км")
                LegendRow(Icons.Outlined.Speed, "Средняя скорость за поездку")
                LegendRow(Icons.Outlined.Bolt, "Заряд: старт – финиш (сколько потрачено суммарно)")
                LegendRow(Icons.Outlined.Thermostat, "Температура воздуха: старт – финиш (средняя за поездку)")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Понятно") }
        },
    )
}

@Composable
private fun LegendRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
        )
        Spacer(Modifier.width(Spacing.s))
        Text(text, style = MaterialTheme.typography.bodyMedium)
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
