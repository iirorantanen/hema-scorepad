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

    /** Exchanges which side of the display Red and Blue appear on. */
    data object SwapSides : RefereeAction

    /** Clears the score, the point log and the bout timer. The side arrangement and the settings are kept. */
    data object NewBout : RefereeAction

    /**
     * Changes the bout length and the point cap. The change applies from the next new bout.
     * A bout length below 30 seconds or a point cap below 1 is raised to that lowest value.
     */
    data class ChangeSettings(val settings: Settings) : RefereeAction
}

/** Something the referee should be told about, without looking at the screen. */
enum class Signal { RED_POINT, BLUE_POINT, LAST_EXCHANGE, END_OF_BOUT }

/** The bout length and the point cap. Each can be switched off and keeps its value while off. */
data class Settings(
    val boutLength: Duration = 3.minutes,
    val timerOn: Boolean = true,
    val pointCap: Int = 10,
    val pointCapOn: Boolean = true,
) : Serializable

data class BoutUpdate(val state: BoutState, val signals: List<Signal>)

/** One awarded point: who got it, the bout timer reading at that moment, and the score after it. */
data class LoggedPoint(val fencer: Fencer, val timeRemaining: Duration, val red: Int, val blue: Int) : Serializable

// Serializable so the screen can keep the bout across rotation without knowing its fields.
data class BoutState(
    /** The settings this bout started with. They hold for the whole bout. */
    val settings: Settings = Settings(),
    /** The settings the referee has chosen for the next new bout. */
    val nextSettings: Settings = settings,
    /** Every point still standing, oldest first. The score is the last entry's score. */
    val log: List<LoggedPoint> = emptyList(),
    val timeRemaining: Duration = settings.boutLength,
    val timerRunning: Boolean = false,
    /** Set when ten seconds remain, and kept for the rest of the bout. */
    val inLastExchange: Boolean = false,
    /** The side arrangement: whether Red and Blue have swapped sides of the display. */
    val sidesSwapped: Boolean = false,
) : Serializable {
    fun score(fencer: Fencer): Int = when (fencer) {
        Fencer.RED -> log.lastOrNull()?.red ?: 0
        Fencer.BLUE -> log.lastOrNull()?.blue ?: 0
    }

    /** Null when the point cap is off, so no score ever equals it. */
    private val pointCap: Int? get() = settings.pointCap.takeIf { settings.pointCapOn }

    fun handle(action: RefereeAction): BoutUpdate = when (action) {
        is RefereeAction.AwardPoint -> if (score(action.fencer) == pointCap) {
            BoutUpdate(this, emptyList())
        } else {
            val red = score(Fencer.RED) + if (action.fencer == Fencer.RED) 1 else 0
            val blue = score(Fencer.BLUE) + if (action.fencer == Fencer.BLUE) 1 else 0
            val signal = when (action.fencer) {
                Fencer.RED -> Signal.RED_POINT
                Fencer.BLUE -> Signal.BLUE_POINT
            }
            val reachesCap = score(action.fencer) + 1 == pointCap
            // The bout ends once: the second fencer reaching the point cap gives no second signal.
            val alreadyEnded = maxOf(score(Fencer.RED), score(Fencer.BLUE)) == pointCap
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
            BoutUpdate(copy(timerRunning = !timerRunning && settings.timerOn && timeRemaining > Duration.ZERO), emptyList())
        RefereeAction.SwapSides -> BoutUpdate(copy(sidesSwapped = !sidesSwapped), emptyList())
        // A fresh state, so nothing else can carry over; only the settings and the side arrangement are kept.
        RefereeAction.NewBout -> BoutUpdate(BoutState(nextSettings, sidesSwapped = sidesSwapped), emptyList())
        is RefereeAction.ChangeSettings -> BoutUpdate(
            copy(
                nextSettings = action.settings.copy(
                    // Longer than the last ten seconds, so the last exchange never starts with the bout.
                    boutLength = action.settings.boutLength.coerceAtLeast(30.seconds),
                    pointCap = action.settings.pointCap.coerceAtLeast(1),
                ),
            ),
            emptyList(),
        )
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
