package org.andstatus.game2048
import com.soywiz.korio.android.withAndroidContext
import com.soywiz.korgw.KorgwActivity
import org.andstatus.game2048.main
class MainActivity : KorgwActivity() {
	override suspend fun activityMain() {
		org.andstatus.game2048.main()
	}
}
