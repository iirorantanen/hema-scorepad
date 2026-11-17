package io.github.iirorantanen.hemascoreboard.bout

import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun `a bout with logged points survives being saved and restored`() {
        val bout = BoutState().handle(RefereeAction.AwardPoint(Fencer.RED)).state
            .handle(RefereeAction.AwardPoint(Fencer.BLUE)).state
            .handle(RefereeAction.Undo).state

        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).writeObject(bout) }.toByteArray()
        val restored = ObjectInputStream(bytes.inputStream()).readObject()

        assertEquals(bout, restored)
    }
}
