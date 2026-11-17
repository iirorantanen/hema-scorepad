package io.github.iirorantanen.hemascoreboard.bout

import kotlin.time.Duration

/** Where a device keeps the settings between launches, as text only the bout module reads. */
interface SettingsStore {
    /** The text last written, or null when nothing has been written. */
    fun read(): String?

    fun write(text: String)
}

/**
 * What the referee operates: keeps the current bout, remembers the settings and passes each signal on as the bout gives it.
 * Every change to the bout starts with a call here, so the returned bout is all a screen needs to draw.
 */
class Scoreboard(private val store: SettingsStore, private val giveSignal: (Signal) -> Unit) {
    // Stored settings arrive as a referee's change to a bout not begun, so values below the lowest are raised.
    var bout: BoutState =
        BoutState().handle(RefereeAction.ChangeSettings(store.read()?.let(Settings::fromText) ?: Settings())).state
        private set

    fun handle(action: RefereeAction): BoutState = keep(bout.handle(action))

    /** Time has passed. The Scoreboard never reads a clock; the caller says how much. */
    fun elapse(elapsed: Duration): BoutState = keep(bout.elapse(elapsed))

    private fun keep(update: BoutUpdate): BoutState {
        // Saved as the referee changes them, so the next launch starts with them.
        if (update.state.nextSettings != bout.nextSettings) store.write(update.state.nextSettings.toText())
        bout = update.state
        update.signals.forEach(giveSignal)
        return bout
    }
}
