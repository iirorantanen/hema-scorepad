package io.github.iirorantanen.hemascoreboard

import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.iirorantanen.hemascoreboard.bout.BoutStore
import io.github.iirorantanen.hemascoreboard.bout.RefereeAction
import io.github.iirorantanen.hemascoreboard.bout.Scoreboard
import io.github.iirorantanen.hemascoreboard.bout.Signal
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Holds the Scoreboard and the signal player, so the bout and the signals still waiting to play survive rotation.
 * This outlives the activity: the vibrator must come from the application context, and the store must not hold the activity.
 */
class ScoreboardViewModel(store: BoutStore, vibrator: Vibrator?) : ViewModel() {
    // Queued, so signals given close together play one after another. Three wait behind the one playing:
    // enough for a double that reaches the point cap, and a mash does not vibrate long after the last tap.
    // The oldest goes first, so the end-of-bout signal, always the newest of its burst, still plays.
    private val signals = Channel<Signal>(3, BufferOverflow.DROP_OLDEST)
    private val scoreboard = Scoreboard(store) { signals.trySend(it) }

    /** The bout the Scoreboard holds, as Compose state so the Screen redraws when it changes. */
    var bout by mutableStateOf(scoreboard.bout)
        private set

    init {
        viewModelScope.launch { vibrator?.play(signals) }
    }

    fun handle(action: RefereeAction) {
        bout = scoreboard.handle(action)
    }

    fun leaveForeground() {
        bout = scoreboard.leaveForeground()
    }

    fun elapse(elapsed: Duration) {
        bout = scoreboard.elapse(elapsed)
    }
}
