package io.github.iirorantanen.hemascoreboard

import android.os.Bundle
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import io.github.iirorantanen.hemascoreboard.bout.BoutState
import io.github.iirorantanen.hemascoreboard.bout.BoutUpdate
import kotlin.time.Duration.Companion.nanoseconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vibrator = getSystemService(Vibrator::class.java)
        setContent {
            // Saveable, so the bout survives rotation.
            var bout by rememberSaveable { mutableStateOf(BoutState()) }
            fun show(update: BoutUpdate) {
                bout = update.state
                update.signals.forEach { vibrator?.vibrate(it.vibration()) }
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
