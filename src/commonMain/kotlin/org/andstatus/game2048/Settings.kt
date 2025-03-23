package org.andstatus.game2048

import korlibs.korge.service.storage.NativeStorage
import org.andstatus.game2048.ai.AiAlgorithm
import org.andstatus.game2048.model.Board
import org.andstatus.game2048.view.BoardSizeEnum
import org.andstatus.game2048.view.BoardSizeEnum.Companion.BOARD_SIZE_DEFAULT
import org.andstatus.game2048.view.ColorThemeEnum

const val keyFullscreen = "fullscreen"
const val keyCurrentGameId = "current"
const val stubGameId = 61
private const val keyAllowResultingTileToMerge = "allowResultingTileToMerge"
private const val keyAllowUsersMoveWithoutBlockMoves = "allowUsersMoveWithoutBlockMoves"
private const val keyAllowUndo = "allowUndo"
private const val keyBoardSize = "boardSize"
private val keyColorTheme = "colorTheme"
private val keyAiAlgorithm = "aiAlgorithm"

/** Game options / tweaks. Default values are for the original game,
see https://en.wikipedia.org/wiki/2048_(video_game)
and for the game in browser: https://play2048.co/
For now you can modify settings in the "game.storage" file. */
data class Settings(
    // false: The resulting tile cannot merge with another tile again in the same move
    val allowResultingTileToMerge: Boolean,
    val allowUsersMoveWithoutBlockMoves: Boolean,
    val allowUndo: Boolean,
    val boardSize: BoardSizeEnum,
    val colorThemeEnum: ColorThemeEnum,
    val aiAlgorithm: AiAlgorithm,
    val pliesPageSize: Int = 1000,
) {
    val defaultBoard get() = Board(this, this.boardSize)
    val gameIdsRange = 1 until stubGameId
    val maxOlderGames = 30

    fun save(storage: MyStorage) {
        storage.native.setBoolean(keyAllowResultingTileToMerge, allowResultingTileToMerge)
            .setBoolean(keyAllowUsersMoveWithoutBlockMoves, allowUsersMoveWithoutBlockMoves)
            .setBoolean(keyAllowUndo, allowUndo)
            .setInt(keyBoardSize, boardSize.width)
            // TODO: Separate screen needed
            //  .setBoolean(keyFullscreen, colorThemeEnum == ColorThemeEnum.DEVICE_DEFAULT)
            .set(keyColorTheme, colorThemeEnum.labelKey)

        storage.native.set(keyAiAlgorithm, aiAlgorithm.id)
    }

    companion object {
        fun load(storage: MyStorage): Settings {
            return Settings(
                allowResultingTileToMerge = storage.getBoolean(keyAllowResultingTileToMerge, false),
                allowUsersMoveWithoutBlockMoves = storage.getBoolean(keyAllowUsersMoveWithoutBlockMoves, false),
                allowUndo = storage.getBoolean(keyAllowUndo, true),
                boardSize = BoardSizeEnum.fromIntWidth(storage.getInt(keyBoardSize, BOARD_SIZE_DEFAULT.width)),
                colorThemeEnum = ColorThemeEnum.load(storage.getOrNull(keyColorTheme)),
                aiAlgorithm = AiAlgorithm.load(storage.getOrNull(keyAiAlgorithm)),
            )
        }
    }
}

private fun NativeStorage.setBoolean(key: String, value: Boolean): NativeStorage {
    set(key, if (value) "true" else "false")
    return this
}

private fun NativeStorage.setInt(key: String, value: Int): NativeStorage {
    set(key, value.toString())
    return this
}
