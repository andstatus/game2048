import com.soywiz.korge.tests.ViewsForTesting
import org.andstatus.game2048.view.StringResources
import kotlin.test.Test
import kotlin.test.assertEquals

class ResourceStringsTest : ViewsForTesting(log = true) {

    @Test
    fun resourceStringsTest() = viewsTest {
        testInitializeGameView() {
            val ru = StringResources.load("ru")
            assertEquals("ЛУЧШИЙ", ru.text("best"))
            val zh = StringResources.load("zh-rCN")
            assertEquals("最高", zh.text("best"))
        }
    }

}