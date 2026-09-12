package com.geely.ex2.range.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.R
import com.geely.ex2.range.ui.layout.LocalRangeLayout
import com.geely.ex2.range.ui.theme.Spacing

/**
 * Справка по прогнозу: на телефоне — модальный bottom sheet,
 * в широком окне — обычный диалог.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastInfoSheet(onDismiss: () -> Unit) {
    val layout = LocalRangeLayout.current
    if (layout.isCompact) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.l)
                    .padding(bottom = Spacing.xxl),
            ) {
                Text(
                    stringResource(R.string.forecast_info_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(Spacing.xs))
                ForecastInfoBody()
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.forecast_info_title)) },
            text = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    ForecastInfoBody()
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.app_info_close)) }
            },
        )
    }
}

@Composable
private fun ForecastInfoBody() {
    Text(
        stringResource(R.string.forecast_info_intro),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    InfoSection(R.string.forecast_info_windows_title, R.string.forecast_info_windows_body)
    InfoSection(R.string.forecast_info_calc_title, R.string.forecast_info_calc_body)
    InfoSection(R.string.forecast_info_values_title, R.string.forecast_info_values_body)
    InfoSection(R.string.forecast_info_vehicle_title, R.string.forecast_info_vehicle_body)
    InfoSection(R.string.forecast_info_chart_title, R.string.forecast_info_chart_body)
    InfoSection(R.string.forecast_info_states_title, R.string.forecast_info_states_body)
}

@Composable
private fun InfoSection(titleRes: Int, bodyRes: Int) {
    Spacer(Modifier.height(Spacing.m))
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(Spacing.xxs))
    Text(
        stringResource(bodyRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
