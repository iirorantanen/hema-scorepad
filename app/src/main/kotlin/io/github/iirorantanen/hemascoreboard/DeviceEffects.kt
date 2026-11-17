package io.github.iirorantanen.hemascoreboard

import android.os.VibrationEffect
import io.github.iirorantanen.hemascoreboard.bout.Signal

private const val PULSE_MS = 120L
private const val GAP_MS = 150L
private const val LAST_EXCHANGE_MS = 1000L

/**
 * One pulse for a Red point, two for a Blue point, one long vibration for the last exchange.
 * Timings alternate off, on, off, on.
 */
fun Signal.vibration(): VibrationEffect = when (this) {
    Signal.RED_POINT -> VibrationEffect.createWaveform(longArrayOf(0, PULSE_MS), -1)
    Signal.BLUE_POINT -> VibrationEffect.createWaveform(longArrayOf(0, PULSE_MS, GAP_MS, PULSE_MS), -1)
    Signal.LAST_EXCHANGE -> VibrationEffect.createWaveform(longArrayOf(0, LAST_EXCHANGE_MS), -1)
}
