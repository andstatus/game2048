package org.andstatus.game2048.view

import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.addTo
import korlibs.korge.view.positionX
import korlibs.korge.view.positionY
import korlibs.korge.view.text
import korlibs.image.text.TextAlignment
import org.andstatus.game2048.ai.AiResult

fun ViewData.setupStatusBar(): StatusBar {
    return StatusBar(this)
}

class StatusBar(val viewData: ViewData): Container() {
    val moveSuggested: Text
    var scoreProjected: Text
    val timeTaken: Text
    var positionProjected: Text

    init {
        with(viewData) {
            val scoreButtonWidth = (viewData.boardWidth - 2 * buttonMargin) / 3
            val barTop = statusBarTop
            val textYPadding = 28 * gameScale
            val scoreLabelSize = buttonSize * 0.53f
            val scoreTextSize = buttonSize * 0.66f

            var posX = statusBarLeft
            moveSuggested = text("", scoreTextSize, gameColors.labelText, font, TextAlignment.MIDDLE_CENTER) {
                positionX(posX + scoreButtonWidth / 4)
                positionY(barTop + scoreLabelSize)
            }

            posX += scoreButtonWidth / 2 + buttonMargin
            text(stringResources.text("score_upper"), scoreLabelSize, gameColors.labelText, font,
                TextAlignment.MIDDLE_CENTER
            ) {
                positionX(posX + scoreButtonWidth / 2)
                positionY(barTop + textYPadding)
            }
            scoreProjected = text("", scoreLabelSize, gameColors.labelText, font, TextAlignment.MIDDLE_CENTER) {
                positionX(posX + scoreButtonWidth / 2)
                positionY(barTop + scoreLabelSize + textYPadding)
            }

            posX += scoreButtonWidth * 1.2f + buttonMargin
            positionProjected = text("", scoreLabelSize, gameColors.labelText, font, TextAlignment.MIDDLE_CENTER) {
                positionX(posX + scoreButtonWidth / 2)
                positionY(barTop + textYPadding)
            }
            text(stringResources.text("milliseconds"), scoreLabelSize, gameColors.labelText, font,
                TextAlignment.MIDDLE_LEFT
            ) {
                positionX(posX + scoreButtonWidth * 3 / 4 + cellMargin)
                positionY(barTop + scoreLabelSize + textYPadding)
            }
            timeTaken = text("", scoreLabelSize, gameColors.labelText, font, TextAlignment.MIDDLE_RIGHT) {
                positionX(posX + scoreButtonWidth * 3 / 4)
                positionY(barTop + scoreLabelSize + textYPadding)
            }

        }
    }

    fun show(parent: Container, aiResult: AiResult) {
        if (viewData.closed) return

        with(aiResult) {
            if (isEmpty()) {
                removeFromParent()
                return
            }

            moveSuggested.text = plyEnum.symbol

            scoreProjected.text = "+$moreScore" +
                    if (moreScoreMax > moreScore) "..$moreScoreMax" else ""

            timeTaken.text = takenMillis.toString()
            positionProjected.text = "+$moreMoves" + (if (moreMovesMax > moreMoves) "..$moreMovesMax " else " ") + (note ?: "")
        }
        addTo(parent)
    }
}
