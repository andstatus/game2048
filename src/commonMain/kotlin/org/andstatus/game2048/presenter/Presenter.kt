package org.andstatus.game2048.presenter

import com.soywiz.klock.milliseconds
import com.soywiz.korge.animate.Animator
import com.soywiz.korge.animate.animateSequence
import com.soywiz.korge.input.SwipeDirection
import com.soywiz.korge.tween.get
import com.soywiz.korge.view.Text
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.concurrent.atomic.incrementAndGet
import com.soywiz.korio.concurrent.atomic.korAtomic
import com.soywiz.korio.lang.substr
import com.soywiz.korma.interpolation.Easing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.andstatus.game2048.ai.AiAlgorithm
import org.andstatus.game2048.ai.AiPlayer
import org.andstatus.game2048.closeGameApp
import org.andstatus.game2048.gameStopWatch
import org.andstatus.game2048.loadJsonGameRecord
import org.andstatus.game2048.model.GameModeEnum
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.GameRecord
import org.andstatus.game2048.model.History
import org.andstatus.game2048.model.Model
import org.andstatus.game2048.model.PieceMoveDelay
import org.andstatus.game2048.model.PieceMoveLoad
import org.andstatus.game2048.model.PieceMoveMerge
import org.andstatus.game2048.model.PieceMoveOne
import org.andstatus.game2048.model.PieceMovePlace
import org.andstatus.game2048.model.PlacedPiece
import org.andstatus.game2048.model.Ply
import org.andstatus.game2048.model.PlyEnum
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.myLog
import org.andstatus.game2048.myMeasured
import org.andstatus.game2048.shareText
import org.andstatus.game2048.view.AppBarButtonsEnum
import org.andstatus.game2048.view.ColorThemeEnum
import org.andstatus.game2048.view.ViewData
import org.andstatus.game2048.view.showBookmarks
import org.andstatus.game2048.view.showGameMenu
import org.andstatus.game2048.view.showHelp
import org.andstatus.game2048.view.showRecentGames

/** @author yvolk@yurivolkov.com */
class Presenter(val view: ViewData, history: History) {
    val model = Model(history)
    private val multithreadedScope: CoroutineScope get() = model.history.settings.multithreadedScope
    val aiPlayer = AiPlayer(history.settings)
    val moveIsInProgress = korAtomic(false)
    val score get() = model.score
    val bestScore get() = model.bestScore
    var boardViews = BoardViews(view)
    val gameMode get() = model.gameMode
    var clickCounter = korAtomic(0)

    private fun presentGameClock(coroutineScope: CoroutineScope, model: Model, textSupplier: () -> Text) {
        coroutineScope.launch {
            while (true) {
                textSupplier().text = model.gameClock.playedSecondsString
                delay(1000)
            }
        }
    }

    fun onAppEntry() = myMeasured("onAppEntry") {
        model.onAppEntry().present()
        presentGameClock(view.gameStage, model) { view.mainView.scoreBar.gameTime }
        if (model.history.recentGames.isEmpty() && model.history.currentGame.id == 0) {
            view.showHelp()
        } else {
            showMainView()
            loadPlies()
        }
    }

    fun onNoMagicClicked() {
        logClick("NoMagic")
        gameMode.aiEnabled = true
        updateMainView()
    }

    fun onMagicClicked() {
        logClick("Magic")
        gameMode.aiEnabled = false
        if (gameMode.modeEnum == GameModeEnum.AI_PLAY) {
            gameMode.modeEnum = GameModeEnum.PLAY
            pauseGame()
        }
        updateMainView()
    }

    fun onAiStartClicked() {
        logClick("AiStart")
        gameMode.aiEnabled = true
        startAiPlay()
    }

    fun onAiStopClicked() {
        logClick("AiStop")
        gameMode.modeEnum = GameModeEnum.PLAY
        pauseGame()
        updateMainView()
    }

    fun onAiForwardClicked() {
        gameMode.incrementSpeed()
    }

    fun onAiAlgorithmSelect(selected: AiAlgorithm) {
        model.settings.aiAlgorithm = selected
        view.settings.save()
        showMainView()
    }

    fun canUndo(): Boolean = model.canUndo()

    fun canRedo(): Boolean = model.canRedo()

    fun onUndoClick() = afterStop {
        logClick("Undo")
        pauseGame()
        undo()
    }

