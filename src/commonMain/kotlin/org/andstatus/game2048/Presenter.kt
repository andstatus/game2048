package org.andstatus.game2048

import com.soywiz.klock.seconds
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

    fun undo() = model.undo().present()

    fun redo() = model.redo().present()

    fun composerMove(board: Board) = model.composerMove(board).present()

    fun restart() = model.restart().present()

    fun usersMove(playerMoveEnum: PlayerMoveEnum) {
        if (model.noMoreMoves()) {
            val newGameOver = showGameOver(stage) {
                restart()
                moveIsInProgress.value = false
            }
            boardViews = boardViews
                    .apply { gameOver?.removeFromParent() }
                    .copy()
                    .apply { gameOver = newGameOver }
        } else {
            model.userMove(playerMoveEnum).also{
                if (it.isNotEmpty()) {
                    it.appendAll(model.computerMove()).present()
                }
            }
        }
    }

    private fun List<PlayerMove>.present() {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        presentMoves(this, 0) {
            if (score.value != model.score) {
                score.update(model.score)
            }
            if (bestScore.value != model.bestScore) {
                bestScore.update(model.bestScore)
            }
            restoreControls(stage)
            moveIsInProgress.value = false
        }
    }

    private fun presentMoves(playerMoves: List<PlayerMove>, index: Int, onEnd: () -> Unit) {
        if (index < playerMoves.size) {
            presentMove(stage, playerMoves[index]) {
                presentMoves(playerMoves, index + 1, onEnd)
            }
        } else {
            onEnd()
        }
    }

    private fun presentMove(stage: Stage, playerMove: PlayerMove, onEnd: () -> Unit) = stage.launchImmediately {
        stage.animateSequence {
            parallel {
                playerMove.moves.forEach { move ->
                    when(move) {
                        is MovePlace -> {
                            boardViews[move.first.square] = Block(move.first.piece).addTo(stage, move.first.square)
                        }
                        is MoveLoad -> {
                            boardViews.load(move.board)
                            restoreControls(stage)
                        }
                        is MoveOne -> {
                            val firstBlock = boardViews[move.first.square]
                            firstBlock?.move(this, move.first.square, move.destination)
                        }
                        is MoveMerge -> {
                            val firstBlock = boardViews[move.first.square]
                            val secondBlock = boardViews[move.second.square]
                            sequence {
                                parallel {
                                    firstBlock?.move(this, move.first.square, move.merged.square)
                                    secondBlock?.move(this, move.second.square, move.merged.square)
                                }
                                block {
                                    firstBlock?.removeFromParent()
                                    secondBlock?.removeFromParent()
                                    boardViews.addBlock(move.merged)
                                }
                                sequenceLazy {
                                    if (animateViews) boardViews[move.merged.square]
                                            ?.let { animateResultingBlock(this, it) }
                                }
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

    private fun Block.move(animator: Animator, from: Square, to: Square) {
        if (animateViews) animator.apply {
            this@move.moveTo(to.positionX(), to.positionY(), 0.15.seconds, Easing.LINEAR)
        }
        boardViews[from] = null
        boardViews[to] = this
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
