package com.geely.ex2.range.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class RangeApplication : Application() {
    lateinit var container: AppContainer
        private set

    /** Disk persistence and other IO off the UI thread. */
    val persistScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, persistScope)
    }
}