    fun undo() {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        view.mainView.hideStatusBar()
        boardViews.hideGameOver()
        (model.undo() + Ply.delay() + model.undo()).presentReversed()
    }

    fun onRedoClick() = afterStop {
        logClick("Redo")
        redo()
    }

    fun redo() {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        (model.redo() + listOf(Ply.delay()) + model.redo()).present()
    }

    fun onRestartClick() = afterStop {
        logClick("Restart")
        showMainView()
        model.pauseGame()
        model.restart().present()
    }

    fun onSwipe(swipeDirection: SwipeDirection) {
        when (gameMode.modeEnum) {
            GameModeEnum.BACKWARDS, GameModeEnum.FORWARD, GameModeEnum.STOP -> {
                when (swipeDirection) {
                    SwipeDirection.LEFT -> startAutoReplay(GameModeEnum.BACKWARDS)
                    SwipeDirection.RIGHT -> startAutoReplay(GameModeEnum.FORWARD)
                    SwipeDirection.BOTTOM -> onStopClick()
                    else -> {
                    }
                }
            }
            GameModeEnum.PLAY -> {
                userMove(
                        when (swipeDirection) {
                            SwipeDirection.LEFT -> PlyEnum.LEFT
                            SwipeDirection.RIGHT -> PlyEnum.RIGHT
                            SwipeDirection.TOP -> PlyEnum.UP
                            SwipeDirection.BOTTOM -> PlyEnum.DOWN
                        }
                )
            }
            GameModeEnum.AI_PLAY -> {
                when (swipeDirection) {
                    SwipeDirection.LEFT -> {
                        gameMode.decrementSpeed()
                        updateMainView()
                    }
                    SwipeDirection.RIGHT -> {
                        gameMode.incrementSpeed()
                        updateMainView()
                    }
                    else -> {
                        onAiStopClicked()
                    }
                }
            }
        }

    }

    fun onBookmarkClick() = afterStop {
        logClick("Bookmark")
        model.createBookmark()
        updateMainView()
    }

    fun onBookmarkedClick() = afterStop {
        logClick("Bookmarked")
        model.deleteBookmark()
        updateMainView()
    }

    fun onPauseClick() = afterStop {
        logClick("Pause")
        pauseGame()
        updateMainView()
    }

    fun onPauseEvent() {
        logClick("onPauseEvent")
        pauseGame()
        updateMainView()
    }

    fun onCloseGameWindowClick() {
        logClick("onCloseGameWindow")
        pauseGame()
        view.gameStage.gameWindow.close()
        view.gameStage.closeGameApp()
    }

    fun onWatchClick() = afterStop {
        logClick("Watch")
        gameMode.modeEnum = GameModeEnum.PLAY
        updateMainView()
    }

    fun onPlayClick() = afterStop {
        logClick("Play")
        gameMode.modeEnum = GameModeEnum.STOP
        pauseGame()
        updateMainView()
    }

    fun onBackwardsClick() {
        logClick("Backwards")
        startAutoReplay(GameModeEnum.BACKWARDS)
    }

    fun onStopClick() = afterStop {
        logClick("Stop")
        gameMode.modeEnum = GameModeEnum.STOP
        updateMainView()
    }

    fun onForwardClick() {
        logClick("Forward")
        startAutoReplay(GameModeEnum.FORWARD)
    }

    fun onToStartClick() = afterStop {
        logClick("ToStart")
        boardViews.hideGameOver()
        (model.undoToStart() + listOf(Ply.delay()) + model.redo()).present()
    }

    fun onToCurrentClick() = afterStop {
        logClick("ToCurrent")
        model.redoToCurrent().present()
    }

    fun onGameMenuClick() = afterStop {
        logClick("GameMenu")
        hideMainView()
        view.showGameMenu(model.gameMode.aiEnabled)
        pauseGame()
    }

    fun hideMainView() {
        view.mainView.removeFromParent()
    }

    fun onCloseMyWindowClick() {
        logClick("CloseMyWindow")
        showMainView()
    }

    fun onDeleteGameClick() = afterStop {
        logClick("DeleteGame")
        showMainView()
        model.history.deleteCurrent()
        model.restart().present()
    }

    fun onBookmarksClick() {
        logClick("Bookmarks")
        view.showBookmarks(model.history.currentGame)
    }

    fun onRecentClick() = afterStop {
        logClick("Recent")
        model.pauseGame()
        view.showRecentGames(model.history.recentGames)
    }

