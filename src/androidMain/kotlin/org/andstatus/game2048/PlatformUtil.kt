package org.andstatus.game2048

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.WindowManager
import com.soywiz.korge.view.Stage
import com.soywiz.korio.android.AndroidCoroutineContext
import com.soywiz.korio.lang.substr
import com.soywiz.korma.geom.SizeInt
import org.andstatus.game2048.data.FileProvider
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import kotlin.coroutines.CoroutineContext

const val platformSourceFolder = "androidMain"

actual val CoroutineContext.gameWindowSize: SizeInt get() =
    mainActivity?.let { context ->
        val metrics = DisplayMetrics()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        return SizeInt(metrics.widthPixels, metrics.heightPixels)
    } ?: defaultPortraitGameWindowSize

private val Stage.mainActivity: MainActivity? get()=
    coroutineContext.mainActivity

private val CoroutineContext.mainActivity: MainActivity? get()=
    get(AndroidCoroutineContext.Key)?.context as MainActivity?

actual val CoroutineContext.isDarkThemeOn: Boolean get() = mainActivity?.let { context ->
    val configuration = context.applicationContext.resources.configuration
    val currentNightMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES
} ?: false

actual val defaultLanguage: String get() = java.util.Locale.getDefault().language

actual fun Stage.shareText(actionTitle: String, fileName: String, value: String) {
    myLog("$platformSourceFolder, shareText '$fileName' (${value.length} bytes): ${value.substr(0, 500)}...")
    mainActivity?.let { context ->
        if (value.length < 100000) {
            shareShortText(context, actionTitle, fileName, value)
        } else {
            shareLongText(context, actionTitle, fileName, value)
        }
    }
}

private fun shareShortText(context: Activity, actionTitle: String, fileName: String, value: String) {
    Intent(Intent.ACTION_SEND).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        putExtra(Intent.EXTRA_TEXT, value)
    }.let { intent ->
        context.startActivity(Intent.createChooser(intent, actionTitle))
    }
}

private fun shareLongText(context: Activity, actionTitle: String, fileName: String, value: String) {
    val file = File(context.cacheDir, fileName)
    try {
        FileOutputStream(file).use { fileOutputStream ->
            BufferedWriter(OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)).use { out ->
                out.write(value)
            }
        }
    } catch (e: Exception) {
        myLog("Error saving ${file.absoluteFile}: ${e.message}")
        return
    }

    Intent(Intent.ACTION_SEND).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        putExtra(Intent.EXTRA_STREAM, FileProvider.cachedFilenameToUri(fileName))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }.let { intent ->
        context.startActivity(Intent.createChooser(intent, actionTitle))
    }
}

actual fun Stage.loadJsonGameRecord(consumer: (String) -> Unit) {
    myLog("$platformSourceFolder, loadJsonGameRecord")
    mainActivity?.openJsonGameRecord(consumer)
}

actual fun Stage.closeGameApp() = mainActivity?.finish() ?: Unit