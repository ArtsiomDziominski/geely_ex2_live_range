package com.geely.ex2.range.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.ui.theme.RangeTextStyles
import com.geely.ex2.range.ui.theme.Spacing

const val NO_VALUE = "—"

/**
 * Ячейка приборного показателя: иконка, значение с единицей и подпись.
 *
 * При отсутствии данных значение показывается как «—», а подпись заменяется
 * человекочитаемым [missingLabel] — вместо технических null / NaN.
 */
@Composable
fun MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    missingLabel: String? = null,
    valueStyle: TextStyle = RangeTextStyles.metricValue,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = null,
) {
    val missing = value == NO_VALUE
    val shownLabel = if (missing && missingLabel != null) missingLabel else label
    val description = contentDescription
        ?: if (missing) "$label: ${missingLabel ?: "нет данных"}" else "$label $value ${unit.orEmpty()}"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { this.contentDescription = description },
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(Spacing.xxs))
            Text(
                shownLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(Spacing.xxs))
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedValue(
                value = value,
                style = valueStyle,
                color = if (missing) MaterialTheme.colorScheme.onSurfaceVariant else valueColor,
            )
            if (unit != null && !missing) {
                Spacer(Modifier.width(Spacing.xxs))
                Text(
                    unit,
                    style = RangeTextStyles.unit,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
    }
}

private const val VALUE_FADE_FROM_ALPHA = 0.35f
private const val VALUE_FADE_MS = 180

/**
 * Плавная смена числа: новое значение коротко проявляется вместо резкого скачка.
 *
 * Анимируется только альфа слоя в фазе отрисовки. Раньше здесь был AnimatedContent: на каждый тик
 * телеметрии он компоновал старое и новое значение и ~0,5 с анимировал размер (SizeTransform),
 * перемеряя всю карточку с её IntrinsicSize-рядами на каждом кадре. Первое значение — без анимации.
 */
@Composable
fun AnimatedValue(
    value: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val fade = remember { Animatable(1f) }
    var lastShown by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value == lastShown) return@LaunchedEffect
        lastShown = value
        fade.snapTo(VALUE_FADE_FROM_ALPHA)
        fade.animateTo(1f, tween(VALUE_FADE_MS))
    }
    Text(
        value,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.graphicsLayer { alpha = fade.value },
    )
}

/** Небольшой цветной чип со статусом (зарядка, предупреждение и т. п.). */
@Composable
fun StatusChip(
    text: String,
    icon: ImageVector?,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(container, MaterialTheme.shapes.small)
            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(Spacing.xxs))
            }
            Text(text, style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}
