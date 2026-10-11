package com.geely.ex2.range.ui.dashboard.styles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.geely.ex2.range.domain.engine.EngineView
import com.geely.ex2.range.domain.model.AppStyle
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.ui.dashboard.ForecastInfoSheet
import com.geely.ex2.range.ui.dashboard.ResetPeriodDialog
import com.geely.ex2.range.ui.theme.LocalStyleColors
import com.geely.ex2.range.ui.theme.Spacing

/**
 * Главная в одном из оформлений-макетов. Данные и правила показа общие ([dashboardUi]),
 * оформления различаются только раскладкой. Диалоги стоят снаружи [FitToDesign] — им нужна
 * обычная плотность приложения, а не «пиксели макета».
 */
@Composable
fun StyledDashboard(
    style: AppStyle,
    engine: EngineView,
    connectError: String?,
    onResetPeriod: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStyleColors.current ?: return
    val ui = remember(engine) { dashboardUi(engine) }
    var selectedKm by rememberSaveable { mutableStateOf(RangeConstants.WINDOW_KM_15) }
    var showInfo by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val onSelectWindow = { km: Double -> selectedKm = km }
    val onAskReset = { confirmReset = true }
    val onForecastInfo = { showInfo = true }

    Box(modifier.fillMaxSize()) {
        when (style) {
            AppStyle.COCKPIT -> FitToDesign(COCKPIT_DESIGN_WIDTH, COCKPIT_DESIGN_HEIGHT) {
                CockpitDashboard(ui, colors, selectedKm, onSelectWindow, onAskReset, onForecastInfo)
            }
            AppStyle.NORDIC -> FitToDesign(NORDIC_DESIGN_WIDTH, NORDIC_DESIGN_HEIGHT) {
                NordicDashboard(ui, colors, selectedKm, onSelectWindow, onAskReset, onForecastInfo)
            }
            AppStyle.BENTO -> FitToDesign(BENTO_DESIGN_WIDTH, BENTO_DESIGN_HEIGHT) {
                BentoDashboard(ui, colors, onAskReset, onForecastInfo)
            }
            AppStyle.NATIVE -> FitToDesign(NATIVE_DESIGN_WIDTH, NATIVE_DESIGN_HEIGHT) {
                NativeDashboard(ui, colors, selectedKm, onSelectWindow, onAskReset, onForecastInfo)
            }
            AppStyle.CLASSIC -> Unit
        }
        if (connectError != null) {
            TelemetryNotice(onRetry, Modifier.align(Alignment.TopCenter))
        }
    }

    if (showInfo) {
        ForecastInfoSheet(onDismiss = { showInfo = false })
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

/** Данные частично есть — узкая плашка сверху, раскладку не сдвигает. */
@Composable
private fun TelemetryNotice(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(top = Spacing.xs)
            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small)
            .padding(start = Spacing.m, end = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            "Телеметрия читается не полностью",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        TextButton(onClick = onRetry) { Text("Повторить") }
    }
}
