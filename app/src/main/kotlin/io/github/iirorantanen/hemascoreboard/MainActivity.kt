package io.github.iirorantanen.hemascoreboard

import android.os.Bundle
import android.os.Vibrator
import android.view.WindowManager
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
                    BoutStorage(getPreferences(MODE_PRIVATE)),
                    applicationContext.getSystemService(Vibrator::class.java),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The referee watches the fencers, not the display: it must not dim or lock while the scoreboard is in front.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
            Screen(bout, viewModel::handle)
        }
    }

    override fun onStop() {
        super.onStop()
        // A rotation also stops the activity, and must not pause the bout timer.
        if (!isChangingConfigurations) viewModel.leaveForeground()
    }
}
