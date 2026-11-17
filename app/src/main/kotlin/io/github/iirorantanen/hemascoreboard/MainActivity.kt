package io.github.iirorantanen.hemascoreboard

import android.os.Bundle
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import io.github.iirorantanen.hemascoreboard.bout.BoutState
import io.github.iirorantanen.hemascoreboard.bout.BoutUpdate
import io.github.iirorantanen.hemascoreboard.bout.RefereeAction
import io.github.iirorantanen.hemascoreboard.bout.Signal
import kotlinx.coroutines.channels.Channel
import kotlin.time.Duration.Companion.nanoseconds

// The bout as the bout module's text. The text reads back paused, so a bout timer that was running is started again.
private val boutSaver = listSaver<BoutState, Any>(
    save = { listOf(it.toText(), it.timerRunning) },
    restore = { (text, timerRunning) ->
        BoutState.fromText(text as String)?.let { if (timerRunning as Boolean) it.handle(RefereeAction.ToggleTimer).state else it }
    },
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vibrator = getSystemService(Vibrator::class.java)
        val preferences = getPreferences(MODE_PRIVATE)
        setContent {
            // Saveable, so the bout survives rotation.
            var bout by rememberSaveable(stateSaver = boutSaver) { mutableStateOf(BoutState(preferences.settings())) }
            // Queued, so signals given close together play one after another.
            val signals = remember { Channel<Signal>(Channel.UNLIMITED) }
            LaunchedEffect(Unit) { vibrator?.play(signals) }
            fun show(update: BoutUpdate) {
                // Saved as the referee changes them, so the next launch starts with them.
                if (update.state.nextSettings != bout.nextSettings) preferences.save(update.state.nextSettings)
                bout = update.state
                update.signals.forEach { signals.trySend(it) }
            }
            val running = bout.timerRunning
            LaunchedEffect(running) {
                if (!running) return@LaunchedEffect
                // Frame times are the clock: each frame tells the bout how long the last one took.
                var last = withFrameNanos { it }
                while (true) {
                    val now = withFrameNanos { it }
                    show(bout.elapse((now - last).nanoseconds))
                    last = now
                }
            }
            Scoreboard(bout) { show(bout.handle(it)) }
        }
    }
}
