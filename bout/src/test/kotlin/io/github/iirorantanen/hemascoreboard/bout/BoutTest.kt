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
