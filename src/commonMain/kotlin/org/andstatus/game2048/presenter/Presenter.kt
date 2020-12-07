package org.andstatus.game2048.presenter

import com.soywiz.klock.milliseconds
import com.soywiz.korge.animate.Animator
import com.soywiz.korge.animate.animateSequence
import com.soywiz.korge.input.SwipeDirection
import com.soywiz.korge.tween.get
import com.soywiz.korge.view.Text
import com.soywiz.korio.async.launch
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.concurrent.atomic.KorAtomicBoolean
import com.soywiz.korio.concurrent.atomic.KorAtomicInt
import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.concurrent.atomic.incrementAndGet
import com.soywiz.korio.lang.substr
import com.soywiz.korio.serialization.json.toJson
import com.soywiz.korma.interpolation.Easing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import org.andstatus.game2048.*
import org.andstatus.game2048.model.*
import org.andstatus.game2048.view.*
import kotlin.math.abs

class Presenter(private val view: GameView) {
    private val coroutineScope: CoroutineScope get() = view.gameStage
    val model = Model()
    private val moveIsInProgress = KorAtomicBoolean(false)
    val score get() = model.score
    val bestScore get() = model.bestScore
    var boardViews = BoardViews(view, settings.boardWidth, settings.boardHeight)

    private fun presentGameClock(coroutineScope: CoroutineScope, model: Model, textSupplier: () -> Text) {
        coroutineScope.launch {
            while (true) {
                textSupplier().text = model.gameClock.playedSecondsString
                delay(1000)
            }
        }
    }

    private enum class GameModeEnum {
        BACKWARDS,
        FORWARD,
        STOP,
        PLAY,
    }

    private data class GameModeData(val mode: GameModeEnum, val speed: Int)

    private val maxSpeed = 6
    private val gameMode = object {
        private val data = KorAtomicRef(initialData())

        fun stop() {
            val old = data.value
            data.compareAndSet(old, GameModeData(GameModeEnum.STOP, 0))
        }

        val speed get() = data.value.speed
        val absSpeed get() = abs(data.value.speed)

        val autoPlaying get() = mode == GameModeEnum.BACKWARDS || mode == GameModeEnum.FORWARD

        var mode : GameModeEnum
            get() = data.value.mode
            set(value) {
                data.value = GameModeData(
                        value,
                        when(value) {
                            GameModeEnum.BACKWARDS -> -1
                            GameModeEnum.FORWARD -> 1
                            GameModeEnum.STOP, GameModeEnum.PLAY -> 0
                        }
                )
            }

        fun initialData() = GameModeData(GameModeEnum.STOP, 0)

        fun incrementSpeed() {
            val value = data.value
            if (value.speed < maxSpeed) {
                val newSpeed = value.speed + 1
                val newMode = when (newSpeed) {
                    in Int.MIN_VALUE .. -1 -> GameModeEnum.BACKWARDS
                    0 -> GameModeEnum.STOP
                    else -> GameModeEnum.FORWARD
                }
                data.compareAndSet(value, GameModeData(newMode, newSpeed))
            }
        }

        fun decrementSpeed() {
            val value = data.value
            if (value.speed > -maxSpeed) {
                val newSpeed = value.speed - 1
                val newMode = when (newSpeed) {
                    in Int.MIN_VALUE .. -1 -> GameModeEnum.BACKWARDS
                    0 -> GameModeEnum.STOP
                    else -> GameModeEnum.FORWARD
                }
                data.compareAndSet(value, GameModeData(newMode, newSpeed))
            }
        }

        val delayMs get() = when(absSpeed){
            in Int.MIN_VALUE .. 1 -> 500
            1 -> 250
            2 -> 125
            3 -> 64
            4 -> 32
            5 -> 16
            else -> 1
        }

        val moveMs get() = when(absSpeed){
            in Int.MIN_VALUE .. 1 -> 150
            1 -> 75
            2 -> 37
            3 -> 18
            4 -> 9
            5 -> 4
            else -> 1
        }

        val resultingBlockMs get() = when(absSpeed){
            in Int.MIN_VALUE .. 1 -> 100
            1 -> 50
            2 -> 28
            3 -> 15
            4 -> 8
            5 -> 4
            else -> 1
        }
    }

