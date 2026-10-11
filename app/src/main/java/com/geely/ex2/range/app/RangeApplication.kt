package com.geely.ex2.range.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class RangeApplication : Application() {
    lateinit var container: AppContainer
        private set

    /** Disk persistence and other IO off the UI thread. */
    val persistScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Сколько экранов приложения сейчас на виду — см. [isUiVisible]. */
    @Volatile
    private var startedActivities = 0

    /** Приложение открыто перед пользователем (а не только сервис в фоне). */
    val isUiVisible: Boolean
        get() = startedActivities > 0

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, persistScope)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities--
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
