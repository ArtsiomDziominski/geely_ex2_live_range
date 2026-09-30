package com.geely.ex2.range.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Size
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.geely.ex2.range.domain.model.OverlayEdge
import com.geely.ex2.range.domain.model.OverlayPlacement
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.ui.theme.RangeTheme
import kotlin.math.roundToInt

/**
 * Окно виджета поверх других приложений. Панель всегда прижата к левому или правому краю:
 * её можно тянуть вдоль края и перетащить к другому (отпущенная — доезжает до ближнего края),
 * спрятать в язычок и вернуть. Место и свёрнутость отдаются в [onPlacementChanged] для настроек.
 */
class RangeOverlayController(
    private val context: Context,
    private val onPlacementChanged: (OverlayPlacement) -> Unit,
    private val onOpenApp: () -> Unit,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: OverlayRootLayout? = null
    private var viewTreeOwner: OverlayViewTreeOwner? = null
    private var attached = false
    private var enabled = false
    private var snapAnimator: ValueAnimator? = null

    /** Желаемый сдвиг от центра экрана; на экран окно ставится с поправкой на свою высоту. */
    private var offsetY = 0
    private var dragStart = DockPosition(OverlayEdge.RIGHT, 0f, 0f)
    private var dragScreen = Size(0, 0)

    private var windows by mutableStateOf<List<RangeWindow>>(emptyList())
    private var charging by mutableStateOf(false)
    private var vehicleRangeRemainingKm by mutableStateOf<Float?>(null)
    private var edge by mutableStateOf(OverlayEdge.RIGHT)
    private var collapsed by mutableStateOf(false)
    private var detailsWindowKm by mutableStateOf<Double?>(null)

    private val rootListener = object : OverlayRootLayout.Listener {
        override fun onDragStart() = startDrag()

        override fun onDrag(dx: Float, dy: Float) = dragBy(dx, dy)

        override fun onDragEnd() = snapToEdge()

        override fun onOutsideTouch() = showDetails(null)

        override fun onConfigurationChanged() {
            // Экран повернули — новые размеры окна и экрана будут после раскладки.
            rootView?.post { keepOnScreen() }
        }
    }

    fun canDraw(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /** Место из настроек — задаётся при старте, дальше виджет ведёт его сам. */
    fun setPlacement(placement: OverlayPlacement) {
        edge = placement.edge
        offsetY = placement.offsetY
        collapsed = placement.collapsed
        val view = rootView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        if (view.parent == null) return
        params.gravity = gravityFor(edge)
        params.x = 0
        params.y = offsetY
        windowManager.updateViewLayout(view, params)
        keepOnScreen()
    }

    fun attach() {
        if (attached) return
        attached = true
        val owner = OverlayViewTreeOwner()
        viewTreeOwner = owner
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                // Панель всегда тёмная, как у края экрана, — от темы приложения не зависит.
                RangeTheme(darkTheme = true) {
                    RangeOverlayContent(
                        windows = windows,
                        charging = charging,
                        vehicleRangeRemainingKm = vehicleRangeRemainingKm,
                        edge = edge,
                        collapsed = collapsed,
                        detailsWindowKm = detailsWindowKm,
                        onWindowClick = ::toggleDetails,
                        onDismissDetails = { showDetails(null) },
                        onCollapsedChange = ::changeCollapsed,
                        onOpenApp = ::openApp,
                    )
                }
            }
        }
        rootView = OverlayRootLayout(context, rootListener).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            addView(
                composeView,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
            // Панель развернули/спрятали или открыли подробности — окно сменило размер.
            addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                    post { keepOnScreen() }
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (!attached) return
        if (this.enabled == enabled) return
        this.enabled = enabled
        if (enabled) {
            show()
        } else {
            hide()
        }
    }

    fun update(windows: List<RangeWindow>, charging: Boolean, vehicleRangeRemainingKm: Float?) {
        this.windows = windows
        this.charging = charging
        this.vehicleRangeRemainingKm = vehicleRangeRemainingKm
    }

    fun release() {
        hide()
        viewTreeOwner?.destroy()
        viewTreeOwner = null
        rootView = null
        attached = false
        enabled = false
    }

    private fun startDrag() {
        val view = rootView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        cancelSnap()
        showDetails(null)
        dragScreen = screenSize()
        dragStart = DockPosition(edge, params.x.toFloat(), params.y.toFloat())
    }

    private fun dragBy(dx: Float, dy: Float) {
        val view = rootView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        if (view.parent == null) return
        val position = dragStart.moveBy(dx, dy, view.width, view.height, dragScreen.width, dragScreen.height)
        edge = position.edge
        offsetY = position.offsetY.roundToInt()
        params.gravity = gravityFor(position.edge)
        params.x = position.x.roundToInt()
        params.y = offsetY
        windowManager.updateViewLayout(view, params)
    }

    /** Отпустили — панель доезжает до своего края, и место запоминается. */
    private fun snapToEdge() {
        val view = rootView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        if (view.parent == null || params.x == 0) {
            commitPlacement()
            return
        }
        snapAnimator = ValueAnimator.ofInt(params.x, 0).apply {
            duration = SNAP_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                if (view.parent != null) {
                    params.x = animator.animatedValue as Int
                    windowManager.updateViewLayout(view, params)
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (snapAnimator === animation) snapAnimator = null
                    if (!cancelled) commitPlacement()
                }
            })
            start()
        }
    }

    private fun toggleDetails(windowKm: Double) {
        showDetails(if (detailsWindowKm == windowKm) null else windowKm)
    }

    /**
     * Открывает или закрывает подробности по окну. Пока они открыты, окну приходят касания мимо
     * него — по ним подробности закрываются, как обычная всплывающая подсказка.
     */
    private fun showDetails(windowKm: Double?) {
        detailsWindowKm = windowKm
        val view = rootView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        if (view.parent == null) return
        val flags = if (windowKm != null) {
            params.flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        } else {
            params.flags and WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH.inv()
        }
        if (flags != params.flags) {
            params.flags = flags
            windowManager.updateViewLayout(view, params)
        }
    }

    private fun changeCollapsed(collapsed: Boolean) {
        showDetails(null)
        if (this.collapsed == collapsed) return
        this.collapsed = collapsed
        commitPlacement()
    }

    private fun openApp() {
        showDetails(null)
        onOpenApp()
    }

    private fun commitPlacement() {
        onPlacementChanged(OverlayPlacement(edge = edge, offsetY = offsetY, collapsed = collapsed))
    }

    /**
     * Окно или экран сменили размер — окно не должно вылезать за экран. Желаемый [offsetY] при
     * этом не трогаем: спрятанный у самого низа язычок после разворота панели вернётся на место.
     */
    private fun keepOnScreen() {
        val view = rootView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        if (view.parent == null) return
        val y = clampOffsetY(offsetY.toFloat(), view.height, screenSize().height).roundToInt()
        if (y != params.y) {
            params.y = y
            windowManager.updateViewLayout(view, params)
        }
    }

    private fun cancelSnap() {
        snapAnimator?.cancel()
        snapAnimator = null
    }

    private fun show() {
        if (!canDraw()) return
        val view = rootView ?: return
        if (view.parent != null) return
        windowManager.addView(view, layoutParams())
    }

    private fun hide() {
        cancelSnap()
        detailsWindowKm = null
        val view = rootView ?: return
        if (view.parent == null) return
        windowManager.removeView(view)
    }

    /**
     * Область, в которой WindowManager ставит окно: экран без системных панелей — за них
     * окно по умолчанию не заходит, и центр по вертикали он считает по ней же.
     */
    private fun screenSize(): Size {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            val bounds = metrics.bounds
            return Size(
                bounds.width() - insets.left - insets.right,
                bounds.height() - insets.top - insets.bottom,
            )
        }
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return Size(metrics.widthPixels, metrics.heightPixels)
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = gravityFor(edge)
            x = 0
            y = offsetY
        }
    }

    private fun gravityFor(edge: OverlayEdge): Int {
        val horizontal = if (edge == OverlayEdge.LEFT) Gravity.LEFT else Gravity.RIGHT
        return horizontal or Gravity.CENTER_VERTICAL
    }

    private companion object {
        const val SNAP_DURATION_MS = 220L
    }
}