    private var clickCounter = KorAtomicInt(0)

    fun onAppEntry() = myMeasured("onAppEntry") {
        gameMode.mode = if (model.history.currentGame.id == 0) GameModeEnum.PLAY else GameModeEnum.STOP
        model.onAppEntry().present()
        presentGameClock(view.gameStage, model) { view.scoreBar.gameTime }
        if (model.history.prevGames.isEmpty() && model.history.currentGame.score == 0) {
            view.showHelp()
        }
    }

    fun canUndo(): Boolean = model.canUndo()

    fun canRedo(): Boolean = model.canRedo()

    fun onUndoClick() = afterStop {
        logClick("Undo")
        undo()
    }

    fun undo() {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        model.gameClock.stop()
        boardViews.removeGameOver() // TODO: make this a move...
        (model.undo() + listOf(PlayerMove.delay()) + model.undo()).presentReversed()
    }

    fun onRedoClick() = afterStop {
        logClick("Redo")
        redo()
    }

    fun redo() {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        (model.redo() + listOf(PlayerMove.delay()) + model.redo()).present()
    }

    fun onRestartClick() = afterStop {
        logClick("Restart")
        restart()
    }

    fun restart() = afterStop {
        gameMode.mode = GameModeEnum.PLAY
        model.saveCurrent()
        model.restart().present()
    }

    fun onSwipe(swipeDirection: SwipeDirection) {
        when(gameMode.mode) {
            GameModeEnum.BACKWARDS, GameModeEnum.FORWARD, GameModeEnum.STOP -> {
                when (swipeDirection) {
                    SwipeDirection.LEFT -> startAutoPlaying(GameModeEnum.BACKWARDS)
                    SwipeDirection.RIGHT -> startAutoPlaying(GameModeEnum.FORWARD)
                    SwipeDirection.BOTTOM -> onStopClick()
                    else -> {}
                }
            }
            GameModeEnum.PLAY -> {
                userMove(when (swipeDirection) {
                    SwipeDirection.LEFT -> PlayerMoveEnum.LEFT
                    SwipeDirection.RIGHT -> PlayerMoveEnum.RIGHT
                    SwipeDirection.TOP -> PlayerMoveEnum.UP
                    SwipeDirection.BOTTOM -> PlayerMoveEnum.DOWN
                })
            }
        }

    }

    fun onBookmarkClick() = afterStop {
        logClick("Bookmark")
        model.createBookmark()
        showControls()
    }

    fun onBookmarkedClick() = afterStop {
        logClick("Bookmarked")
        model.deleteBookmark()
        showControls()
    }

    fun onPauseClick() = afterStop {
        logClick("Pause")
        model.gameClock.stop()
        model.saveCurrent()
        showControls()
    }

    fun onPauseEvent() {
        myLog("onPauseEvent")
        model.gameClock.stop()
        model.saveCurrent()
        showControls()
    }

    fun onCloseGameWindowClick() {
        logClick("onCloseGameWindow")
        model.gameClock.stop()
        model.saveCurrent()
        view.gameStage.gameWindow.close()
        view.gameStage.closeGameApp()
    }

    fun onWatchClick() = afterStop {
        logClick("Watch")
        gameMode.mode = GameModeEnum.STOP
        showControls()
    }

    fun onPlayClick() = afterStop {
        logClick("Play")
        gameMode.mode = GameModeEnum.PLAY
        showControls()
    }

    fun onBackwardsClick() {
        logClick("Backwards")
        startAutoPlaying(GameModeEnum.BACKWARDS)
    }

    fun onStopClick() = afterStop {
        logClick("Stop")
        gameMode.mode = GameModeEnum.STOP
        showControls()
    }

    fun onForwardClick() {
        logClick("Forward")
        startAutoPlaying(GameModeEnum.FORWARD)
    }

    fun onToStartClick() = afterStop {
        logClick("ToStart")
        boardViews.removeGameOver() // TODO: make this a move...
        (model.undoToStart() + listOf(PlayerMove.delay()) + model.redo()).present()
    }

    fun onToCurrentClick() = afterStop {
        logClick("ToCurrent")
        model.redoToCurrent().present()
    }

