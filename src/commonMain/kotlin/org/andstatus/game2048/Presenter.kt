package org.andstatus.game2048

import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korge.animate.Animator
import com.soywiz.korge.animate.animateSequence
import com.soywiz.korge.tween.get
import com.soywiz.korge.view.Text
import com.soywiz.korio.async.launch
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.concurrent.atomic.KorAtomicBoolean
import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.lang.format
import com.soywiz.korma.interpolation.Easing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

class Presenter(private val view: GameView) {
    private val coroutineScope: CoroutineScope get() = view.gameStage
    val model = Model()
    private val moveIsInProgress = KorAtomicBoolean(false)
    val score get() = model.score
    val bestScore get() = model.bestScore
    var boardViews = BoardViews(view.gameStage, settings.boardWidth, settings.boardHeight)

    init {
        presentGameClock(view.gameStage, model) { view.gameTime }
    }

    private enum class AutoPlayingEnum {
        UNDO,
        REDO,
        NONE
    }
    private val autoPlayingEnum: KorAtomicRef<AutoPlayingEnum> = KorAtomicRef(AutoPlayingEnum.NONE)
    private val preferableAutoPlayingEnum: KorAtomicRef<AutoPlayingEnum> = KorAtomicRef(AutoPlayingEnum.NONE)
    private var autoPlayCount = 0

    private fun presentGameClock(coroutineScope: CoroutineScope, model: Model, textSupplier: () -> Text) {
        fun Int.format() = "%02d".format(this)

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
        logClick("Undo")
        preferableAutoPlayingEnum.value = AutoPlayingEnum.UNDO
        autoPlayCount++
        undo()
    }

    fun undo() {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        model.gameClock.stop()
        boardViews.removeGameOver() // TODO: make this a move...
        (model.undo() + listOf(PlayerMove.delay()) + model.undo()).presentReversed()
    }

    fun onRedoClick() {
        logClick("Redo")
        preferableAutoPlayingEnum.value = AutoPlayingEnum.REDO
        autoPlayCount++
        redo()
    }

    fun redo() {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        (model.redo() + listOf(PlayerMove.delay()) + model.redo()).present()
    }

    fun composerMove(board: Board) = model.composerMove(board).present()

    fun onRestartClick() {
        logClick("Restart")
        autoPlayCount++
        restart()
    }

    fun restart() {
        autoPlayCount = 0
        autoPlayingEnum.value = AutoPlayingEnum.NONE
        model.restart(true).present()
    }

    fun userMove(playerMoveEnum: PlayerMoveEnum) {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        preferableAutoPlayingEnum.value = AutoPlayingEnum.NONE
        if (model.noMoreMoves()) {
            boardViews = boardViews
                    .removeGameOver()
                    .copy()
                    .apply { gameOver = view.showGameOver() }
            onPresentEnd()
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

    private fun showControls() {
        view.showControls(buttonsToShow())
    }

    private fun buttonsToShow(): List<AppBarButtonsEnum> {
        val list = ArrayList<AppBarButtonsEnum>()
        when(autoPlayingEnum.value) {
            AutoPlayingEnum.NONE -> {
                if (preferableAutoPlayingEnum.value == AutoPlayingEnum.NONE && model.gameClock.started) {
                    list.add(AppBarButtonsEnum.PAUSE)
                } else if (canRedo() && (preferableAutoPlayingEnum.value == AutoPlayingEnum.REDO || !canUndo())) {
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
            AutoPlayingEnum.UNDO -> {
                list.add(AppBarButtonsEnum.PAUSE)
                if (canUndo()) {
                    list.add(AppBarButtonsEnum.TO_START)
                }
            }
            AutoPlayingEnum.REDO -> {
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
                                        move.delayMs.milliseconds, Easing.LINEAR)
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
                                        move.delayMs.milliseconds, Easing.LINEAR)
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
            this@move.moveTo(to.positionX(), to.positionY(), 0.15.seconds, Easing.LINEAR)
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
                time = 0.1.seconds,
                easing = Easing.LINEAR
        )
        animator.tween(
                block::x[x],
                block::y[y],
                block::scale[scale],
                time = 0.1.seconds,
                easing = Easing.LINEAR
        )
    }

    fun onLogoClick() {
        logClick("Logo")
        showControls()
        autoPlayCount++
    }

    fun onPauseClick() {
        logClick("Pause")
        model.gameClock.stop()
        showControls()
        autoPlayCount++
    }

    fun onToStartClick() {
        logClick("ToStart")
        preferableAutoPlayingEnum.value = AutoPlayingEnum.UNDO
        autoPlayCount++
        if (autoPlayingEnum.value == AutoPlayingEnum.UNDO) {
            val startCount = autoPlayCount
            coroutineScope.launch {
                while (autoPlayingEnum.value == AutoPlayingEnum.UNDO && startCount == autoPlayCount) {
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
        preferableAutoPlayingEnum.value = AutoPlayingEnum.UNDO
        autoPlayCount++
        if (canUndo() && autoPlayingEnum.value != AutoPlayingEnum.UNDO) {
            val startCount = autoPlayCount
            autoPlayingEnum.value = AutoPlayingEnum.UNDO
            showControls()
            coroutineScope.launch {
                while (canUndo() && startCount == autoPlayCount) {
                    undo()
                    delay(500)
                }
                autoPlayingEnum.compareAndSet(AutoPlayingEnum.UNDO, AutoPlayingEnum.NONE)
                showControls()
            }
        }
    }

    fun onPlayClick() {
        logClick("Play")
        preferableAutoPlayingEnum.value = AutoPlayingEnum.REDO
        autoPlayCount++
        if (canRedo() && autoPlayingEnum.value != AutoPlayingEnum.REDO) {
            val startCount = autoPlayCount
            autoPlayingEnum.value = AutoPlayingEnum.REDO
            coroutineScope.launch {
                while (canRedo() && startCount == autoPlayCount) {
                    redo()
                    delay(1000)
                }
                autoPlayingEnum.compareAndSet(AutoPlayingEnum.REDO, AutoPlayingEnum.NONE)
                showControls()
            }
        }
    }

    fun onToCurrentClick() {
        logClick("ToCurrent")
        preferableAutoPlayingEnum.value = AutoPlayingEnum.REDO
        autoPlayCount++
        val startCount = autoPlayCount
        coroutineScope.launch {
            while (autoPlayingEnum.value != AutoPlayingEnum.NONE && startCount == autoPlayCount) {
                delay(100)
            }
            if (autoPlayingEnum.value == AutoPlayingEnum.NONE) {
                model.redoToCurrent().present()
            }
        }
    }

    fun onGameMenuClick() {
        logClick("GameMenu")
        autoPlayCount++
        model.gameClock.stop()
        view.showGameMenu(model.history.currentGame)
    }

    fun onDeleteGameClick() {
        logClick("DeleteGame")
        autoPlayCount++

        model.history.deleteCurrent()
        model.restart(false).present()
    }

    fun onRestoreClick() {
        logClick("Restore")
        autoPlayCount++
        view.showGameHistory(model.history.prevGames)
    }

    fun onHistoryItemClick(id: Int) {
        logClick("History$id")
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        autoPlayCount++
        model.restoreGame(id).present()
    }

    private fun logClick(buttonName: String) {
        Console.log("$buttonName clicked $autoPlayCount , autoplay:${autoPlayingEnum.value}")
    }
}
