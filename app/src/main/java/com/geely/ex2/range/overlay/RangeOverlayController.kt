package com.geely.ex2.range.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.geely.ex2.range.domain.model.AppThemeMode
import com.geely.ex2.range.domain.model.RangeWindow
import com.geely.ex2.range.ui.theme.RangeTheme
import com.geely.ex2.range.ui.theme.resolveDarkTheme
import kotlin.math.roundToInt

class RangeOverlayController(
    private val context: Context,
    private val onPositionChanged: (x: Int, y: Int) -> Unit,
    private val onOpenApp: () -> Unit,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var viewTreeOwner: OverlayViewTreeOwner? = null
    private var attached = false
    private var enabled = false
    private var savedX: Int? = null
    private var savedY: Int? = null

    private var windows by mutableStateOf<List<RangeWindow>>(emptyList())
    private var charging by mutableStateOf(false)
    private var vehicleRangeRemainingKm by mutableStateOf<Float?>(null)
    private var appThemeMode by mutableStateOf(AppThemeMode.SYSTEM)

    fun canDraw(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun setSavedPosition(x: Int?, y: Int?) {
        savedX = x
        savedY = y
        val view = composeView ?: return
        if (view.parent == null || x == null || y == null) return
        val params = view.layoutParams as WindowManager.LayoutParams
        params.x = x
        params.y = y
        windowManager.updateViewLayout(view, params)
    }

    fun setThemeMode(mode: AppThemeMode) {
        appThemeMode = mode
    }

    fun attach() {
        if (attached) return
        attached = true
        val owner = OverlayViewTreeOwner()
        viewTreeOwner = owner
        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                RangeTheme(darkTheme = resolveDarkTheme(appThemeMode)) {
                    Box(Modifier.wrapContentSize(unbounded = true)) {
                        RangeOverlayContent(
                            windows = windows,
                            charging = charging,
                            vehicleRangeRemainingKm = vehicleRangeRemainingKm,
                            onDrag = ::moveBy,
                            onClick = onOpenApp,
                        )
                    }
                }
            }
        }
        composeView = view
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
        composeView = null
        attached = false
        enabled = false
    }

    private fun moveBy(dx: Float, dy: Float) {
        val view = composeView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        val metrics = context.resources.displayMetrics
        val maxX = (metrics.widthPixels - view.width).coerceAtLeast(0)
        val maxY = (metrics.heightPixels - view.height).coerceAtLeast(0)
        params.x = (params.x + dx.roundToInt()).coerceIn(0, maxX)
        params.y = (params.y + dy.roundToInt()).coerceIn(0, maxY)
        windowManager.updateViewLayout(view, params)
        savedX = params.x
        savedY = params.y
        onPositionChanged(params.x, params.y)
    }

    private fun show() {
        if (!canDraw()) return
        val view = composeView ?: return
        if (view.parent != null) return
        val params = layoutParams()
        if (savedX != null && savedY != null) {
            params.x = savedX!!
            params.y = savedY!!
        }
        windowManager.addView(view, params)
        if (savedX == null || savedY == null) {
            view.post { alignDefaultTopEnd(view) }
        }
    }

    private fun alignDefaultTopEnd(view: ComposeView) {
        if (view.parent == null) return
        val params = view.layoutParams as WindowManager.LayoutParams
        val metrics = context.resources.displayMetrics
        val margin = (24 * metrics.density).roundToInt()
        val top = (96 * metrics.density).roundToInt()
        params.x = (metrics.widthPixels - view.width - margin).coerceAtLeast(0)
        params.y = top
        windowManager.updateViewLayout(view, params)
        savedX = params.x
        savedY = params.y
        onPositionChanged(params.x, params.y)
    }

    private fun hide() {
        val view = composeView ?: return
        if (view.parent == null) return
        windowManager.removeView(view)
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }
}
