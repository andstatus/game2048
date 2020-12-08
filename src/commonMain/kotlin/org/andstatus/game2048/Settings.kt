package org.andstatus.game2048

import com.soywiz.klock.DateTime
import com.soywiz.korge.service.storage.NativeStorage
import com.soywiz.korge.view.Stage
import com.soywiz.korio.util.OS
import org.andstatus.game2048.view.ColorThemeEnum

private const val keyAllowResultingTileToMerge = "allowResultingTileToMerge"
private const val keyAllowUsersMoveWithoutBlockMoves = "allowUsersMoveWithoutBlockMoves"
private const val keyAllowUndo = "allowUndo"

/** Game options / tweaks. Default values are for original game,
see https://en.wikipedia.org/wiki/2048_(video_game)
and the game in browser: https://play2048.co/
For now you can modify settings in the "game.storage" file. */
class Settings(val storage: NativeStorage) {
    val keyColorTheme = "colorTheme"

    // false: The resulting tile cannot merge with another tile again in the same move
    var allowResultingTileToMerge = false
    var allowUsersMoveWithoutBlockMoves = false
    var allowUndo = true  // Default == false
    var boardWidth = 4
    var boardHeight = boardWidth
    var colorTheme: ColorThemeEnum = ColorThemeEnum.DEVICE_DEFAULT

    fun save() {
        storage.setBoolean(keyAllowResultingTileToMerge, allowResultingTileToMerge)
                .setBoolean(keyAllowUsersMoveWithoutBlockMoves, allowUsersMoveWithoutBlockMoves)
                .setBoolean(keyAllowUndo, allowUndo)
                .set(keyColorTheme, colorTheme.labelKey)
    }

    private fun NativeStorage.setBoolean(key: String, value: Boolean): NativeStorage {
        set(key, if (value) "true" else "false")
        return this
    }
}

fun loadSettings(stage: Stage): Settings = myMeasured("Settings loaded") {
    getStorage(stage).let { storage ->
        Settings(storage).apply {
            storage.getOrNull(keyAllowResultingTileToMerge)?.let{
                allowResultingTileToMerge = it.toBoolean()
            } ?: run {
                save()
                return@apply
            }
            storage.getOrNull(keyAllowUsersMoveWithoutBlockMoves)?.let{
                allowUsersMoveWithoutBlockMoves = it.toBoolean()
            }
            storage.getOrNull(keyAllowUndo)?.let{
                allowUndo = it.toBoolean()
            }
            colorTheme = ColorThemeEnum.load(storage.getOrNull(keyColorTheme))
        }
    }
}

fun getStorage(stage: Stage): NativeStorage {
    val storage = NativeStorage(stage.views)
    val keyOpened = "opened"

    myLog("Platform:${OS.platformName}, \nStorage " +
            (storage.getOrNull(keyOpened)?.let { "last opened: $it" } ?: "is new") +
            "\nStorage keys: ${storage.keys().sorted()}"
    )
    storage[keyOpened] = DateTime.now().toString()
    return storage
}