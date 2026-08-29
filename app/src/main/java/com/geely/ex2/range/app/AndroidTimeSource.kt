package com.geely.ex2.range.app

import android.os.SystemClock
import com.geely.ex2.range.domain.time.TimeSource

class AndroidTimeSource : TimeSource {
    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
    override fun wallClockMs(): Long = System.currentTimeMillis()
}
