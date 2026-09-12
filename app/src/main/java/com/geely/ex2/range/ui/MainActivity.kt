package com.geely.ex2.range.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.geely.ex2.range.service.RangeTrackingService

class MainActivity : AppCompatActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        startTracking()
    }

    // Растёт при каждом запросе «открыть на главной» через уже запущенный экземпляр
    // (см. onNewIntent) — RangeApp следит за значением и переключает вкладку на Дашборд.
    private var navigateHomeSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationAndStart()
        setContent {
            RangeApp(navigateHomeSignal = navigateHomeSignal)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_DASHBOARD, false)) {
            navigateHomeSignal++
        }
    }

    private fun requestNotificationAndStart() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startTracking()
        }
    }

    private fun startTracking() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, RangeTrackingService::class.java),
        )
    }

    companion object {
        const val EXTRA_OPEN_DASHBOARD = "com.geely.ex2.range.OPEN_DASHBOARD"
    }
}
