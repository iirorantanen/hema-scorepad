package io.github.iirorantanen.hemascoreboard.bout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BoutTest {
    @Test
    fun `awarding a point to Red gives Red one point and Blue none`() {
        val update = BoutState().handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(1, update.state.score(Fencer.RED))
        assertEquals(0, update.state.score(Fencer.BLUE))
    }

    @Test
    fun `awarding a point to Blue gives Blue one point and Red none`() {
        val update = BoutState().handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(0, update.state.score(Fencer.RED))
        assertEquals(1, update.state.score(Fencer.BLUE))
    }

    @Test
    fun `a Red point gives the Red point signal`() {
        val update = BoutState().handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(listOf(Signal.RED_POINT), update.signals)
    }

    @Test
    fun `a Blue point gives the Blue point signal`() {
        val update = BoutState().handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(listOf(Signal.BLUE_POINT), update.signals)
    }

    @Test
    fun `points add up over a bout`() {
        val state = listOf(Fencer.RED, Fencer.BLUE, Fencer.RED)
            .fold(BoutState()) { bout, fencer -> bout.handle(RefereeAction.AwardPoint(fencer)).state }

        assertEquals(2, state.score(Fencer.RED))
        assertEquals(1, state.score(Fencer.BLUE))
    }

    @Test
    fun `a new bout has the bout timer paused at three minutes`() {
        val bout = BoutState()

        assertEquals(3.minutes, bout.timeRemaining)
        assertFalse(bout.timerRunning)
    }

    @Test
    fun `a tap on the bout timer starts it counting down`() {
        val bout = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(10.seconds).state

        assertTrue(bout.timerRunning)
        assertEquals(2.minutes + 50.seconds, bout.timeRemaining)
    }

    @Test
    fun `the bout timer does not count down before it is started`() {
        val bout = BoutState().elapse(10.seconds).state

        assertEquals(3.minutes, bout.timeRemaining)
    }

    @Test
    fun `a second tap pauses the bout timer and a third resumes it`() {
        val paused = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(10.seconds).state
            .handle(RefereeAction.ToggleTimer).state
            .elapse(30.seconds).state

        assertFalse(paused.timerRunning)
        assertEquals(2.minutes + 50.seconds, paused.timeRemaining)

        val resumed = paused.handle(RefereeAction.ToggleTimer).state
            .elapse(5.seconds).state

        assertTrue(resumed.timerRunning)
        assertEquals(2.minutes + 45.seconds, resumed.timeRemaining)
    }

    @Test
    fun `awarding a point leaves a running bout timer running`() {
        val bout = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(10.seconds).state
            .handle(RefereeAction.AwardPoint(Fencer.RED)).state
            .elapse(10.seconds).state

        assertEquals(1, bout.score(Fencer.RED))
        assertTrue(bout.timerRunning)
        assertEquals(2.minutes + 40.seconds, bout.timeRemaining)
    }

    @Test
    fun `awarding a point leaves a paused bout timer paused`() {
        val bout = BoutState().handle(RefereeAction.AwardPoint(Fencer.BLUE)).state

        assertEquals(1, bout.score(Fencer.BLUE))
        assertFalse(bout.timerRunning)
        assertEquals(3.minutes, bout.timeRemaining)
    }

    @Test
    fun `the bout timer stops at zero without a signal`() {
        val update = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(2.minutes + 59.seconds).state
            .elapse(5.seconds)

        assertEquals(Duration.ZERO, update.state.timeRemaining)
        assertFalse(update.state.timerRunning)
        assertEquals(emptyList(), update.signals)
    }

    @Test
    fun `a tap on the bout timer at zero does not restart it`() {
        val bout = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(3.minutes).state
            .handle(RefereeAction.ToggleTimer).state

        assertFalse(bout.timerRunning)
        assertEquals(Duration.ZERO, bout.timeRemaining)
    }

    @Test
    fun `a point can be awarded after the bout timer reaches zero`() {
        val update = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(3.minutes).state
            .handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(1, update.state.score(Fencer.BLUE))
        assertEquals(listOf(Signal.BLUE_POINT), update.signals)
    }

    @Test
    fun `the last exchange signal is given once, when ten seconds remain`() {
        val before = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(2.minutes + 49.seconds)

        assertEquals(emptyList(), before.signals)
        assertFalse(before.state.inLastExchange)

        val atTen = before.state.elapse(1.seconds)

        assertEquals(listOf(Signal.LAST_EXCHANGE), atTen.signals)
        assertTrue(atTen.state.inLastExchange)
        assertTrue(atTen.state.timerRunning)

        val after = atTen.state.elapse(1.seconds)

        assertEquals(emptyList(), after.signals)
        assertTrue(after.state.inLastExchange)

        val atZero = after.state.elapse(9.seconds)

        assertEquals(emptyList(), atZero.signals)
        assertTrue(atZero.state.inLastExchange)
    }

    @Test
    fun `a bout timer that passes ten seconds and zero in one step stops without a signal`() {
        val update = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(3.minutes)

        assertEquals(emptyList(), update.signals)
        assertFalse(update.state.timerRunning)
        assertTrue(update.state.inLastExchange)
    }

    @Test
    fun `pausing and resuming after the last exchange signal does not repeat it`() {
        val resumed = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(2.minutes + 50.seconds).state
            .handle(RefereeAction.ToggleTimer).state
            .handle(RefereeAction.ToggleTimer)

        assertEquals(emptyList(), resumed.signals)

        val update = resumed.state.elapse(1.seconds)

        assertEquals(emptyList(), update.signals)
        assertTrue(update.state.inLastExchange)
        assertEquals(9.seconds, update.state.timeRemaining)
    }

    @Test
    fun `a bout paused just before ten seconds gives the last exchange signal once it is resumed`() {
        val paused = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(2.minutes + 49.seconds).state
            .handle(RefereeAction.ToggleTimer).state
            .elapse(30.seconds)

        assertEquals(emptyList(), paused.signals)
        assertFalse(paused.state.inLastExchange)

        val update = paused.state.handle(RefereeAction.ToggleTimer).state
            .elapse(1.seconds)

        assertEquals(listOf(Signal.LAST_EXCHANGE), update.signals)
    }

    @Test
    fun `points can be awarded after the last exchange signal and after zero`() {
        val afterSignal = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(2.minutes + 50.seconds).state
            .handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(1, afterSignal.state.score(Fencer.RED))
        assertEquals(listOf(Signal.RED_POINT), afterSignal.signals)

        val afterZero = afterSignal.state
            .elapse(10.seconds).state
            .handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(Duration.ZERO, afterZero.state.timeRemaining)
        assertEquals(1, afterZero.state.score(Fencer.RED))
        assertEquals(1, afterZero.state.score(Fencer.BLUE))
        assertEquals(listOf(Signal.BLUE_POINT), afterZero.signals)
        assertTrue(afterZero.state.inLastExchange)
    }

    @Test
    fun `each point is logged with the fencer, the bout timer reading and the score after it`() {
        val bout = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(20.seconds).state
            .handle(RefereeAction.AwardPoint(Fencer.BLUE)).state
            .elapse(15.seconds).state
            .handle(RefereeAction.AwardPoint(Fencer.RED)).state

        assertEquals(
            listOf(
                LoggedPoint(Fencer.BLUE, 2.minutes + 40.seconds, red = 0, blue = 1),
                LoggedPoint(Fencer.RED, 2.minutes + 25.seconds, red = 1, blue = 1),
            ),
            bout.log,
        )
    }

    @Test
    fun `undo removes the most recent point and its log entry without a signal`() {
        val update = BoutState().handle(RefereeAction.AwardPoint(Fencer.RED)).state
            .handle(RefereeAction.AwardPoint(Fencer.BLUE)).state
            .handle(RefereeAction.Undo)

        assertEquals(1, update.state.score(Fencer.RED))
        assertEquals(0, update.state.score(Fencer.BLUE))
        assertEquals(listOf(LoggedPoint(Fencer.RED, 3.minutes, red = 1, blue = 0)), update.state.log)
        assertEquals(emptyList(), update.signals)
    }

    @Test
    fun `undo at 0-0 does nothing`() {
        val update = BoutState().handle(RefereeAction.Undo)

        assertEquals(BoutState(), update.state)
        assertEquals(emptyList(), update.signals)
    }

    @Test
    fun `undo can be repeated back to 0-0 with the log matching the score at each step`() {
        val first = LoggedPoint(Fencer.RED, 3.minutes, red = 1, blue = 0)
        val second = LoggedPoint(Fencer.BLUE, 3.minutes, red = 1, blue = 1)
        var bout = listOf(Fencer.RED, Fencer.BLUE, Fencer.BLUE)
            .fold(BoutState()) { state, fencer -> state.handle(RefereeAction.AwardPoint(fencer)).state }

        fun undoLeaves(red: Int, blue: Int, log: List<LoggedPoint>) {
            bout = bout.handle(RefereeAction.Undo).state
            assertEquals(red, bout.score(Fencer.RED))
            assertEquals(blue, bout.score(Fencer.BLUE))
            assertEquals(log, bout.log)
        }

        undoLeaves(red = 1, blue = 1, log = listOf(first, second))
        undoLeaves(red = 1, blue = 0, log = listOf(first))
        undoLeaves(red = 0, blue = 0, log = emptyList())
        undoLeaves(red = 0, blue = 0, log = emptyList())
    }

    @Test
    fun `undo does not change the bout timer`() {
        val bout = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(10.seconds).state
            .handle(RefereeAction.AwardPoint(Fencer.RED)).state
            .elapse(10.seconds).state
            .handle(RefereeAction.Undo).state

        assertEquals(0, bout.score(Fencer.RED))
        assertTrue(bout.timerRunning)
        assertEquals(2.minutes + 40.seconds, bout.timeRemaining)
    }

    private fun BoutState.award(fencer: Fencer, points: Int): BoutState =
        (1..points).fold(this) { bout, _ -> bout.handle(RefereeAction.AwardPoint(fencer)).state }

    @Test
    fun `the point that reaches the point cap gives the point signal, then the end of bout signal`() {
        val ninth = BoutState().award(Fencer.RED, 8).handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(listOf(Signal.RED_POINT), ninth.signals)

        val tenth = ninth.state.handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(10, tenth.state.score(Fencer.RED))
        assertEquals(listOf(Signal.RED_POINT, Signal.END_OF_BOUT), tenth.signals)
    }

    @Test
    fun `the bout timer stops when a fencer reaches the point cap`() {
        val bout = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(1.minutes).state
            .award(Fencer.BLUE, 10)
            .elapse(10.seconds).state

        assertFalse(bout.timerRunning)
        assertEquals(2.minutes, bout.timeRemaining)
    }

    @Test
    fun `a bout timer resumed after the point cap stops again when the other fencer reaches it`() {
        val bout = BoutState().award(Fencer.BLUE, 9).award(Fencer.RED, 10)
            .handle(RefereeAction.ToggleTimer).state
            .elapse(10.seconds).state
            .handle(RefereeAction.AwardPoint(Fencer.BLUE)).state
            .elapse(10.seconds).state

        assertEquals(10, bout.score(Fencer.BLUE))
        assertFalse(bout.timerRunning)
        assertEquals(2.minutes + 50.seconds, bout.timeRemaining)
    }

    @Test
    fun `a tap for a fencer at the point cap changes nothing and gives no signal`() {
        val capped = BoutState().award(Fencer.RED, 10)

        val update = capped.handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(capped, update.state)
        assertEquals(10, update.state.score(Fencer.RED))
        assertEquals(emptyList(), update.signals)
    }

    @Test
    fun `the other fencer can still be awarded points up to the point cap, without a second end of bout signal`() {
        val redCapped = BoutState().award(Fencer.BLUE, 8).award(Fencer.RED, 10)

        val ninth = redCapped.handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(9, ninth.state.score(Fencer.BLUE))
        assertEquals(listOf(Signal.BLUE_POINT), ninth.signals)

        val tenth = ninth.state.handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(10, tenth.state.score(Fencer.RED))
        assertEquals(10, tenth.state.score(Fencer.BLUE))
        assertEquals(listOf(Signal.BLUE_POINT), tenth.signals)

        val eleventh = tenth.state.handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(10, eleventh.state.score(Fencer.BLUE))
        assertEquals(emptyList(), eleventh.signals)
    }

    @Test
    fun `undo after the point cap leaves the bout timer paused until the referee resumes it`() {
        val undone = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(1.minutes).state
            .award(Fencer.RED, 10)
            .handle(RefereeAction.Undo).state
            .elapse(10.seconds).state

        assertEquals(9, undone.score(Fencer.RED))
        assertFalse(undone.timerRunning)
        assertEquals(2.minutes, undone.timeRemaining)

        val resumed = undone.handle(RefereeAction.ToggleTimer).state
            .elapse(10.seconds).state

        assertTrue(resumed.timerRunning)
        assertEquals(1.minutes + 50.seconds, resumed.timeRemaining)
    }

    @Test
    fun `undo after the point cap lets that fencer be awarded again, ending the bout again`() {
        val update = BoutState().award(Fencer.RED, 10)
            .handle(RefereeAction.Undo).state
            .handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(10, update.state.score(Fencer.RED))
        assertEquals(listOf(Signal.RED_POINT, Signal.END_OF_BOUT), update.signals)
    }

    @Test
    fun `swap sides changes only the side arrangement, and a second swap restores it`() {
        val bout = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(2.minutes + 50.seconds).state
            .handle(RefereeAction.AwardPoint(Fencer.RED)).state

        assertFalse(bout.sidesSwapped)

        val swapped = bout.handle(RefereeAction.SwapSides)

        assertTrue(swapped.state.sidesSwapped)
        assertEquals(bout, swapped.state.copy(sidesSwapped = false))
        assertEquals(emptyList(), swapped.signals)

        val back = swapped.state.handle(RefereeAction.SwapSides).state

        assertEquals(bout, back)
    }

    @Test
    fun `a point awarded after swap sides goes to the fencer it was awarded to`() {
        val bout = BoutState().handle(RefereeAction.SwapSides).state
            .handle(RefereeAction.AwardPoint(Fencer.BLUE)).state

        assertEquals(0, bout.score(Fencer.RED))
        assertEquals(1, bout.score(Fencer.BLUE))
        assertTrue(bout.sidesSwapped)
    }

    @Test
    fun `a new bout clears the score, the point log, the bout timer and the last exchange`() {
        val update = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(2.minutes + 50.seconds).state
            .handle(RefereeAction.AwardPoint(Fencer.RED)).state
            .handle(RefereeAction.AwardPoint(Fencer.BLUE)).state
            .handle(RefereeAction.NewBout)

        assertEquals(0, update.state.score(Fencer.RED))
        assertEquals(0, update.state.score(Fencer.BLUE))
        assertEquals(emptyList(), update.state.log)
        assertEquals(3.minutes, update.state.timeRemaining)
        assertFalse(update.state.timerRunning)
        assertFalse(update.state.inLastExchange)
        assertEquals(emptyList(), update.signals)
    }

    @Test
    fun `a new bout keeps the side arrangement`() {
        val swapped = BoutState().handle(RefereeAction.SwapSides).state
            .handle(RefereeAction.AwardPoint(Fencer.RED)).state
            .handle(RefereeAction.NewBout).state

        assertTrue(swapped.sidesSwapped)
        assertEquals(0, swapped.score(Fencer.RED))

        val unswapped = BoutState().handle(RefereeAction.AwardPoint(Fencer.RED)).state
            .handle(RefereeAction.NewBout).state

        assertFalse(unswapped.sidesSwapped)
    }

    @Test
    fun `a new bout gives its own last exchange and end of bout signals`() {
        val next = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(2.minutes + 50.seconds).state
            .award(Fencer.RED, 10)
            .handle(RefereeAction.NewBout).state

        val atTen = next.handle(RefereeAction.ToggleTimer).state
            .elapse(2.minutes + 50.seconds)

        assertEquals(listOf(Signal.LAST_EXCHANGE), atTen.signals)

        val tenth = atTen.state.award(Fencer.RED, 9).handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(listOf(Signal.RED_POINT, Signal.END_OF_BOUT), tenth.signals)
    }

    @Test
    fun `a bout written out and read back has the same score, point log, bout timer reading, last-exchange state and side arrangement, and is paused`() {
        val bout = BoutState().handle(RefereeAction.SwapSides).state
            .handle(RefereeAction.ToggleTimer).state
            .elapse(2.minutes + 50.5.seconds).state
            .handle(RefereeAction.AwardPoint(Fencer.RED)).state
            .handle(RefereeAction.AwardPoint(Fencer.BLUE)).state
            .handle(RefereeAction.AwardPoint(Fencer.RED)).state
            .handle(RefereeAction.Undo).state
        assertTrue(bout.timerRunning)

        val readBack = assertNotNull(BoutState.fromText(bout.toText()))

        assertEquals(1, readBack.score(Fencer.RED))
        assertEquals(1, readBack.score(Fencer.BLUE))
        assertEquals(
            listOf(LoggedPoint(Fencer.RED, 9.5.seconds, 1, 0), LoggedPoint(Fencer.BLUE, 9.5.seconds, 1, 1)),
            readBack.log,
        )
        assertEquals(9.5.seconds, readBack.timeRemaining)
        assertTrue(readBack.inLastExchange)
        assertTrue(readBack.sidesSwapped)
        assertFalse(readBack.timerRunning)
    }

    @Test
    fun `settings written out and read back are the same`() {
        val settings = Settings(boutLength = 90.seconds, timerOn = false, pointCap = 5, pointCapOn = false)

        assertEquals(settings, Settings.fromText(settings.toText()))
    }

    @Test
    fun `text that is not a written-out bout or settings reads back as nothing`() {
        val bout = BoutState().handle(RefereeAction.AwardPoint(Fencer.RED)).state.toText()

        assertNull(Settings.fromText(""))
        assertNull(Settings.fromText("three minutes"))
        assertNull(BoutState.fromText(""))
        assertNull(BoutState.fromText(Settings().toText()))
        assertNull(BoutState.fromText(bout.replace("RED", "GREEN")))
        assertNull(BoutState.fromText(bout.replace("true", "yes")))
    }

    @Test
    fun `with the point cap off, scores are not limited, no end of bout signal is given and the bout timer keeps running`() {
        val started = BoutState(Settings(pointCapOn = false)).handle(RefereeAction.ToggleTimer).state
            .award(Fencer.RED, 9)

        val tenth = started.handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(listOf(Signal.RED_POINT), tenth.signals)
        assertTrue(tenth.state.timerRunning)

        val later = tenth.state.award(Fencer.RED, 15)

        assertEquals(25, later.score(Fencer.RED))
        assertTrue(later.timerRunning)
    }

    @Test
    fun `with the bout timer off, a tap on it starts no countdown and no last exchange signal is given`() {
        val tapped = BoutState(Settings(timerOn = false)).handle(RefereeAction.ToggleTimer).state

        assertFalse(tapped.timerRunning)

        val later = tapped.elapse(2.minutes + 55.seconds)

        assertFalse(later.state.timerRunning)
        assertFalse(later.state.inLastExchange)
        assertEquals(emptyList(), later.signals)
    }

    @Test
    fun `with the bout timer off, the point cap still ends the bout`() {
        val tenth = BoutState(Settings(timerOn = false)).award(Fencer.BLUE, 9)
            .handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(listOf(Signal.BLUE_POINT, Signal.END_OF_BOUT), tenth.signals)

        val eleventh = tenth.state.handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(10, eleventh.state.score(Fencer.BLUE))
        assertEquals(emptyList(), eleventh.signals)
    }

    @Test
    fun `with the bout timer and the point cap both off, the scoreboard is a plain counter`() {
        val bout = BoutState(Settings(timerOn = false, pointCapOn = false))
            .handle(RefereeAction.ToggleTimer).state
            .award(Fencer.RED, 12)
            .award(Fencer.BLUE, 11)

        val point = bout.handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(12, point.state.score(Fencer.RED))
        assertEquals(12, point.state.score(Fencer.BLUE))
        assertEquals(listOf(Signal.BLUE_POINT), point.signals)
        assertFalse(point.state.timerRunning)
        assertEquals(emptyList(), point.state.elapse(5.minutes).signals)

        val undone = point.state.handle(RefereeAction.Undo).state

        assertEquals(11, undone.score(Fencer.BLUE))
    }

    @Test
    fun `a bout length and point cap changed during a bout do not apply to that bout`() {
        val changed = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(1.minutes).state
            .award(Fencer.RED, 4)
            .handle(RefereeAction.ChangeSettings(Settings(boutLength = 90.seconds, pointCap = 5)))

        assertEquals(emptyList(), changed.signals)
        assertEquals(2.minutes, changed.state.timeRemaining)
        assertTrue(changed.state.timerRunning)
        assertEquals(Settings(boutLength = 90.seconds, pointCap = 5), changed.state.nextSettings)

        val fifth = changed.state.handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(listOf(Signal.RED_POINT), fifth.signals)
        assertEquals(10, fifth.state.award(Fencer.RED, 8).score(Fencer.RED))
    }

    @Test
    fun `a changed bout length and point cap apply from the next new bout, and to the bouts after it`() {
        val next = BoutState().award(Fencer.RED, 4)
            .handle(RefereeAction.ChangeSettings(Settings(boutLength = 90.seconds, pointCap = 5))).state
            .handle(RefereeAction.NewBout).state

        assertEquals(90.seconds, next.timeRemaining)
        assertFalse(next.timerRunning)

        val fifth = next.award(Fencer.BLUE, 4).handle(RefereeAction.AwardPoint(Fencer.BLUE))

        assertEquals(listOf(Signal.BLUE_POINT, Signal.END_OF_BOUT), fifth.signals)

        val afterThat = fifth.state.handle(RefereeAction.NewBout).state

        assertEquals(90.seconds, afterThat.timeRemaining)
        assertEquals(5, afterThat.award(Fencer.RED, 8).score(Fencer.RED))
    }

    @Test
    fun `a bout timer or point cap switched off during a bout is off from the next new bout`() {
        val current = BoutState().handle(RefereeAction.ToggleTimer).state
            .handle(RefereeAction.ChangeSettings(Settings(timerOn = false, pointCapOn = false))).state
            .elapse(2.minutes + 50.seconds)

        assertEquals(listOf(Signal.LAST_EXCHANGE), current.signals)
        assertEquals(10, current.state.award(Fencer.RED, 12).score(Fencer.RED))

        val next = current.state.handle(RefereeAction.NewBout).state
            .handle(RefereeAction.ToggleTimer).state

        assertFalse(next.settings.timerOn)
        assertFalse(next.timerRunning)
        assertEquals(12, next.award(Fencer.RED, 12).score(Fencer.RED))
    }

    @Test
    fun `a bout with changed settings written out and read back keeps its settings and the change for the next new bout`() {
        val bout = BoutState(Settings(boutLength = 90.seconds, timerOn = false))
            .handle(RefereeAction.AwardPoint(Fencer.RED)).state
            .handle(RefereeAction.ChangeSettings(Settings(pointCap = 5, pointCapOn = false))).state

        val readBack = assertNotNull(BoutState.fromText(bout.toText()))

        assertEquals(Settings(boutLength = 90.seconds, timerOn = false), readBack.settings)
        assertEquals(Settings(pointCap = 5, pointCapOn = false), readBack.nextSettings)
    }

    @Test
    fun `a bout length below thirty seconds or a point cap below one is raised to that lowest value`() {
        val changed = BoutState()
            .handle(RefereeAction.ChangeSettings(Settings(boutLength = Duration.ZERO, pointCap = 0))).state
            .handle(RefereeAction.NewBout).state

        assertEquals(30.seconds, changed.timeRemaining)

        val first = changed.handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(1, first.state.score(Fencer.RED))
        assertEquals(listOf(Signal.RED_POINT, Signal.END_OF_BOUT), first.signals)
    }

    @Test
    fun `a bout length and point cap changed before the bout has begun apply to that bout`() {
        val changed = BoutState().handle(RefereeAction.SwapSides).state
            .handle(RefereeAction.ChangeSettings(Settings(boutLength = 90.seconds, pointCap = 5)))

        assertEquals(emptyList(), changed.signals)
        assertEquals(90.seconds, changed.state.timeRemaining)
        assertFalse(changed.state.timerRunning)
        assertTrue(changed.state.sidesSwapped)

        val fifth = changed.state.award(Fencer.RED, 4).handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(listOf(Signal.RED_POINT, Signal.END_OF_BOUT), fifth.signals)
    }

    @Test
    fun `a bout whose only point was undone has not begun, so a changed bout length applies to it`() {
        val changed = BoutState().handle(RefereeAction.AwardPoint(Fencer.RED)).state
            .handle(RefereeAction.Undo).state
            .handle(RefereeAction.ChangeSettings(Settings(boutLength = 90.seconds))).state

        assertEquals(90.seconds, changed.timeRemaining)
    }

    @Test
    fun `a bout whose bout timer has run has begun, even while paused with no points`() {
        val changed = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(1.seconds).state
            .handle(RefereeAction.ToggleTimer).state
            .handle(RefereeAction.ChangeSettings(Settings(boutLength = 90.seconds))).state

        assertEquals(2.minutes + 59.seconds, changed.timeRemaining)
        assertEquals(90.seconds, changed.handle(RefereeAction.NewBout).state.timeRemaining)
    }

    @Test
    fun `undoing the only point after a settings change leaves a bout that has not begun, so the change applies to it`() {
        val undone = BoutState().handle(RefereeAction.SwapSides).state
            .handle(RefereeAction.AwardPoint(Fencer.RED)).state
            .handle(RefereeAction.ChangeSettings(Settings(boutLength = 90.seconds, pointCap = 5))).state
            .handle(RefereeAction.Undo)

        assertEquals(emptyList(), undone.signals)
        assertEquals(90.seconds, undone.state.timeRemaining)
        assertTrue(undone.state.sidesSwapped)

        val fifth = undone.state.award(Fencer.RED, 4).handle(RefereeAction.AwardPoint(Fencer.RED))

        assertEquals(listOf(Signal.RED_POINT, Signal.END_OF_BOUT), fifth.signals)
    }

    @Test
    fun `undo after a settings change keeps the old settings while a point stands or the bout timer has run`() {
        val changed = BoutState().award(Fencer.RED, 2)
            .handle(RefereeAction.ChangeSettings(Settings(boutLength = 90.seconds))).state

        val onePointStands = changed.handle(RefereeAction.Undo).state

        assertEquals(1, onePointStands.score(Fencer.RED))
        assertEquals(3.minutes, onePointStands.timeRemaining)

        val timerHasRun = BoutState().handle(RefereeAction.ToggleTimer).state
            .elapse(1.seconds).state
            .handle(RefereeAction.AwardPoint(Fencer.RED)).state
            .handle(RefereeAction.ChangeSettings(Settings(boutLength = 90.seconds))).state
            .handle(RefereeAction.Undo).state

        assertEquals(0, timerHasRun.score(Fencer.RED))
        assertEquals(2.minutes + 59.seconds, timerHasRun.timeRemaining)
        assertEquals(90.seconds, timerHasRun.handle(RefereeAction.NewBout).state.timeRemaining)
    }
}
