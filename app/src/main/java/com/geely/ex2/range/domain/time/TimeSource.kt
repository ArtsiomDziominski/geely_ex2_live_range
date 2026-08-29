package com.geely.ex2.range.domain.time

interface TimeSource {
    fun elapsedRealtimeMs(): Long
    fun wallClockMs(): Long
}
