package io.github.iirorantanen.hemascoreboard.bout

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
}
