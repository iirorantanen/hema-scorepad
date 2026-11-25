package io.github.iirorantanen.hemascoreboard.browser

import io.github.iirorantanen.hemascoreboard.bout.Scoreboard

fun main() {
    showScreen(Scoreboard(BoutStorage, ::play))
    keepScreenAwake()
}
