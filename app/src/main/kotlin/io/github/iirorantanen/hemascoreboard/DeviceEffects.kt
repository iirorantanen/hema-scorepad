package io.github.iirorantanen.hemascoreboard

import android.os.VibrationEffect
import android.os.Vibrator
import io.github.iirorantanen.hemascoreboard.bout.Signal
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay

private const val PULSE_MS = 120L
private const val GAP_MS = 150L
private const val LAST_EXCHANGE_MS = 1000L
private const val END_OF_BOUT_PULSE_MS = 400L

// Longer than the gap inside a Blue point, so Red then Blue does not feel like Blue then Red.
private const val BETWEEN_SIGNALS_MS = 400L

/**
 * One pulse for a Red point, two for a Blue point, one long vibration for the last exchange,
 * three medium pulses for the end of the bout.
 * Timings alternate off, on, off, on.
 */
private fun Signal.timings(): LongArray = when (this) {
    Signal.RED_POINT -> longArrayOf(0, PULSE_MS)
    Signal.BLUE_POINT -> longArrayOf(0, PULSE_MS, GAP_MS, PULSE_MS)
    Signal.LAST_EXCHANGE -> longArrayOf(0, LAST_EXCHANGE_MS)
    Signal.END_OF_BOUT ->
        longArrayOf(0, END_OF_BOUT_PULSE_MS, GAP_MS, END_OF_BOUT_PULSE_MS, GAP_MS, END_OF_BOUT_PULSE_MS)
}

/** Plays each signal to its end before the next. A vibration started while another plays replaces it. */
suspend fun Vibrator.play(signals: ReceiveChannel<Signal>) {
    for (signal in signals) {
        val timings = signal.timings()
        vibrate(VibrationEffect.createWaveform(timings, -1))
        delay(timings.sum() + BETWEEN_SIGNALS_MS)
    }
}
