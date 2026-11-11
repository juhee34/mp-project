package com.tinyrunners.school.story

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryTest {
    private val scripted = """[{"id":"mint_intro","characterId":"mint","title":"t","body":"개요.","status":"draft",
        "lines":[
          {"speaker":"mint","text":"a"},
          {"choice":[{"text":"x","reply":[{"speaker":"mint","text":"rx1"},{"speaker":"player","text":"rx2"}]},
                     {"text":"y","reply":[{"speaker":"mint","text":"ry"}]}]},
          {"run":true},
          {"speaker":"narrator","text":"end"}]},
        {"id":"mint_next","characterId":"mint","title":"t2","body":"첫 문장. 둘째 문장! 셋째 문장?","status":"outline",
         "unlock":{"afterStory":"mint_intro"}}]"""

    private fun stories() = Story.parseAll(scripted)

    @Test fun parsesScriptedSteps() {
        val s = stories()[0]
        assertTrue(s.scripted)
        assertEquals(4, s.steps.size)
        assertEquals(Step.Line("mint", "a"), s.steps[0])
        assertEquals(2, (s.steps[1] as Step.Choice).options.size)
        assertEquals(Step.Run, s.steps[2])
        assertNull(s.afterStory)
    }

    @Test fun outlineBecomesNarrationWithRunBeforeLastSentence() {
        val s = stories()[1]
        assertFalse(s.scripted)
        assertEquals(listOf(
            Step.Line(Story.NARRATOR, "첫 문장."), Step.Line(Story.NARRATOR, "둘째 문장!"),
            Step.Run, Step.Line(Story.NARRATOR, "셋째 문장?")), s.steps)
    }

    @Test fun unlockFollowsPreviousStory() {
        val next = stories()[1]
        assertFalse(next.isUnlocked(emptySet()))
        assertTrue(next.isUnlocked(setOf("mint_intro")))
        assertTrue(stories()[0].isUnlocked(emptySet()))
    }

    @Test fun choiceShowsOnlyThatReplyThenContinues() {
        val p = StoryPlayer(stories()[0])
        p.advance()
        assertTrue(p.current is Step.Choice)
        p.advance() // tapping does not skip a choice
        assertTrue(p.current is Step.Choice)
        p.choose(0)
        assertEquals("rx1", (p.current as Step.Line).text)
        p.choose(1) // cannot change after choosing
        assertEquals("rx1", (p.current as Step.Line).text)
        p.advance()
        assertEquals("rx2", (p.current as Step.Line).text)
        p.advance()
        assertEquals(Step.Run, p.current)
    }

    @Test fun runWaitsForGameThenStoryEnds() {
        val p = StoryPlayer(stories()[0])
        p.advance(); p.choose(1); p.advance()
        assertEquals(Step.Run, p.current)
        p.advance() // a tap does not skip the run
        assertEquals(Step.Run, p.current)
        p.completeRun()
        assertEquals(Step.Line(Story.NARRATOR, "end"), p.current)
        p.advance()
        assertTrue(p.finished)
        assertNull(p.current)
    }

    @Test fun saveAndRestoreKeepsPositionInsideReply() {
        val p = StoryPlayer(stories()[0])
        p.advance(); p.choose(0); p.advance()
        val state = p.save()
        assertArrayEquals(intArrayOf(1, 0, 1), state)
        val q = StoryPlayer(stories()[0]).apply { restore(state) }
        assertEquals("rx2", (q.current as Step.Line).text)
        val r = StoryPlayer(stories()[0]).apply { restore(intArrayOf(99, 5, 5)) }
        assertTrue("out of range clamps to the end", r.finished)
    }

    @Test fun skipFinishes() {
        val p = StoryPlayer(stories()[0])
        p.skip()
        assertTrue(p.finished)
    }

    @Test fun nextStoryIsTheFirstUnlockedUnreadOneOfThatFriend() {
        val all = Story.parseAll(java.io.File("src/main/assets/content/village_stories.json").readText())
        assertEquals("mint_intro", Story.nextUnread(all, "mint", emptySet())?.id)
        assertEquals("mint_episode_1", Story.nextUnread(all, "mint", setOf("mint_intro"))?.id)
        assertEquals("cheese_intro", Story.nextUnread(all, "cheese", setOf("mint_intro"))?.id)
        val mintAll = all.filter { it.characterId == "mint" }.map { it.id }.toSet()
        assertNull(Story.nextUnread(all, "mint", mintAll))
        assertEquals(listOf("intro", "episode_1", "episode_2", "episode_3"),
            all.filter { it.characterId == "piko" }.map { it.id.removePrefix("piko_") })
    }
}
