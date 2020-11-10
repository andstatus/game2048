package org.andstatus.game2048
import android.app.Activity
import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korgw.KorgwActivity

class MainActivity : KorgwActivity() {
	override suspend fun activityMain() {
		mainActivity = this
		main()
		Console.log("game2048.main ended")
		mainActivity = null
		finish()
	}

	companion object {
		var mainActivity: Activity? = null
	}
}