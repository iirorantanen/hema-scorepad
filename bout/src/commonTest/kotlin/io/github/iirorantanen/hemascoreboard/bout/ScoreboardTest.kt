package io.github.iirorantanen.hemascoreboard.bout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** The bout store of a device that forgets nothing between launches: a second Scoreboard on it is the next launch. */
private class InMemoryBoutStore(private var text: String? = null) : BoutStore {
    override fun read(): String? = text
    override fun write(text: String) {
        this.text = text
    }
}

private val defaultSettings = Settings(boutLength = 3.minutes, timerOn = true, pointCap = 10, pointCapOn = true)

class ScoreboardTest {
    @Test
    fun `a settings change is in effect at the next launch`() {
        val store = InMemoryBoutStore()
        Scoreboard(store) {}.handle(RefereeAction.ChangeSettings(Settings(boutLength = 2.minutes, pointCap = 5)))

        val nextLaunch = Scoreboard(store) {}

        assertEquals(Settings(boutLength = 2.minutes, pointCap = 5), nextLaunch.bout.settings)
        assertEquals(2.minutes, nextLaunch.bout.timeRemaining)
    }

    @Test
    fun `a settings change during a bout applies from the next new bout, also when that bout starts after the next launch`() {
        val store = InMemoryBoutStore()
        val scoreboard = Scoreboard(store) {}
        scoreboard.handle(RefereeAction.AwardPoint(Fencer.RED))

        val during = scoreboard.handle(RefereeAction.ChangeSettings(Settings(pointCap = 5)))
        val nextLaunch = Scoreboard(store) {}
        val broughtBack = nextLaunch.bout
        val nextBout = nextLaunch.handle(RefereeAction.NewBout)

        assertEquals(defaultSettings, during.settings)
        assertEquals(defaultSettings, broughtBack.settings)
        assertEquals(1, broughtBack.score(Fencer.RED))
        assertEquals(Settings(pointCap = 5), nextBout.settings)
        assertEquals(0, nextBout.score(Fencer.RED))
    }

    @Test
    fun `nothing stored starts with the default settings`() {
        assertEquals(defaultSettings, Scoreboard(InMemoryBoutStore()) {}.bout.settings)
    }

    @Test
    fun `unreadable stored text starts with the default settings`() {
        assertEquals(defaultSettings, Scoreboard(InMemoryBoutStore("not settings")) {}.bout.settings)
    }

    @Test
    fun `a stored bout with a bout length or point cap below its lowest value is not brought back, so points can be awarded`() {
        val tooShort = Scoreboard(InMemoryBoutStore(BoutState(Settings(boutLength = 5.seconds)).toText())) {}
        val noPoints = Scoreboard(InMemoryBoutStore(BoutState(Settings(pointCap = 0)).toText())) {}

        assertEquals(defaultSettings, tooShort.bout.settings)
        assertEquals(3.minutes, tooShort.bout.timeRemaining)
        assertEquals(defaultSettings, noPoints.bout.settings)
        assertEquals(1, noPoints.handle(RefereeAction.AwardPoint(Fencer.RED)).score(Fencer.RED))
    }

    @Test
    fun `undo after a settings change while the bout has not begun keeps the change, now and at the next launch`() {
        val store = InMemoryBoutStore()
        val scoreboard = Scoreboard(store) {}
        scoreboard.handle(RefereeAction.ChangeSettings(Settings(boutLength = 2.minutes)))

        val bout = scoreboard.handle(RefereeAction.Undo)

        assertEquals(2.minutes, bout.timeRemaining)
        assertEquals(Settings(boutLength = 2.minutes), Scoreboard(store) {}.bout.settings)
    }

    @Test
    fun `leaving the foreground pauses the bout timer, so the time away is not counted`() {
        val scoreboard = Scoreboard(InMemoryBoutStore()) {}
        scoreboard.handle(RefereeAction.ToggleTimer)
        scoreboard.elapse(10.seconds)

        val away = scoreboard.leaveForeground()
        val back = scoreboard.elapse(5.minutes)

        assertFalse(away.timerRunning)
        assertEquals(2.minutes + 50.seconds, back.timeRemaining)
    }

    @Test
    fun `leaving the foreground with the bout timer paused leaves it paused at its reading`() {
        val scoreboard = Scoreboard(InMemoryBoutStore()) {}
        scoreboard.handle(RefereeAction.ToggleTimer)
        scoreboard.elapse(10.seconds)
        scoreboard.handle(RefereeAction.ToggleTimer)

        val away = scoreboard.leaveForeground()

        assertFalse(away.timerRunning)
        assertEquals(2.minutes + 50.seconds, away.timeRemaining)
    }

