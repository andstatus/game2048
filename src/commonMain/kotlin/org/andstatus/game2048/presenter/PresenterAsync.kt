package org.andstatus.game2048.presenter

import korlibs.io.concurrent.atomic.KorAtomicInt
import korlibs.io.concurrent.atomic.incrementAndGet
import korlibs.time.Stopwatch
import korlibs.time.millisecondsLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.andstatus.game2048.ai.AiResult
import org.andstatus.game2048.model.GameModeEnum
import org.andstatus.game2048.myLog

private val aiLaunchCounter = KorAtomicInt(0)

/** @author yvolk@yurivolkov.com */
fun CoroutineScope.showAiTip(presenter: Presenter) = launch {
    with(presenter) {
        val aiCount = aiLaunchCounter.incrementAndGet()
        val startCount = clickCounter.value
        try {
            myLog("AI-$aiCount tip launched")
            val position = model.gamePosition
            do {
                val result = aiPlayer.calcNextPly(position)
                    .onSuccess { showAiResult(aiCount, it) }
                if (result.isFailure) {
                    // TODO: Avoided onFailure for now to not use non-local break
                    //   https://github.com/Kotlin/KEEP/blob/master/proposals/break-continue-in-inline-lambdas.md
                    if (position != model.gamePosition) {
                        myLog("AI-$aiCount tip skipped: position changed")
                        break
                    }
                    if (startCount != clickCounter.value) {
                        myLog("AI-$aiCount tip skipped: click counter changed")
                        break
                    }
                    aiPlayer.stop()
                    delay(10)
                }
            } while (result.isFailure)
        } finally {
            myLog("AI-$aiCount completed")
        }
    }
}

fun CoroutineScope.aiPlayLoop(presenter: Presenter, startCount: Int) = launch {
    with(presenter) {
        val aiCount = aiLaunchCounter.incrementAndGet()
        try {
            myLog("AI-$aiCount play loop launched")
            while (startCount == clickCounter.value && !model.noMoreMoves()
                && gameMode.modeEnum == GameModeEnum.AI_PLAY
            ) {
                Stopwatch().start().let { stopWatch ->
                    presenter.delayWhilePresenting()
                    do {
                        val result = aiPlayer.calcNextPly(model.gamePosition)
                            .onSuccess { aiResult ->
                                if (gameMode.speed in 1..3 || aiResult.takenMillis > 500) {
                                    showAiResult(aiCount, aiResult)
                                } else {
                                    withContext(view.korgeCoroutineContext) {
                                        view.mainView.hideStatusBar()
                                    }
                                }
                                delay(gameMode.delayMs.toLong() - stopWatch.elapsed.millisecondsLong)
                                if (!isPresenting.value && model.gamePosition === aiResult.initialPosition &&
                                    gameMode.modeEnum == GameModeEnum.AI_PLAY
                                ) {
                                    userMove(aiResult.plyEnum)
                                }
                            }
                        if (result.isFailure) {
                            if (startCount != clickCounter.value) {
                                break
                            }
                            aiPlayer.stop()
                            delay(10)
                        }
                    } while (result.isFailure)
                }
            }
            if (gameMode.modeEnum == GameModeEnum.AI_PLAY) {
                withContext(view.korgeCoroutineContext) {
                    onAiStopClick()
                }
            }
        } finally {
            myLog("AI-$aiCount play loop completed")
        }
    }
}

suspend fun Presenter.showAiResult(aiCount: Int, aiResult: AiResult) {
    if (model.gamePosition == aiResult.initialPosition) {
        withContext(view.korgeCoroutineContext) {
            myLog("AI-$aiCount showing result ${aiResult.initialPosition.moveNumber}, ${aiResult.plyEnum}")
            view.mainView.showStatusBar(aiResult)
        }
    } else {
        myLog("AI-$aiCount show result skipped: position changed")
    }
}
