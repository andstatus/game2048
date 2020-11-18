package org.andstatus.game2048

import com.soywiz.klock.milliseconds
import com.soywiz.klogger.Console
import com.soywiz.klogger.log
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
import kotlin.math.abs

class Presenter(private val view: GameView) {
    private val coroutineScope: CoroutineScope get() = view.gameStage
    val model = Model()
    private val moveIsInProgress = KorAtomicBoolean(false)
    val score get() = model.score
    val bestScore get() = model.bestScore
    var boardViews = BoardViews(view, settings.boardWidth, settings.boardHeight)

    init {
        presentGameClock(view.gameStage, model) { view.gameTime }
    }

    private enum class AutoPlayingEnum {
        BACKWARDS,
        FORWARD,
        NONE
    }

    private data class AutoPlayingData(val state: AutoPlayingEnum,
                                       val preferable: AutoPlayingEnum,
                                       val speed: Int)

    private val autoPlaying = object {
        private val data = KorAtomicRef(initialData())

        fun stop() {
            val old = data.value
            data.compareAndSet(old, AutoPlayingData(AutoPlayingEnum.NONE, old.preferable, 0))
        }

        val speed get() = abs(data.value.speed)

        var state : AutoPlayingEnum
            get() = data.value.state
            set(value) {
                data.value = AutoPlayingData(
                        value,
                        value,
                        when(value) {
                            AutoPlayingEnum.BACKWARDS -> -1
                            AutoPlayingEnum.FORWARD -> 1
                            AutoPlayingEnum.NONE -> 0
                        }
                )
            }

        var preferable
            get() = data.value.preferable
            set(value) {
                data.value = AutoPlayingData(data.value.state, value, data.value.speed)
            }

        fun initialData() = AutoPlayingData(AutoPlayingEnum.NONE, AutoPlayingEnum.NONE, 0)

        fun increase() {
            val value = data.value
            if (value.speed < 10) {
                val newSpeed = value.speed + 1
                val newState = if (newSpeed > 0) AutoPlayingEnum.FORWARD else AutoPlayingEnum.BACKWARDS
                data.compareAndSet(value, AutoPlayingData(newState, newState, newSpeed))
            }
        }

        fun decrease() {
            val value = data.value
            if (value.speed > -10) {
                val newSpeed = value.speed - 1
                val newState = if (newSpeed > 0) AutoPlayingEnum.FORWARD else AutoPlayingEnum.BACKWARDS
                data.compareAndSet(value, AutoPlayingData(newState, newState, newSpeed))
            }
        }

        val delayMs get() = when(speed){
            in Int.MIN_VALUE .. 1 -> 500
            1 -> 250
            2 -> 125
            3 -> 64
            4 -> 32
            5 -> 16
            6 -> 8
            7 -> 4
            8 -> 2
            else -> 1
        }

        val moveMs get() = when(speed){
            in Int.MIN_VALUE .. 1 -> 150
            1 -> 75
            2 -> 37
            3 -> 18
            4 -> 9
            5 -> 4
            6 -> 2
            else -> 1
        }

        val resultingBlockMs get() = when(speed){
            in Int.MIN_VALUE .. 1 -> 100
            1 -> 50
            2 -> 28
            3 -> 15
            4 -> 8
            5 -> 4
            6 -> 2
            else -> 1
        }
    }

    private var autoPlayCount = KorAtomicInt(0)

    private fun presentGameClock(coroutineScope: CoroutineScope, model: Model, textSupplier: () -> Text) {
        coroutineScope.launch {
            while (true) {
                delay(1000)
                textSupplier().text = model.gameClock.playedSecondsString
            }
        }
    }

    fun onAppEntry() = model.onAppEntry().present()

    fun computerMove() = model.computerMove().present()

    fun computerMove(placedPiece: PlacedPiece) = model.computerMove(placedPiece).present()

    fun canUndo(): Boolean = model.canUndo()

    fun canRedo(): Boolean = model.canRedo()

    fun onUndoClick() {
        afterStop {
            logClick("Undo")
            autoPlaying.preferable = AutoPlayingEnum.BACKWARDS
            undo()
        }
    }

    fun undo() {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        model.gameClock.stop()
        boardViews.removeGameOver() // TODO: make this a move...
        (model.undo() + listOf(PlayerMove.delay()) + model.undo()).presentReversed()
    }

    fun onRedoClick() {
        afterStop {
            logClick("Redo")
            autoPlaying.preferable = AutoPlayingEnum.FORWARD
            redo()
        }
    }

    fun redo() {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        (model.redo() + listOf(PlayerMove.delay()) + model.redo()).present()
    }

