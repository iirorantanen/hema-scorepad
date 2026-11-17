package io.github.iirorantanen.hemascoreboard.bout

import java.io.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class Fencer { RED, BLUE }

sealed interface RefereeAction {
    data class AwardPoint(val fencer: Fencer) : RefereeAction

    /** Takes back the most recent point. There is no redo. */
    data object Undo : RefereeAction

    /** A tap on the bout timer: starts it, pauses it or resumes it. */
    data object ToggleTimer : RefereeAction
}

/** Something the referee should be told about, without looking at the screen. */
enum class Signal { RED_POINT, BLUE_POINT, LAST_EXCHANGE, END_OF_BOUT }

// Fixed for now; the referee chooses it once bout settings exist.
private const val POINT_CAP = 10

data class BoutUpdate(val state: BoutState, val signals: List<Signal>)

/** One awarded point: who got it, the bout timer reading at that moment, and the score after it. */
data class LoggedPoint(val fencer: Fencer, val timeRemaining: Duration, val red: Int, val blue: Int) : Serializable

// Serializable so the screen can keep the bout across rotation without knowing its fields.
data class BoutState(
    /** Every point still standing, oldest first. The score is the last entry's score. */
    val log: List<LoggedPoint> = emptyList(),
    val timeRemaining: Duration = 3.minutes,
    val timerRunning: Boolean = false,
    /** Set when ten seconds remain, and kept for the rest of the bout. */
    val inLastExchange: Boolean = false,
) : Serializable {
    fun score(fencer: Fencer): Int = when (fencer) {
        Fencer.RED -> log.lastOrNull()?.red ?: 0
        Fencer.BLUE -> log.lastOrNull()?.blue ?: 0
    }

    fun handle(action: RefereeAction): BoutUpdate = when (action) {
        is RefereeAction.AwardPoint -> if (score(action.fencer) >= POINT_CAP) {
            BoutUpdate(this, emptyList())
        } else {
            val red = score(Fencer.RED) + if (action.fencer == Fencer.RED) 1 else 0
            val blue = score(Fencer.BLUE) + if (action.fencer == Fencer.BLUE) 1 else 0
            val signal = when (action.fencer) {
                Fencer.RED -> Signal.RED_POINT
                Fencer.BLUE -> Signal.BLUE_POINT
            }
            val reachesCap = score(action.fencer) + 1 == POINT_CAP
            // The bout ends once: the second fencer reaching the point cap gives no second signal.
            val alreadyEnded = maxOf(score(Fencer.RED), score(Fencer.BLUE)) == POINT_CAP
            BoutUpdate(
                copy(
                    log = log + LoggedPoint(action.fencer, timeRemaining, red, blue),
                    timerRunning = timerRunning && !reachesCap,
                ),
                if (reachesCap && !alreadyEnded) listOf(signal, Signal.END_OF_BOUT) else listOf(signal),
            )
        }
        RefereeAction.Undo -> BoutUpdate(copy(log = log.dropLast(1)), emptyList())
        RefereeAction.ToggleTimer ->
            BoutUpdate(copy(timerRunning = !timerRunning && timeRemaining > Duration.ZERO), emptyList())
    }

    /** Time has passed. The bout never reads a clock; the caller says how much. */
    fun elapse(elapsed: Duration): BoutUpdate {
        if (!timerRunning) return BoutUpdate(this, emptyList())
        val remaining = (timeRemaining - elapsed).coerceAtLeast(Duration.ZERO)
        val lastTenSeconds = remaining <= 10.seconds
        // Given once, on entering the last ten seconds, so pause and resume do not repeat it.
        // Not at 0:00: the bout timer stops without a signal.
        val signal = lastTenSeconds && !inLastExchange && remaining > Duration.ZERO
        return BoutUpdate(
            copy(
                timeRemaining = remaining,
                timerRunning = remaining > Duration.ZERO,
                inLastExchange = inLastExchange || lastTenSeconds,
            ),
            if (signal) listOf(Signal.LAST_EXCHANGE) else emptyList(),
        )
    }
}