    fun onGameMenuClick() = afterStop {
        logClick("GameMenu")
        model.gameClock.stop()
        view.showGameMenu()
    }

    fun onCloseMyWindowClick() {
        logClick("CloseMyWindow")
        showControls()
    }

    fun onDeleteGameClick() = afterStop {
        logClick("DeleteGame")
        gameMode.mode = GameModeEnum.PLAY
        model.history.deleteCurrent()
        model.restart().present()
    }

    fun onBookmarksClick() {
        logClick("Bookmarks")
        view.showBookmarks(model.history.currentGame)
    }

    fun onRestoreClick() = afterStop {
        logClick("Restore")
        model.saveCurrent()
        view.showRestoreGame(model.history.prevGames)
    }

    fun onGoToBookmarkClick(board: Board) = afterStop {
        logClick("GoTo${board.moveNumber}")
        if (moveIsInProgress.compareAndSet(expect = false, update = true)) {
            gameMode.mode = GameModeEnum.STOP
            model.gotoBookmark(board).present()
        }
    }

    fun onHistoryItemClick(id: Int) = afterStop {
        logClick("History$id")
        if (moveIsInProgress.compareAndSet(expect = false, update = true)) {
            gameMode.mode = GameModeEnum.STOP
            model.restoreGame(id).present()
        }
    }

    fun onShareClick() = afterStop {
        logClick("Share")
        view.gameStage.shareText(view.stringResources.text("share"), model.history.currentGame.shortRecord.jsonFileName,
                model.history.currentGame.toMap().toJson())
    }

    fun onLoadClick() = afterStop {
        logClick("Load")
        view.gameStage.loadJsonGameRecord { json ->
            view.gameStage.launch {
                myLog("Opened game: ${json.substr(0, 140)}")
                GameRecord.fromJson(json, newId = 0)?.let {
                    myLog("Loaded game: $it")
                    model.history.currentGame = it
                    onToCurrentClick()
                    model.saveCurrent()
                    // I noticed some kind of KorGe window reset after return from the other activity:
                    // GLSurfaceView.onSurfaceChanged
                    // If really needed, we could re-create activity...
                }
            }
        }
    }

    fun onHelpClick() = afterStop {
        logClick("Help")
        view.showHelp()
    }

    fun onHelpOkClick() = afterStop {
        logClick("Help OK")
        showControls()
    }

    private fun startAutoPlaying(newMode: GameModeEnum) {
        if (gameMode.mode == newMode) {
            if (newMode == GameModeEnum.BACKWARDS) gameMode.decrementSpeed() else gameMode.incrementSpeed()
        } else if (if (newMode == GameModeEnum.BACKWARDS) canUndo() else canRedo()) {
            afterStop {
                val startCount = clickCounter.incrementAndGet()
                gameMode.mode = newMode
                showControls()
                coroutineScope.launch {
                    while (startCount == clickCounter.value &&
                            if (gameMode.mode == GameModeEnum.BACKWARDS) canUndo() else canRedo()) {
                        if (gameMode.speed != 0) {
                            if (gameMode.mode == GameModeEnum.BACKWARDS) undo() else redo()
                        }
                        delay(gameMode.delayMs.toLong())
                    }
                    gameMode.stop()
                    showControls()
                }
            }
        }
    }

    private fun logClick(buttonName: String) {
        myLog("$buttonName clicked ${clickCounter.value}, mode:${gameMode.mode}")
        gameStopWatch.start()
    }

    private fun afterStop(action: () -> Unit) {
        val startCount = clickCounter.incrementAndGet()
        coroutineScope.launch {
            while (gameMode.autoPlaying && startCount == clickCounter.value) {
                delay(100)
            }
            action()
        }
    }

    fun composerMove(board: Board) = model.composerMove(board).present()

    fun computerMove() = model.computerMove().present()

    fun computerMove(placedPiece: PlacedPiece) = model.computerMove(placedPiece).present()

    private fun userMove(playerMoveEnum: PlayerMoveEnum) {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        if (model.noMoreMoves()) {
            onPresentEnd()
            boardViews = boardViews
                    .removeGameOver()
                    .copy()
                    .apply { gameOver = view.boardView.showGameOver() }
        } else {
            model.userMove(playerMoveEnum).let{
                if (it.isEmpty()) it else it + model.computerMove()
            }.present()
        }
    }