    @Test
    fun `undo after leaving the foreground takes back the point and leaves the bout timer paused at its reading`() {
        val scoreboard = Scoreboard(InMemoryBoutStore()) {}
        scoreboard.handle(RefereeAction.ToggleTimer)
        scoreboard.elapse(10.seconds)
        scoreboard.handle(RefereeAction.AwardPoint(Fencer.RED))
        scoreboard.leaveForeground()

        val undone = scoreboard.handle(RefereeAction.Undo)

        assertEquals(0, undone.score(Fencer.RED))
        assertFalse(undone.timerRunning)
        assertEquals(2.minutes + 50.seconds, undone.timeRemaining)
    }

    @Test
    fun `a bout left running comes back at the next launch paused, with its score, point log, bout timer reading, last-exchange state and side arrangement`() {
        val store = InMemoryBoutStore()
        val scoreboard = Scoreboard(store) {}
        scoreboard.handle(RefereeAction.ChangeSettings(Settings(boutLength = 1.minutes)))
        scoreboard.handle(RefereeAction.SwapSides)
        scoreboard.handle(RefereeAction.ToggleTimer)
        scoreboard.elapse(20.seconds)
        scoreboard.handle(RefereeAction.AwardPoint(Fencer.RED))
        scoreboard.elapse(32.seconds)
        scoreboard.leaveForeground()

        val back = Scoreboard(store) {}.bout

        assertEquals(1, back.score(Fencer.RED))
        assertEquals(0, back.score(Fencer.BLUE))
        assertEquals(listOf(LoggedPoint(Fencer.RED, 40.seconds, 1, 0)), back.log)
        assertEquals(8.seconds, back.timeRemaining)
        assertTrue(back.inLastExchange)
        assertTrue(back.sidesSwapped)
        assertFalse(back.timerRunning)
    }

    @Test
    fun `a bout whose bout timer ran out comes back at the next launch at zero`() {
        val store = InMemoryBoutStore()
        val scoreboard = Scoreboard(store) {}
        scoreboard.handle(RefereeAction.ToggleTimer)
        scoreboard.elapse(4.minutes)

        val back = Scoreboard(store) {}.bout

        assertEquals(Duration.ZERO, back.timeRemaining)
        assertTrue(back.inLastExchange)
    }

    @Test
    fun `a scoreboard killed while the bout timer runs brings the bout back paused, with its score and a bout timer reading less than a second behind`() {
        val store = InMemoryBoutStore()
        val scoreboard = Scoreboard(store) {}
        scoreboard.handle(RefereeAction.ToggleTimer)
        scoreboard.elapse(50.seconds)
        scoreboard.handle(RefereeAction.AwardPoint(Fencer.BLUE))
        repeat(54) { scoreboard.elapse(100.milliseconds) }
        assertEquals(2.minutes + 4.6.seconds, scoreboard.bout.timeRemaining)

        val back = Scoreboard(store) {}.bout

        assertEquals(1, back.score(Fencer.BLUE))
        assertEquals(listOf(LoggedPoint(Fencer.BLUE, 2.minutes + 10.seconds, 0, 1)), back.log)
        assertTrue(back.timeRemaining >= 2.minutes + 4.6.seconds)
        assertTrue(back.timeRemaining < 2.minutes + 5.6.seconds)
        assertFalse(back.timerRunning)
    }

    @Test
    fun `a bout brought back gives no signal, and does not repeat the last-exchange or end-of-bout signal`() {
        val store = InMemoryBoutStore()
        val scoreboard = Scoreboard(store) {}
        scoreboard.handle(RefereeAction.ChangeSettings(Settings(boutLength = 1.minutes, pointCap = 1)))
        scoreboard.handle(RefereeAction.ToggleTimer)
        scoreboard.elapse(51.seconds)
        scoreboard.handle(RefereeAction.AwardPoint(Fencer.RED))

        val signals = mutableListOf<Signal>()
        val nextLaunch = Scoreboard(store) { signals += it }
        assertEquals(emptyList(), signals)

        nextLaunch.handle(RefereeAction.ToggleTimer)
        val later = nextLaunch.elapse(1.seconds)
        nextLaunch.handle(RefereeAction.AwardPoint(Fencer.RED))
        nextLaunch.handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(8.seconds, later.timeRemaining)
        assertEquals(listOf(Signal.BLUE_POINT), signals)
    }

