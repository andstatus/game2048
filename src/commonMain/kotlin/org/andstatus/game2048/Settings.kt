package org.andstatus.game2048

import com.soywiz.korge.service.storage.NativeStorage
import com.soywiz.korge.view.Stage
import com.soywiz.korim.font.readBitmapFont
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.lang.parseInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.andstatus.game2048.ai.AiAlgorithm
import org.andstatus.game2048.model.Board
import org.andstatus.game2048.model.ShortRecord
import org.andstatus.game2048.view.ColorThemeEnum
import org.andstatus.game2048.view.StringResources

private const val keyAllowResultingTileToMerge = "allowResultingTileToMerge"
private const val keyAllowUsersMoveWithoutBlockMoves = "allowUsersMoveWithoutBlockMoves"
private const val keyAllowUndo = "allowUndo"
private const val keyMaxMovesToStore = "maxMovesToStore"
const val keyCurrentGameId = "current"

/** Game options / tweaks. Default values are for the original game,
see https://en.wikipedia.org/wiki/2048_(video_game)
and for the game in browser: https://play2048.co/
For now you can modify settings in the "game.storage" file. */
class Settings(private val stage: Stage) {
    val multithreadedScope: CoroutineScope
        get() = CoroutineScope(stage.coroutineContext + Job() + Dispatchers.Default)
    val storage: MyStorage = MyStorage.load(stage)
    val keyColorTheme = "colorTheme"
    val keyAiAlgorithm = "aiAlgorithm"

    // false: The resulting tile cannot merge with another tile again in the same move
    var allowResultingTileToMerge = storage.getBoolean(keyAllowResultingTileToMerge, false)
    var allowUsersMoveWithoutBlockMoves = storage.getBoolean(keyAllowUsersMoveWithoutBlockMoves,false)
    var allowUndo = storage.getBoolean(keyAllowUndo,true)
    var boardWidth = 4
    var boardHeight = boardWidth
    var colorThemeEnum: ColorThemeEnum = ColorThemeEnum.load(storage.getOrNull(keyColorTheme))
    var aiAlgorithm: AiAlgorithm = AiAlgorithm.load(storage.getOrNull(keyAiAlgorithm))
    var defaultBoard = Board(this)
    val isTestRun = "TestGameWindow" == stage.views.gameWindow::class.simpleName
    val pliesPageSize = 1000

    companion object {
        fun load(stage: Stage): Settings = myMeasured("Settings loaded") { Settings(stage) }
    }

    fun save() {
        storage.native.setBoolean(keyAllowResultingTileToMerge, allowResultingTileToMerge)
                .setBoolean(keyAllowUsersMoveWithoutBlockMoves, allowUsersMoveWithoutBlockMoves)
                .setBoolean(keyAllowUndo, allowUndo)
                .set(keyColorTheme, colorThemeEnum.labelKey)
        storage.native.set(keyAiAlgorithm, aiAlgorithm.id)
    }

    private fun NativeStorage.setBoolean(key: String, value: Boolean): NativeStorage {
        set(key, if (value) "true" else "false")
        return this
    }

    val currentGameId: Int?
        get() = storage.getOrNull(keyCurrentGameId)
            ?.let {
                // TODO: for compatibility with previous versions:
                if (it.startsWith("{"))
                    ShortRecord.sharedJsonToId(it, 1)
                else it.parseInt()
            }

}

suspend fun loadFont(strings: StringResources) = myMeasured("Font loaded") {
    val fontFolder = when(strings.lang) {
        "zh" -> "noto_sans_sc"
        "si" -> "abhaya_libre"
        else -> "clear_sans"
    }
    resourcesVfs["assets/fonts/$fontFolder/font.fnt"].readBitmapFont()
}
