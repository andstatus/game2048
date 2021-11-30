package org.andstatus.game2048.view

import com.soywiz.korge.ui.uiScrollableArea
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.container
import com.soywiz.korge.view.position
import com.soywiz.korge.view.roundRect
import com.soywiz.korge.view.text
import com.soywiz.korim.text.TextAlignment
import org.andstatus.game2048.model.GameRecord
import kotlin.math.max

fun ViewData.showBookmarks(game: GameRecord) = myWindow("goto_bookmark") {
    val listTop = winTop + cellMargin + buttonSize + buttonMargin
    val nItems = game.shortRecord.bookmarks.size + 1
    val itemHeight = buttonSize
    val textWidth = winWidth * 2
    val textSize = defaultTextSize

    fun Container.rowText(value: String, xPosition: Double) = text(value, textSize,
        gameColors.buttonText, font, TextAlignment.MIDDLE_LEFT
    ) {
        position(xPosition, itemHeight / 2)
    }

    fun Container.oneRow(index: Int, score: String, moveNumber: String, lastChanged: String, duration: String,
                         action: () -> Unit) {
        container {
            roundRect(textWidth, itemHeight, buttonRadius, fill = gameColors.buttonBackground)
            var xPos = cellMargin
            rowText(score, xPos)
            xPos += textSize * 2.8
            rowText(moveNumber, xPos)
            xPos += textSize * 2.4
            rowText(lastChanged, xPos)
            xPos += textSize * 6.4
            rowText(duration, xPos)

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
        oneRow(0, stringResources.text("score"),
                stringResources.text("move"),
                stringResources.text("last_changed"),
                stringResources.text("duration")) {}
        with(game.shortRecord.finalPosition) {
            oneRow(1, score.toString(),
                    moveNumber.toString(),
                    startingDateTimeString,
                    gameClock.playedSecondsString) {
                window.removeFromParent()
                presenter.onGoToBookmarkClick(this)
            }
        }
        game.shortRecord.bookmarks.reversed().forEachIndexed {index, position ->
            oneRow(index + 2, position.score.toString(),
                    position.moveNumber.toString(),
                    position.startingDateTimeString,
                position.gameClock.playedSecondsString) {
                window.removeFromParent()
                presenter.onGoToBookmarkClick(position)
            }
        }
    }
}
