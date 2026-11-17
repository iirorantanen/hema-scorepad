package io.github.iirorantanen.hemascoreboard.bout

import kotlin.time.Duration

/**
 * What the referee operates: keeps the current bout and passes each signal on as the bout gives it.
 * Every change to the bout starts with a call here, so the returned bout is all a screen needs to draw.
 */
class Scoreboard(settings: Settings, private val giveSignal: (Signal) -> Unit) {
    var bout: BoutState = BoutState(settings)
        private set

    fun handle(action: RefereeAction): BoutState = keep(bout.handle(action))

    /** Time has passed. The Scoreboard never reads a clock; the caller says how much. */
    fun elapse(elapsed: Duration): BoutState = keep(bout.elapse(elapsed))

    private fun keep(update: BoutUpdate): BoutState {
        bout = update.state
        update.signals.forEach(giveSignal)
        return bout
    }
}