    @Test
    fun `undo at the next launch takes back the last point of the bout brought back`() {
        val store = InMemoryBoutStore()
        val scoreboard = Scoreboard(store) {}
        scoreboard.handle(RefereeAction.AwardPoint(Fencer.RED))
        scoreboard.handle(RefereeAction.AwardPoint(Fencer.BLUE))

        val nextLaunch = Scoreboard(store) {}
        val undone = nextLaunch.handle(RefereeAction.Undo)

        assertEquals(listOf(LoggedPoint(Fencer.RED, 3.minutes, 1, 0)), undone.log)
        assertEquals(0, Scoreboard(store) {}.bout.score(Fencer.BLUE))
    }

    @Test
    fun `a deduction is brought back at the next launch, and undo there restores the score`() {
        val store = InMemoryBoutStore()
        val scoreboard = Scoreboard(store) {}
        scoreboard.handle(RefereeAction.AwardPoint(Fencer.RED))
        scoreboard.handle(RefereeAction.DeductPoint(Fencer.RED))

        val nextLaunch = Scoreboard(store) {}
        val broughtBack = nextLaunch.bout
        val undone = nextLaunch.handle(RefereeAction.Undo)

        assertEquals(LoggedPoint(Fencer.RED, 3.minutes, 0, 0, deduction = true), broughtBack.log.last())
        assertEquals(1, undone.score(Fencer.RED))
    }

    @Test
    fun `a bout stored by v1_1 is brought back`() {
        // Written by the v1.1 release, before deductions.
        val stored = "180000000000 true 10 true\n120000000000 true 5 true\n170000000000 false false\n" +
            "RED 175000000000 1 0\nBLUE 172000000000 1 1"

        val bout = Scoreboard(InMemoryBoutStore(stored)) {}.bout

        assertEquals(
            listOf(LoggedPoint(Fencer.RED, 2.minutes + 55.seconds, 1, 0), LoggedPoint(Fencer.BLUE, 2.minutes + 52.seconds, 1, 1)),
            bout.log,
        )
        assertEquals(Settings(boutLength = 2.minutes, pointCap = 5), bout.nextSettings)
        assertEquals(2.minutes + 50.seconds, bout.timeRemaining)
    }

    @Test
    fun `a new bout started after the bout timer ran is still new at the next launch`() {
        val store = InMemoryBoutStore()
        val scoreboard = Scoreboard(store) {}
        scoreboard.handle(RefereeAction.ToggleTimer)
        scoreboard.elapse(30.seconds)
        scoreboard.handle(RefereeAction.ToggleTimer)
        scoreboard.handle(RefereeAction.NewBout)

        assertEquals(3.minutes, Scoreboard(store) {}.bout.timeRemaining)
    }

    @Test
    fun `the scoreboard keeps the bout as the referee acts`() {
        val scoreboard = Scoreboard(InMemoryBoutStore()) {}

        val bout = scoreboard.handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(1, bout.score(Fencer.BLUE))
        assertEquals(1, scoreboard.bout.score(Fencer.BLUE))
    }

    @Test
    fun `a point that reaches the point cap gives its point signal, then the end-of-bout signal`() {
        val signals = mutableListOf<Signal>()
        val scoreboard = Scoreboard(InMemoryBoutStore()) { signals += it }
        scoreboard.handle(RefereeAction.ChangeSettings(Settings(pointCap = 1)))

        val bout = scoreboard.handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(listOf(Signal.RED_POINT, Signal.END_OF_BOUT), signals)
        assertEquals(1, bout.score(Fencer.RED))
    }

    @Test
    fun `elapsed time into the last ten seconds gives the last-exchange signal once, across a pause and resume`() {
        val signals = mutableListOf<Signal>()
        val scoreboard = Scoreboard(InMemoryBoutStore()) { signals += it }
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

    @Test
    fun `a bout timer reading is rounded up to whole seconds, so it shows zero only once the bout timer has stopped`() {
        val scoreboard = Scoreboard(InMemoryBoutStore()) {}
        val full = scoreboard.bout.timeRemaining.reading()
        scoreboard.handle(RefereeAction.ToggleTimer)

        scoreboard.elapse(1.minutes + 54.seconds + 500.milliseconds)
        val logged = scoreboard.handle(RefereeAction.AwardPoint(Fencer.RED)).log.single().timeRemaining.reading()
        val almostOut = scoreboard.elapse(1.minutes + 5.seconds + 499.milliseconds)
        val out = scoreboard.elapse(1.milliseconds)

        assertEquals("3:00", full)
        assertEquals("1:06", logged)
        assertEquals("0:01", almostOut.timeRemaining.reading())
        assertTrue(almostOut.timerRunning)
        assertEquals("0:00", out.timeRemaining.reading())
        assertFalse(out.timerRunning)
    }
}
