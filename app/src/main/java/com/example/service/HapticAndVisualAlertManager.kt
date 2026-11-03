package com.example.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class HapticAndVisualAlertManager(private val context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val _flashEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 5)
    val flashEvents: SharedFlow<Unit> = _flashEvents.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    fun triggerSpeechStartAlert(vibrationEnabled: Boolean, flashEnabled: Boolean) {
        if (vibrationEnabled) {
            vibrate(longArrayOf(0, 100), intArrayOf(0, 255), -1)
        }
        if (flashEnabled) {
            triggerScreenFlash()
        }
    }

    fun triggerSpeechEndAlert(vibrationEnabled: Boolean) {
        if (vibrationEnabled) {
            vibrate(longArrayOf(0, 60, 60, 80), intArrayOf(0, 180, 0, 200), -1)
        }
    }

    fun triggerTranscriptionCompleteAlert(vibrationEnabled: Boolean, flashEnabled: Boolean) {
        if (vibrationEnabled) {
            vibrate(longArrayOf(0, 120, 80, 140, 80, 180), intArrayOf(0, 220, 0, 240, 0, 255), -1)
        }
        if (flashEnabled) {
            triggerScreenFlash()
        }
    }

    fun triggerQuickCardAlert(vibrationEnabled: Boolean, flashEnabled: Boolean) {
        if (vibrationEnabled) {
            vibrate(longArrayOf(0, 150, 100, 150), intArrayOf(0, 255, 0, 255), -1)
        }
        if (flashEnabled) {
            triggerScreenFlash()
        }
    }

    fun triggerSoftTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } catch (_: Exception) {
                // Ignore fallback
            }
        }
    }

    private fun triggerScreenFlash() {
        scope.launch {
            _flashEvents.emit(Unit)
        }
    }

    private fun vibrate(timings: LongArray, amplitudes: IntArray, repeat: Int) {
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, repeat)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(timings, repeat)
                }
            }
        } catch (_: Exception) {
            // Devices without vibrator or missing permission fallback
        }
    }
}
