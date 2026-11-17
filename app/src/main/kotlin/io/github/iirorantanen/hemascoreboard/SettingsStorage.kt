package io.github.iirorantanen.hemascoreboard

import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.iirorantanen.hemascoreboard.bout.Settings

private const val SETTINGS = "settings"

/** The settings saved on this device, or the defaults on first launch. */
fun SharedPreferences.settings(): Settings = getString(SETTINGS, null)?.let(Settings::fromText) ?: Settings()

fun SharedPreferences.save(settings: Settings) = edit { putString(SETTINGS, settings.toText()) }
