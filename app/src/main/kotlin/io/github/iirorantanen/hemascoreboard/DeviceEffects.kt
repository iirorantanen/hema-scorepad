package io.github.iirorantanen.hemascoreboard

import android.media.AudioAttributes
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
 * One pulse for a Red point, two for a Blue point, three for a deduction, one long vibration for the last exchange,
 * three medium pulses for the end of the bout.
 * Timings alternate off, on, off, on.
 */
private fun Signal.timings(): LongArray = when (this) {
    Signal.RED_POINT -> longArrayOf(0, PULSE_MS)
    Signal.BLUE_POINT -> longArrayOf(0, PULSE_MS, GAP_MS, PULSE_MS)
    Signal.DEDUCTION -> longArrayOf(0, PULSE_MS, GAP_MS, PULSE_MS, GAP_MS, PULSE_MS)
    Signal.LAST_EXCHANGE -> longArrayOf(0, LAST_EXCHANGE_MS)
    Signal.END_OF_BOUT ->
        longArrayOf(0, END_OF_BOUT_PULSE_MS, GAP_MS, END_OF_BOUT_PULSE_MS, GAP_MS, END_OF_BOUT_PULSE_MS)
}

// Without a usage, Android treats a vibration of one pulse as touch feedback and drops it while that setting is off.
// Alarm is the usage that plays with touch feedback off and with the ringer silent.
// AudioAttributes carries the usage back to API 26; VibrationAttributes would need a branch at API 33 (ADR 0003).
private val SIGNAL_USAGE = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()

/** Plays each signal to its end before the next. A vibration started while another plays replaces it. */
suspend fun Vibrator.play(signals: ReceiveChannel<Signal>) {
    for (signal in signals) {
        val timings = signal.timings()
        @Suppress("DEPRECATION") // deprecated at API 33, and the only overload that takes a usage at API 26
        vibrate(VibrationEffect.createWaveform(timings, -1), SIGNAL_USAGE)
        delay(timings.sum() + BETWEEN_SIGNALS_MS)
    }
}
