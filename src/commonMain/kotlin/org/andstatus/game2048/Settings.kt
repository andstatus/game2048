package org.andstatus.game2048

import korlibs.image.font.readBitmapFont
import korlibs.io.file.std.resourcesVfs
import korlibs.io.lang.parseInt
import korlibs.korge.service.storage.NativeStorage
import korlibs.korge.view.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.andstatus.game2048.ai.AiAlgorithm
import org.andstatus.game2048.model.Board
import org.andstatus.game2048.model.PliesPageData
import org.andstatus.game2048.view.BoardSizeEnum.Companion.BOARD_SIZE_DEFAULT
import org.andstatus.game2048.view.BoardSizeEnum.Companion.fixBoardWidth
import org.andstatus.game2048.view.ColorThemeEnum
import org.andstatus.game2048.view.StringResources

private const val keyAllowResultingTileToMerge = "allowResultingTileToMerge"
private const val keyAllowUsersMoveWithoutBlockMoves = "allowUsersMoveWithoutBlockMoves"
private const val keyAllowUndo = "allowUndo"
private const val keyFullscreen = "fullscreen"
private const val keyMaxMovesToStore = "maxMovesToStore"
private const val keyBoardSize = "boardSize"
const val keyCurrentGameId = "current"
const val stubGameId = 61

/** Game options / tweaks. Default values are for the original game,
see https://en.wikipedia.org/wiki/2048_(video_game)
and for the game in browser: https://play2048.co/
For now you can modify settings in the "game.storage" file. */
class Settings(private val stage: Stage) {
    val multithreadedScope: CoroutineScope
        get() = CoroutineScope(
            stage.coroutineContext + Job()
                + Dispatchers.Default
        )
    val storage: MyStorage = MyStorage.load(stage)
    val keyColorTheme = "colorTheme"
    val keyAiAlgorithm = "aiAlgorithm"

    // false: The resulting tile cannot merge with another tile again in the same move
    var allowResultingTileToMerge = storage.getBoolean(keyAllowResultingTileToMerge, false)
    var allowUsersMoveWithoutBlockMoves = storage.getBoolean(keyAllowUsersMoveWithoutBlockMoves, false)
    var allowUndo = storage.getBoolean(keyAllowUndo, true)
    var boardWidth = storage.getInt(keyBoardSize, BOARD_SIZE_DEFAULT.width).let(::fixBoardWidth)

    val boardHeight get() =  boardWidth
    var colorThemeEnum: ColorThemeEnum = ColorThemeEnum.load(storage.getOrNull(keyColorTheme))
    var aiAlgorithm: AiAlgorithm = AiAlgorithm.load(storage.getOrNull(keyAiAlgorithm))
    val defaultBoard get() = Board(this)
    var pliesPageSize = 1000

    val gameIdsRange = 1 until stubGameId
    val maxOlderGames = 30
    val pliesPageData = PliesPageData(this)

    companion object {
        fun load(stage: Stage): Settings = myMeasured("Settings loaded") { Settings(stage) }
    }

    fun save() {
        storage.native.setBoolean(keyAllowResultingTileToMerge, allowResultingTileToMerge)
            .setBoolean(keyAllowUsersMoveWithoutBlockMoves, allowUsersMoveWithoutBlockMoves)
            .setBoolean(keyAllowUndo, allowUndo)
            .setInt(keyBoardSize, boardWidth)
            // TODO: Separate screen needed
            //  .setBoolean(keyFullscreen, colorThemeEnum == ColorThemeEnum.DEVICE_DEFAULT)
            .set(keyColorTheme, colorThemeEnum.labelKey)

        storage.native.set(keyAiAlgorithm, aiAlgorithm.id)
    }

    private fun NativeStorage.setBoolean(key: String, value: Boolean): NativeStorage {
        set(key, if (value) "true" else "false")
        return this
    }

    private fun NativeStorage.setInt(key: String, value: Int): NativeStorage {
        set(key, value.toString())
        return this
    }

    val currentGameId: Int?
        get() = storage.getOrNull(keyCurrentGameId)?.parseInt()
}

suspend fun loadFont(strings: StringResources) = myMeasured("Font loaded") {
    val fontFolder = when (strings.lang) {
        "zh" -> "noto_sans_sc"
        "si" -> "abhaya_libre"
        else -> "clear_sans"
    }
    resourcesVfs["assets/fonts/$fontFolder/font.fnt"].readBitmapFont()
}
