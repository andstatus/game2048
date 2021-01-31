package org.andstatus.game2048.view

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Text
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.centerXOn
import com.soywiz.korge.view.position
import com.soywiz.korge.view.positionX
import com.soywiz.korge.view.positionY
import com.soywiz.korge.view.roundRect
import com.soywiz.korge.view.text
import com.soywiz.korim.text.TextAlignment
import kotlin.math.abs

fun ViewData.setupScoreBar(): ScoreBar {
    return ScoreBar(this)
}

class ScoreBar(val viewData: ViewData): Container() {
    val gameTime: Text
    val usersMoveNumber: Text
    var score: Text
    val bestScore: Text

    init {
        with(viewData) {
            val scoreButtonWidth = (viewData.boardWidth - 2 * buttonMargin) / 3
            val scoreButtonTop = buttonYs[2]
            val textYPadding = 28 * gameScale
            val scoreLabelSize = cellSize * 0.30
            val scoreTextSize = cellSize * 0.5

            var posX = boardLeft
            val bgScore = roundRect(scoreButtonWidth, buttonSize, buttonRadius, fill = gameColors.buttonBackground) {
                position(posX, scoreButtonTop)
            }
            text(stringResources.text("score_upper"), scoreLabelSize, gameColors.buttonLabelText, font,
                TextAlignment.MIDDLE_CENTER
            ) {
                positionX(bgScore.pos.x + scoreButtonWidth / 2)
                positionY(scoreButtonTop + textYPadding)
            }
            score = text("", scoreTextSize, gameColors.buttonText, font, TextAlignment.MIDDLE_CENTER) {
                centerXOn(bgScore)
                positionY(scoreButtonTop + scoreLabelSize + textYPadding)
            }

            posX += scoreButtonWidth + buttonMargin
            val bgBest = roundRect(scoreButtonWidth, buttonSize, buttonRadius, fill = gameColors.buttonBackground) {
                position(posX, scoreButtonTop)
            }
            text(stringResources.text("best"), scoreLabelSize, gameColors.buttonLabelText, font,
                TextAlignment.MIDDLE_CENTER
            ) {
                positionX(bgBest.pos.x + scoreButtonWidth / 2)
                positionY(scoreButtonTop + textYPadding)
            }
            bestScore = text("", scoreTextSize, gameColors.buttonText, font, TextAlignment.MIDDLE_CENTER) {
                centerXOn(bgBest)
                positionY(scoreButtonTop + scoreLabelSize + textYPadding)
            }

            posX += scoreButtonWidth + buttonMargin
            gameTime = text("00:00:00", scoreLabelSize, gameColors.labelText, font, TextAlignment.MIDDLE_CENTER) {
                positionX(posX + scoreButtonWidth / 2)
                positionY(scoreButtonTop + textYPadding)
            }
            usersMoveNumber = text("", scoreTextSize, gameColors.labelText, font, TextAlignment.MIDDLE_CENTER) {
                centerXOn(gameTime)
                positionY(scoreButtonTop + scoreLabelSize + textYPadding)
            }

        }
    }

    fun show(parent: Container, playSpeed: Int) {
        addTo(parent)
        usersMoveNumber.text = viewData.presenter.model.usersMoveNumber.toString() +
                (if (playSpeed < 0) " «" else "") +
                (if (playSpeed > 0) " »" else "") +
                (if (playSpeed != 0) abs(playSpeed) else "")
        bestScore.text = viewData.presenter.bestScore.toString()
        score.text = viewData.presenter.score.toString()
    }
}