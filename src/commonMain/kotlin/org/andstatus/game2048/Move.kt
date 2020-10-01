package org.andstatus.game2048

import com.soywiz.klock.seconds
import com.soywiz.korge.animate.Animator
import com.soywiz.korge.animate.animateSequence
import com.soywiz.korge.tween.get
import com.soywiz.korge.view.Stage
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.concurrent.atomic.KorAtomicBoolean
import com.soywiz.korma.interpolation.Easing
import kotlin.random.Random

class Move(val first: Block, val second: Block?, val destination: Square)

private val moveIsInProgress = KorAtomicBoolean(false)

fun placeRandomBlock(stage: Stage) {
    val newBoard = board.copy()
    val square = newBoard.getRandomFreeSquare() ?: return

    val piece = if (Random.nextDouble() < 0.9) Piece.N2 else Piece.N4
    newBoard[square] = Block(piece).addTo(stage, square)
    board = newBoard
}

fun moveBlocksTo(stage: Stage, direction: Direction) {
    if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

    if (board.noMoreMoves()) {
        showGameOver(stage) {
            restart()
            computersMove(stage)
            moveIsInProgress.value = false
        }
    } else {
        val (newBoard, moves) = moveBlocksOnTheBoard(board, direction)
        if (moves.isEmpty() && !settings.allowUsersMoveWithoutBlockMoves) {
            moveIsInProgress.value = false
        } else {
            board = newBoard
            animateMoves(stage, moves) {
                val points = moves.fold(0) { acc, move ->
                    acc + (move.second?.piece?.value ?: 0)
                }
                score.update(score.value + points)
                computersMove(stage)
                moveIsInProgress.value = false
            }
        }
    }
}

private fun moveBlocksOnTheBoard(prevBoard: Board, moveDirection: Direction): Pair<Board, List<Move>> {
    val board = prevBoard.copy()
    val moves = mutableListOf<Move>()
    val direction = moveDirection.reverse()
    var square: Square? = board.firstSquareToIterate(direction)
    while (square != null) {
        val found = square.nextPlacedBlockInThe(direction, board)
        if (found == null) {
            square = square.nextToIterate(direction, board)
        } else {
            board[found.square] = null
            val next = found.square.nextInThe(direction, board)?.nextPlacedBlockInThe(direction, board)
            if (next != null && found.block.piece == next.block.piece) {
                // merge equal blocks
                board[square] = Block(found.block.piece.next())
                board[next.square] = null
                moves += Move(found.block, next.block, square)
                if (!settings.allowResultingTileToMerge) {
                    square = square.nextToIterate(direction, board)
                }
            } else {
                if (found.square != square) {
                    moves += Move(found.block, null, square)
                }
                board[square] = found.block
                square = square.nextToIterate(direction, board)
            }
        }
    }
    return Pair(board, moves)
}

private fun animateMoves(stage: Stage, moves: List<Move>, onEnd: () -> Unit) = stage.launchImmediately {
    stage.animateSequence {
        parallel {
            moves.forEach { move ->
                if (move.second == null) {
                    move.first.moveTo(move.destination.positionX(), move.destination.positionY(), 0.15.seconds, Easing.LINEAR)
                } else {
                    sequence {
                        parallel {
                            move.first.moveTo(move.destination.positionX(), move.destination.positionY(), 0.15.seconds, Easing.LINEAR)
                            move.second.moveTo(move.destination.positionX(), move.destination.positionY(), 0.15.seconds, Easing.LINEAR)
                        }
                        block {
                            board[move.destination]?.apply { addTo(stage, move.destination) }
                            move.first.removeFromParent()
                            move.second.removeFromParent()
                        }
                        sequenceLazy {
                            board[move.destination]?.let { animateResultingBlock(this, it) }
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
