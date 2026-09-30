package com.geely.ex2.range.overlay

import com.geely.ex2.range.domain.model.OverlayEdge
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayDockTest {
    private val screenWidth = 1920
    private val screenHeight = 1000
    private val width = 100
    private val height = 400

    private fun DockPosition.move(dx: Float, dy: Float) =
        moveBy(dx, dy, width, height, screenWidth, screenHeight)

    @Test
    fun dragAwayFromRightEdgeGrowsOffsetFromThatEdge() {
        val moved = DockPosition(OverlayEdge.RIGHT, 0f, 0f).move(dx = -300f, dy = 120f)
        assertEquals(DockPosition(OverlayEdge.RIGHT, 300f, 120f), moved)
    }

    @Test
    fun dragPastEdgeStaysOnScreen() {
        val right = DockPosition(OverlayEdge.RIGHT, 0f, 0f).move(dx = 500f, dy = -900f)
        assertEquals(DockPosition(OverlayEdge.RIGHT, 0f, -300f), right)

        val left = DockPosition(OverlayEdge.LEFT, 0f, 0f).move(dx = -500f, dy = 900f)
        assertEquals(DockPosition(OverlayEdge.LEFT, 0f, 300f), left)
    }

    @Test
    fun crossingMiddleSwitchesToOtherEdge() {
        // Середина окна у правого края сейчас на 1870; перетащили на 1000 влево — стала 870 < 960.
        val moved = DockPosition(OverlayEdge.RIGHT, 0f, 0f).move(dx = -1000f, dy = 0f)
        // Левый край окна — 1920 - 100 - 1000 = 820 от левого края экрана.
        assertEquals(DockPosition(OverlayEdge.LEFT, 820f, 0f), moved)
    }

    @Test
    fun staysOnSameEdgeUntilMiddleIsCrossed() {
        // Середина окна ровно по центру экрана — ещё не перешла.
        val moved = DockPosition(OverlayEdge.RIGHT, 0f, 0f).move(dx = -910f, dy = 0f)
        assertEquals(DockPosition(OverlayEdge.RIGHT, 910f, 0f), moved)
    }

    @Test
    fun leftEdgeMirrorsRightEdge() {
        val moved = DockPosition(OverlayEdge.LEFT, 0f, 0f).move(dx = 1000f, dy = 0f)
        assertEquals(DockPosition(OverlayEdge.RIGHT, 820f, 0f), moved)
    }

    @Test
    fun offsetIsLimitedByWindowHeight() {
        assertEquals(300f, clampOffsetY(1_000f, height = 400, screenHeight = 1000))
        assertEquals(-300f, clampOffsetY(-1_000f, height = 400, screenHeight = 1000))
        assertEquals(120f, clampOffsetY(120f, height = 400, screenHeight = 1000))
        // Окно выше экрана — только по центру.
        assertEquals(0f, clampOffsetY(50f, height = 1200, screenHeight = 1000))
    }
}
