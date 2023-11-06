import korlibs.korge.tests.ViewsForTesting
import org.andstatus.game2048.ai.AiAlgorithm
import org.andstatus.game2048.ai.AiPlayer
import kotlin.test.Test
import kotlin.test.assertTrue

class AiPlayerTest : ViewsForTesting(log = true) {

    @Test
    fun aiPlayerTest() = myViewsTest(this) {
        val storedAiAlgorithm = presenter.model.settings.aiAlgorithm
        presenter.model.settings.aiAlgorithm = AiAlgorithm.LONGEST_RANDOM_PLAY
        val expectedPliesCount = 15
        generateGame(expectedPliesCount)

        val position1 = presenter.model.gamePosition.copy()
        val aiPlayer = AiPlayer(settings)
        val result1 = aiPlayer.calcNextPly(position1)
        assertTrue(result1.isSuccess, result1.toString())
        result1.onSuccess {aiResult ->
            assertTrue(aiResult.maxPosition.score > 100, aiResult.toString() +
                ", maxPosition: " + aiResult.maxPosition.toString())
        }

        presenter.model.settings.aiAlgorithm = storedAiAlgorithm
    }
}