    private fun List<PlayerMove>.present(index: Int = 0) {
        if (index < size) {
            present(this[index]) {
                present(index + 1)
            }
        } else {
            onPresentEnd()
        }
    }

    private fun onPresentEnd() {
        showControls()
        moveIsInProgress.value = false
    }

    fun showControls() {
        view.showControls(buttonsToShow(), gameMode.speed)
    }

    private fun buttonsToShow(): List<AppBarButtonsEnum> {
        val list = ArrayList<AppBarButtonsEnum>()
        when(gameMode.mode) {
            GameModeEnum.PLAY -> {
                if (model.gameClock.started) {
                    list.add(AppBarButtonsEnum.PAUSE)
                    list.add(
                        if (model.isBookmarked) AppBarButtonsEnum.BOOKMARKED else AppBarButtonsEnum.BOOKMARK
                    )
                } else if (model.history.currentGame.playerMoves.size > 1) {
                    if (model.isBookmarked) {
                        list.add(AppBarButtonsEnum.BOOKMARKED)
                    } else {
                        list.add(AppBarButtonsEnum.WATCH)
                    }
                    if (!canRedo()) {
                        list.add(AppBarButtonsEnum.RESTART)
                    }
                }

                if (canUndo()) {
                    list.add(AppBarButtonsEnum.UNDO)
                }
                list.add(if (canRedo()) AppBarButtonsEnum.REDO else AppBarButtonsEnum.REDO_PLACEHOLDER)

                list.add(AppBarButtonsEnum.GAME_MENU)
            }
            else -> {
                list.add(AppBarButtonsEnum.PLAY)
                if (canUndo()) {
                    if (gameMode.speed == -maxSpeed) {
                        list.add(AppBarButtonsEnum.TO_START)
                    } else {
                        list.add(AppBarButtonsEnum.BACKWARDS)
                    }
                }
                if (gameMode.mode == GameModeEnum.STOP) {
                    list.add(AppBarButtonsEnum.STOP_PLACEHOLDER)
                } else {
                    list.add(AppBarButtonsEnum.STOP)
                }
                if (canRedo()) {
                    if (gameMode.speed == maxSpeed) {
                        list.add(AppBarButtonsEnum.TO_CURRENT)
                    } else {
                        list.add(AppBarButtonsEnum.FORWARD)
                    }
                } else {
                    list.add(AppBarButtonsEnum.FORWARD_PLACEHOLDER)
                }
                if (gameMode.mode == GameModeEnum.STOP) {
                    list.add(AppBarButtonsEnum.GAME_MENU)
                }
            }
        }
        return list
    }

    private fun present(playerMove: PlayerMove, onEnd: () -> Unit) = view.gameStage.launchImmediately {
        view.gameStage.animateSequence {
            parallel {
                playerMove.moves.forEach { move ->
                    when(move) {
                        is MovePlace -> boardViews.addBlock(move.first)
                        is MoveLoad -> boardViews.load(move.board)
                        is MoveOne -> boardViews[move.first]?.move(this, move.destination)
                        is MoveMerge -> {
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
                        is MoveDelay -> with(view) {
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

    private fun List<PlayerMove>.presentReversed(index: Int = 0) {
        if (index < size) {
            presentReversed(this[index]) {
                presentReversed(index + 1)
            }
        } else {
            onPresentEnd()
        }
    }

    private fun presentReversed(playerMove: PlayerMove, onEnd: () -> Unit) = view.gameStage.launchImmediately {
        view.gameStage.animateSequence {
            parallel {
                playerMove.moves.asReversed().forEach { move ->
                    when(move) {
                        is MovePlace -> boardViews[move.first]
                                ?.remove()
                                ?: myLog("No Block at destination during undo: $move")
                        is MoveLoad -> boardViews.load(move.board)
                        is MoveOne -> boardViews[PlacedPiece(move.first.piece, move.destination)]
                                ?.move(this, move.first.square)
                                ?: myLog("No Block at destination during undo: $move")
                        is MoveMerge -> {
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
                        is MoveDelay -> with(view) {
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
}