    fun onGoToBookmarkClick(position: GamePosition) = afterStop {
        logClick("GoTo${position.plyNumber}")
        showMainView()
        if (moveIsInProgress.compareAndSet(expect = false, update = true)) {
            model.gotoBookmark(position).present()
        }
    }

    fun onHistoryItemClick(id: Int) = afterStop {
        logClick("History$id")
        showMainView()
        if (moveIsInProgress.compareAndSet(expect = false, update = true)) {
            model.openGame(id).present()
            loadPlies()
        }
    }

    private fun loadPlies() {
        with(model.history.currentGame.plies) {
            if (notCompleted) afterStop {
                multithreadedScope.launch {
                    load()
                    showMainView()
                }
            }
        }
    }

    fun onShareClick() = afterStop {
        logClick("Share")
        view.gameStage.shareText(
                view.stringResources.text("share"), model.history.currentGame.shortRecord.jsonFileName,
                model.history.currentGame.toJsonString()
        )
    }

    fun onLoadClick() = afterStop {
        logClick("Load")
        view.gameStage.loadJsonGameRecord { json ->
            view.gameStage.launch {
                myLog("Opened game string: ${json.substr(0, 140)}")
                GameRecord.fromJson(model.settings, json, newId = 0)?.let {
                    myLog("Opened game: $it")
                    model.history.currentGame = it
                    model.saveCurrent()
                    // I noticed some kind of KorGe window reset after return from the other activity:
                    // GLSurfaceView.onSurfaceChanged
                    // If really needed, we could re-create activity...
                    view.reInitialize()
                }
            }
        }
    }

    fun onHelpClick() = afterStop {
        logClick("Help")
        view.showHelp()
    }

    suspend fun onSelectColorTheme(colorThemeEnum: ColorThemeEnum) {
        logClick("onSelectColorTheme $colorThemeEnum")
        if (colorThemeEnum == view.settings.colorThemeEnum) return

        view.settings.colorThemeEnum = colorThemeEnum
        view.settings.save()
        pauseGame()
        view.reInitialize()
    }

    fun pauseGame() {
        clickCounter.incrementAndGet()
        model.pauseGame()
    }

    private fun startAutoReplay(newMode: GameModeEnum) {
        if (gameMode.modeEnum == newMode) {
            if (newMode == GameModeEnum.BACKWARDS) gameMode.decrementSpeed() else gameMode.incrementSpeed()
            updateMainView()
        } else if (if (newMode == GameModeEnum.BACKWARDS) canUndo() else canRedo()) {
            afterStop {
                val startCount = clickCounter.incrementAndGet()
                gameMode.modeEnum = newMode
                updateMainView()
                multithreadedScope.launch {
                    while (startCount == clickCounter.value &&
                        if (gameMode.modeEnum == GameModeEnum.BACKWARDS) canUndo() else canRedo()
                    ) {
                        delayWhileMoveInProgress()
                        if (gameMode.speed != 0) {
                            if (gameMode.modeEnum == GameModeEnum.BACKWARDS) undo() else redo()
                        }
                        delay(gameMode.delayMs.toLong())
                    }
                    gameMode.stop()
                    updateMainView()
                }
            }
        }
    }

    private fun startAiPlay() {
        afterStop {
            val startCount = clickCounter.incrementAndGet()
            gameMode.modeEnum = GameModeEnum.AI_PLAY
            updateMainView()
            multithreadedScope.aiPlayLoop(this, startCount)
        }
    }

    private fun logClick(buttonName: String) {
        myLog("$buttonName-${view.id} clicked:${clickCounter.value}, mode:${gameMode.modeEnum}")
        gameStopWatch.start()
    }

    private fun afterStop(action: suspend () -> Unit) {
        val startCount = clickCounter.incrementAndGet()
        multithreadedScope.launch {
            while (gameMode.autoPlaying && startCount == clickCounter.value) {
                delay(100)
            }
            action()
        }
    }

    fun composerMove(position: GamePosition) = model.composerMove(position, false).present()

    fun computerMove() = model.randomComputerMove().present()

    fun computerMove(placedPiece: PlacedPiece) = model.computerMove(placedPiece).present()

    fun userMove(plyEnum: PlyEnum) {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        model.userMove(plyEnum).let {
            if (it.isEmpty()) it else it + model.randomComputerMove()
        }.present()
    }

