package com.geely.ex2.range.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.app.RangeUiState
import com.geely.ex2.range.domain.model.AppThemeMode

@Composable
fun SettingsScreen(
    state: RangeUiState,
    onOverlayEnabledChange: (Boolean) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 8.dp),
    ) {
        Text(
            "Настройки",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        ThemeSettingRow(
            selected = state.settings.themeMode,
            onSelected = onThemeModeChange,
            modifier = Modifier.padding(top = 24.dp),
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        OverlaySettingRow(
            enabled = state.settings.overlayEnabled,
            onEnabledChange = onOverlayEnabledChange,
        )
    }
}

@Composable
private fun ThemeSettingRow(
    selected: AppThemeMode,
    onSelected: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Тема",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            "Светлая, тёмная или как в системе",
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            AppThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = selected == mode,
                    onClick = { onSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = AppThemeMode.entries.size,
                    ),
                ) {
                    Text(mode.label)
                }
            }
        }
    }
}

@Composable
private fun OverlaySettingRow(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Виджет поверх окон",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "Остаток хода по окнам 5, 15 и 30 км",
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
        )
    }
}
