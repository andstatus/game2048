package org.andstatus.game2048

import com.soywiz.klock.DateTime
import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korge.service.storage.NativeStorage
import com.soywiz.korge.view.Stage
import com.soywiz.korio.util.OS
import kotlin.properties.Delegates

// Game options / tweaks. Default values are for original game,
// see https://en.wikipedia.org/wiki/2048_(video_game)
// and the game in browser: https://play2048.co/
// For now you can modify settings in the "game.storage" file.
var settings: Settings by Delegates.notNull()

private const val keyAllowResultingTileToMerge = "allowResultingTileToMerge"
private const val keyAllowUsersMoveWithoutBlockMoves = "allowUsersMoveWithoutBlockMoves"
private const val keyAllowUndo = "allowUndo"

class Settings(val storage: NativeStorage) {
    // false: The resulting tile cannot merge with another tile again in the same move
    var allowResultingTileToMerge = false
    var allowUsersMoveWithoutBlockMoves = false
    var allowUndo = true  // Default == false
    var boardWidth = 4
    var boardHeight = boardWidth

    fun save(storage: NativeStorage) {
        storage.setBoolean(keyAllowResultingTileToMerge, allowResultingTileToMerge)
                .setBoolean(keyAllowUsersMoveWithoutBlockMoves, allowUsersMoveWithoutBlockMoves)
                .setBoolean(keyAllowUndo, allowUndo)
    }

    private fun NativeStorage.setBoolean(key: String, value: Boolean): NativeStorage {
        set(key, if (value) "true" else "false")
        return this
    }
}

fun loadSettings(stage: Stage) {
    val storage = getStorage(stage)
    settings = Settings(storage).apply {
        storage.getOrNull(keyAllowResultingTileToMerge)?.let{
            allowResultingTileToMerge = it.toBoolean()
        } ?: run {
            save(storage)
            return@apply
        }
        storage.getOrNull(keyAllowUsersMoveWithoutBlockMoves)?.let{
            allowUsersMoveWithoutBlockMoves = it.toBoolean()
        }
        storage.getOrNull(keyAllowUndo)?.let{
            allowUndo = it.toBoolean()
        }
    }
}

fun getStorage(stage: Stage): NativeStorage {
    val storage = NativeStorage(stage.views)
    val keyOpened = "opened"

    Console.log("Platform:${OS.platformName}, \nStorage " +
            (storage.getOrNull(keyOpened)?.let { "last opened: $it" } ?: "is new") +
            "\nStorage keys: ${storage.keys().sorted()}"
    )
    storage[keyOpened] = DateTime.now().toString()
    return storage
}