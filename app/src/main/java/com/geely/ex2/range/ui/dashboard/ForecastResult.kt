package com.geely.ex2.range.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowStatus
import com.geely.ex2.range.ui.components.AnimatedValue
import com.geely.ex2.range.ui.components.EmptyState
import com.geely.ex2.range.ui.theme.RangeTextStyles
import com.geely.ex2.range.ui.theme.RangeThemeColors
import com.geely.ex2.range.ui.theme.Spacing

/**
 * Готовый прогноз окна: запас до 0 % SOC, запас до резерва
 * и сравнение с собственной оценкой головного устройства.
 */
@Composable
fun ForecastResult(
    window: RangeWindow,
    vehicleRangeRemainingKm: Float?,
    modifier: Modifier = Modifier,
) {
    val reserve = window.rangeToReserveKm ?: 0.0
    val reserveSpent = reserve <= 0.0

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ForecastValue(
                value = DisplayFormat.kmNumber(window.rangeTo0Km),
                caption = "до 0% SOC",
                emphasized = true,
                description = "Прогноз до нуля процентов: " +
                    DisplayFormat.kmNumber(window.rangeTo0Km) + " километров",
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(
                Modifier
                    .fillMaxHeight()
                    .padding(vertical = Spacing.xs),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            ForecastValue(
                value = if (reserveSpent) "0" else DisplayFormat.kmNumber(reserve),
                caption = if (reserveSpent) {
                    "резерв пройден"
                } else {
                    "до резерва " + RangeConstants.RESERVE_SOC_PERCENT.toInt() + "%"
                },
                emphasized = false,
                description = if (reserveSpent) {
                    "Резерв уже пройден"
                } else {
                    "Прогноз до резерва: " + DisplayFormat.kmNumber(reserve) + " километров"
                },
                modifier = Modifier.weight(1f),
            )
        }
        if (vehicleRangeRemainingKm != null && vehicleRangeRemainingKm.isFinite()) {
            Spacer(Modifier.height(Spacing.s))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(Spacing.xs))
            VehicleComparison(
                predictedKm = window.rangeTo0Km,
                vehicleRangeRemainingKm = vehicleRangeRemainingKm,
            )
        }
    }
}

@Composable
private fun ForecastValue(
    value: String,
    caption: String,
    emphasized: Boolean,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "≈",
                style = RangeTextStyles.unit,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 2.dp, bottom = 4.dp),
            )
            AnimatedValue(
                value = value,
                style = if (emphasized) RangeTextStyles.forecastValue else RangeTextStyles.statValue,
                color = if (emphasized) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                "км",
                style = RangeTextStyles.unit,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.xxs, bottom = 4.dp),
            )
        }
        Text(
            caption,
            style = RangeTextStyles.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Оценка головного устройства — вторичный источник сравнения. */
@Composable
private fun VehicleComparison(
    predictedKm: Double?,
    vehicleRangeRemainingKm: Float,
) {
    val extra = RangeThemeColors.extra
    val deltaPercent = DisplayFormat.rangeDeltaPercent(predictedKm, vehicleRangeRemainingKm)
    val deltaLabel = DisplayFormat.rangeDeltaLabel(deltaPercent)
    val tone = vehicleDeltaTone(deltaPercent)
    val close = tone == VehicleDeltaTone.CLOSE
    val toneColor = if (close) extra.success else extra.warning

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "ГУ " + DisplayFormat.km(vehicleRangeRemainingKm.toDouble(), digits = 0),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (deltaLabel != null) {
            Spacer(Modifier.width(Spacing.xs))
            Icon(
                if (close) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = toneColor,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(Spacing.xxs))
            Text(
                deltaLabel + " от ГУ",
                style = MaterialTheme.typography.labelLarge,
                color = toneColor,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = if (close) {
                        "Расхождение с оценкой головного устройства " + deltaLabel + ", в пределах нормы"
                    } else {
                        "Расхождение с оценкой головного устройства " + deltaLabel + ", заметное"
                    }
                },
            )
        }
    }
}

/**
 * Состояния, когда прогноз по окну недоступен.
 * Формулировки статусов движка сохранены.
 */
@Composable
fun ForecastStatus(
    window: RangeWindow?,
    windowKm: Double,
    modifier: Modifier = Modifier,
) {
    val label = DisplayFormat.windowKmLabel(windowKm)
    if (window == null) {
        EmptyState(
            icon = Icons.Outlined.HourglassEmpty,
            title = "Ещё нет окна",
            description = "Едем — буфер копится.",
            modifier = modifier,
        )
        return
    }
    when (window.status) {
        WindowStatus.NEED_MORE_KM -> {
            val left = window.remainingToFillKm ?: window.windowKm
            EmptyState(
                icon = Icons.Outlined.HourglassEmpty,
                title = "Ещё " + DisplayFormat.km(left) + " до окна " + label,
                description = "Продолжайте движение, чтобы накопить данные для прогноза.",
                progress = windowFilledFraction(window),
                modifier = modifier,
            )
        }
        WindowStatus.SOC_UNCHANGED -> EmptyState(
            icon = Icons.Outlined.Info,
            title = "SOC не изменился",
            description = "Расход на этом участке слишком мал для оценки.",
            modifier = modifier,
        )
        WindowStatus.CHARGING -> EmptyState(
            icon = Icons.Outlined.Bolt,
            title = "Зарядка",
            description = "Прогноз возобновится, когда автомобиль поедет.",
            tint = RangeThemeColors.extra.charging,
            modifier = modifier,
        )
        WindowStatus.GAP -> EmptyState(
            icon = Icons.Outlined.LinkOff,
            title = "Разрыв в окне",
            description = "Телеметрия прерывалась — окно наберётся заново.",
            modifier = modifier,
        )
        WindowStatus.INVALID -> EmptyState(
            icon = Icons.Outlined.Info,
            title = "Мало данных",
            description = "Недостаточно данных для оценки по окну " + label + ".",
            modifier = modifier,
        )
        WindowStatus.READY -> Unit
    }
}
