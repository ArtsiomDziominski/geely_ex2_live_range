package com.geely.ex2.range.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geely.ex2.range.domain.format.DisplayFormat
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.ui.theme.RangeThemeColors

private const val SEGMENT_COUNT = 4

/**
 * Horizontal battery gauge styled after a segmented SVG icon.
 * Fill follows SOC; the exact percent (no integer rounding) sits in the center.
 */
@Composable
fun SocBatteryIndicator(
    socPercent: Float?,
    charging: Boolean,
    deltaLabel: String?,
    modifier: Modifier = Modifier,
) {
    val extra = RangeThemeColors.extra
    val outline = MaterialTheme.colorScheme.onSurface
    val fillGreen = extra.charging
    val fillRed = extra.lowSoc
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val captionColor = if (charging) extra.charging else MaterialTheme.colorScheme.onSurfaceVariant
    val reserveSoc = RangeConstants.RESERVE_SOC_PERCENT.toFloat()
    val level = socPercent?.takeIf { it.isFinite() }?.coerceIn(0f, 100f)
    val label = if (level == null) "нет SOC" else "${DisplayFormat.socNumber(level)}%"

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(504.dp)
                .height(168.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize().padding(vertical = 12.dp)) {
                val stroke = size.height * 0.12f
                val tipW = size.width * 0.055f
                val tipH = size.height * 0.42f
                val gap = size.height * 0.08f
                val bodyW = size.width - tipW - gap
                val bodyH = size.height
                val radius = bodyH * 0.18f

                val body = RoundRect(
                    Rect(0f, 0f, bodyW, bodyH),
                    CornerRadius(radius, radius),
                )
                val bodyPath = Path().apply { addRoundRect(body) }

                drawPath(bodyPath, color = empty)

                if (level != null && level > 0f) {
                    val inset = stroke * 0.85f
                    val innerLeft = inset
                    val innerTop = inset
                    val innerRight = bodyW - inset
                    val innerBottom = bodyH - inset
                    val innerW = (innerRight - innerLeft).coerceAtLeast(0f)
                    val innerH = (innerBottom - innerTop).coerceAtLeast(0f)
                    val segGap = innerH * 0.08f
                    val segW = (innerW - segGap * (SEGMENT_COUNT - 1)) / SEGMENT_COUNT
                    val filled = level / 100f * SEGMENT_COUNT
                    val lastFilledIndex = (filled - 1e-5f)
                        .toInt()
                        .coerceIn(0, SEGMENT_COUNT - 1)

                    clipPath(bodyPath, ClipOp.Intersect) {
                        for (i in 0 until SEGMENT_COUNT) {
                            val segFill = (filled - i).coerceIn(0f, 1f)
                            if (segFill <= 0f) continue
                            val x = innerLeft + i * (segW + segGap)
                            val segmentColor = when {
                                i == lastFilledIndex && level < reserveSoc -> fillRed
                                else -> fillGreen
                            }
                            drawRoundRect(
                                color = segmentColor,
                                topLeft = Offset(x, innerTop),
                                size = Size(segW * segFill, innerH),
                                cornerRadius = CornerRadius(innerH * 0.12f, innerH * 0.12f),
                            )
                        }
                    }
                }

                drawRoundRect(
                    color = outline,
                    topLeft = Offset.Zero,
                    size = Size(bodyW, bodyH),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = stroke),
                )

                val tipLeft = bodyW + gap
                val tipTop = (bodyH - tipH) / 2f
                drawRoundRect(
                    color = outline,
                    topLeft = Offset(tipLeft, tipTop),
                    size = Size(tipW, tipH),
                    cornerRadius = CornerRadius(tipW * 0.35f, tipW * 0.35f),
                )
            }
            Text(
                text = label,
                color = textColor,
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 30.dp),
            )
        }
        if (deltaLabel != null) {
            Text(
                deltaLabel,
                color = captionColor,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else if (level == null) {
            Text(
                "SOC",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
