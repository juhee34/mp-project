package com.tinyrunners.school.talk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class TalkPlanTest {
    private val full = listOf(TalkLine(TalkScript.NARRATOR, "아침"), TalkLine("mint", "안녕"))
    private val pick: (String) -> String? = { event -> "line:$event" }

    @Test fun firstStartIsTheLongTalkLaterStartsAreShort() {
        assertEquals(full, TalkPlan.lines(TalkMode.START, "mint", firstTime = true, full, null, pick))
        val later = TalkPlan.lines(TalkMode.START, "mint", firstTime = false, full, null, pick)
        assertEquals(listOf(TalkLine("mint", "line:select"), TalkLine("mint", "line:start")), later)
    }

    @Test fun retryAndStumbleUseTheirOwnLines() {
        assertEquals(listOf(TalkLine("mint", "line:retry")), TalkPlan.lines(TalkMode.RETRY, "mint", false, full, null, pick))
        assertEquals(listOf(TalkLine(TalkScript.NARRATOR, "쿵!"), TalkLine("mint", "line:stumble")),
            TalkPlan.lines(TalkMode.STUMBLE, "mint", false, full, "쿵!", pick))
    }

    @Test fun replayShowsTheLongTalkAgain() {
        assertEquals(full, TalkPlan.lines(TalkMode.REPLAY, "mint", firstTime = false, full, null, pick))
    }

    @Test fun missingLinesAreSkippedNotShownEmpty() {
        assertEquals(emptyList<TalkLine>(), TalkPlan.lines(TalkMode.RETRY, "mint", false, full, null) { null })
    }

    @Test fun deckShowsAllThreeBeforeRepeatingAndNeverTheSameTwiceInARow() {
        val all = ShortLineParser.parse(File("src/main/assets/content/short_lines.json").readText())
        val deck = LineDeck(Random(7))
        var previous: String? = null
        repeat(5) {
            val round = List(3) { deck.draw(all, "lumi", "stumble")!!.id }
            assertEquals(3, round.toSet().size)
            assertNotEquals(previous, round.first())
            previous = round.last()
        }
    }

    @Test fun realPackHasLinesForEveryFriendAndRunEvent() {
        val all = ShortLineParser.parse(File("src/main/assets/content/short_lines.json").readText())
        for (id in listOf("mint", "cheese", "popo", "lumi", "nunu", "piko")) for (event in listOf("select", "start", "retry", "stumble")) {
            assertEquals("$id/$event", 3, all.count { it.characterId == id && it.event == event })
        }
        assertTrue(all.all { it.text.isNotBlank() })
    }
}
