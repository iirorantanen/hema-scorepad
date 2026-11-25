package io.github.iirorantanen.hemascoreboard.browser

import io.github.iirorantanen.hemascoreboard.bout.Signal
import kotlinx.browser.window

// The Android app's timings (its DeviceEffects.kt).
private const val PULSE_MS = 120
private const val GAP_MS = 150
private const val LAST_EXCHANGE_MS = 1000
private const val END_OF_BOUT_PULSE_MS = 400

// Longer than the gap inside a Blue point, so Red then Blue does not feel like Blue then Red.
private const val BETWEEN_SIGNALS_MS = 400

/**
 * One pulse for a Red point, two for a Blue point, one long vibration for the last exchange,
 * three medium pulses for the end of the bout.
 * Timings alternate on, off, on.
 */
private fun Signal.timings(): Array<Int> = when (this) {
    Signal.RED_POINT -> arrayOf(PULSE_MS)
    Signal.BLUE_POINT -> arrayOf(PULSE_MS, GAP_MS, PULSE_MS)
    Signal.LAST_EXCHANGE -> arrayOf(LAST_EXCHANGE_MS)
    Signal.END_OF_BOUT -> arrayOf(END_OF_BOUT_PULSE_MS, GAP_MS, END_OF_BOUT_PULSE_MS, GAP_MS, END_OF_BOUT_PULSE_MS)
}

// Signals given close together play one after another. Three wait behind the one playing:
// enough for a double that reaches the point cap, and a mash does not vibrate long after the last tap.
// The oldest waiting gives way, so the end-of-bout signal, always the newest of its burst, still plays.
private const val MOST_WAITING = 3
private val waiting = ArrayDeque<Signal>()
private var playing = false

/** Gives a signal as vibration, where the browser has a vibration interface. Where it has none, nothing takes its place. */
fun play(signal: Signal) {
    if (window.navigator.asDynamic().vibrate == undefined) return
    if (waiting.size == MOST_WAITING) waiting.removeFirst()
    waiting.addLast(signal)
    if (!playing) playNext()
}

/** Plays each signal to its end before the next. A vibration started while another plays would replace it. */
private fun playNext() {
    val timings = waiting.removeFirstOrNull()?.timings()
    playing = timings != null
    if (timings == null) return
    window.navigator.vibrate(timings)
    window.setTimeout(::playNext, timings.sum() + BETWEEN_SIGNALS_MS)
}
