package org.andstatus.game2048

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.WindowManager
import korlibs.io.android.AndroidCoroutineContext
import korlibs.io.concurrent.atomic.KorAtomicRef
import korlibs.io.concurrent.atomic.korAtomic
import korlibs.korge.view.Stage
import korlibs.math.geom.SizeInt
import org.andstatus.game2048.data.FileProvider
import org.andstatus.game2048.presenter.Presenter
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import kotlin.coroutines.CoroutineContext

private const val platformSourceFolder = "androidMain"

actual val CoroutineContext.gameWindowSize: SizeInt
    get() =
        mainActivity?.let { context ->
            val metrics = DisplayMetrics()
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            return SizeInt(metrics.widthPixels, metrics.heightPixels)
        } ?: defaultPortraitGameWindowSize

private val Stage.mainActivity: MyMainActivity?
    get() =
        coroutineContext.mainActivity

private val CoroutineContext.mainActivity: MyMainActivity?
    get() =
        get(AndroidCoroutineContext.Key)?.context as MyMainActivity?

actual val CoroutineContext.isDarkThemeOn: Boolean
    get() = mainActivity?.let { context ->
        val configuration = context.applicationContext.resources.configuration
        val currentNightMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    } ?: false

actual val defaultLanguage: String get() = java.util.Locale.getDefault().language

actual fun Presenter.shareText(actionTitle: String, fileName: String, value: Sequence<String>) {
    myLog("$platformSourceFolder, shareText '$fileName'")
    view.gameStage.mainActivity?.let { context ->
        val file = File(context.cacheDir, fileName)
        try {
            FileOutputStream(file).use { fileOutputStream ->
                var lineNumber = 0
                var charsWritten = 0
                BufferedWriter(OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)).use { out ->
                    value.forEach {
                        lineNumber++
                        myLog("writing line $lineNumber, ${it.length} chars")
                        out.write(it)
                        out.write("\n")
                        charsWritten += it.length
                    }
                }
                myLog("shareText '$fileName' completed, $lineNumber lines written, $charsWritten chars + line breaks")
            }
        } catch (e: Exception) {
            myLog("Error saving ${file.absoluteFile}: ${e.message}")
            return
        }

        Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension.startsWith("json")) "application/json" else "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            putExtra(Intent.EXTRA_STREAM, FileProvider.cachedFilenameToUri(fileName))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { intent ->
            context.startActivity(Intent.createChooser(intent, actionTitle))
        }
    }
}

actual fun Stage.loadJsonGameRecord(myContext: MyContext, sharedJsonHandler: (Sequence<String>) -> Unit) {
    myLog("$platformSourceFolder, loadJsonGameRecord")
    mainActivity?.openJsonGameRecord(sharedJsonHandler)
}

actual fun Stage.exitApp() = mainActivity?.finish() ?: Unit

actual fun <T> initAtomicReference(initial: T): KorAtomicRef<T> = korAtomic(initial)

actual fun <T> KorAtomicRef<T>.compareAndSetFixed(expect: T, update: T): Boolean = compareAndSet(expect, update)
