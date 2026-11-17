package io.github.iirorantanen.hemascoreboard

import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.iirorantanen.hemascoreboard.bout.SettingsStore

private const val SETTINGS = "settings"

/** The Scoreboard's settings text, kept in this device's key-value storage. */
class SettingsStorage(private val preferences: SharedPreferences) : SettingsStore {
    override fun read(): String? = preferences.getString(SETTINGS, null)

    override fun write(text: String) = preferences.edit { putString(SETTINGS, text) }
}
