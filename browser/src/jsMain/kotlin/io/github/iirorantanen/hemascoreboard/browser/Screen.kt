package io.github.iirorantanen.hemascoreboard.browser

import io.github.iirorantanen.hemascoreboard.bout.BoutState
import io.github.iirorantanen.hemascoreboard.bout.Fencer
import io.github.iirorantanen.hemascoreboard.bout.RefereeAction
import io.github.iirorantanen.hemascoreboard.bout.Scoreboard
import io.github.iirorantanen.hemascoreboard.bout.Settings
import io.github.iirorantanen.hemascoreboard.bout.reading
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.appendElement
import kotlinx.dom.clear
import org.w3c.dom.HTMLDialogElement
import org.w3c.dom.HTMLElement
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val Fencer.label get() = when (this) {
    Fencer.RED -> "Red"
    Fencer.BLUE -> "Blue"
}

/** The id of the fencer's half in index.html, and the class that gives the fencer's colour. */
private val Fencer.id get() = name.lowercase()

private fun element(id: String) = document.getElementById(id) as HTMLElement

/** A dialog in index.html. A tap outside closes it. */
private fun dialog(id: String) = (element(id) as HTMLDialogElement).also { dialog ->
    // The dialog's content fills it, so a tap that lands on the dialog itself is a tap outside.
    dialog.onclick = { if (it.target == dialog) dialog.close() }
}

/** Draws the bout the Scoreboard holds on the page in index.html and turns taps into referee actions. Holds no scoring rules. */
fun showScreen(scoreboard: Scoreboard) {
    fun handle(action: RefereeAction) = draw(scoreboard.handle(action))
    Fencer.entries.forEach { fencer -> element(fencer.id).onclick = { handle(RefereeAction.AwardPoint(fencer)) } }
    element("timer").onclick = { handle(RefereeAction.ToggleTimer) }
    element("undo").onclick = { handle(RefereeAction.Undo) }
    val pointLog = dialog("point-log")
    element("log").onclick = {
        drawPointLog(scoreboard.bout)
        pointLog.showModal()
    }
    element("swap").onclick = { handle(RefereeAction.SwapSides) }
    // Asks before a new bout clears the current one. Cancel, a tap outside and back send no referee action.
    val confirmation = dialog("new-bout-confirmation")
    element("new-bout").onclick = { confirmation.showModal() }
    element("confirm-new-bout").onclick = { handle(RefereeAction.NewBout) }
    val settings = dialog("settings-dialog")
    element("settings").onclick = { settings.showModal() }
    // The bout rules stop a step below the lowest bout length and point cap.
    fun change(id: String, changed: Settings.() -> Settings) {
        element(id).onclick = { handle(RefereeAction.ChangeSettings(scoreboard.bout.nextSettings.changed())) }
    }
    change("bout-length-down") { copy(boutLength = boutLength - 30.seconds) }
    change("bout-length-up") { copy(boutLength = boutLength + 30.seconds) }
    change("bout-length-switch") { copy(timerOn = !timerOn) }
    change("point-cap-down") { copy(pointCap = pointCap - 1) }
    change("point-cap-up") { copy(pointCap = pointCap + 1) }
    change("point-cap-switch") { copy(pointCapOn = !pointCapOn) }
    // A long press opens no browser menu.
    document.addEventListener("contextmenu", { it.preventDefault() })
    draw(scoreboard.bout)
    // Frame times are the clock: each frame tells the Scoreboard how long the last one took.
    var last = window.performance.now()
    fun frame(now: Double) {
        if (scoreboard.bout.timerRunning) drawTimer(scoreboard.elapse((now - last).milliseconds))
        last = now
        window.requestAnimationFrame(::frame)
    }
    window.requestAnimationFrame(::frame)
}

private fun draw(bout: BoutState) {
    Fencer.entries.forEach { fencer ->
        val score = bout.score(fencer).toString()
        val half = element(fencer.id)
        half.setAttribute("aria-label", "${fencer.label} $score, award a point")
        half.firstElementChild!!.textContent = score
        // The style sheet sizes the digits to fit the half.
        half.style.setProperty("--digits", score.length.toString())
    }
    // The halves are in the page in the order of the side arrangement, so a screen reader meets them left to right.
    val halves = element("halves")
    val left = if (bout.sidesSwapped) Fencer.BLUE else Fencer.RED
    if (halves.firstElementChild!!.id != left.id) halves.appendChild(halves.firstElementChild!!)
    drawTimer(bout)
    drawSetting("bout-length", bout.nextSettings.boutLength.reading(), bout.nextSettings.timerOn)
    drawSetting("point-cap", bout.nextSettings.pointCap.toString(), bout.nextSettings.pointCapOn)
}

/** One setting the referee has chosen, in the settings of index.html. */
private fun drawSetting(id: String, value: String, on: Boolean) {
    element("$id-value").textContent = if (on) value else "Off"
    // Hidden while off, where a step would change a value the referee cannot see.
    element("$id-down").hidden = !on
    element("$id-up").hidden = !on
    val switch = element("$id-switch")
    switch.textContent = if (on) "Switch off" else "Switch on"
    switch.setAttribute("aria-label", "Switch the ${id.replace('-', ' ')} ${if (on) "off" else "on"}")
}

private fun drawTimer(bout: BoutState) {
    val timer = element("timer")
    timer.hidden = !bout.settings.timerOn
    val reading = bout.timeRemaining.reading()
    val name = "Bout timer $reading" + (if (bout.inLastExchange) ", last exchange" else "") +
        if (bout.timerRunning) ", pause" else ", start"
    // The accessible name says everything the bout timer shows. A running bout timer is drawn every frame,
    // and only a changed name touches the page.
    if (timer.getAttribute("aria-label") == name) return
    timer.setAttribute("aria-label", name)
    timer.classList.toggle("running", bout.timerRunning)
    element("last-exchange").hidden = !bout.inLastExchange
    timer.lastElementChild!!.textContent = reading
}

/** Every point still standing, oldest first. */
private fun drawPointLog(bout: BoutState) {
    element("point-log-title").textContent = if (bout.log.isEmpty()) "No points yet" else "Point log"
    val points = element("points")
    points.clear()
    bout.log.forEach { point ->
        // Two short texts rather than one line, which would wrap on a narrow phone.
        points.appendElement("li") {
            appendElement("span") {
                className = point.fencer.id
                // With the bout timer off there is no reading to show.
                val reading = if (bout.settings.timerOn) "${point.timeRemaining.reading()}  " else ""
                textContent = reading + point.fencer.label
            }
            appendElement("span") { textContent = "Red ${point.red} – Blue ${point.blue}" }
        }
    }
}
