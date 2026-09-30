package com.geely.ex2.range.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.hypot

/**
 * Корень окна виджета. Перетаскивание ловим здесь, на уровне View, по экранным координатам
 * (rawX/rawY): окно едет вместе с пальцем, и координаты внутри окна, которые видит Compose,
 * съезжают вместе с ним — по ним панель отставала бы от пальца и дёргалась. Нажатия на кольца
 * и кнопки остаются за Compose: как только палец ушёл дальше порога, жест забираем себе,
 * а Compose получает ACTION_CANCEL — нажатие не срабатывает.
 */
@SuppressLint("ViewConstructor")
internal class OverlayRootLayout(
    context: Context,
    private val listener: Listener,
) : FrameLayout(context) {
    interface Listener {
        fun onDragStart()

        /** Палец сместился на ([dx], [dy]) px экрана от точки, где началось перетаскивание. */
        fun onDrag(dx: Float, dy: Float)

        fun onDragEnd()

        /** Касание мимо окна — приходит, пока у окна стоит FLAG_WATCH_OUTSIDE_TOUCH. */
        fun onOutsideTouch()

        fun onConfigurationChanged()
    }

    // Порог вдвое больше системного: в машине палец на нажатии гуляет сильнее.
    private val dragSlop = ViewConfiguration.get(context).scaledTouchSlop * 2f
    private var startX = 0f
    private var startY = 0f
    private var dragging = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
            listener.onOutsideTouch()
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean = track(event)

    // Сюда доходят касания, которые не взял Compose (пустые места панели), и весь жест
    // после того, как перетаскивание перехвачено.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        track(event)
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        listener.onConfigurationChanged()
    }

    /** true — жест наш: идёт перетаскивание. */
    private fun track(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> if (dragging) {
                listener.onDrag(event.rawX - startX, event.rawY - startY)
            } else if (hypot(event.rawX - startX, event.rawY - startY) > dragSlop) {
                // Отсчёт — от точки срыва, иначе панель прыгнула бы на величину порога.
                startX = event.rawX
                startY = event.rawY
                dragging = true
                listener.onDragStart()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (dragging) {
                dragging = false
                listener.onDragEnd()
            }
        }
        return dragging
    }
}
