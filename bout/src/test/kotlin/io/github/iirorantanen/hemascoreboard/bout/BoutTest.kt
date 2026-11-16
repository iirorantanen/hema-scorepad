package io.github.iirorantanen.hemascoreboard.bout

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
