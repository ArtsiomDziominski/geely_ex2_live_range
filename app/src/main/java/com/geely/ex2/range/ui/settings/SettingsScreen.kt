package com.geely.ex2.range.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.BuildConfig
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.AppStyle
import com.geely.ex2.range.domain.model.AppThemeMode
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.SettingsSnapshot
import com.geely.ex2.range.domain.model.UpdateInfo
import com.geely.ex2.range.domain.model.UpdateState
import com.geely.ex2.range.ui.layout.LocalRangeLayout
import com.geely.ex2.range.ui.theme.Spacing

@Composable
fun SettingsScreen(
    settings: SettingsSnapshot,
    onOverlayEnabledChange: (Boolean) -> Unit,
    onOverlayWindowShownChange: (windowKm: Double, shown: Boolean) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onStyleChange: (AppStyle) -> Unit,
    update: UpdateState,
    selfUpdateSupported: Boolean,
    onCheckUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
    onAutoUpdateCheckChange: (Boolean) -> Unit,
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
        UpdateSection(
            state = update,
            autoCheck = settings.autoUpdateCheck,
            selfUpdateSupported = selfUpdateSupported,
            onCheck = onCheckUpdates,
            onInstall = onInstallUpdate,
            onAutoCheckChange = onAutoUpdateCheckChange,
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        DeveloperSection()
    }
}

private const val DEVELOPER_TELEGRAM_USERNAME = "i_am_artsiom"

/** Контакт разработчика: по тапу открывается его Telegram. */
@Composable
private fun DeveloperSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$DEVELOPER_TELEGRAM_USERNAME"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            }
            .padding(vertical = 4.dp),
    ) {
        Text(
            "Разработчик",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            "@$DEVELOPER_TELEGRAM_USERNAME",
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "Вопросы, предложения и поддержка — напишите в Telegram",
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Версия и обновление по сети. Найденное обновление ставится только по кнопке и после
 * подтверждения: установка перезапускает приложение.
 */
@Composable
private fun UpdateSection(
    state: UpdateState,
    autoCheck: Boolean,
    selfUpdateSupported: Boolean,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    onAutoCheckChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirm by remember { mutableStateOf<UpdateInfo?>(null) }
    val busy = state is UpdateState.Checking ||
        state is UpdateState.Downloading ||
        state is UpdateState.Installing
    val found = when (state) {
        is UpdateState.Available -> state.info
        is UpdateState.Failed -> state.info
        else -> null
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Обновление",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            "Установлена версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        UpdateStatus(state, Modifier.padding(top = 12.dp))
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onCheck, enabled = !busy) {
                Text("Проверить обновления")
            }
            if (found != null && selfUpdateSupported) {
                Button(onClick = { confirm = found }, enabled = !busy) {
                    Text("Установить ${found.versionName}")
                }
            }
        }
        if (!selfUpdateSupported) {
            Text(
                "Эта сборка не обновляется сама: она подписана отладочным ключом. " +
                    "По сети обновляется сборка для головного устройства",
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Проверять автоматически", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Раз в сутки при запуске. О новой версии напомнит точка на «Настройках», " +
                            "ставить её всё равно нужно кнопкой",
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(checked = autoCheck, onCheckedChange = onAutoCheckChange)
            }
        }
    }

    confirm?.let { info ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Установить версию ${info.label}?") },
            text = { Text("Приложение скачает обновление и перезапустится. Лучше делать это на парковке.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = null
                        onInstall()
                    },
                ) { Text("Установить") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun UpdateStatus(state: UpdateState, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state) {
            UpdateState.Idle -> Text(
                "Обновления ещё не проверялись",
                color = muted,
                style = MaterialTheme.typography.bodyLarge,
            )
            UpdateState.Checking -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Проверяем…", style = MaterialTheme.typography.bodyLarge)
            }
            UpdateState.UpToDate -> Text(
                "Установлена последняя версия",
                style = MaterialTheme.typography.bodyLarge,
            )
            is UpdateState.Available -> {
                Text(
                    "Доступна версия ${state.info.label}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (state.info.notes.isNotEmpty()) {
                    Text(state.info.notes, color = muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            is UpdateState.Downloading -> {
                val progress = state.progress
                Text(
                    if (progress != null) {
                        "Загрузка версии ${state.info.label} · ${(progress * 100).toInt()} %"
                    } else {
                        "Загрузка версии ${state.info.label}…"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (progress != null) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            is UpdateState.Installing -> Text(
                "Устанавливаем версию ${state.info.label} — приложение перезапустится",
                style = MaterialTheme.typography.bodyLarge,
            )
            is UpdateState.Failed -> Text(
                state.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
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
