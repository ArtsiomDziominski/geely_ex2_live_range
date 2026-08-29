package com.geely.ex2.range.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.app.RangeUiState
import com.geely.ex2.range.domain.format.DisplayFormat

@Composable
fun HelpScreen(
    state: RangeUiState,
    onCapacityChange: (Double?) -> Unit,
) {
    var capacityText by remember {
        mutableStateOf(state.settings.usableCapacityKwh?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "")
    }
    LaunchedEffect(state.settings.usableCapacityKwh) {
        if (capacityText.isBlank() && state.settings.usableCapacityKwh != null) {
            capacityText = String.format(java.util.Locale.US, "%.1f", state.settings.usableCapacityKwh)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text("Справка", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "На улице сейчас: ${DisplayFormat.tempC(state.engine?.outsideTempC)}",
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))

        Section("Температура снаружи и запас хода")
        TempRow("ниже 0 °C", "хуже всего: отопление салона + холодная батарея; короткое окно 5 км часто «страшнее» 30 км")
        TempRow("0…10 °C", "расход выше летнего, климат ещё ест заметно")
        TempRow("15…25 °C", "комфортный диапазон для электромобиля, ориентир «нормального» кВт·ч/100 км")
        TempRow("выше 30 °C", "кондиционер; на жаре короткое окно снова хуже длинного")
        Body("Салон обычно комфортен около 20–23 °C. Каждая лишняя ступень климата на месте жрёт SOC без километров — поэтому прогноз считается только по пройденному пути.")

        Section("Почему запас до 20%, а не до 0%")
        Body("Ниже ~20% зимой запас падает нелинейно (отопление + сопротивление батареи). Нужен резерв на объезд, пробку и поиск зарядки. Глубокий разряд хуже для батареи, чем езда в середине диапазона.")
        Body("Цифра в скобках на Главной — сколько км до 20% при текущем темпе, не до пустой батареи.")

        Section("Как читать три окна")
        Body("5 км — сейчас (пробка, горка, обгон, климат). 15 км — короткий участок. 30 км — более устойчивый темп. Если 5 км сильно меньше 30 км — сейчас едете «дороже», чем в среднем за полчаса пути.")

        Section("Ёмкость и кВт·ч")
        Body("кВт·ч считаются из SOC и заданной полезной ёмкости C. Пока C не подтверждена на авто, цифры кВт·ч ориентировочные.")
        OutlinedTextField(
            value = capacityText,
            onValueChange = { text ->
                capacityText = text
                val parsed = text.replace(',', '.').trim().toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    onCapacityChange(parsed)
                }
            },
            label = { Text("Полезная ёмкость C, кВт·ч") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        val vehicleC = state.engine?.usableCapacityKwh
        if (vehicleC != null && state.engine?.capacityIsUserSet != true) {
            Text(
                "Из VHAL INFO_EV_BATTERY_CAPACITY: ${String.format(java.util.Locale.US, "%.1f", vehicleC)} кВт·ч",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Section("Передача")
        Body("Поездка привязана к выезду с парковки. Короткий ложный P (меньше ~2 с) поездку не режет. R после P — та же поездка, не вторая.")

        Section("Сырые значения VHAL")
        if (state.raw.connectError != null) {
            Text(state.raw.connectError, color = MaterialTheme.colorScheme.error)
        }
        if (!state.raw.carReady) {
            Text("Car API не подключен — справка читается без него.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.raw.lines.forEach { line ->
            val status = if (line.ok) "OK" else "нет"
            Text(
                "${line.name} ${line.propertyHex}: $status ${line.rawText}" +
                    (line.decodedText?.let { " → $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = if (line.ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(16.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun Body(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun TempRow(band: String, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(band, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.28f))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.72f))
    }
}
