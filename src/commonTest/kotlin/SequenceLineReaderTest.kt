import korlibs.io.serialization.json.Json
import org.andstatus.game2048.model.SequenceLineReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SequenceLineReaderTest {
    val header1 =
        "{\"note\":\"\",\"start\":\"2022-03-04T21:30:43+03:00\",\"finalBoard\":{\"plyNumber\":1,\"pieces\":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"score\":0,\"time\":\"2022-03-04T21:30:43+03:00\",\"playedSeconds\":0},\"bookmarks\":[],\"id\":33,\"type\":\"org.andstatus.game2048:GameRecord:2\"}"
    val plies1 = "{\"seconds\":0,\"moves\":[{\"move\":\"place\",\"first\":{\"piece\":1,\"square\":{\"x\":1,\"y\":3}}}]}"

    @Test
    fun oneLineOneSequenceElement() {
        assertOneLine(sequenceOf(header1 + plies1))
        assertTwoJsons(sequenceOf(header1 + plies1))
    }

    @Test
    fun oneLineTwoSequenceElements() {
        assertOneLine(sequenceOf(header1, plies1))
        assertTwoJsons(sequenceOf(header1, plies1))
    }

    private fun assertOneLine(strSequence: Sequence<String>) {
        val reader = SequenceLineReader(strSequence)
        assertEquals(header1 + plies1, reader.readLine())
        assertFalse(reader.hasMore)
        assertNull(reader.readLine())
    }

    @Test
    fun twoLinesOneSequenceElement() {
        assertTwoLines(sequenceOf(header1 + "\n" + plies1))
        assertTwoJsons(sequenceOf(header1 + "\n" + plies1))
        assertTwoLines(sequenceOf(header1 + "\r\n" + plies1))
        assertTwoJsons(sequenceOf(header1 + "\r\n" + plies1))
        assertTwoLines(sequenceOf(header1 + "\r\n" + plies1 + "\n"))
        assertTwoJsons(sequenceOf(header1 + "\r\n" + plies1 + "\n"))
    }

    @Test
    fun twoLinesTwoSequenceElements() {
        assertTwoLines(sequenceOf(header1 + "\n", plies1))
        assertTwoJsons(sequenceOf(header1 + "\n", plies1))
        assertTwoLines(sequenceOf(header1, "\n" + plies1))
        assertTwoJsons(sequenceOf(header1, "\n" + plies1))
        assertTwoLines(sequenceOf(header1 + "\r\n", plies1))
        assertTwoJsons(sequenceOf(header1 + "\r\n", plies1))
        assertTwoLines(sequenceOf(header1 + "\r", "\n" + plies1))
        assertTwoJsons(sequenceOf(header1 + "\r" + "\n" + plies1))
        assertTwoLines(sequenceOf(header1, "\r\n" + plies1))
        assertTwoJsons(sequenceOf(header1, "\r\n" + plies1))
    }

    @Test
    fun twoLinesThreeSequenceElements() {
        assertTwoLines(sequenceOf(header1 + "\n", plies1, "\r"))
        assertTwoJsons(sequenceOf(header1 + "\n", plies1, "\r"))
        assertTwoLines(sequenceOf(header1, "\n" + plies1, "  \n"))
        assertTwoJsons(sequenceOf(header1, "\n" + plies1, "  \n"))
        assertTwoLines(sequenceOf(header1 + "\r\n", plies1, "  \n  "))
        assertTwoJsons(sequenceOf(header1 + "\r\n", plies1, "  \n  "))
        assertTwoLines(sequenceOf(header1 + "\r", "\n" + plies1, "  "))
        assertTwoJsons(sequenceOf(header1 + "\r" + "\n" + plies1, "  "))
        assertTwoLines(sequenceOf(header1, "\r\n" + plies1, " \n  \n "))
        assertTwoJsons(sequenceOf(header1, "\r\n" + plies1, " \n  \n "))
    }

    private fun assertTwoLines(strSequence: Sequence<String>) {
        val reader = SequenceLineReader(strSequence)
        assertEquals(header1, reader.readLine())
        assertTrue(reader.hasMore)
        reader.unRead()
        assertEquals(header1, reader.readLine())
        assertTrue(reader.hasMore)
        assertEquals(plies1, reader.readLine())
        assertNull(reader.readLine())
    }

    private fun assertTwoJsons(strSequence: Sequence<String>) {
        val reader = SequenceLineReader(strSequence)
        assertEquals(Json.parse(header1), reader.readNext(Json::parse).getOrNull())
        assertTrue(reader.hasMore)
        assertEquals(Json.parse(plies1), reader.readNext(Json::parse).getOrNull())
        reader.unRead()
        assertEquals(Json.parse(plies1), reader.readNext(Json::parse).getOrNull())
    }

}