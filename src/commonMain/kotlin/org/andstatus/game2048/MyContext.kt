package org.andstatus.game2048

import korlibs.image.font.BitmapFont
import korlibs.image.font.readBitmapFont
import korlibs.io.concurrent.atomic.KorAtomicRef
import korlibs.io.file.std.resourcesVfs
import korlibs.io.lang.parseInt
import korlibs.korge.view.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.andstatus.game2048.model.PliesPageData
import org.andstatus.game2048.view.StringResources

class MyContext(private val stage: Stage) {
    val multithreadedScope: CoroutineScope
        get() = CoroutineScope(
            stage.coroutineContext + Job()
                + Dispatchers.Default
        )
    val storage: MyStorage = MyStorage.load(stage)
    private val settingsRef: KorAtomicRef<Settings> = initAtomicReference(
        Settings.load(storage)
    )
    val settings: Settings get() = settingsRef.value

    val pliesPageData = PliesPageData(this)

    fun update(updater: (Settings) -> Settings): Unit {
        settingsRef.update(updater).also {
            it.save(storage)
        }
    }

    companion object {
        fun load(stage: Stage): MyContext = myMeasured("Settings loaded") { MyContext(stage) }
    }

    fun save() {
        settings.save(storage)
    }

    val currentGameId: Int?
        get() = storage.getOrNull(keyCurrentGameId)?.parseInt()
}

suspend fun loadFont(strings: StringResources): BitmapFont = myMeasured("Font loaded") {
    val fontFolder = when (strings.lang) {
        "zh" -> "noto_sans_sc"
        "si" -> "abhaya_libre"
        else -> "clear_sans"
    }
    resourcesVfs["assets/fonts/$fontFolder/font.fnt"].readBitmapFont()
}
