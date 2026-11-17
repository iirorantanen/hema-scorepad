package io.github.iirorantanen.hemascoreboard

import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.iirorantanen.hemascoreboard.bout.BoutState
import io.github.iirorantanen.hemascoreboard.bout.RefereeAction
import io.github.iirorantanen.hemascoreboard.bout.Scoreboard
import io.github.iirorantanen.hemascoreboard.bout.Settings
import io.github.iirorantanen.hemascoreboard.bout.Signal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Holds the Scoreboard and the signal player, so the bout and the signals still waiting to play survive rotation.
 * The vibrator must come from the application context: this outlives the activity.
 */
class ScoreboardViewModel(settings: Settings, vibrator: Vibrator?) : ViewModel() {
    // Queued, so signals given close together play one after another.
    private val signals = Channel<Signal>(Channel.UNLIMITED)
    private val scoreboard = Scoreboard(settings) { signals.trySend(it) }

    /** The bout the Scoreboard holds, as Compose state so the Screen redraws when it changes. */
    var bout by mutableStateOf(scoreboard.bout)
        private set

    init {
        viewModelScope.launch { vibrator?.play(signals) }
    }

    fun handle(action: RefereeAction): BoutState = scoreboard.handle(action).also { bout = it }

    fun elapse(elapsed: Duration) {
        bout = scoreboard.elapse(elapsed)
    }
}
