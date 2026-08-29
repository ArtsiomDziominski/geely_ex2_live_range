package com.geely.ex2.range.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.geely.ex2.range.app.RangeApplication

class RangeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as RangeApplication).container
    val uiState = container.uiState

    fun resetPeriod() {
        container.resetPeriod()
    }

    fun setUserCapacityKwh(value: Double?) {
        container.setUserCapacityKwh(value)
    }
}
