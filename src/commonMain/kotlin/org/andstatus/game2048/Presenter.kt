package org.andstatus.game2048

import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korge.animate.Animator
import com.soywiz.korge.animate.animateSequence
import com.soywiz.korge.tween.get
import com.soywiz.korge.view.Stage
import com.soywiz.korio.async.ObservableProperty
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.concurrent.atomic.KorAtomicBoolean
import com.soywiz.korma.interpolation.Easing

class Presenter(private val stage: Stage, private val animateViews: Boolean) {
    val model = Model()
    private val moveIsInProgress = KorAtomicBoolean(false)
    val score = ObservableProperty(-1)
    val bestScore = ObservableProperty(-1)
    var boardViews = BoardViews(stage, settings.boardWidth, settings.boardHeight)

    fun onAppEntry() = model.onAppEntry().present()

    fun computerMove() = model.computerMove().present()

    fun computerMove(placedPiece: PlacedPiece) = model.computerMove(placedPiece).present()

    fun canUndo(): Boolean = model.canUndo()

    fun canRedo(): Boolean = model.canRedo()

    fun undo() {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        boardViews.removeGameOver() // TODO: make this a move...
        (model.undo() + listOf(PlayerMove.delay()) + model.undo()).presentReversed()
    }

    fun redo() {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        (model.redo() + listOf(PlayerMove.delay()) + model.redo()).present()
    }

    fun composerMove(board: Board) = model.composerMove(board).present()

    fun restart() = model.restart().present()

    fun userMove(playerMoveEnum: PlayerMoveEnum) {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        if (model.noMoreMoves()) {
            boardViews = boardViews
                    .removeGameOver()
                    .copy()
                    .apply { gameOver = showGameOver(stage) }
            onPresentEnd()
        } else {
            model.userMove(playerMoveEnum).let{
                if (it.isEmpty()) it else it + model.computerMove()
            }.present()
        }
    }

    private fun List<PlayerMove>.present(index: Int = 0) {
        if (index < size) {
            present(stage, this[index]) {
                present(index + 1)
            }
        } else {
            onPresentEnd()
        }
    }

    private fun onPresentEnd() {
        if (score.value != model.score) {
            score.update(model.score)
        }
        if (bestScore.value != model.bestScore) {
            bestScore.update(model.bestScore)
        }
        restoreControls(stage)
        moveIsInProgress.value = false
    }

    private fun present(stage: Stage, playerMove: PlayerMove, onEnd: () -> Unit) = stage.launchImmediately {
        stage.animateSequence {
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
                                    if (animateViews) boardViews[move.merged]
                                            ?.let { animateResultingBlock(this, it) }
                                }
                            }
                        }
                        is MoveDelay -> if (animateViews) {
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
            presentReversed(stage, this[index]) {
                presentReversed(index + 1)
            }
        } else {
            onPresentEnd()
        }
    }

    private fun presentReversed(stage: Stage, playerMove: PlayerMove, onEnd: () -> Unit) = stage.launchImmediately {
        stage.animateSequence {
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
                        is MoveDelay -> if (animateViews) {
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
        if (animateViews) animator.apply {
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
}
