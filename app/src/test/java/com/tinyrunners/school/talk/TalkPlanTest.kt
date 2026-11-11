package com.tinyrunners.school.talk

import com.tinyrunners.school.story.Option
import com.tinyrunners.school.story.Step
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
        assertEquals(full, TalkPlan.lines(TalkMode.START, "mint", firstTime = true, full, null, pick = pick))
        val later = TalkPlan.lines(TalkMode.START, "mint", firstTime = false, full, null, pick = pick)
        assertEquals(listOf(TalkLine("mint", "line:select"), TalkLine("mint", "line:start")), later)
    }

    @Test fun retryAndStumbleUseTheirOwnLines() {
        assertEquals(listOf(TalkLine("mint", "line:retry")), TalkPlan.lines(TalkMode.RETRY, "mint", false, full, null, pick = pick))
        assertEquals(listOf(TalkLine(TalkScript.NARRATOR, "쿵!"), TalkLine("mint", "line:stumble")),
            TalkPlan.lines(TalkMode.STUMBLE, "mint", false, full, "쿵!", pick = pick))
    }

    @Test fun replayShowsTheLongTalkAgain() {
        assertEquals(full, TalkPlan.lines(TalkMode.REPLAY, "mint", firstTime = false, full, null, pick = pick))
    }

    @Test fun villageTalkIsGreetingThenStoryThenGoodbye() {
        val lines = TalkPlan.lines(TalkMode.VILLAGE, "cheese", false, full, null, storyOutline = "치즈는 햇볕을 모은다. 통은 비어 있다! 그래도 웃는다?", pick = pick)
        assertEquals(TalkLine("cheese", "line:select"), lines.first())
        assertEquals(TalkLine("cheese", "line:home"), lines.last())
        assertEquals(listOf("치즈는 햇볕을 모은다.", "통은 비어 있다!", "그래도 웃는다?"),
            lines.drop(1).dropLast(1).map { it.text })
        assertTrue(lines.drop(1).dropLast(1).all { it.speaker == TalkScript.NARRATOR })
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

    private val story = listOf(
        Step.Line(TalkScript.NARRATOR, "숲이 열렸다."),
        Step.Line("piko", "여기야!"),
        Step.Choice(listOf(Option("가자", listOf(Step.Line("lumi", "좋아!"))), Option("잠깐", emptyList()))),
        Step.Run,
        Step.Line("lumi", "끝!"),
    )

    @Test fun villageWithItemsAsksFirstThenThanksTellsTheStoryAndSaysGoodbye() {
        val parts = TalkPlan.village("lumi", "솔방울 3개!", "고마워!", story, pick)
        assertTrue(parts.askItems)
        assertEquals(listOf(TalkLine("lumi", "line:select"), TalkLine("lumi", "솔방울 3개!")), parts.before)
        assertEquals(listOf(Step.Line("lumi", "고마워!")) + story.filter { it !is Step.Run } + Step.Line("lumi", "line:home"), parts.after)
    }

    @Test fun seeingAStoryAgainSkipsTheRequest() {
        val again = TalkPlan.village("lumi", null, null, story, pick)
        assertEquals(VillageParts(listOf(TalkLine("lumi", "line:select")),
            story.filter { it !is Step.Run } + Step.Line("lumi", "line:home"), askItems = false), again)
    }

    @Test fun storyIsPlayedInPartsBetweenChoicesWithOtherFriendsSpeaking() {
        val (first, at) = TalkPlan.segment(story, 0)
        assertEquals(listOf(TalkLine(TalkScript.NARRATOR, "숲이 열렸다."), TalkLine("piko", "여기야!")), first)
        assertEquals(2, at)
        // After the choice (index 2) the run step is skipped and the rest is told.
        assertEquals(listOf(TalkLine("lumi", "끝!")) to story.size, TalkPlan.segment(story, at + 1))
    }

    @Test fun koreanParticleFollowsTheLastSyllable() {
        assertEquals("라일과", Josa.with("라일", "과", "와"))
        assertEquals("치즈와", Josa.with("치즈", "과", "와"))
        assertEquals("솔방울을", Josa.with("솔방울", "을", "를"))
        assertEquals("종이배를", Josa.with("종이배", "을", "를"))
    }
}
