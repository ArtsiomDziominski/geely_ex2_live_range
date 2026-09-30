package com.geely.ex2.range.overlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.geely.ex2.range.domain.model.OverlayEdge
import kotlin.math.min

/**
 * Силуэт панели, прижатой к краю экрана. Со стороны экрана — тело со скруглёнными углами
 * [cornerRadius], а у самого края оно вогнутыми дугами радиуса [flareRadius] плавно перетекает
 * в рамку экрана сверху и снизу. Дуги занимают по [flareRadius] высоты над телом и под ним —
 * содержимое ставится с таким отступом.
 */
internal data class EdgeDockShape(
    val edge: OverlayEdge,
    val cornerRadius: Dp,
    val flareRadius: Dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val f = with(density) { flareRadius.toPx() }.coerceIn(0f, min(w, h / 2f))
        val r = with(density) { cornerRadius.toPx() }.coerceIn(0f, min(w - f, h / 2f - f).coerceAtLeast(0f))
        // Контур строится для правого края; у левого — зеркально.
        val mirror = edge == OverlayEdge.LEFT
        fun x(value: Float) = if (mirror) w - value else value
        val path = Path()
        fun arc(centerX: Float, centerY: Float, radius: Float, startDegrees: Float, sweepDegrees: Float) {
            path.arcTo(
                rect = Rect(center = Offset(x(centerX), centerY), radius = radius),
                startAngleDegrees = if (mirror) 180f - startDegrees else startDegrees,
                sweepAngleDegrees = if (mirror) -sweepDegrees else sweepDegrees,
                forceMoveTo = false,
            )
        }
        path.moveTo(x(w), 0f)
        arc(w - f, 0f, f, 0f, 90f)
        path.lineTo(x(r), f)
        arc(r, f + r, r, 270f, -90f)
        path.lineTo(x(0f), h - f - r)
        arc(r, h - f - r, r, 180f, -90f)
        path.lineTo(x(w - f), h - f)
        arc(w - f, h, f, 270f, 90f)
        path.close()
        return Outline.Generic(path)
    }
}

/** Уголок карточки подробностей — треугольник остриём к панели. */
internal data class PointerShape(val pointsRight: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val base = if (pointsRight) 0f else size.width
        val tip = if (pointsRight) size.width else 0f
        val path = Path().apply {
            moveTo(base, 0f)
            lineTo(tip, size.height / 2f)
            lineTo(base, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}
