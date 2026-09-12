package com.geely.ex2.range.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.domain.engine.EngineView
import com.geely.ex2.range.ui.components.ErrorState
import com.geely.ex2.range.ui.components.SectionCard
import com.geely.ex2.range.ui.layout.LocalRangeLayout
import com.geely.ex2.range.ui.theme.Spacing

/**
 * Главный экран: вертикальный дашборд с приоритетом
 * SOC → запас хода → приборные показатели → поездка → прогноз.
 *
 * Высоты блоков определяются содержимым; фиксированных процентов экрана нет.
 */
@Composable
fun DashboardScreen(
    engine: EngineView?,
    connectError: String?,
    onResetPeriod: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val layout = LocalRangeLayout.current

    when {
        engine == null && connectError != null -> DashboardError(
            message = connectError,
            onRetry = onRetry,
            modifier = modifier,
            contentPadding = contentPadding,
        )

        engine == null -> DashboardSkeleton(modifier = modifier, contentPadding = contentPadding)

        layout.twoPane -> Row(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = layout.screenPadding, vertical = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(layout.sectionGap),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(layout.sectionGap),
            ) {
                LiveVehicleCard(engine)
                TripStatsSection(engine, onResetPeriod)
                if (connectError != null) {
                    TelemetryWarning(connectError, onRetry)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                ForecastCard(
                    windows = engine.windows,
                    charging = engine.charging,
                    vehicleRangeRemainingKm = engine.vehicleRangeRemainingKm,
                )
            }
        }

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = layout.screenPadding,
                end = layout.screenPadding,
                top = Spacing.s + contentPadding.calculateTopPadding(),
                bottom = Spacing.m + contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(layout.sectionGap),
        ) {
            item(key = "live") { LiveVehicleCard(engine) }
            item(key = "trip") { TripStatsSection(engine, onResetPeriod) }
            item(key = "forecast") {
                ForecastCard(
                    windows = engine.windows,
                    charging = engine.charging,
                    vehicleRangeRemainingKm = engine.vehicleRangeRemainingKm,
                )
            }
            if (connectError != null) {
                item(key = "warning") { TelemetryWarning(connectError, onRetry) }
            }
        }
    }
}

/** Полноэкранная ошибка — только когда телеметрии нет совсем. */
@Composable
private fun DashboardError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val layout = LocalRangeLayout.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = layout.screenPadding, vertical = Spacing.m),
        contentAlignment = Alignment.Center,
    ) {
        SectionCard(contentPadding = Spacing.l) {
            ErrorState(
                title = "Не удалось получить данные",
                description = message,
                onRetry = onRetry,
            )
        }
    }
}

/** Данные частично есть — предупреждение не перекрывает дашборд. */
@Composable
private fun TelemetryWarning(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier, contentPadding = Spacing.m) {
        ErrorState(
            title = "Телеметрия читается не полностью",
            description = message,
            onRetry = onRetry,
        )
    }
}

/** Скелет главной на время первого получения данных. */
@Composable
private fun DashboardSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val layout = LocalRangeLayout.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = layout.screenPadding, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(layout.sectionGap),
    ) {
        LiveVehicleCardSkeleton()
        TripStatsSkeleton()
        ForecastSkeleton()
        Spacer(Modifier.height(Spacing.m))
    }
}
