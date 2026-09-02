package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.sqrt

/**
 * Shake detector using Android Accelerometer sensor.
 * Detects vigorous device shaking to trigger 'Shake to Clear' instant wipe of chat history.
 * Uses applicationContext to avoid leaking Activity instances.
 */
class ShakeDetector(
    context: Context,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val appContext = context.applicationContext

    private val sensorManager: SensorManager? =
        appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val accelerometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTimestamp: Long = 0L
    private var isListening: Boolean = false

    // Sensitivity threshold in g-force (1.0 = normal gravity at rest)
    // 2.4f = standard shake, 1.8f = sensitive, 3.2f = hard shake
    var sensitivityThreshold: Float = 2.4f

    fun startListening() {
        if (isListening || accelerometer == null || sensorManager == null) return
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
        isListening = true
    }

    fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // Net g-force vector magnitude
        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        if (gForce > sensitivityThreshold) {
            val now = System.currentTimeMillis()
            // Ignore shake events too close to each other (1.2s debounce)
            if (now - lastShakeTimestamp >= 1200L) {
                lastShakeTimestamp = now
                triggerHapticPulse()
                onShakeDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    /**
     * Triggers distinct double-pulse haptic vibration on wipe
     */
    fun triggerHapticPulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val pattern = longArrayOf(0, 80, 50, 120)
                val amplitudes = intArrayOf(0, 200, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 120), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 80, 50, 120), -1)
                }
            }
        } catch (_: Exception) {
            // Ignore if vibration is not supported
        }
    }

    /**
     * Manually simulates a shake event for emulator testing and UI triggers.
     */
    fun simulateShake() {
        val now = System.currentTimeMillis()
        if (now - lastShakeTimestamp >= 800L) {
            lastShakeTimestamp = now
            triggerHapticPulse()
            onShakeDetected()
        }
    }
}
