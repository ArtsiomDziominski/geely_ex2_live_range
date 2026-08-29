package com.geely.ex2.range.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geely.ex2.range.app.RangeUiState
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowStatus

@Composable
fun DashboardScreen(
    state: RangeUiState,
    onResetPeriod: () -> Unit,
) {
    val engine = state.engine
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderValue(DisplayFormat.socPercent(engine?.socPercent))
            HeaderValue(DisplayFormat.speedKmh(engine?.speedKmh))
            HeaderValue(DisplayFormat.tempC(engine?.outsideTempC))
            HeaderValue(engine?.gear?.label ?: "—")
        }
        Spacer(Modifier.height(16.dp))

        MetricRow(
            title = "Период",
            kwh = DisplayFormat.kWhPer100(engine?.period?.kWhPer100),
            pct = DisplayFormat.pctPer100(engine?.period?.pctPer100),
            trailing = {
                Button(onClick = onResetPeriod) { Text("Сброс") }
            },
        )
        val tripHint = when {
            engine == null -> "нет данных"
            engine.waitingForDrive && engine.trip.distanceKm <= 0.0 -> "ждём выезд"
            engine.waitingForDrive -> "последняя поездка"
            engine.tripIncomplete -> "поездка неполная"
            else -> "с выезда · ${DisplayFormat.km(engine.trip.distanceKm)}"
        }
        MetricRow(
            title = "Поездка",
            kwh = DisplayFormat.kWhPer100(engine?.trip?.kWhPer100),
            pct = DisplayFormat.pctPer100(engine?.trip?.pctPer100),
            subtitle = tripHint,
            muted = engine?.waitingForDrive == true,
        )

        Spacer(Modifier.height(20.dp))
        Text(
            "Остаток при текущем темпе",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        val windows = engine?.windows.orEmpty()
        if (windows.isEmpty()) {
            Text("ещё нет окна — едем, буфер копится", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            windows.forEach { window ->
                WindowRow(window)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HeaderValue(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun MetricRow(
    title: String,
    kwh: String,
    pct: String,
    subtitle: String? = null,
    muted: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val color = if (muted) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(kwh, color = color, modifier = Modifier.padding(horizontal = 12.dp))
        Text(pct, color = color, modifier = Modifier.padding(end = 12.dp))
        trailing?.invoke()
    }
}

@Composable
private fun WindowRow(window: RangeWindow) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            DisplayFormat.windowKmLabel(window.windowKm),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(0.22f),
        )
        when (window.status) {
            WindowStatus.READY -> {
                Text(
                    DisplayFormat.km(window.rangeTo0Km, digits = 0),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.38f),
                )
                val reserve = window.rangeToReserveKm ?: 0.0
                val reserveText = if (reserve <= 0.0) {
                    "до 20%: резерв"
                } else {
                    "до 20%: ${DisplayFormat.km(reserve, digits = 0)}"
                }
                Text(
                    "($reserveText)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.4f),
                )
            }
            WindowStatus.NEED_MORE_KM -> {
                val left = window.remainingToFillKm ?: window.windowKm
                Text(
                    "ещё ${DisplayFormat.km(left)} до окна",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(0.78f),
                )
            }
            WindowStatus.SOC_UNCHANGED -> {
                Text("SOC не изменился", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp, modifier = Modifier.weight(0.78f))
            }
            WindowStatus.CHARGING -> {
                Text("зарядка", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp, modifier = Modifier.weight(0.78f))
            }
            WindowStatus.GAP -> {
                Text("разрыв в окне (ГУ выключался)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp, modifier = Modifier.weight(0.78f))
            }
            WindowStatus.INVALID -> {
                Text("мало данных", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp, modifier = Modifier.weight(0.78f))
            }
        }
    }
}
