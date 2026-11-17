package io.github.iirorantanen.hemascoreboard.bout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ScoreboardTest {
    @Test
    fun `the scoreboard starts a bout with the starting settings and keeps it as the referee acts`() {
        val scoreboard = Scoreboard(Settings(boutLength = 2.minutes)) {}
        assertEquals(2.minutes, scoreboard.bout.timeRemaining)

        val bout = scoreboard.handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(1, bout.score(Fencer.BLUE))
        assertEquals(1, scoreboard.bout.score(Fencer.BLUE))
    }

    @Test
    fun `a point that reaches the point cap gives its point signal, then the end-of-bout signal`() {
        val signals = mutableListOf<Signal>()
        val scoreboard = Scoreboard(Settings(pointCap = 1)) { signals += it }

        val bout = scoreboard.handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(listOf(Signal.RED_POINT, Signal.END_OF_BOUT), signals)
        assertEquals(1, bout.score(Fencer.RED))
    }

    @Test
    fun `elapsed time into the last ten seconds gives the last-exchange signal once, across a pause and resume`() {
        val signals = mutableListOf<Signal>()
        val scoreboard = Scoreboard(Settings(boutLength = 1.minutes)) { signals += it }

        scoreboard.handle(RefereeAction.ToggleTimer)
        scoreboard.elapse(51.seconds)
        scoreboard.handle(RefereeAction.ToggleTimer)
        scoreboard.handle(RefereeAction.ToggleTimer)
        val bout = scoreboard.elapse(1.seconds)

        assertEquals(listOf(Signal.LAST_EXCHANGE), signals)
        assertEquals(8.seconds, bout.timeRemaining)
        assertTrue(bout.timerRunning)
    }
}
