package org.andstatus.game2048.presenter

import com.soywiz.klock.Stopwatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.andstatus.game2048.model.GameModeEnum
import org.andstatus.game2048.myLog

/** @author yvolk@yurivolkov.com */
fun CoroutineScope.showAiTip(presenter: Presenter) = launch {
    with(presenter) {
        myLog("AI launch")
        val gamePosition = model.gamePosition
        aiPlayer.nextPly(gamePosition).also {
            myLog("AI tip: ${it.move}")
            if (model.gamePosition === gamePosition) {
                view.gameStage.launch {
                    myLog("Showing AI tip: ${it.move}")
                    view.mainView.showStatusBar(it)
                }
            }
        }
    }
}

fun CoroutineScope.aiPlayLoop(presenter: Presenter, startCount: Int) = launch {
    with(presenter) {
        while (startCount == clickCounter.value && !model.noMoreMoves()
                && gameMode.modeEnum == GameModeEnum.AI_PLAY) {
            while (moveIsInProgress.value) delay(20)
            val gamePosition = model.gamePosition
            val nextMove = Stopwatch().start().let { stopWatch ->
                aiPlayer.nextPly(gamePosition).also {
                    delay(gameMode.delayMs.toLong() - stopWatch.elapsed.millisecondsLong)
                }
            }
            if (!moveIsInProgress.value && model.gamePosition === gamePosition && gameMode.modeEnum == GameModeEnum.AI_PLAY) {
                view.gameStage.launch {
                    userMove(nextMove.move)
                }.join()
            }
        }
        if (gameMode.modeEnum == GameModeEnum.AI_PLAY) {
            view.gameStage.launch {
                onAiStopClicked()
            }
        }
    }
}