    private fun List<Ply>.present(index: Int = 0) {
        if (gameMode.modeEnum != GameModeEnum.AI_PLAY || gameMode.speed != 1) {
            view.mainView.hideStatusBar()
        }
        if (isEmpty()) {
            onPresentEnd()
            if (model.noMoreMoves()) {
                boardViews = boardViews
                        .hideGameOver()
                        .copy()
                        .apply {
                            view.mainView.boardView.showGameOver()
                            pauseGame()
                        }
            }
        } else if (index < size) {
            present(this[index]) {
                present(index + 1)
            }
        } else {
            onPresentEnd()
        }
    }

    private fun onPresentEnd() {
        updateMainView()
        moveIsInProgress.value = false
    }

    fun updateMainView() {
        if (view.mainView.parent == null) return
        showMainView()
    }

    fun showMainView() {
        if (view.closed) return

        view.mainView.show(buttonsToShow(), gameMode.speed)
        if (gameMode.aiEnabled && gameMode.speed == 0) {
            multithreadedScope.showAiTip(this)
            myLog("After AI Launch")
        }
    }

    private fun buttonsToShow(): List<AppBarButtonsEnum> {
        val list = ArrayList<AppBarButtonsEnum>()
        when (gameMode.modeEnum) {
            GameModeEnum.PLAY -> {
                list.add(AppBarButtonsEnum.PLAY)
                list.add(
                    if (gameMode.aiEnabled) AppBarButtonsEnum.AI_ON else AppBarButtonsEnum.AI_OFF
                )
                if (model.gameClock.started) {
                    list.add(AppBarButtonsEnum.PAUSE)
                } else {
                    list.add(AppBarButtonsEnum.APP_LOGO)
                    if (!canRedo()) {
                        list.add(AppBarButtonsEnum.RESTART)
                    }
                }
                list.add(
                    if (model.isBookmarked) AppBarButtonsEnum.BOOKMARKED else AppBarButtonsEnum.BOOKMARK
                )
                if (gameMode.aiEnabled) {
                    list.add(AppBarButtonsEnum.AI_START)
                }

                if (canUndo()) {
                    list.add(AppBarButtonsEnum.UNDO)
                }
                list.add(if (canRedo()) AppBarButtonsEnum.REDO else AppBarButtonsEnum.REDO_PLACEHOLDER)

                list.add(AppBarButtonsEnum.GAME_MENU)
            }
            GameModeEnum.AI_PLAY -> {
                list.add(AppBarButtonsEnum.PLAY)
                list.add(AppBarButtonsEnum.AI_ON)
                list.add(AppBarButtonsEnum.BOOKMARK_PLACEHOLDER)
                list.add(AppBarButtonsEnum.AI_STOP)
                list.add(AppBarButtonsEnum.AI_FORWARD)
            }
            else -> {
                list.add(AppBarButtonsEnum.WATCH)
                list.add(
                    if (gameMode.aiEnabled) AppBarButtonsEnum.AI_ON else AppBarButtonsEnum.AI_OFF
                )
                if (model.moveNumber > 1 && model.isBookmarked) {
                    list.add(AppBarButtonsEnum.BOOKMARKED)
                } else {
                    list.add(AppBarButtonsEnum.BOOKMARK_PLACEHOLDER)
                }
                if (canUndo()) {
                    if (gameMode.speed == -gameMode.maxSpeed) {
                        list.add(AppBarButtonsEnum.TO_START)
                    } else {
                        list.add(AppBarButtonsEnum.BACKWARDS)
                    }
                }
                if (gameMode.modeEnum == GameModeEnum.STOP) {
                    list.add(AppBarButtonsEnum.APP_LOGO)
                    list.add(AppBarButtonsEnum.STOP_PLACEHOLDER)
                } else {
                    list.add(AppBarButtonsEnum.STOP)
                }
                if (canRedo()) {
                    if (gameMode.speed == gameMode.maxSpeed) {
                        list.add(AppBarButtonsEnum.TO_CURRENT)
                    } else {
                        list.add(AppBarButtonsEnum.FORWARD)
                    }
                } else {
                    list.add(AppBarButtonsEnum.FORWARD_PLACEHOLDER)
                }
                if (gameMode.modeEnum == GameModeEnum.STOP) {
                    list.add(AppBarButtonsEnum.GAME_MENU)
                }
            }
        }
        return list
    }

