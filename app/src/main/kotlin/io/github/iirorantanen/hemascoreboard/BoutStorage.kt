package io.github.iirorantanen.hemascoreboard

import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.iirorantanen.hemascoreboard.bout.BoutStore

private const val BOUT = "bout"

/** The Scoreboard's bout text, kept in this device's key-value storage. */
class BoutStorage(private val preferences: SharedPreferences) : BoutStore {
    override fun read(): String? = preferences.getString(BOUT, null)

    override fun write(text: String) = preferences.edit { putString(BOUT, text) }
}
