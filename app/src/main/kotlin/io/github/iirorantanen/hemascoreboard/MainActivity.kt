package io.github.iirorantanen.hemascoreboard

import android.os.Bundle
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.time.Duration.Companion.nanoseconds

class MainActivity : ComponentActivity() {
    private val viewModel: ScoreboardViewModel by viewModels {
        viewModelFactory {
            initializer {
                ScoreboardViewModel(
                    getPreferences(MODE_PRIVATE).settings(),
                    applicationContext.getSystemService(Vibrator::class.java),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getPreferences(MODE_PRIVATE)
        setContent {
            val bout = viewModel.bout
            val running = bout.timerRunning
            LaunchedEffect(running) {
                if (!running) return@LaunchedEffect
                // Frame times are the clock: each frame tells the Scoreboard how long the last one took.
                var last = withFrameNanos { it }
                while (true) {
                    val now = withFrameNanos { it }
                    viewModel.elapse((now - last).nanoseconds)
                    last = now
                }
            }
            Screen(bout) { action ->
                val before = viewModel.bout.nextSettings
                val after = viewModel.handle(action).nextSettings
                // Saved as the referee changes them, so the next launch starts with them.
                if (after != before) preferences.save(after)
            }
        }
    }
}
