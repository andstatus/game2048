package org.andstatus.game2048.view

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Text
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.positionX
import com.soywiz.korge.view.positionY
import com.soywiz.korge.view.text
import com.soywiz.korim.text.TextAlignment
import org.andstatus.game2048.ai.AiResult

fun ViewData.setupStatusBar(): StatusBar {
    return StatusBar(this)
}

class StatusBar(val viewData: ViewData): Container() {
    val moveSuggested: Text
    var scoreProjected: Text
    val timeTaken: Text

    init {
        with(viewData) {
            val scoreButtonWidth = (viewData.boardWidth - 2 * buttonMargin) / 3
            val barTop = buttonYs[8]
            val textYPadding = 28 * gameScale
            val scoreLabelSize = cellSize * 0.30
            val scoreTextSize = cellSize * 0.5

            var posX = boardLeft
            moveSuggested = text("", scoreTextSize, gameColors.labelText, font, TextAlignment.MIDDLE_CENTER) {
                positionX(posX + scoreButtonWidth / 2)
                positionY(barTop + scoreLabelSize)
            }

            posX += scoreButtonWidth + buttonMargin
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

            posX += scoreButtonWidth + buttonMargin
            text(stringResources.text("milliseconds"), scoreLabelSize, gameColors.labelText, font,
                TextAlignment.MIDDLE_LEFT
            ) {
                positionX(posX + scoreButtonWidth / 2 + cellMargin)
                positionY(barTop + textYPadding)
            }
            timeTaken = text("", scoreLabelSize, gameColors.labelText, font, TextAlignment.MIDDLE_RIGHT) {
                positionX(posX + scoreButtonWidth / 2)
                positionY(barTop + textYPadding)
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

            moveSuggested.text = move.symbol
            scoreProjected.text =  if (maxScore > referenceScore) {
                "$referenceScore - $maxScore"
            } else if (referenceScore == 0) {
                "-"
            } else {
                referenceScore.toString()
            }
            timeTaken.text = if (takenMillis == 0) "-" else takenMillis.toString()
        }
        addTo(parent)
    }
}