package com.geely.ex2.range.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Longitudinal road grade from the device accelerometer, degrees.
 * Positive — uphill, negative — downhill, 0 — level.
 */
class InclinationSensor(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    @Volatile
    private var pitchDegrees: Float? = null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val horizontal = sqrt(y * y + z * z)
            if (horizontal < 0.05f) return
            pitchDegrees = Math.toDegrees(atan2(-x.toDouble(), horizontal.toDouble())).toFloat()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        val sensor = accelerometer ?: return
        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_UI,
        )
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }

    fun pitchDegrees(): Float? = pitchDegrees?.takeIf { it.isFinite() }
}
