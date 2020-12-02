package org.andstatus.game2048

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.soywiz.korgw.KorgwActivity
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class MainActivity : KorgwActivity() {
	private val REQUEST_CODE_OPEN_JSON_GAME = 1
	private var gameRecordConsumer: ((String) -> Unit)? = null

	override suspend fun activityMain() {
		myLog("activityMain started")
        main()
		myLog("activityMain ended")
	}

	override fun finish() {
		myLog("activityMain finish")
		super.finish()
	}

	fun openJsonGameRecord(consumer: (String) -> Unit) {
		gameRecordConsumer = consumer
		val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
			type = "*/*"
		}
		startActivityForResult(intent, REQUEST_CODE_OPEN_JSON_GAME)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		myLog("Got result $resultCode on request $requestCode")
		if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_OPEN_JSON_GAME && gameRecordConsumer != null) {
			data?.data?.let { uri ->
				documentUri2String(this, uri)?.let { gameRecordConsumer?.invoke(it) }
				gameRecordConsumer = null
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data)
		}
	}

	private fun documentUri2String(context: Context, uri: Uri): String? {
		val BUFFER_LENGTH = 10000
		try {
			context.contentResolver.openInputStream(uri).use { inputStream ->
				InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
					val buffer = CharArray(BUFFER_LENGTH)
					val builder = StringBuilder()
					var count: Int
					while (reader.read(buffer).also { count = it } != -1) {
						builder.append(buffer, 0, count)
					}
					return builder.toString()
				}
			}
		} catch (e: Exception) {
			myLog("Error while reading $uri: ${e.message}")
		}
		return null
	}
}