    fun composerMove(board: Board) = model.composerMove(board).present()

    fun onRestartClick() {
        afterStop {
            logClick("Restart")
            restart()
        }
    }

    fun restart() {
        afterStop {
            model.restart(true).present()
        }
    }

    fun onSwipe(swipeDirection: SwipeDirection) {
        when(autoPlaying.state) {
            AutoPlayingEnum.BACKWARDS, AutoPlayingEnum.FORWARD -> {
                when (swipeDirection) {
                    SwipeDirection.LEFT -> autoPlaying.decrease()
                    SwipeDirection.RIGHT -> autoPlaying.increase()
                    else -> {}
                }
            }
            AutoPlayingEnum.NONE -> {
                userMove(when (swipeDirection) {
                    SwipeDirection.LEFT -> PlayerMoveEnum.LEFT
                    SwipeDirection.RIGHT -> PlayerMoveEnum.RIGHT
                    SwipeDirection.TOP -> PlayerMoveEnum.UP
                    SwipeDirection.BOTTOM -> PlayerMoveEnum.DOWN
                })
            }
        }

    }

    private fun userMove(playerMoveEnum: PlayerMoveEnum) {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        autoPlaying.preferable = AutoPlayingEnum.NONE
        if (model.noMoreMoves()) {
            onPresentEnd()
            boardViews = boardViews
                    .removeGameOver()
                    .copy()
                    .apply { gameOver = view.showGameOver() }
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
        view.showControls(buttonsToShow(), autoPlaying.speed)
    }

    private fun buttonsToShow(): List<AppBarButtonsEnum> {
        val list = ArrayList<AppBarButtonsEnum>()
        when(autoPlaying.state) {
            AutoPlayingEnum.NONE -> {
                if (autoPlaying.preferable == AutoPlayingEnum.NONE && model.gameClock.started) {
                    list.add(AppBarButtonsEnum.PAUSE)
                } else if (canRedo() && (autoPlaying.preferable == AutoPlayingEnum.FORWARD || !canUndo())) {
                    list.add(AppBarButtonsEnum.FORWARD)
                } else if (canUndo()) {
                    list.add(AppBarButtonsEnum.BACKWARDS)
                } else {
                    list.add(AppBarButtonsEnum.APP_LOGO)
                }

                if (canUndo()) {
                    list.add(AppBarButtonsEnum.UNDO)
                }
                if (canRedo()) {
                    list.add(AppBarButtonsEnum.REDO)
                }

                list.add(AppBarButtonsEnum.GAME_MENU)
            }
            AutoPlayingEnum.BACKWARDS -> {
                list.add(AppBarButtonsEnum.PAUSE)
                if (canUndo()) {
                    list.add(AppBarButtonsEnum.TO_START)
                }
            }
            AutoPlayingEnum.FORWARD -> {
                list.add(AppBarButtonsEnum.PAUSE)
                if (canRedo()) {
                    list.add(AppBarButtonsEnum.TO_CURRENT)
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
                        is MoveDelay -> if (view.animateViews) {
                            boardViews.blocks.lastOrNull()?.also {
                                it.block.moveTo(it.square.positionX(), it.square.positionY(),
                                        autoPlaying.delayMs.milliseconds, Easing.LINEAR)
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
                                ?: Console.log("No Block at destination during undo: $move")
                        is MoveLoad -> boardViews.load(move.board)
                        is MoveOne -> boardViews[PlacedPiece(move.first.piece, move.destination)]
                                ?.move(this, move.first.square)
                                ?: Console.log("No Block at destination during undo: $move")
                        is MoveMerge -> {
                            val destination = move.merged.square
                            val effectiveBlock = boardViews[move.merged]
                            sequence {
                                block {
                                    effectiveBlock?.remove()
                                            ?: Console.log("No Block at destination during undo: $move")
                                }
                                parallel {
                                    val secondBlock = boardViews.addBlock(PlacedPiece(move.second.piece, destination))
                                    val firstBlock = boardViews.addBlock(PlacedPiece(move.first.piece, destination))
                                    secondBlock.move(this, move.second.square)
                                    firstBlock.move(this, move.first.square)
                                }
                            }
                        }
                        is MoveDelay -> if (view.animateViews) {
                            boardViews.blocks.lastOrNull()?.also {
                                it.block.moveTo(it.square.positionX(), it.square.positionY(),
                                        autoPlaying.delayMs.milliseconds, Easing.LINEAR)
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
        if (view.animateViews) animator.apply {
            this@move.moveTo(to.positionX(), to.positionY(), autoPlaying.moveMs.milliseconds, Easing.LINEAR)
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
                time = autoPlaying.resultingBlockMs.milliseconds,
                easing = Easing.LINEAR
        )
        animator.tween(
                block::x[x],
                block::y[y],
                block::scale[scale],
                time = autoPlaying.resultingBlockMs.milliseconds,
                easing = Easing.LINEAR
        )
    }

    fun onPauseClick() {
        afterStop {
            logClick("Pause")
            model.gameClock.stop()
            showControls()
        }
    }

    fun onToStartClick() {
        logClick("ToStart")
        autoPlaying.preferable = AutoPlayingEnum.BACKWARDS
        val startCount = autoPlayCount.incrementAndGet()
        if (autoPlaying.state == AutoPlayingEnum.BACKWARDS) {
            coroutineScope.launch {
                while (autoPlaying.state == AutoPlayingEnum.BACKWARDS && startCount == autoPlayCount.value) {
                    delay(100)
                }
                if (autoPlaying.state == AutoPlayingEnum.NONE) {
                    boardViews.removeGameOver() // TODO: make this a move...
                    (model.undoToStart() + listOf(PlayerMove.delay()) + model.redo()).present()
                }
            }
            return
        }
    }

    fun onWatchClick() {
        logClick("Watch")
        TODO()
    }

    fun onPlayClick() {
        logClick("Play")
        TODO()
    }

    fun onBackwardsClick() {
        logClick("Backwards")
        startAutoPlaying(AutoPlayingEnum.BACKWARDS)
    }

    fun onStopClick() {
        logClick("Stop")
        TODO()
    }

    private fun startAutoPlaying(newState: AutoPlayingEnum) {
        autoPlaying.preferable = newState
        val startCount = autoPlayCount.incrementAndGet()
        if (autoPlaying.state != newState &&
                if (newState == AutoPlayingEnum.BACKWARDS) canUndo() else canRedo()) {
            autoPlaying.state = newState
            showControls()
            coroutineScope.launch {
                while (startCount == autoPlayCount.value &&
                        if (autoPlaying.state == AutoPlayingEnum.BACKWARDS) canUndo() else canRedo()) {
                    if (autoPlaying.speed != 0) {
                        if (autoPlaying.state == AutoPlayingEnum.BACKWARDS) undo() else redo()
                    }
                    delay(autoPlaying.delayMs.toLong())
                }
                autoPlaying.stop()
                showControls()
            }
        }
    }

    fun onForwardClick() {
        logClick("Forward")
        startAutoPlaying(AutoPlayingEnum.FORWARD)
    }

    fun onToCurrentClick() {
        afterStop {
            logClick("ToCurrent")
            autoPlaying.preferable = AutoPlayingEnum.FORWARD
            model.redoToCurrent().present()
        }
    }

    private fun afterStop(action: () -> Unit) {
        val startCount = autoPlayCount.incrementAndGet()
        coroutineScope.launch {
            while (autoPlaying.state != AutoPlayingEnum.NONE && startCount == autoPlayCount.value) {
                delay(100)
            }
            action()
        }
    }

    fun onGameMenuClick() {
        afterStop {
            logClick("GameMenu")
            model.gameClock.stop()
            view.showGameMenu(model.history.currentGame)
        }
    }

    fun onCloseGameMenuClick() {
        logClick("CloseGameMenu")
        showControls()
    }

    fun onDeleteGameClick() {
        afterStop {
            logClick("DeleteGame")
            model.history.deleteCurrent()
            model.restart(false).present()
        }
    }

    fun onRestoreClick() {
        afterStop {
            logClick("Restore")
            view.showGameHistory(model.history.prevGames)
        }
    }

    fun onHistoryItemClick(id: Int) {
        afterStop {
            logClick("History$id")
            if (moveIsInProgress.compareAndSet(expect = false, update = true)) {
                model.restoreGame(id).present()
            }
        }
    }

    private fun logClick(buttonName: String) {
        Console.log("$buttonName clicked $autoPlayCount , autoplay:${autoPlaying.state}")
    }

    fun onShareClick() {
        logClick("Share")
        shareText(view.stringResources.text("share"), model.history.currentGame.shortRecord.jsonFileName,
                model.history.currentGame.toMap().toJson())
    }

    fun onLoadClick() {
        logClick("Load")
        loadJsonGameRecord { json ->
            view.gameStage.launch {
                Console.log("Opened game: ${json.substr(0, 140)}")
                GameRecord.fromJson(json, newId = 0)?.let {
                    // I noticed some kind of KorGe window reset after return from the other activity,
                    //   so let's wait for awhile and redraw everything a bit later...
                    delay(3000)
                    Console.log("Restored game: $it")
                    model.history.currentGame = it
                    onToCurrentClick()
                }
            }
        }
    }
}
