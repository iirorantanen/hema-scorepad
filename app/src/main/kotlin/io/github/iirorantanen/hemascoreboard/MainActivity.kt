package io.github.iirorantanen.hemascoreboard

import android.os.Bundle
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.iirorantanen.hemascoreboard.bout.BoutState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vibrator = getSystemService(Vibrator::class.java)
        setContent {
            // Saveable, so the bout survives rotation.
            var bout by rememberSaveable { mutableStateOf(BoutState()) }
            Scoreboard(bout) { action ->
                val update = bout.handle(action)
                bout = update.state
                update.signals.forEach { vibrator?.vibrate(it.vibration()) }
            }
        }
    }
}
