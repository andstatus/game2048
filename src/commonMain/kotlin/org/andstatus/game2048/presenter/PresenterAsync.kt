package org.andstatus.game2048.presenter

import korlibs.time.Stopwatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.andstatus.game2048.model.GameModeEnum
import org.andstatus.game2048.myLog

/** @author yvolk@yurivolkov.com */
fun CoroutineScope.showAiTip(presenter: Presenter) = launch {
    with(presenter) {
        myLog("AI launch 0")
        val gamePosition = model.gamePosition
        aiPlayer.nextPly(gamePosition).also {
            if (model.gamePosition === gamePosition) {
                view.gameStage.launch {
                    myLog("Showing AI tip 0: ${it.plyEnum}")
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
            Stopwatch().start().let { stopWatch ->
                presenter.delayWhilePresenting()
                val gamePosition = model.gamePosition
                aiPlayer.nextPly(gamePosition).also { aiResult ->
                    if (gameMode.speed in 1..3) {
                        myLog("Showing AI tip ${gameMode.speed}: ${aiResult.plyEnum}")
                        view.mainView.showStatusBar(aiResult)
                    }
                    delay(gameMode.delayMs.toLong() - stopWatch.elapsed.millisecondsLong)
                    if (!isPresenting.value && model.gamePosition === gamePosition && gameMode.modeEnum == GameModeEnum.AI_PLAY) {
                        view.gameStage.launch {
                            userMove(aiResult.plyEnum)
                        }.join()
                    }
                }
            }
        }
        if (gameMode.modeEnum == GameModeEnum.AI_PLAY) {
            view.gameStage.launch {
                onAiStopClicked()
            }
        }
    }
}