import com.soywiz.korge.tests.ViewsForTesting
import org.andstatus.game2048.myLog
import org.andstatus.game2048.view.StringResources
import org.andstatus.game2048.view.StringResources.Companion.existingLangCodes
import kotlin.test.Test
import kotlin.test.assertEquals

class ResourceStringsTest : ViewsForTesting(log = true) {

    @Test
    fun resourceStringsTest() = viewsTest {
        testInitializeGameView() {
            testLanguageFallback()
            collectCharacterCodes()
        }
    }

    private suspend fun testLanguageFallback() {
        val ru = StringResources.load("ru")
        assertEquals("ЛУЧШИЙ", ru.text("best"))
        val zh = StringResources.load("zh-rCN")
        assertEquals("最高", zh.text("best"))
    }

    private suspend fun collectCharacterCodes() {
        val initial = mutableSetOf<Int>()
        addCodePoints(initial, "0123456789: «»-")
        val characterCodes = existingLangCodes.fold(initial) { acc, lang ->
            val acc2 = mutableSetOf<Int>()
            acc2.addAll(acc)
            val strings = StringResources.load(lang)
            strings.strings.values.forEach { str ->
                addCodePoints(acc2, str)
            }
            acc2
        }.toList().sorted()

        var charsString: String = ""
        val oneLine: String = characterCodes.fold("") { acc, codePoint ->
            val acc2 = if (acc.isEmpty()) codePoint.toString() else "$acc,$codePoint"
            if (acc2.length > 114) {
                charsString += "\nchars=$acc2"
                ""
            } else {
                acc2
            }
        }
        charsString += "\nchars=$oneLine"
        myLog("Total chars: ${characterCodes.size}$charsString\n--- end")
    }

    private fun addCodePoints(acc: MutableSet<Int>, str: String) {
        (0 until str.length).forEach {
            acc.add(str[it].toInt())
        }
    }
}