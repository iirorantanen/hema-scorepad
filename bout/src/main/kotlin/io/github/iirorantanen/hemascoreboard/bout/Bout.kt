package io.github.iirorantanen.hemascoreboard.bout

import java.io.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

enum class Fencer { RED, BLUE }

sealed interface RefereeAction {
    data class AwardPoint(val fencer: Fencer) : RefereeAction

    /** A tap on the bout timer: starts it, pauses it or resumes it. */
    data object ToggleTimer : RefereeAction
}

/** Something the referee should be told about, without looking at the screen. */
enum class Signal { RED_POINT, BLUE_POINT }

data class BoutUpdate(val state: BoutState, val signals: List<Signal>)

// Serializable so the screen can keep the bout across rotation without knowing its fields.
data class BoutState(
    private val red: Int = 0,
    private val blue: Int = 0,
    val timeRemaining: Duration = 3.minutes,
    val timerRunning: Boolean = false,
) : Serializable {
    fun score(fencer: Fencer): Int = when (fencer) {
        Fencer.RED -> red
        Fencer.BLUE -> blue
    }

    fun handle(action: RefereeAction): BoutUpdate = when (action) {
        is RefereeAction.AwardPoint -> when (action.fencer) {
            Fencer.RED -> BoutUpdate(copy(red = red + 1), listOf(Signal.RED_POINT))
            Fencer.BLUE -> BoutUpdate(copy(blue = blue + 1), listOf(Signal.BLUE_POINT))
        }
        RefereeAction.ToggleTimer ->
            BoutUpdate(copy(timerRunning = !timerRunning && timeRemaining > Duration.ZERO), emptyList())
    }

    /** Time has passed. The bout never reads a clock; the caller says how much. */
    fun elapse(elapsed: Duration): BoutUpdate {
        if (!timerRunning) return BoutUpdate(this, emptyList())
        val remaining = (timeRemaining - elapsed).coerceAtLeast(Duration.ZERO)
        return BoutUpdate(copy(timeRemaining = remaining, timerRunning = remaining > Duration.ZERO), emptyList())
    }
}
