package io.github.iirorantanen.hemascoreboard.bout

import java.io.Serializable

enum class Fencer { RED, BLUE }

sealed interface RefereeAction {
    data class AwardPoint(val fencer: Fencer) : RefereeAction
}

/** Something the referee should be told about, without looking at the screen. */
enum class Signal { RED_POINT, BLUE_POINT }

data class BoutUpdate(val state: BoutState, val signals: List<Signal>)

// Serializable so the screen can keep the bout across rotation without knowing its fields.
data class BoutState(private val red: Int = 0, private val blue: Int = 0) : Serializable {
    fun score(fencer: Fencer): Int = when (fencer) {
        Fencer.RED -> red
        Fencer.BLUE -> blue
    }

    fun handle(action: RefereeAction): BoutUpdate = when (action) {
        is RefereeAction.AwardPoint -> when (action.fencer) {
            Fencer.RED -> BoutUpdate(copy(red = red + 1), listOf(Signal.RED_POINT))
            Fencer.BLUE -> BoutUpdate(copy(blue = blue + 1), listOf(Signal.BLUE_POINT))
        }
    }
}
