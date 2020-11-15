package org.andstatus.game2048

import android.app.Activity
import android.content.Intent
import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korio.lang.substr
import org.andstatus.game2048.MainActivity.Companion.mainActivity
import org.andstatus.game2048.data.FileProvider
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

const val platformSourceFolder = "androidMain"

actual val defaultLanguage: String get() = java.util.Locale.getDefault().language

actual fun shareText(actionTitle: String, fileName: String, value: String) {
    Console.log("$platformSourceFolder, shareText '$fileName' (${value.length} bytes): ${value.substr(0, 500)}...")
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
        FileOutputStream(file).use {fileOutputStream ->
            BufferedWriter(OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)).use {out ->
                out.write(value)
            }
        }
    } catch (e: Exception) {
        Console.log("Error saving ${file.absoluteFile}: ${e.message}")
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

actual fun loadJsonGameRecord(consumer: (String) -> Unit) {
    Console.log("$platformSourceFolder, loadJsonGameRecord")
    mainActivity?.openJsonGameRecord(consumer)
}