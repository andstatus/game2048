import com.soywiz.korge.tests.ViewsForTesting
import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.History
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseGameRecordTest: ViewsForTesting(log = true) {

    private val gameRecordSingleJson = """{"note":"","start":"2021-11-29T17:54:51+03:00","finalBoard":{"moveNumber":11,"pieces":[0,0,0,3,0,0,0,0,1,0,0,0,0,0,0,1],"score":8,"time":"2021-11-29T17:55:04+03:00","playedSeconds":17},"bookmarks":[{"moveNumber":9,"pieces":[0,0,2,2,0,0,0,0,0,0,0,0,1,0,0,0],"score":4,"time":"2021-11-29T17:55:04+03:00","playedSeconds":10}],"id":17,"type":"org.andstatus.game2048:GameRecord:1","playersMoves":[{"playerEnum":"computer","moveEnum":"place","seconds":0,"moves":[{"moveName":"place","first":{"piece":1,"square":{"x":3,"y":1}}}]},{"playerEnum":"user","moveEnum":"down","seconds":0,"moves":[{"moveName":"one","first":{"piece":1,"square":{"x":3,"y":1}},"destination":{"x":3,"y":3}}]},{"playerEnum":"computer","moveEnum":"place","seconds":0,"moves":[{"moveName":"place","first":{"piece":1,"square":{"x":1,"y":2}}}]},{"playerEnum":"user","moveEnum":"right","seconds":1,"moves":[{"moveName":"one","first":{"piece":1,"square":{"x":1,"y":2}},"destination":{"x":3,"y":2}}]},{"playerEnum":"computer","moveEnum":"place","seconds":1,"moves":[{"moveName":"place","first":{"piece":1,"square":{"x":2,"y":1}}}]},{"playerEnum":"user","moveEnum":"up","seconds":3,"moves":[{"moveName":"one","first":{"piece":1,"square":{"x":2,"y":1}},"destination":{"x":2,"y":0}},{"moveName":"merge","first":{"piece":1,"square":{"x":3,"y":2}},"second":{"piece":1,"square":{"x":3,"y":3}},"merged":{"piece":2,"square":{"x":3,"y":0}}}]},{"playerEnum":"computer","moveEnum":"place","seconds":3,"moves":[{"moveName":"place","first":{"piece":1,"square":{"x":1,"y":0}}}]},{"playerEnum":"user","moveEnum":"right","seconds":8,"moves":[{"moveName":"merge","first":{"piece":1,"square":{"x":2,"y":0}},"second":{"piece":1,"square":{"x":1,"y":0}},"merged":{"piece":2,"square":{"x":2,"y":0}}}]},{"playerEnum":"computer","moveEnum":"place","seconds":8,"moves":[{"moveName":"place","first":{"piece":1,"square":{"x":0,"y":3}}}]},{"playerEnum":"user","moveEnum":"right","seconds":10,"moves":[{"moveName":"one","first":{"piece":1,"square":{"x":0,"y":3}},"destination":{"x":3,"y":3}},{"moveName":"merge","first":{"piece":2,"square":{"x":3,"y":0}},"second":{"piece":2,"square":{"x":2,"y":0}},"merged":{"piece":3,"square":{"x":3,"y":0}}}]},{"playerEnum":"computer","moveEnum":"place","seconds":10,"moves":[{"moveName":"place","first":{"piece":1,"square":{"x":0,"y":2}}}]}]}"""
    private val gameRecordConcatenatedJson = """{"note":"","start":"2021-11-29T17:54:51+03:00","finalBoard":{"moveNumber":11,"pieces":[0,0,0,3,0,0,0,0,1,0,0,0,0,0,0,1],"score":8,"time":"2021-11-29T17:55:04+03:00","playedSeconds":17},"bookmarks":[{"moveNumber":9,"pieces":[0,0,2,2,0,0,0,0,0,0,0,0,1,0,0,0],"score":4,"time":"2021-11-29T17:55:04+03:00","playedSeconds":10}],"id":17,"type":"org.andstatus.game2048:GameRecord:1"}{"playerEnum":"computer","moveEnum":"place","seconds":0,"moves":[{"moveName":"place","first":{"piece":1,"square":{"x":3,"y":1}}}]}{"playerEnum":"user","moveEnum":"down","seconds":0,"moves":[{"moveName":"one","first":{"piece":1,"square":{"x":3,"y":1}},"destination":{"x":3,"y":3}}]}{"playerEnum":"computer","moveEnum":"place","seconds":0,"moves":[{"moveName":"place","first":{"piece":1,"square":{"x":1,"y":2}}}]}{"playerEnum":"user","moveEnum":"right","seconds":1,"moves":[{"moveName":"one","first":{"piece":1,"square":{"x":1,"y":2}},"destination":{"x":3,"y":2}}]}{"playerEnum":"computer","moveEnum":"place","seconds":1,"moves":[{"moveName":"place","first":{"piece":1,"square":{"x":2,"y":1}}}]}{"playerEnum":"user","moveEnum":"up","seconds":3,"moves":[{"moveName":"one","first":{"piece":1,"square":{"x":2,"y":1}},"destination":{"x":2,"y":0}},{"moveName":"merge","first":{"piece":1,"square":{"x":3,"y":2}},"second":{"piece":1,"square":{"x":3,"y":3}},"merged":{"piece":2,"square":{"x":3,"y":0}}}]}{"playerEnum":"computer","moveEnum":"place","seconds":3,"moves":[{"moveName":"place","first":{"piece":1,"square":{"x":1,"y":0}}}]}{"playerEnum":"user","moveEnum":"right","seconds":8,"moves":[{"moveName":"merge","first":{"piece":1,"square":{"x":2,"y":0}},"second":{"piece":1,"square":{"x":1,"y":0}},"merged":{"piece":2,"square":{"x":2,"y":0}}}]}{"playerEnum":"computer","moveEnum":"place","seconds":8,"moves":[{"moveName":"place","first":{"piece":1,"square":{"x":0,"y":3}}}]}{"playerEnum":"user","moveEnum":"right","seconds":10,"moves":[{"moveName":"one","first":{"piece":1,"square":{"x":0,"y":3}},"destination":{"x":3,"y":3}},{"moveName":"merge","first":{"piece":2,"square":{"x":3,"y":0}},"second":{"piece":2,"square":{"x":2,"y":0}},"merged":{"piece":3,"square":{"x":3,"y":0}}}]}{"playerEnum":"computer","moveEnum":"place","seconds":10,"moves":[{"moveName":"place","first":{"piece":1,"square":{"x":0,"y":2}}}]}"""

    @Test
    fun parseGameRecordTest() = viewsTest {
        unsetGameView()
        val settings = Settings.load(stage)
        testOneString(settings, gameRecordSingleJson)
        testOneString(settings, gameRecordConcatenatedJson)
    }

    private suspend fun testOneString(settings: Settings, gameRecordString: String) {
        val gameId = History.load(settings).idForNewGame()
        settings.storage.set("game$gameId", gameRecordString)

        val history = History.load(settings)
        history.restoreGame(gameId)
        assertEquals(8, history.currentGame.score, "Score of: ${history.currentGame}")
        assertEquals(1, history.currentGame.shortRecord.bookmarks.size, "Bookmarks in: ${history.currentGame}")
        assertEquals(11, history.currentGame.plies.size, "Plies stored in: ${history.currentGame}")
    }

}