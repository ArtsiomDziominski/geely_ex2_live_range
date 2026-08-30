package com.geely.ex2.range.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.geely.ex2.range.app.RangeApplication
import com.geely.ex2.range.domain.model.AppThemeMode

class RangeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as RangeApplication).container
    val uiState = container.uiState

    private var pendingOverlayEnable = false

    fun resetPeriod() {
        container.resetPeriod()
    }

    fun setUserCapacityKwh(value: Double?) {
        container.setUserCapacityKwh(value)
    }

    fun canDrawOverlays(): Boolean = container.canDrawOverlays()

    fun setOverlayEnabled(enabled: Boolean): Boolean {
        if (enabled && !container.canDrawOverlays()) {
            pendingOverlayEnable = true
            return false
        }
        pendingOverlayEnable = false
        container.setOverlayEnabled(enabled)
        return true
    }

    fun onOverlayPermissionResult() {
        if (pendingOverlayEnable && container.canDrawOverlays()) {
            pendingOverlayEnable = false
            container.setOverlayEnabled(true)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        container.setThemeMode(mode)
    }

    fun clearPendingOverlayEnable() {
        pendingOverlayEnable = false
    }
}
