package io.github.iirorantanen.hemascoreboard.browser

import io.github.iirorantanen.hemascoreboard.bout.BoutStore
import kotlinx.browser.localStorage

// Every project site of the owner shares one origin and so one local storage: the key carries a prefix of its own (ADR 0004).
private const val BOUT = "hema-scorepad.bout"

/**
 * The Scoreboard's bout text, kept in this browser's local storage.
 * A browser can refuse local storage, with site data blocked for example. Scoring still works, and the bout lasts as long as the page.
 */
object BoutStorage : BoutStore {
    override fun read(): String? = runCatching { localStorage.getItem(BOUT) }.getOrNull()

    override fun write(text: String) {
        runCatching { localStorage.setItem(BOUT, text) }
    }
}
