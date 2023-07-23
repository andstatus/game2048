package org.andstatus.game2048.view

import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.ui.uiScrollable
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.position
import korlibs.korge.view.roundRect
import korlibs.korge.view.text
import korlibs.image.text.TextAlignment
import korlibs.math.geom.RectCorners
import korlibs.math.geom.Size
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.GameRecord

@OptIn(KorgeExperimental::class)
fun ViewData.showBookmarks(game: GameRecord) = myWindow("goto_bookmark") {
    val listTop = winTop + cellMargin + buttonSize + buttonMargin
    val nItems = game.shortRecord.bookmarks.size + 1
    val itemHeight = buttonSize
    val textWidth = winWidth * 2
    val textSize = defaultTextSize

    fun Container.rowText(value: String, xPosition: Float) = text(value, textSize,
        gameColors.buttonText, font, TextAlignment.MIDDLE_LEFT
    ) {
        position(xPosition, itemHeight / 2)
    }

    fun Container.oneRow(index: Int, score: String, moveNumber: String, lastChanged: String, duration: String,
                         action: () -> Unit = {}) {
        container {
            roundRect(Size(textWidth, itemHeight), RectCorners(buttonRadius), fill = gameColors.buttonBackground)
            var xPos = cellMargin
            rowText(score, xPos)
            xPos += textSize * 3.1f
            rowText(moveNumber, xPos)
            xPos += textSize * 2.7f
            rowText(lastChanged, xPos)
            xPos += textSize * 6.4f
            rowText(duration, xPos)

            position(0.0f, index * (itemHeight + cellMargin))
            customOnClick { action() }
        }
    }

    fun Container.onePositionRow(index: Int, position: GamePosition) {
        oneRow(
            index,
            position.score.toString(),
            position.moveNumber.toString(),
            position.startingDateTimeString,
            position.gameClock.playedSecondsString
        ) {
            window.removeFromParent()
            presenter.onGoToBookmarkClick(position)
        }
    }

    uiScrollable(config = {
        position(winLeft + cellMargin, listTop)
        scaledWidth = winWidth - cellMargin * 2
        scaledHeight = winTop + winHeight - listTop - cellMargin
        backgroundColor = gameColors.stageBackground
    }) {
        var index = 0
        oneRow(
            index++,
            stringResources.text("score"),
            stringResources.text("move"),
            stringResources.text("last_changed"),
            stringResources.text("duration")
        )
        if (game.shortRecord.bookmarks.none { it.plyNumber == game.shortRecord.finalPosition.plyNumber }) {
            onePositionRow(index++, game.shortRecord.finalPosition)
        }
        game.shortRecord.bookmarks
            .sortedByDescending { it.plyNumber }
            .forEach { position -> onePositionRow(index++, position) }
    }
}
