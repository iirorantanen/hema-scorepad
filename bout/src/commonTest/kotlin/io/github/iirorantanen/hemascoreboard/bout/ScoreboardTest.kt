package io.github.iirorantanen.hemascoreboard.bout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** The settings store of a device that forgets nothing between launches: a second Scoreboard on it is the next launch. */
private class InMemorySettingsStore(private var text: String? = null) : SettingsStore {
    override fun read(): String? = text
    override fun write(text: String) {
        this.text = text
    }
}

private val defaultSettings = Settings(boutLength = 3.minutes, timerOn = true, pointCap = 10, pointCapOn = true)

class ScoreboardTest {
    @Test
    fun `a settings change is in effect at the next launch`() {
        val store = InMemorySettingsStore()
        Scoreboard(store) {}.handle(RefereeAction.ChangeSettings(Settings(boutLength = 2.minutes, pointCap = 5)))

        val nextLaunch = Scoreboard(store) {}

        assertEquals(Settings(boutLength = 2.minutes, pointCap = 5), nextLaunch.bout.settings)
        assertEquals(2.minutes, nextLaunch.bout.timeRemaining)
    }

    @Test
    fun `a settings change during a bout applies from the next new bout and is remembered at the next launch`() {
        val store = InMemorySettingsStore()
        val scoreboard = Scoreboard(store) {}
        scoreboard.handle(RefereeAction.AwardPoint(Fencer.RED))

        val during = scoreboard.handle(RefereeAction.ChangeSettings(Settings(pointCap = 5)))
        val nextLaunch = Scoreboard(store) {}
        val nextBout = scoreboard.handle(RefereeAction.NewBout)

        assertEquals(defaultSettings, during.settings)
        assertEquals(Settings(pointCap = 5), nextBout.settings)
        assertEquals(Settings(pointCap = 5), nextLaunch.bout.settings)
    }

    @Test
    fun `nothing stored starts with the default settings`() {
        assertEquals(defaultSettings, Scoreboard(InMemorySettingsStore()) {}.bout.settings)
    }

    @Test
    fun `unreadable stored text starts with the default settings`() {
        assertEquals(defaultSettings, Scoreboard(InMemorySettingsStore("not settings")) {}.bout.settings)
    }

    @Test
    fun `a stored bout length or point cap below its lowest value is raised to it, so points can be awarded`() {
        val store = InMemorySettingsStore(Settings(boutLength = 5.seconds, pointCap = 0).toText())

        val scoreboard = Scoreboard(store) {}

        assertEquals(Settings(boutLength = 30.seconds, pointCap = 1), scoreboard.bout.settings)
        assertEquals(30.seconds, scoreboard.bout.timeRemaining)
        assertEquals(1, scoreboard.handle(RefereeAction.AwardPoint(Fencer.RED)).score(Fencer.RED))
    }

    @Test
    fun `undo after a settings change while the bout has not begun keeps the change, now and at the next launch`() {
        val store = InMemorySettingsStore()
        val scoreboard = Scoreboard(store) {}
        scoreboard.handle(RefereeAction.ChangeSettings(Settings(boutLength = 2.minutes)))

        val bout = scoreboard.handle(RefereeAction.Undo)

        assertEquals(2.minutes, bout.timeRemaining)
        assertEquals(Settings(boutLength = 2.minutes), Scoreboard(store) {}.bout.settings)
    }

    @Test
    fun `the scoreboard keeps the bout as the referee acts`() {
        val scoreboard = Scoreboard(InMemorySettingsStore()) {}

        val bout = scoreboard.handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(1, bout.score(Fencer.BLUE))
        assertEquals(1, scoreboard.bout.score(Fencer.BLUE))
    }

    @Test
    fun `a point that reaches the point cap gives its point signal, then the end-of-bout signal`() {
        val signals = mutableListOf<Signal>()
        val scoreboard = Scoreboard(InMemorySettingsStore()) { signals += it }
        scoreboard.handle(RefereeAction.ChangeSettings(Settings(pointCap = 1)))

        val bout = scoreboard.handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(listOf(Signal.RED_POINT, Signal.END_OF_BOUT), signals)
        assertEquals(1, bout.score(Fencer.RED))
    }

    @Test
    fun `elapsed time into the last ten seconds gives the last-exchange signal once, across a pause and resume`() {
        val signals = mutableListOf<Signal>()
        val scoreboard = Scoreboard(InMemorySettingsStore()) { signals += it }
        scoreboard.handle(RefereeAction.ChangeSettings(Settings(boutLength = 1.minutes)))

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
