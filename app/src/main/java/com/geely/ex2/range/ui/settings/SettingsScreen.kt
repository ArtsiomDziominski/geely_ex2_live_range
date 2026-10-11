package com.geely.ex2.range.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.BuildConfig
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.AppStyle
import com.geely.ex2.range.domain.model.AppThemeMode
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.SettingsSnapshot
import com.geely.ex2.range.ui.layout.LocalRangeLayout
import com.geely.ex2.range.ui.theme.Spacing

@Composable
fun SettingsScreen(
    settings: SettingsSnapshot,
    onOverlayEnabledChange: (Boolean) -> Unit,
    onOverlayWindowShownChange: (windowKm: Double, shown: Boolean) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onStyleChange: (AppStyle) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val layout = LocalRangeLayout.current
    Column(
        modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = layout.screenPadding, vertical = Spacing.s),
    ) {
        Text(
            "Настройки",
            style = MaterialTheme.typography.headlineSmall,
        )
        StyleSetting(
            selected = settings.style,
            onSelected = onStyleChange,
            modifier = Modifier.padding(top = 24.dp),
        )
        ThemeSettingRow(
            selected = settings.themeMode,
            onSelected = onThemeModeChange,
            modifier = Modifier.padding(top = 20.dp),
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        OverlaySettingRow(
            enabled = settings.overlayEnabled,
            onEnabledChange = onOverlayEnabledChange,
        )
        OverlayWindowsSetting(
            shownKm = settings.overlayWindowsKm,
            onShownChange = onOverlayWindowShownChange,
            modifier = Modifier.padding(top = 20.dp),
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        VersionRow()
    }
}

@Composable
private fun VersionRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Версия приложения",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Оформление: раскладка главной, навигация и палитра. Светлая или тёмная — выбирается ниже, в «Теме». */
@Composable
private fun StyleSetting(
    selected: AppStyle,
    onSelected: (AppStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Оформление",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            "Вид главного экрана и меню разделов. У каждого оформления есть светлая и тёмная тема",
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Column(Modifier.selectableGroup()) {
            AppStyle.entries.forEach { style ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected == style,
                            role = Role.RadioButton,
                            onClick = { onSelected(style) },
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected == style, onClick = null)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(style.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            style.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
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
                "Панель у края экрана с остатком хода по окнам 5, 15 и 30 км. " +
                    "Её можно перетащить вдоль края или к другому краю и спрятать стрелкой",
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

/** Какие окна стоят кольцами на виджете. Последнее включённое не выключается — пустой панели не бывает. */
@Composable
private fun OverlayWindowsSetting(
    shownKm: Set<Double>,
    onShownChange: (windowKm: Double, shown: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Окна на виджете",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            "Какие окна прогноза показывать на панели. Хотя бы одно остаётся включённым",
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        RangeConstants.RANGE_WINDOWS_KM.forEach { windowKm ->
            val shown = windowKm in shownKm
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    DisplayFormat.windowKmLabel(windowKm),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = shown,
                    onCheckedChange = { onShownChange(windowKm, it) },
                    enabled = !shown || shownKm.size > 1,
                )
            }
        }
    }
}
