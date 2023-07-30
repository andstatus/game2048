package org.andstatus.game2048.view

import korlibs.io.concurrent.atomic.incrementAndGet
import korlibs.io.concurrent.atomic.korAtomic
import korlibs.korge.view.Stage
import org.andstatus.game2048.defaultPortraitGameWindowSize
import org.andstatus.game2048.defaultPortraitRatio
import org.andstatus.game2048.defaultPortraitTextSize
import org.andstatus.game2048.gameWindowSize
import org.andstatus.game2048.myLog

/** @author yvolk@yurivolkov.com */
interface ViewDataBase {
    val gameStage: Stage
    val id: Int
    val duplicateKeyPressFilter: DuplicateKeyPressFilter
    val isPortrait: Boolean
    val gameViewLeft: Int
    val gameViewTop: Int
    val gameViewWidth: Int
    val gameViewHeight: Int
    val gameScale: Float
    val defaultTextSize: Float
    val buttonMargin: Float
    val cellMargin: Float
    val buttonRadius: Float
    val buttonSize: Float
    val boardLeft: Float
    val buttonXs: List<Float>
    val buttonYs: List<Float>
    val boardTop: Float
    val statusBarLeft: Float
    val statusBarTop: Float
}

/** The object is initialized instantly */
class ViewDataQuick(override val gameStage: Stage) : ViewDataBase {
    override val id: Int = nextId()
    override val duplicateKeyPressFilter = DuplicateKeyPressFilter()

    override val isPortrait: Boolean = gameStage.views.virtualWidth < gameStage.views.virtualHeight
    override val gameViewLeft: Int
    override val gameViewTop: Int
    override val gameViewWidth: Int
    override val gameViewHeight: Int

    override val gameScale: Float
    override val defaultTextSize: Float
    override val buttonMargin: Float
    override val cellMargin: Float
    override val buttonRadius: Float
    override val buttonSize: Float
    override val boardLeft: Float
    override val buttonXs: List<Float>
    override val buttonYs: List<Float>
    override val boardTop: Float
    override val statusBarLeft: Float
    override val statusBarTop: Float

    init {
        if (isPortrait) {
            val windowRatio = gameStage.views.virtualWidth.toFloat() / gameStage.views.virtualHeight
            if (windowRatio >= defaultPortraitRatio) {
                gameViewHeight = gameStage.views.virtualHeight
                gameViewWidth = (gameViewHeight * defaultPortraitRatio).toInt()
                gameViewLeft = (gameStage.views.virtualWidth - gameViewWidth) / 2
                gameViewTop = 0
                gameScale = gameViewHeight.toFloat() / defaultPortraitGameWindowSize.height
            } else {
                gameViewWidth = gameStage.views.virtualWidth
                gameViewHeight = (gameViewWidth / defaultPortraitRatio).toInt()
                gameViewLeft = 0
                gameViewTop = (gameStage.views.virtualHeight - gameViewHeight) / 2
                gameScale = gameViewWidth.toFloat() / defaultPortraitGameWindowSize.width
            }
            defaultTextSize = if (gameViewHeight == defaultPortraitGameWindowSize.height) defaultPortraitTextSize
            else defaultPortraitTextSize * gameViewHeight / defaultPortraitGameWindowSize.height
        } else {
            val windowRatio = gameStage.views.virtualHeight.toFloat() / gameStage.views.virtualWidth
            if (windowRatio >= defaultPortraitRatio) {
                gameViewWidth = gameStage.views.virtualWidth
                gameViewHeight = (gameViewWidth * defaultPortraitRatio).toInt()
                gameViewLeft = 0
                gameViewTop = (gameStage.views.virtualHeight - gameViewHeight) / 2
                gameScale = gameViewHeight.toFloat() / defaultPortraitGameWindowSize.width
            } else {
                gameViewHeight = gameStage.views.virtualHeight
                gameViewWidth = (gameViewHeight / defaultPortraitRatio).toInt()
                gameViewLeft = (gameStage.views.virtualWidth - gameViewWidth) / 2
                gameViewTop = 0
                gameScale = gameViewWidth.toFloat() / defaultPortraitGameWindowSize.height
            }

            defaultTextSize = if (gameViewWidth == defaultPortraitGameWindowSize.height) defaultPortraitTextSize
            else defaultPortraitTextSize * gameViewWidth / defaultPortraitGameWindowSize.height
        }

        buttonMargin = 27 * gameScale

        cellMargin = 15 * gameScale
        buttonRadius = 8 * gameScale

        if (isPortrait) {
            buttonSize = (gameViewWidth - buttonMargin * 6) / 5
            boardLeft = gameViewLeft + buttonMargin
        } else {
            buttonSize = (gameViewWidth / 2 - buttonMargin * 6) / 5
            boardLeft = gameViewLeft + gameViewWidth / 2 + buttonMargin
        }
        statusBarLeft = gameViewLeft + buttonMargin

        buttonXs = (0..if (isPortrait) 4 else 9).fold(emptyList()) { acc, i ->
            acc + (statusBarLeft + i * (buttonSize + buttonMargin) +
                if (!isPortrait && (i > 4)) buttonMargin else 0.0f)
        }
        buttonYs = (0..if (isPortrait) 9 else 4).fold(emptyList()) { acc, i ->
            acc + (gameViewTop + buttonMargin + i * (buttonSize + buttonMargin))
        }

        if (isPortrait) {
            boardTop = buttonYs[4]
            statusBarTop = buttonYs[9]
        } else {
            boardTop = buttonYs[0]
            statusBarTop = buttonYs[4]
        }

        myLog(
            "Window:${gameStage.coroutineContext.gameWindowSize.width}x${gameStage.coroutineContext.gameWindowSize.height}" +
                " -> Virtual:${gameStage.views.virtualWidth}x${gameStage.views.virtualHeight}" +
                " -> Game:${gameViewWidth}x$gameViewHeight, top:$gameViewTop, left:$gameViewLeft"
        )
    }

    companion object {
        private val nextIdHolder = korAtomic(0)
        fun nextId(): Int = nextIdHolder.incrementAndGet()
    }
}
