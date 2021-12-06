package org.andstatus.game2048.view

import com.soywiz.korge.ui.uiScrollableArea
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.container
import com.soywiz.korge.view.position
import com.soywiz.korge.view.roundRect
import com.soywiz.korge.view.text
import com.soywiz.korim.text.TextAlignment
import org.andstatus.game2048.model.ShortRecord
import kotlin.math.max

fun ViewData.showRecentGames(recentGames: List<ShortRecord>) = myWindow("recent_games") {
    val listTop = winTop + cellMargin + buttonSize + buttonMargin
    val nItems = recentGames.size
    val itemHeight = buttonSize
    val textWidth = winWidth * 2
    val textSize = defaultTextSize

    fun Container.rowText(value: String, xPosition: Double) = text(value, textSize,
        gameColors.buttonText, font, TextAlignment.MIDDLE_LEFT
    ) {
        position(xPosition, itemHeight / 2)
    }

    fun Container.oneRow(index: Int, score: String, lastChanged: String, duration: String, id: String,
                         note: String, action: () -> Unit) {
        container {
            roundRect(textWidth, itemHeight, buttonRadius, fill = gameColors.buttonBackground)
            var xPos = cellMargin
            rowText(score, xPos)
            xPos += textSize * 2.8
            rowText(lastChanged, xPos)
            xPos += textSize * 6.4
            rowText(duration, xPos)
            xPos += textSize * 4.6
            rowText(id, xPos)
            if (note.isNotBlank()) {
                xPos += textSize * 1.4
                rowText(note, xPos)
            }

            position(0.0, index * (itemHeight + cellMargin))
            customOnClick { action() }
        }
    }

    uiScrollableArea(config = {
        position(winLeft + cellMargin, listTop)
        buttonSize = itemHeight
        scaledWidth = winWidth - cellMargin * 2
        contentWidth = textWidth
        scaledHeight = winTop + winHeight - listTop - cellMargin
        contentHeight = max((itemHeight + cellMargin) * (nItems + 1), height)
    }) {
        oneRow(0, stringResources.text("score"), stringResources.text("last_changed"),
            stringResources.text("duration"), stringResources.text("id"),
            stringResources.text("note")) {}

        recentGames.sortedByDescending { it.finalPosition.startingDateTime }.forEachIndexed { index, game ->
            oneRow(
                index + 1,
                game.finalPosition.score.toString(),
                game.finalPosition.startingDateTimeString,
                game.finalPosition.gameClock.playedSecondsString,
                game.id.toString(),
                game.note
            ) {
                window.removeFromParent()
                presenter.onHistoryItemClick(game.id)
            }
        }
    }
}