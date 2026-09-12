package com.geely.ex2.range.ui.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.ui.theme.Spacing

/**
 * Переключатель окна усреднения прогноза (5 / 15 / 30 км).
 * Выбранное окно подсвечивается и озвучивается скринридером.
 */
@Composable
fun ForecastWindowSelector(
    windowsKm: List<Double>,
    selectedKm: Double,
    onSelect: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.touchTarget),
    ) {
        windowsKm.forEachIndexed { index, km ->
            val label = DisplayFormat.windowKmLabel(km)
            SegmentedButton(
                selected = selectedKm == km,
                onClick = { onSelect(km) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = windowsKm.size),
                modifier = Modifier.semantics {
                    contentDescription = "Окно усреднения " + label
                },
            ) {
                Text(label, maxLines = 1)
            }
        }
    }
}
