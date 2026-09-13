package com.geely.ex2.range.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.domain.engine.EngineView
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.ui.components.AnimatedValue
import com.geely.ex2.range.ui.components.MetricTile
import com.geely.ex2.range.ui.components.NO_VALUE
import com.geely.ex2.range.ui.components.SectionCard
import com.geely.ex2.range.ui.components.SkeletonBox
import com.geely.ex2.range.ui.components.StatusChip
import com.geely.ex2.range.ui.layout.LocalRangeLayout
import com.geely.ex2.range.ui.theme.RangeTextStyles
import com.geely.ex2.range.ui.theme.RangeThemeColors
import com.geely.ex2.range.ui.theme.Spacing

/** Ниже этой ширины SOC и приборные плитки ставятся друг под друга. */
private val SIDE_BY_SIDE_MIN_WIDTH = 680.dp

/**
 * Главный блок экрана: SOC, запас хода и четыре приборных показателя.
 * В узком окне — колонкой, в широком — в две колонки.
 */
@Composable
fun LiveVehicleCard(
    engine: EngineView?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        // Две колонки только если каждой половине хватает ширины на крупные числа.
        val sideBySide = maxWidth >= SIDE_BY_SIDE_MIN_WIDTH
        SectionCard(contentPadding = Spacing.l, contentGap = Spacing.m) {
            if (!sideBySide) {
                SocAndRange(engine)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                VehicleStatusGrid(engine)
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SocAndRange(engine, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(Spacing.l))
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Spacer(Modifier.width(Spacing.l))
                    VehicleStatusGrid(engine, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SocAndRange(
    engine: EngineView?,
    modifier: Modifier = Modifier,
) {
    val extra = RangeThemeColors.extra
    val short = LocalRangeLayout.current.isShort
    val socStyle = if (short) RangeTextStyles.socValueCompact else RangeTextStyles.socValue
    val rangeStyle = if (short) RangeTextStyles.rangeValueCompact else RangeTextStyles.rangeValue
    val soc = engine?.socPercent
    val charging = engine?.charging == true
    val socText = DisplayFormat.socNumber(soc)
    val hasSoc = soc != null && soc.isFinite()
    val hero = heroRange(engine)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.semantics {
                    contentDescription = if (hasSoc) {
                        "Заряд батареи $socText процентов"
                    } else {
                        "Заряд батареи: нет данных"
                    }
                },
            ) {
                AnimatedValue(
                    value = socText,
                    style = socStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (hasSoc) {
                    Text(
                        "%",
                        style = RangeTextStyles.unit,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Spacing.xxs, bottom = 8.dp),
                    )
                }
            }
            RangeHero(hero, rangeStyle)
        }
        KmUntilLowSocLine(hero, soc)
        AnimatedVisibility(visible = charging, enter = fadeIn(), exit = fadeOut()) {
            val delta = DisplayFormat.socDeltaShort(engine?.socDeltaPoints)
            StatusChip(
                text = if (delta != null) "Зарядка · $delta" else "Зарядка",
                icon = Icons.Outlined.Bolt,
                container = extra.charging.copy(alpha = 0.16f),
                content = extra.charging,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }

        Spacer(Modifier.height(Spacing.s))
        SocBatteryIndicator(socPercent = soc, charging = charging)
    }
}

@Composable
private fun RangeHero(hero: HeroRange, valueStyle: TextStyle) {
    val hasRange = hero.km != null
    val description = if (hasRange) {
        hero.caption + ", примерно " + DisplayFormat.kmNumber(hero.km) + " километров"
    } else {
        hero.caption
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        if (hasRange) {
            Text(
                "≈",
                style = RangeTextStyles.unit,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = Spacing.xxs, bottom = 6.dp),
            )
            AnimatedValue(
                value = DisplayFormat.kmNumber(hero.km),
                style = valueStyle,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "км",
                style = RangeTextStyles.unit,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.xxs, bottom = 6.dp),
            )
        } else {
            Text(
                NO_VALUE,
                style = valueStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Сколько км хода останется до предупреждающего порога SOC — см. [kmUntilLowSoc]. */
@Composable
private fun KmUntilLowSocLine(hero: HeroRange, socPercent: Float?) {
    val km = kmUntilLowSoc(hero, socPercent) ?: return
    val kmText = DisplayFormat.kmNumber(km)
    val thresholdText = RangeConstants.RESERVE_SOC_PERCENT.toInt().toString()
    Text(
        "≈$kmText км до $thresholdText%",
        style = RangeTextStyles.unit,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(top = Spacing.xxs)
            .semantics {
                contentDescription = "Осталось примерно $kmText километров до $thresholdText процентов заряда"
            },
    )
}

@Composable
private fun VehicleStatusGrid(
    engine: EngineView?,
    modifier: Modifier = Modifier,
) {
    val divider = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetricTile(
                icon = Icons.Outlined.Speed,
                label = "Скорость",
                value = DisplayFormat.speedNumber(engine?.speedKmh),
                unit = "км/ч",
                missingLabel = "Нет скорости",
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.s),
            )
            VerticalDivider(
                Modifier
                    .fillMaxHeight()
                    .padding(vertical = Spacing.xs),
                color = divider,
            )
            MetricTile(
                icon = Icons.Outlined.Thermostat,
                label = "За бортом",
                value = DisplayFormat.tempNumber(engine?.outsideTempC),
                unit = "°C",
                missingLabel = "Нет температуры",
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.s),
            )
        }
        HorizontalDivider(Modifier.padding(horizontal = Spacing.xs), color = divider)
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetricTile(
                icon = Icons.Outlined.DirectionsCar,
                label = "Передача",
                value = engine?.gear?.label ?: NO_VALUE,
                missingLabel = "Нет передачи",
                contentDescription = engine?.gear?.let { "Передача " + it.label } ?: "Передача: нет данных",
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.s),
            )
            VerticalDivider(
                Modifier
                    .fillMaxHeight()
                    .padding(vertical = Spacing.xs),
                color = divider,
            )
            MetricTile(
                icon = Icons.Outlined.Straighten,
                label = "Одометр",
                value = DisplayFormat.odometerNumber(engine?.odometerKm) ?: NO_VALUE,
                unit = "км",
                missingLabel = "Нет пробега",
                valueStyle = RangeTextStyles.statValue,
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.s),
            )
        }
    }
}

/** Заглушка-скелет главного блока на время первого получения телеметрии. */
@Composable
fun LiveVehicleCardSkeleton(modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier, contentPadding = Spacing.l, contentGap = Spacing.s) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            SkeletonBox(height = 46.dp, widthFraction = 0.45f)
            SkeletonBox(height = 26.dp, widthFraction = 0.35f)
            SkeletonBox(height = 14.dp)
        }
        Spacer(Modifier.height(Spacing.xs))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            SkeletonBox(height = 56.dp, modifier = Modifier.weight(1f))
            SkeletonBox(height = 56.dp, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            SkeletonBox(height = 56.dp, modifier = Modifier.weight(1f))
            SkeletonBox(height = 56.dp, modifier = Modifier.weight(1f))
        }
    }
}
