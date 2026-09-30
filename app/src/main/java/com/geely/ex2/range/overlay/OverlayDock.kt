package com.geely.ex2.range.overlay

import com.geely.ex2.range.domain.model.OverlayEdge

/**
 * Где окно виджета, прижатого к краю экрана: [x] — отступ окна от края [edge] (у самого края 0),
 * [offsetY] — сдвиг от центра экрана по вертикали (плюс — вниз). Всё в px — так же окно ставит
 * WindowManager с гравитацией к краю и по центру вертикали.
 */
internal data class DockPosition(
    val edge: OverlayEdge,
    val x: Float,
    val offsetY: Float,
)

/**
 * Окно размером [width]×[height] сдвинули пальцем на ([dx], [dy]) по экрану
 * [screenWidth]×[screenHeight]. За экран окно не уходит. Если середина окна перешла середину
 * экрана, окно переезжает к другому краю, и отступ [DockPosition.x] считается уже от него.
 */
internal fun DockPosition.moveBy(
    dx: Float,
    dy: Float,
    width: Int,
    height: Int,
    screenWidth: Int,
    screenHeight: Int,
): DockPosition {
    val maxX = (screenWidth - width).coerceAtLeast(0).toFloat()
    val inward = if (edge == OverlayEdge.RIGHT) -dx else dx
    val movedX = (x + inward).coerceIn(0f, maxX)
    val movedY = clampOffsetY(offsetY + dy, height, screenHeight)
    val pastMiddle = maxX > 0f && movedX + width / 2f > screenWidth / 2f
    return if (pastMiddle) {
        DockPosition(edge.opposite(), maxX - movedX, movedY)
    } else {
        DockPosition(edge, movedX, movedY)
    }
}

/** Сдвиг от центра, при котором окно высотой [height] целиком помещается на экране. */
internal fun clampOffsetY(offsetY: Float, height: Int, screenHeight: Int): Float {
    val max = ((screenHeight - height) / 2f).coerceAtLeast(0f)
    return offsetY.coerceIn(-max, max)
}

private fun OverlayEdge.opposite(): OverlayEdge = when (this) {
    OverlayEdge.LEFT -> OverlayEdge.RIGHT
    OverlayEdge.RIGHT -> OverlayEdge.LEFT
}
