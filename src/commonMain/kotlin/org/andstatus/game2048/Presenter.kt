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
    private val autoPlayingEnum: KorAtomicRef<AutoPlayingEnum> = KorAtomicRef(AutoPlayingEnum.NONE)

    private val autoPlaying = object {
        private val speed = KorAtomicInt(1)

        fun increase() {
            val value = speed.value
            if (value < 10) speed.compareAndSet(value, value + 1)
        }

        fun decrease() {
            val value = speed.value
            if (value > 1) speed.compareAndSet(value, value - 1)
        }

        fun reset() {
            speed.value = 1
        }

        val delayMs get() = when(speed.value){
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

        val moveMs get() = when(speed.value){
            in Int.MIN_VALUE .. 1 -> 150
            1 -> 75
            2 -> 37
            3 -> 18
            4 -> 9
            5 -> 4
            6 -> 2
            else -> 1
        }

        val resultingBlockMs get() = when(speed.value){
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

    private val preferableAutoPlayingEnum: KorAtomicRef<AutoPlayingEnum> = KorAtomicRef(AutoPlayingEnum.NONE)
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
            preferableAutoPlayingEnum.value = AutoPlayingEnum.BACKWARDS
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
            preferableAutoPlayingEnum.value = AutoPlayingEnum.FORWARD
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
        when(autoPlayingEnum.value) {
            AutoPlayingEnum.BACKWARDS -> {
                when (swipeDirection) {
                    SwipeDirection.LEFT -> autoPlaying.increase()
                    SwipeDirection.RIGHT -> autoPlaying.decrease()
                    else -> {}
                }
            }
            AutoPlayingEnum.FORWARD -> {
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

        preferableAutoPlayingEnum.value = AutoPlayingEnum.NONE
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
        view.showControls(buttonsToShow())
    }

    private fun buttonsToShow(): List<AppBarButtonsEnum> {
        val list = ArrayList<AppBarButtonsEnum>()
        when(autoPlayingEnum.value) {
            AutoPlayingEnum.NONE -> {
                if (preferableAutoPlayingEnum.value == AutoPlayingEnum.NONE && model.gameClock.started) {
                    list.add(AppBarButtonsEnum.PAUSE)
                } else if (canRedo() && (preferableAutoPlayingEnum.value == AutoPlayingEnum.FORWARD || !canUndo())) {
                    list.add(AppBarButtonsEnum.PLAY)
                } else if (canUndo()) {
                    list.add(AppBarButtonsEnum.PLAY_BACKWARDS)
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
        preferableAutoPlayingEnum.value = AutoPlayingEnum.BACKWARDS
        val startCount = autoPlayCount.incrementAndGet()
        if (autoPlayingEnum.value == AutoPlayingEnum.BACKWARDS) {
            coroutineScope.launch {
                while (autoPlayingEnum.value == AutoPlayingEnum.BACKWARDS && startCount == autoPlayCount.value) {
                    delay(100)
                }
                if (autoPlayingEnum.value == AutoPlayingEnum.NONE) {
                    boardViews.removeGameOver() // TODO: make this a move...
                    (model.undoToStart() + listOf(PlayerMove.delay()) + model.redo()).present()
                }
            }
            return
        }
    }

    fun onPlayBackwardsClick() {
        logClick("Play Backwards")
        preferableAutoPlayingEnum.value = AutoPlayingEnum.BACKWARDS
        val startCount = autoPlayCount.incrementAndGet()
        if (canUndo() && autoPlayingEnum.value != AutoPlayingEnum.BACKWARDS) {
            autoPlayingEnum.value = AutoPlayingEnum.BACKWARDS
            autoPlaying.reset()
            showControls()
            coroutineScope.launch {
                while (canUndo() && startCount == autoPlayCount.value) {
                    undo()
                    delay(autoPlaying.delayMs.toLong())
                }
                if (autoPlayingEnum.compareAndSet(AutoPlayingEnum.BACKWARDS, AutoPlayingEnum.NONE)) {
                    autoPlaying.reset()
                }
                showControls()
            }
        }
    }

    fun onPlayClick() {
        logClick("Play")
        preferableAutoPlayingEnum.value = AutoPlayingEnum.FORWARD
        val startCount = autoPlayCount.incrementAndGet()
        if (canRedo() && autoPlayingEnum.value != AutoPlayingEnum.FORWARD) {
            autoPlayingEnum.value = AutoPlayingEnum.FORWARD
            autoPlaying.reset()
            coroutineScope.launch {
                while (canRedo() && startCount == autoPlayCount.value) {
                    redo()
                    delay(autoPlaying.delayMs.toLong())
                }
                if (autoPlayingEnum.compareAndSet(AutoPlayingEnum.FORWARD, AutoPlayingEnum.NONE)) {
                    autoPlaying.reset()
                }
                showControls()
            }
        }
    }

    fun onToCurrentClick() {
        afterStop {
            logClick("ToCurrent")
            preferableAutoPlayingEnum.value = AutoPlayingEnum.FORWARD
            model.redoToCurrent().present()
        }
    }

    private fun afterStop(action: () -> Unit) {
        val startCount = autoPlayCount.incrementAndGet()
        coroutineScope.launch {
            while (autoPlayingEnum.value != AutoPlayingEnum.NONE && startCount == autoPlayCount.value) {
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
        Console.log("$buttonName clicked $autoPlayCount , autoplay:${autoPlayingEnum.value}")
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
