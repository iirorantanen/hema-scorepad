package io.github.iirorantanen.hemascoreboard

import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.iirorantanen.hemascoreboard.bout.Settings
import kotlin.time.Duration.Companion.seconds

private const val BOUT_LENGTH_SECONDS = "boutLengthSeconds"
private const val TIMER_ON = "timerOn"
private const val POINT_CAP = "pointCap"
private const val POINT_CAP_ON = "pointCapOn"

/** The settings saved on this device, or the defaults on first launch. */
fun SharedPreferences.settings(): Settings {
    val defaults = Settings()
    return Settings(
        boutLength = getLong(BOUT_LENGTH_SECONDS, defaults.boutLength.inWholeSeconds).seconds,
        timerOn = getBoolean(TIMER_ON, defaults.timerOn),
        pointCap = getInt(POINT_CAP, defaults.pointCap),
        pointCapOn = getBoolean(POINT_CAP_ON, defaults.pointCapOn),
    )
}

fun SharedPreferences.save(settings: Settings) = edit {
    putLong(BOUT_LENGTH_SECONDS, settings.boutLength.inWholeSeconds)
    putBoolean(TIMER_ON, settings.timerOn)
    putInt(POINT_CAP, settings.pointCap)
    putBoolean(POINT_CAP_ON, settings.pointCapOn)
}
