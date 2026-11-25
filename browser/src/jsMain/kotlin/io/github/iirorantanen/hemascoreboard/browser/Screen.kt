package io.github.iirorantanen.hemascoreboard.browser

import io.github.iirorantanen.hemascoreboard.bout.BoutState
import io.github.iirorantanen.hemascoreboard.bout.Fencer
import io.github.iirorantanen.hemascoreboard.bout.RefereeAction
import io.github.iirorantanen.hemascoreboard.bout.Scoreboard
import io.github.iirorantanen.hemascoreboard.bout.reading
import kotlinx.browser.document
import kotlinx.dom.appendElement
import kotlinx.dom.clear
import org.w3c.dom.HTMLDialogElement
import org.w3c.dom.HTMLElement

private val Fencer.label get() = when (this) {
    Fencer.RED -> "Red"
    Fencer.BLUE -> "Blue"
}

/** The id of the fencer's half in index.html, and the class that gives the fencer's colour. */
private val Fencer.id get() = name.lowercase()

private fun element(id: String) = document.getElementById(id) as HTMLElement

/** Draws the bout the Scoreboard holds on the page in index.html and turns taps into referee actions. Holds no scoring rules. */
fun showScreen(scoreboard: Scoreboard) {
    fun handle(action: RefereeAction) = draw(scoreboard.handle(action))
    Fencer.entries.forEach { fencer -> element(fencer.id).onclick = { handle(RefereeAction.AwardPoint(fencer)) } }
    element("undo").onclick = { handle(RefereeAction.Undo) }
    val pointLog = element("point-log") as HTMLDialogElement
    element("log").onclick = {
        drawPointLog(scoreboard.bout)
        pointLog.showModal()
    }
    // The dialog's content fills it, so a tap that lands on the dialog itself is a tap outside.
    pointLog.onclick = { if (it.target == pointLog) pointLog.close() }
    // A long press opens no browser menu.
    document.addEventListener("contextmenu", { it.preventDefault() })
    draw(scoreboard.bout)
}

private fun draw(bout: BoutState) = Fencer.entries.forEach { fencer ->
    val score = bout.score(fencer).toString()
    val half = element(fencer.id)
    half.setAttribute("aria-label", "${fencer.label} $score, award a point")
    half.firstElementChild!!.textContent = score
    // The style sheet sizes the digits to fit the half.
    half.style.setProperty("--digits", score.length.toString())
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
