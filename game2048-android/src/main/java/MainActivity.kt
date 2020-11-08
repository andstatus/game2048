package org.andstatus.game2048
import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korgw.KorgwActivity

class MainActivity : KorgwActivity() {
	override suspend fun activityMain() {
		org.andstatus.game2048.main()
		Console.log("game2048.main ended")
		finish()
	}
}
