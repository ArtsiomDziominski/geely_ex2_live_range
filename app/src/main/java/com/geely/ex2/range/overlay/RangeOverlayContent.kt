package com.geely.ex2.range.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.domain.model.WindowStatus
import com.geely.ex2.range.ui.theme.RangeThemeColors

@Composable
fun RangeOverlayContent(
    windows: List<RangeWindow>,
    charging: Boolean,
    vehicleRangeRemainingKm: Float?,
    onDrag: (dx: Float, dy: Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .wrapContentSize(unbounded = true)
            .pointerInput(onDrag) {
                detectDragGesturesAfterLongPress { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        windows.forEachIndexed { index, window ->
            if (index > 0) {
                VerticalDivider(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .height(88.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            OverlayWindowColumn(window, charging, vehicleRangeRemainingKm)
        }
    }
}

@Composable
private fun OverlayWindowColumn(
    window: RangeWindow,
    charging: Boolean,
    vehicleRangeRemainingKm: Float?,
) {
    val extra = RangeThemeColors.extra
    val deltaPercent = if (!charging && window.status == WindowStatus.READY) {
        DisplayFormat.rangeDeltaPercent(window.rangeTo0Km, vehicleRangeRemainingKm)
    } else {
        null
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            DisplayFormat.windowKmLabel(window.windowKm),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 24.sp,
        )
        Text(
            overlayRangeValue(window, charging),
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
        )
        DisplayFormat.rangeDeltaLabel(deltaPercent)?.let { label ->
            Text(
                label,
                color = if ((deltaPercent ?: 0.0) < 0.0) extra.lowSoc else extra.charging,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun overlayRangeValue(window: RangeWindow, charging: Boolean): String {
    if (charging || window.status == WindowStatus.CHARGING) return "—"
    return when (window.status) {
        WindowStatus.READY -> DisplayFormat.km(window.rangeTo0Km, digits = 0).removeSuffix(" км")
        WindowStatus.NEED_MORE_KM -> "…"
        WindowStatus.SOC_UNCHANGED -> "—"
        WindowStatus.GAP -> "!"
        WindowStatus.INVALID -> "?"
        WindowStatus.CHARGING -> "—"
    }
}
