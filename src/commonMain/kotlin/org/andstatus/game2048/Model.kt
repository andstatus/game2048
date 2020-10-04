package org.andstatus.game2048

fun moveBlocksOnTheBoard(prevBoard: Board, moveDirection: Direction): Pair<Board, List<Move>> {
    val board = prevBoard.copy()
    val moves = mutableListOf<Move>()
    val direction = moveDirection.reverse()
    var square: Square? = board.firstSquareToIterate(direction)
    while (square != null) {
        val found = square.nextPlacedPieceInThe(direction, board)
        if (found == null) {
            square = square.nextToIterate(direction, board)
        } else {
            board[found.square] = null
            val next = found.square.nextInThe(direction, board)?.nextPlacedPieceInThe(direction, board)
            if (next != null && found.piece == next.piece) {
                // merge equal blocks
                board[square] = found.piece.next()
                board[next.square] = null
                moves += Move(found, next, square)
                if (!settings.allowResultingTileToMerge) {
                    square = square.nextToIterate(direction, board)
                }
            } else {
                if (found.square != square) {
                    moves += Move(found, null, square)
                }
                board[square] = found.piece
                square = square.nextToIterate(direction, board)
            }
        }
    }
    return Pair(board, moves)
}