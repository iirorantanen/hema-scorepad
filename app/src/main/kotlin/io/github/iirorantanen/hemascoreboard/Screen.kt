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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.iirorantanen.hemascoreboard.bout.BoutState
import io.github.iirorantanen.hemascoreboard.bout.Fencer
import io.github.iirorantanen.hemascoreboard.bout.LoggedPoint
import io.github.iirorantanen.hemascoreboard.bout.RefereeAction
import io.github.iirorantanen.hemascoreboard.bout.Settings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
fun Screen(bout: BoutState, onAction: (RefereeAction) -> Unit) {
    var showLog by rememberSaveable { mutableStateOf(false) }
    var showNewBoutConfirmation by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val (left, right) = if (bout.sidesSwapped) Fencer.BLUE to Fencer.RED else Fencer.RED to Fencer.BLUE
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        // The controls already sit inside the bottom inset, so the halves must not pad for it again.
        Row(Modifier.weight(1f).consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))) {
            Half(left, bout.score(left), onAction)
            Box(Modifier.width(2.dp).fillMaxHeight().background(Color.DarkGray))
            Half(right, bout.score(right), onAction)
        }
        // Below the halves so a thumb reaches it one-handed, above the controls so it clears the home-swipe edge.
        if (bout.settings.timerOn) Timer(bout.timeRemaining, bout.timerRunning, bout.inLastExchange, onAction)
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
            // A glyph, not the word, so five controls fit in one row.
            Control("⚙", "Open the settings") { showSettings = true }
        }
    }
    if (showLog) PointLog(bout.log, showReadings = bout.settings.timerOn) { showLog = false }
    if (showSettings) {
        SettingsDialog(bout.nextSettings, { onAction(RefereeAction.ChangeSettings(it)) }) { showSettings = false }
    }
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
private val dialogText = TextStyle(color = Color.White, fontSize = 18.sp, fontFeatureSettings = "tnum")

/** The bout length and the point cap the referee has chosen. Closes on a tap outside or back. */
@Composable
private fun SettingsDialog(settings: Settings, onChange: (Settings) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().background(Color(0xFF202020)).padding(16.dp)) {
            BasicText(text = "Settings", style = dialogTitle)
            // The bout rules stop a step below the lowest bout length and point cap.
            Setting(
                name = "Bout length",
                value = settings.boutLength.reading(),
                step = "30 s",
                on = settings.timerOn,
                onStep = { onChange(settings.copy(boutLength = settings.boutLength + 30.seconds * it)) },
                onSwitch = { onChange(settings.copy(timerOn = it)) },
            )
            Setting(
                name = "Point cap",
                value = settings.pointCap.toString(),
                step = "1",
                on = settings.pointCapOn,
                onStep = { onChange(settings.copy(pointCap = settings.pointCap + it)) },
                onSwitch = { onChange(settings.copy(pointCapOn = it)) },
            )
            BasicText(
                text = "A change during a bout applies from the next new bout.",
                modifier = Modifier.padding(top = 12.dp),
                style = dialogText.copy(color = Color.LightGray),
            )
        }
    }
}

/** One setting: its value, a step down, a step up, and the switch that turns it off or back on. */
@Composable
private fun Setting(
    name: String,
    value: String,
    step: String,
    on: Boolean,
    onStep: (Int) -> Unit,
    onSwitch: (Boolean) -> Unit,
) {
    val setting = name.lowercase()
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        BasicText(text = name, style = dialogText)
        BasicText(text = if (on) value else "Off", style = dialogText)
    }
    // The switch stays at the end of the row, whether or not the steps are shown.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (on) Arrangement.SpaceBetween else Arrangement.End) {
        // Hidden while off, where a step would change a value the referee cannot see.
        if (on) {
            Control("−$step", "Lower the $setting by $step") { onStep(-1) }
            Control("+$step", "Raise the $setting by $step") { onStep(1) }
        }
        Control(if (on) "Switch off" else "Switch on", "Switch the $setting ${if (on) "off" else "on"}") {
            onSwitch(!on)
        }
    }
}

/** Asks before a new bout clears the current one. A tap outside or back cancels. */
@Composable
private fun ConfirmNewBout(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().background(Color(0xFF202020)).padding(16.dp)) {
            BasicText(text = "Start a new bout?", style = dialogTitle)
            BasicText(
                text = "This clears the score and the point log, and resets the bout timer.",
                modifier = Modifier.padding(top = 12.dp),
                style = dialogText,
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
        // 18sp text plus the vertical padding keeps the tap target about 48dp high, and the minimum
        // width keeps a short label 48dp wide. Narrow horizontal padding so five controls fit across
        // a 320dp phone at the default font size.
        modifier = Modifier
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .defaultMinSize(minWidth = 32.dp),
        style = TextStyle(color = Color.LightGray, fontSize = 18.sp, textAlign = TextAlign.Center),
    )
}

/** Every point still standing, oldest first. Closes on a tap outside or back. */
@Composable
private fun PointLog(log: List<LoggedPoint>, showReadings: Boolean, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF202020))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            BasicText(
                text = if (log.isEmpty()) "No points yet" else "Point log",
                style = dialogTitle,
            )
            log.forEach { point ->
                // Two short texts rather than one line, which would wrap on a narrow phone.
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    BasicText(
                        // With the bout timer off there is no reading to show.
                        text = (if (showReadings) "${point.timeRemaining.reading()}  " else "") + point.fencer.label,
                        style = dialogText.copy(color = point.fencer.colour),
                    )
                    BasicText(text = "Red ${point.red} – Blue ${point.blue}", style = dialogText)
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
