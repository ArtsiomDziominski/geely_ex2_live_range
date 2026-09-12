package com.geely.ex2.range.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.R
import com.geely.ex2.range.domain.engine.EngineView
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.ConsumptionRates
import com.geely.ex2.range.ui.components.AnimatedValue
import com.geely.ex2.range.ui.components.NO_VALUE
import com.geely.ex2.range.ui.components.SectionCard
import com.geely.ex2.range.ui.components.SkeletonBox
import com.geely.ex2.range.ui.components.StatusChip
import com.geely.ex2.range.ui.layout.LocalRangeLayout
import com.geely.ex2.range.ui.theme.RangeTextStyles
import com.geely.ex2.range.ui.theme.RangeThemeColors
import com.geely.ex2.range.ui.theme.Spacing

/** Ниже этой ширины пара карточек уже нечитаема — раскладываем в столбец. */
private val SIDE_BY_SIDE_MIN_WIDTH: Dp = 360.dp

/**
 * Блок поездки: расход за период и расход текущей поездки.
 * В узком окне — колонкой, иначе — рядом, на равную высоту.
 */
@Composable
fun TripStatsSection(
    engine: EngineView?,
    onResetPeriod: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = LocalRangeLayout.current
    val gap = layout.sectionGap
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < SIDE_BY_SIDE_MIN_WIDTH) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
                ConsumptionCard(engine, onResetPeriod, Modifier.fillMaxWidth())
                TripConsumptionCard(engine, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                ConsumptionCard(
                    engine,
                    onResetPeriod,
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                TripConsumptionCard(
                    engine,
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

/** Расход за всё время с момента последнего сброса. */
@Composable
fun ConsumptionCard(
    engine: EngineView?,
    onResetPeriod: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmReset by remember { mutableStateOf(false) }
    val charging = engine?.charging == true

    SectionCard(
        modifier = modifier,
        title = "Расход",
        iconPainter = painterResource(R.drawable.directions_car_24),
        subtitle = "всего",
        contentGap = Spacing.xxs,
        action = {
            IconButton(
                onClick = { confirmReset = true },
                modifier = Modifier.size(Spacing.touchTarget),
            ) {
                Icon(
                    Icons.Outlined.RestartAlt,
                    contentDescription = "Сбросить статистику за всё время",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) {
        ConsumptionValue(rates = engine?.period, muted = charging, label = "Расход за всё время")
        if (charging) {
            ChargingConsumptionNote()
        }
    }

    if (confirmReset) {
        ResetPeriodDialog(
            onConfirm = {
                confirmReset = false
                onResetPeriod()
            },
            onDismiss = { confirmReset = false },
        )
    }
}

/** Расход текущей поездки со всеми состояниями движка. */
@Composable
fun TripConsumptionCard(
    engine: EngineView?,
    modifier: Modifier = Modifier,
) {
    val charging = engine?.charging == true
    val waiting = engine?.waitingForDrive == true
    val muted = charging || waiting

    SectionCard(
        modifier = modifier,
        title = "Поездка",
        icon = Icons.Outlined.Route,
        subtitle = tripHint(engine),
        contentGap = Spacing.xxs,
    ) {
        ConsumptionValue(rates = engine?.trip, muted = muted, label = "Расход за текущую поездку")
        when {
            charging -> ChargingConsumptionNote()
            waiting && (engine?.trip?.distanceKm ?: 0.0) <= 0.0 -> Text(
                "Данные появятся после выезда",
                style = RangeTextStyles.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            engine?.tripIncomplete == true -> Text(
                "Часть пути не попала в расчёт",
                style = RangeTextStyles.caption,
                color = RangeThemeColors.extra.warning,
            )
        }
    }
}

@Composable
private fun ConsumptionValue(
    rates: ConsumptionRates?,
    muted: Boolean,
    label: String,
) {
    val kwh = if (muted) NO_VALUE else DisplayFormat.kWhPer100Number(rates?.kWhPer100)
    val pct = if (muted) NO_VALUE else DisplayFormat.pctPer100Number(rates?.pctPer100)
    val description = if (muted) {
        label + ": нет данных"
    } else {
        label + " " + kwh + " киловатт-часов и " + pct + " процентов на 100 километров"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedValue(
                value = kwh,
                style = RangeTextStyles.statValue,
                color = if (muted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                "кВт·ч / 100 км",
                style = RangeTextStyles.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.xxs, bottom = 4.dp),
            )
        }
        Text(
            pct + " % / 100 км",
            style = RangeTextStyles.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChargingConsumptionNote() {
    val extra = RangeThemeColors.extra
    Column {
        Spacer(Modifier.height(Spacing.xxs))
        StatusChip(
            text = "Зарядка",
            icon = Icons.Outlined.Bolt,
            container = extra.charging.copy(alpha = 0.16f),
            content = extra.charging,
        )
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            "Расход не считается во время зарядки",
            style = RangeTextStyles.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResetPeriodDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сбросить статистику?") },
        text = { Text("Все данные текущего периода будут сброшены.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Сбросить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

/** Скелет блока поездки на время первой загрузки. */
@Composable
fun TripStatsSkeleton(modifier: Modifier = Modifier) {
    val layout = LocalRangeLayout.current
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(layout.sectionGap)) {
        repeat(2) {
            SectionCard(modifier = Modifier.weight(1f), contentGap = Spacing.xs) {
                SkeletonBox(height = 14.dp, widthFraction = 0.5f)
                SkeletonBox(height = 26.dp, widthFraction = 0.7f)
                SkeletonBox(height = 12.dp, widthFraction = 0.9f)
            }
        }
    }
}
