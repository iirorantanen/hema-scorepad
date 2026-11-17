package io.github.iirorantanen.hemascoreboard.bout

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
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
     * Changes the bout length and the point cap. The change applies from the next new bout,
     * or to this bout when it has not begun: no points stand and the bout timer has not run.
     * A change stored for the next new bout applies to this bout once undo leaves it not begun.
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
) {
    /** The settings written out as one line of a written-out bout. */
    internal fun toText(): String = "${boutLength.inWholeNanoseconds} $timerOn $pointCap $pointCapOn"
}

private fun readSettings(text: String): Settings {
    val (boutLength, timerOn, pointCap, pointCapOn) = text.split(' ')
    return Settings(boutLength.toLong().nanoseconds, timerOn.toBooleanStrict(), pointCap.toInt(), pointCapOn.toBooleanStrict())
        // No version writes values below the lowest, and a point cap of 0 would block every point.
        .also { require(it.boutLength >= LOWEST_BOUT_LENGTH && it.pointCap >= LOWEST_POINT_CAP) }
}

// Longer than the last ten seconds, so the last exchange never starts with the bout.
private val LOWEST_BOUT_LENGTH = 30.seconds
private const val LOWEST_POINT_CAP = 1

// Stored text can come from another version or be damaged: every conversion then fails with a RuntimeException.
private inline fun <T> readOrNull(read: () -> T): T? = try {
    read()
} catch (e: RuntimeException) {
    null
}

data class BoutUpdate(val state: BoutState, val signals: List<Signal>)

/** One awarded point: who got it, the bout timer reading at that moment, and the score after it. */
data class LoggedPoint(val fencer: Fencer, val timeRemaining: Duration, val red: Int, val blue: Int)

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
) {
    /**
     * The bout written out as text, for the device to store. [fromText] reads it back.
     * Whether the bout timer is running is left out: a bout read back is paused.
     */
    fun toText(): String = (
        listOf(settings.toText(), nextSettings.toText(), "${timeRemaining.inWholeNanoseconds} $inLastExchange $sidesSwapped") +
            log.map { "${it.fencer} ${it.timeRemaining.inWholeNanoseconds} ${it.red} ${it.blue}" }
        ).joinToString("\n")

    companion object {
        /** The bout [toText] wrote, paused, or null when the text is not a bout this version wrote. */
        fun fromText(text: String): BoutState? = readOrNull {
            val lines = text.lines()
            val (timeRemaining, inLastExchange, sidesSwapped) = lines[2].split(' ')
            BoutState(
                settings = readSettings(lines[0]),
                nextSettings = readSettings(lines[1]),
                log = lines.drop(3).map { line ->
                    val (fencer, time, red, blue) = line.split(' ')
                    LoggedPoint(Fencer.valueOf(fencer), time.toLong().nanoseconds, red.toInt(), blue.toInt())
                },
                timeRemaining = timeRemaining.toLong().nanoseconds,
                inLastExchange = inLastExchange.toBooleanStrict(),
                sidesSwapped = sidesSwapped.toBooleanStrict(),
            )
        }
    }

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
        RefereeAction.Undo -> {
            val undone = copy(log = log.dropLast(1))
            // Undoing the only point leaves a bout that has not begun, so a change stored during it applies now.
            BoutUpdate(if (undone.begun) undone else undone.fresh(nextSettings), emptyList())
        }
        RefereeAction.ToggleTimer ->
            BoutUpdate(copy(timerRunning = !timerRunning && settings.timerOn && timeRemaining > Duration.ZERO), emptyList())
        RefereeAction.SwapSides -> BoutUpdate(copy(sidesSwapped = !sidesSwapped), emptyList())
        RefereeAction.NewBout -> BoutUpdate(fresh(nextSettings), emptyList())
        is RefereeAction.ChangeSettings -> {
            val chosen = action.settings.copy(
                boutLength = action.settings.boutLength.coerceAtLeast(LOWEST_BOUT_LENGTH),
                pointCap = action.settings.pointCap.coerceAtLeast(LOWEST_POINT_CAP),
            )
            BoutUpdate(if (begun) copy(nextSettings = chosen) else fresh(chosen), emptyList())
        }
    }

    /** False while no points stand and the bout timer has not run. */
    private val begun: Boolean get() = log.isNotEmpty() || timerRunning || timeRemaining != settings.boutLength

    // A fresh state, so nothing else can carry over; only the side arrangement is kept.
    private fun fresh(settings: Settings) = BoutState(settings, sidesSwapped = sidesSwapped)

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
