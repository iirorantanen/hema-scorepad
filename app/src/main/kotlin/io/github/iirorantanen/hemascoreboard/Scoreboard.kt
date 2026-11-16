package io.github.iirorantanen.hemascoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.iirorantanen.hemascoreboard.bout.BoutState
import io.github.iirorantanen.hemascoreboard.bout.Fencer
import io.github.iirorantanen.hemascoreboard.bout.RefereeAction

// Lightened from pure red and blue so both stay readable on black.
private val Fencer.colour get() = when (this) {
    Fencer.RED -> Color(0xFFFF5252)
    Fencer.BLUE -> Color(0xFF4DA3FF)
}

private val Fencer.label get() = when (this) {
    Fencer.RED -> "Red"
    Fencer.BLUE -> "Blue"
}

/** Draws the bout and turns taps into referee actions. Holds no scoring rules. */
@Composable
fun Scoreboard(bout: BoutState, onAction: (RefereeAction) -> Unit) {
    Row(Modifier.fillMaxSize().background(Color.Black)) {
        Half(Fencer.RED, bout.score(Fencer.RED), onAction)
        Box(Modifier.width(2.dp).fillMaxHeight().background(Color.DarkGray))
        Half(Fencer.BLUE, bout.score(Fencer.BLUE), onAction)
    }
}

@Composable
private fun RowScope.Half(fencer: Fencer, score: Int, onAction: (RefereeAction) -> Unit) {
    Box(
        // clickable comes before the padding so the whole half is the tap target.
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClickLabel = "Award a point to ${fencer.label}") {
                onAction(RefereeAction.AwardPoint(fencer))
            }
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = score.toString(),
            modifier = Modifier.semantics { contentDescription = "${fencer.label} $score" },
            style = TextStyle(color = fencer.colour, fontWeight = FontWeight.Bold),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 48.sp, maxFontSize = 400.sp, stepSize = 8.sp),
        )
    }
}
