package io.github.iirorantanen.hemascoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.iirorantanen.hemascoreboard.bout.BoutState
import io.github.iirorantanen.hemascoreboard.bout.Fencer
import io.github.iirorantanen.hemascoreboard.bout.LoggedPoint
import io.github.iirorantanen.hemascoreboard.bout.RefereeAction
import kotlin.time.Duration

// Lightened from pure red and blue so both stay readable on black.
private val Fencer.colour get() = when (this) {
    Fencer.RED -> Color(0xFFFF5252)
    Fencer.BLUE -> Color(0xFF4DA3FF)
}

private val Fencer.label get() = when (this) {
    Fencer.RED -> "Red"
    Fencer.BLUE -> "Blue"
}

// Rounded up to whole seconds, so 0:00 shows only once the timer has stopped.
private fun Duration.reading(): String {
    val seconds = (inWholeMilliseconds + 999) / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

/** Draws the bout and turns taps into referee actions. Holds no scoring rules. */
@Composable
fun Scoreboard(bout: BoutState, onAction: (RefereeAction) -> Unit) {
    var showLog by rememberSaveable { mutableStateOf(false) }
    var showNewBoutConfirmation by rememberSaveable { mutableStateOf(false) }
    val (left, right) = if (bout.sidesSwapped) Fencer.BLUE to Fencer.RED else Fencer.RED to Fencer.BLUE
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        // The controls already sit inside the bottom inset, so the halves must not pad for it again.
        Row(Modifier.weight(1f).consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))) {
            Half(left, bout.score(left), onAction)
            Box(Modifier.width(2.dp).fillMaxHeight().background(Color.DarkGray))
            Half(right, bout.score(right), onAction)
        }
        // Below the halves so a thumb reaches it one-handed, above the controls so it clears the home-swipe edge.
        Timer(bout.timeRemaining, bout.timerRunning, bout.inLastExchange, onAction)
        // Small controls in their own bar. The top padding is a dead strip, so a tap slightly high
        // on a control, or slightly low on the timer, lands on nothing.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Control("Undo", "Undo the most recent point") { onAction(RefereeAction.Undo) }
            Control("Log", "Show the point log") { showLog = true }
            Control("Swap", "Swap sides") { onAction(RefereeAction.SwapSides) }
            Control("New bout", "Start a new bout") { showNewBoutConfirmation = true }
        }
    }
    if (showLog) PointLog(bout.log) { showLog = false }
    if (showNewBoutConfirmation) {
        ConfirmNewBout(
            onConfirm = {
                showNewBoutConfirmation = false
                onAction(RefereeAction.NewBout)
            },
            onDismiss = { showNewBoutConfirmation = false },
        )
    }
}

private val dialogTitle = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

/** Asks before a new bout clears the current one. A tap outside or back cancels. */
@Composable
private fun ConfirmNewBout(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().background(Color(0xFF202020)).padding(16.dp)) {
            BasicText(text = "Start a new bout?", style = dialogTitle)
            BasicText(
                text = "This clears the score and the point log, and resets the bout timer.",
                modifier = Modifier.padding(top = 12.dp),
                style = TextStyle(color = Color.White, fontSize = 18.sp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Control("Cancel", "Keep the current bout", onDismiss)
                Control("New bout", "Clear the current bout", onConfirm)
            }
        }
    }
}

@Composable
private fun Control(label: String, onClickLabel: String, onClick: () -> Unit) {
    BasicText(
        text = label,
        // 18sp text plus the padding keeps the tap target at about 48dp.
        // Narrow padding so four controls fit across a 320dp phone at the default font size.
        modifier = Modifier
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
            .padding(12.dp),
        style = TextStyle(color = Color.LightGray, fontSize = 18.sp),
    )
}

/** Every point still standing, oldest first. Closes on a tap outside or back. */
@Composable
private fun PointLog(log: List<LoggedPoint>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF202020))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            val entry = TextStyle(color = Color.White, fontSize = 18.sp, fontFeatureSettings = "tnum")
            BasicText(
                text = if (log.isEmpty()) "No points yet" else "Point log",
                style = dialogTitle,
            )
            log.forEach { point ->
                // Two short texts rather than one line, which would wrap on a narrow phone.
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    BasicText(
                        text = "${point.timeRemaining.reading()}  ${point.fencer.label}",
                        style = entry.copy(color = point.fencer.colour),
                    )
                    BasicText(text = "Red ${point.red} – Blue ${point.blue}", style = entry)
                }
            }
        }
    }
}

@Composable
private fun Timer(remaining: Duration, running: Boolean, inLastExchange: Boolean, onAction: (RefereeAction) -> Unit) {
    val reading = remaining.reading()
    Column(
        // clickable comes before the padding so the whole bar is the tap target.
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = if (running) "Pause the bout timer" else "Start the bout timer") {
                onAction(RefereeAction.ToggleTimer)
            }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Inside the timer's tap target, so it adds no dead strip between the halves and the timer.
        if (inLastExchange) {
            BasicText(
                text = "Last exchange",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = TextStyle(color = Color.Yellow, fontSize = 24.sp, fontWeight = FontWeight.Bold),
            )
        }
        BasicText(
            text = reading,
            modifier = Modifier.semantics { contentDescription = "Bout timer $reading" },
            // Dimmed while paused: the reading alone takes up to a second to show a start.
            style = TextStyle(
                color = if (running) Color.White else Color.Gray,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
        )
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
