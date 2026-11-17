package io.github.iirorantanen.hemascoreboard.bout

import kotlin.time.Duration

/** Where a device keeps the bout, and the settings with it, between launches, as text only the bout module reads. */
interface BoutStore {
    /** The text last written, or null when nothing has been written. */
    fun read(): String?

    fun write(text: String)
}

/**
 * What the referee operates: keeps the current bout, brings it back paused at the next launch, remembers the settings
 * and passes each signal on as the bout gives it.
 * Every change to the bout starts with a call here, so the returned bout is all a screen needs to draw.
 */
class Scoreboard(private val store: BoutStore, private val giveSignal: (Signal) -> Unit) {
    var bout: BoutState = store.read()?.let(BoutState::fromText) ?: BoutState()
        private set

    fun handle(action: RefereeAction): BoutState = keep(bout.handle(action))

    /** Time has passed. The Scoreboard never reads a clock; the caller says how much. */
    fun elapse(elapsed: Duration): BoutState = bout.elapse(elapsed).let { update ->
        // A running bout timer changes the bout every frame. Saving it once a second is enough:
        // a scoreboard that dies without leaving the foreground brings the bout back at most a second behind.
        val countingWithinASecond = update.state.timerRunning &&
            update.state.timeRemaining.inWholeSeconds == bout.timeRemaining.inWholeSeconds
        keep(update, save = !countingWithinASecond)
    }

    /** The scoreboard is no longer in front of the referee: the bout timer pauses, so the time away is not counted. */
    fun leaveForeground(): BoutState = if (bout.timerRunning) handle(RefereeAction.ToggleTimer) else bout

    private fun keep(update: BoutUpdate, save: Boolean = true): BoutState {
        // Saved as it changes, unless the caller holds the save back, so the next launch brings it back.
        if (save && update.state != bout) store.write(update.state.toText())
        bout = update.state
        update.signals.forEach(giveSignal)
        return bout
    }
}
