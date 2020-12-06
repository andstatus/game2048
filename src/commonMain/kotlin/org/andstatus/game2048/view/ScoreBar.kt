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

fun GameView.setupScoreBar(): ScoreBar {
    return ScoreBar(this).addTo(gameStage)
}

class ScoreBar(val gameView: GameView): Container() {
    val gameTime: Text
    val usersMoveNumber: Text
    var score: Text
    val bestScore: Text

    init {
        with(gameView) {
            val scoreButtonWidth = (gameView.boardWidth - 2 * buttonPadding) / 3
            val scoreButtonTop = buttonYs[2]
            val textYPadding = 28 * gameScale
            val scoreLabelSize = cellSize * 0.30
            val scoreTextSize = cellSize * 0.5

            gameTime = text("00:00:00", scoreLabelSize, gameColors.labelText, font, TextAlignment.MIDDLE_CENTER) {
                positionX(boardLeft + scoreButtonWidth / 2)
                positionY(scoreButtonTop + textYPadding)
            }
            usersMoveNumber = text("", scoreTextSize, gameColors.labelText, font, TextAlignment.MIDDLE_CENTER) {
                centerXOn(gameTime)
                positionY(scoreButtonTop + scoreLabelSize + textYPadding)
            }

            val bgScore = roundRect(scoreButtonWidth, buttonSize, buttonRadius, fill = gameColors.buttonBackground) {
                position(boardLeft + (scoreButtonWidth + buttonPadding), scoreButtonTop)
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

            val bgBest = roundRect(scoreButtonWidth, buttonSize, buttonRadius, fill = gameColors.buttonBackground) {
                position(boardLeft + (scoreButtonWidth + buttonPadding) * 2, scoreButtonTop)
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
        }
    }

    fun show(playSpeed: Int) {
        usersMoveNumber.text = gameView.presenter.model.usersMoveNumber.toString() +
                (if (playSpeed < 0) " «" else "") +
                (if (playSpeed > 0) " »" else "") +
                (if (playSpeed != 0) abs(playSpeed) else "")
        bestScore.text = gameView.presenter.bestScore.toString()
        score.text = gameView.presenter.score.toString()
    }
}