package org.andstatus.game2048

import android.content.Intent
import android.os.Bundle
import com.soywiz.korgw.KorgwActivity

class MainActivity : KorgwActivity() {
    private val REQUEST_CODE_OPEN_JSON_GAME = 1
    private var gameRecordConsumer: ((Sequence<String>) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        myLog("onCreate MainActivity")
        super.onCreate(savedInstanceState)
    }

    override suspend fun activityMain() {
        myLog("activityMain started")
        main()
        myLog("activityMain ended")
    }

    override fun finish() {
        myLog("activityMain finish")
        super.finish()
    }

    fun openJsonGameRecord(consumer: (Sequence<String>) -> Unit) {
        gameRecordConsumer = consumer
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
        }
        startActivityForResult(intent, REQUEST_CODE_OPEN_JSON_GAME)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        myLog("Got result $resultCode on request $requestCode")
        val consumer = gameRecordConsumer
        gameRecordConsumer = null
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_OPEN_JSON_GAME && consumer != null) {
            data?.data?.let { uri ->
                DocumentSequence(this, uri)
                    .let { strValue -> consumer.invoke(strValue) }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

}