    private fun present(ply: Ply, onEnd: () -> Unit) = view.gameStage.launchImmediately {
        view.gameStage.animateSequence {
            parallel {
                ply.pieceMoves.forEach { move ->
                    when (move) {
                        is PieceMovePlace -> boardViews.addBlock(move.first)
                        is PieceMoveLoad -> boardViews.load(move.position)
                        is PieceMoveOne -> boardViews[move.first]?.move(this, move.destination)
                        is PieceMoveMerge -> {
                            val firstBlock = boardViews[move.first]
                            val secondBlock = boardViews[move.second]
                            sequence {
                                parallel {
                                    firstBlock?.move(this, move.merged.square)
                                    secondBlock?.move(this, move.merged.square)
                                }
                                block {
                                    firstBlock?.remove()
                                    secondBlock?.remove()
                                    boardViews.addBlock(move.merged)
                                }
                                sequenceLazy {
                                    if (view.animateViews) boardViews[move.merged]
                                        ?.let { animateResultingBlock(this, it) }
                                }
                            }
                        }
                        is PieceMoveDelay -> with(view) {
                            if (animateViews) boardViews.blocks.lastOrNull()?.also {
                                moveTo(it.block, it.square, gameMode.delayMs.milliseconds, Easing.LINEAR)
                            }
                        }
                    }
                }
            }
            block {
                onEnd()
            }
        }
    }

    private fun List<Ply>.presentReversed(index: Int = 0) {
        if (index < size) {
            presentReversed(this[index]) {
                presentReversed(index + 1)
            }
        } else {
            onPresentEnd()
        }
    }

    private fun presentReversed(ply: Ply, onEnd: () -> Unit) = view.gameStage.launchImmediately {
        view.gameStage.animateSequence {
            parallel {
                ply.pieceMoves.asReversed().forEach { move ->
                    when (move) {
                        is PieceMovePlace -> boardViews[move.first]
                            ?.remove()
                            ?: myLog("No Block at destination during undo: $move")
                        is PieceMoveLoad -> boardViews.load(move.position)
                        is PieceMoveOne -> boardViews[PlacedPiece(move.first.piece, move.destination)]
                            ?.move(this, move.first.square)
                            ?: myLog("No Block at destination during undo: $move")
                        is PieceMoveMerge -> {
                            val destination = move.merged.square
                            val effectiveBlock = boardViews[move.merged]
                            sequence {
                                block {
                                    effectiveBlock?.remove()
                                        ?: myLog("No Block at destination during undo: $move")
                                }
                                parallel {
                                    val secondBlock = boardViews.addBlock(PlacedPiece(move.second.piece, destination))
                                    val firstBlock = boardViews.addBlock(PlacedPiece(move.first.piece, destination))
                                    secondBlock.move(this, move.second.square)
                                    firstBlock.move(this, move.first.square)
                                }
                            }
                        }
                        is PieceMoveDelay -> with(view) {
                            if (animateViews) boardViews.blocks.lastOrNull()?.also {
                                moveTo(it.block, it.square, gameMode.delayMs.milliseconds, Easing.LINEAR)
                            }
                        }
                    }
                }
            }
            block {
                onEnd()
            }
        }
    }

    private fun Block.move(animator: Animator, to: Square) {
        with(view) {
            if (animateViews) animator.moveTo(this@move, to, gameMode.moveMs.milliseconds, Easing.LINEAR)
        }
        boardViews[PlacedPiece(piece, to)] = this
    }

    private fun Block.remove() {
        boardViews.removeBlock(this)
    }

    private fun animateResultingBlock(animator: Animator, block: Block) {
        val x = block.x
        val y = block.y
        val scale = block.scale
        val scaleChange = 0.1
        val shift = block.scaledWidth * scaleChange / 2
        animator.tween(
            block::x[x - shift],
            block::y[y - shift],
            block::scale[scale + scaleChange],
            time = gameMode.resultingBlockMs.milliseconds,
            easing = Easing.LINEAR
        )
        animator.tween(
            block::x[x],
            block::y[y],
            block::scale[scale],
            time = gameMode.resultingBlockMs.milliseconds,
            easing = Easing.LINEAR
        )
    }

    fun onResumeEvent() {
        logClick("onResumeEvent")
        with(view) {
            gameStage.launch {
                reInitialize()
            }
        }
    }

    suspend fun delayWhileMoveInProgress() {
        while (moveIsInProgress.value) delay(20)
    }
}
