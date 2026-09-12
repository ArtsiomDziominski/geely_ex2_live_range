package com.geely.ex2.range.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.R
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowStatus
import com.geely.ex2.range.ui.components.InfoBanner
import com.geely.ex2.range.ui.components.SectionCard
import com.geely.ex2.range.ui.components.SkeletonBox
import com.geely.ex2.range.ui.layout.LocalRangeLayout
import com.geely.ex2.range.ui.theme.RangeTextStyles
import com.geely.ex2.range.ui.theme.RangeThemeColors
import com.geely.ex2.range.ui.theme.Spacing
import kotlin.math.abs

private val WINDOWS_KM = RangeConstants.RANGE_WINDOWS_KM.toList()

/**
 * Блок прогноза: одно выбранное окно усреднения — один большой график и один результат.
 * Данные всех трёх окон приходят от движка без изменений, меняется только подача.
 */
@Composable
fun ForecastCard(
    windows: List<RangeWindow>,
    charging: Boolean,
    vehicleRangeRemainingKm: Float?,
    modifier: Modifier = Modifier,
) {
    val layout = LocalRangeLayout.current
    var selectedKm by rememberSaveable { mutableStateOf(RangeConstants.WINDOW_KM_15) }
    var showInfo by remember { mutableStateOf(false) }
    val selectedWindow = remember(windows, selectedKm) {
        windows.firstOrNull { abs(it.windowKm - selectedKm) < 1e-6 }
    }

    SectionCard(
        modifier = modifier,
        title = "Прогноз запаса хода",
        icon = Icons.AutoMirrored.Outlined.ShowChart,
        subtitle = "темп последних километров",
        contentGap = Spacing.s,
        action = {
            IconButton(
                onClick = { showInfo = true },
                modifier = Modifier.size(Spacing.touchTarget),
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.forecast_info_content_description),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) {
        AnimatedVisibility(
            visible = charging,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
        ) {
            val extra = RangeThemeColors.extra
            InfoBanner(
                icon = Icons.Outlined.Bolt,
                title = "Зарядка",
                description = "SOC растёт — прогноз по расходу временно недоступен.",
                container = extra.charging.copy(alpha = 0.16f),
                content = extra.charging,
            )
        }

        ForecastWindowSelector(
            windowsKm = WINDOWS_KM,
            selectedKm = selectedKm,
            onSelect = { selectedKm = it },
        )

        AnimatedContent(
            targetState = selectedKm,
            transitionSpec = {
                val forward = targetState > initialState
                val offset = if (forward) 1 else -1
                (
                    slideInHorizontally(tween(220)) { width -> offset * width / 6 } +
                        fadeIn(tween(220))
                    ) togetherWith fadeOut(tween(140))
            },
            label = "forecast-window",
        ) { km ->
            val window = if (abs(km - selectedKm) < 1e-6) {
                selectedWindow
            } else {
                windows.firstOrNull { abs(it.windowKm - km) < 1e-6 }
            }
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "Окно усреднения · " + DisplayFormat.windowKmLabel(km),
                    style = RangeTextStyles.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.xs))
                if (window != null && window.chart.size >= 2) {
                    WindowPaceChart(
                        windowKm = km,
                        points = window.chart,
                        height = layout.chartHeight,
                        xMaxKm = if (window.status == WindowStatus.NEED_MORE_KM) {
                            window.chart.maxOf { it.km }.toDouble()
                        } else {
                            km
                        },
                    )
                    Spacer(Modifier.height(Spacing.s))
                }
                if (window != null && window.status == WindowStatus.READY) {
                    ForecastResult(
                        window = window,
                        vehicleRangeRemainingKm = vehicleRangeRemainingKm,
                    )
                } else {
                    ForecastStatus(
                        window = window,
                        windowKm = km,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showInfo) {
        ForecastInfoSheet(onDismiss = { showInfo = false })
    }
}

/** Скелет блока прогноза на время первой загрузки. */
@Composable
fun ForecastSkeleton(modifier: Modifier = Modifier) {
    val layout = LocalRangeLayout.current
    SectionCard(modifier = modifier, contentGap = Spacing.s) {
        SkeletonBox(height = 18.dp, widthFraction = 0.55f)
        SkeletonBox(height = 40.dp)
        SkeletonBox(height = layout.chartHeight)
        Spacer(Modifier.height(Spacing.xxs))
        SkeletonBox(height = 30.dp, widthFraction = 0.7f)
    }
}
