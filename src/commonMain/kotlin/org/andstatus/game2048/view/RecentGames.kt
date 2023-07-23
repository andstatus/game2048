package org.andstatus.game2048.view

import korlibs.image.text.TextAlignment
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.ui.uiScrollable
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.position
import korlibs.korge.view.roundRect
import korlibs.korge.view.text
import korlibs.math.geom.RectCorners
import korlibs.math.geom.Size
import org.andstatus.game2048.model.ShortRecord

@OptIn(KorgeExperimental::class)
fun ViewData.showRecentGames(recentGames: List<ShortRecord>) = myWindow("recent_games") {
    val listTop = winTop + cellMargin + buttonSize + buttonMargin
    val nItems = recentGames.size
    val itemHeight = buttonSize
    val textWidth = winWidth * 2
    val textSize = defaultTextSize

    fun Container.rowText(value: String, xPosition: Float) = text(
        value, textSize,
        gameColors.buttonText, font, TextAlignment.MIDDLE_LEFT
    ) {
        position(xPosition, itemHeight / 2)
    }

    fun Container.oneRow(
        index: Int, score: String, lastChanged: String, duration: String, id: String,
        note: String, action: () -> Unit
    ) {
        container {
            roundRect(Size(textWidth, itemHeight), RectCorners(buttonRadius), fill = gameColors.buttonBackground)
            var xPos = cellMargin
            rowText(score, xPos)
            xPos += textSize * 3.1f
            rowText(lastChanged, xPos)
            xPos += textSize * 6.4f
            rowText(duration, xPos)
            xPos += textSize * 4.6f
            rowText(id, xPos)
            if (note.isNotBlank()) {
                xPos += textSize * 1.4f
                rowText(note, xPos)
            }

            position(0.0f, index * (itemHeight + cellMargin))
            customOnClick { action() }
        }
    }

    uiScrollable(config = {
        position(winLeft + cellMargin, listTop)
        scaledWidth = winWidth - cellMargin * 2
        scaledHeight = winTop + winHeight - listTop - cellMargin
        backgroundColor = gameColors.stageBackground
    }) {
        oneRow(
            0, stringResources.text("score"), stringResources.text("last_changed"),
            stringResources.text("duration"), stringResources.text("id"),
            stringResources.text("note")
        ) {}

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