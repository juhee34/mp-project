package com.tinyrunners.school.talk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TalkTest {
    private val script get() = TalkScript.parse(File("src/main/assets/content/talks.json").readText())
    private val friends = listOf("mint", "cheese", "popo", "lumi", "nunu", "piko")

    @Test fun realScriptHasPrologueAndATalkForEveryFriend() {
        val s = script
        assertTrue(s.prologue.isNotEmpty())
        assertEquals(friends.toSet(), s.talks.keys)
        for (id in friends) {
            val lines = s.talkFor(id)
            assertTrue("$id talk is empty", lines.isNotEmpty())
            for (l in lines) {
                assertTrue("$id: bad speaker ${l.speaker}", l.speaker in setOf(id, TalkScript.PLAYER, TalkScript.NARRATOR))
                assertTrue(l.text.isNotBlank())
            }
            // The friend speaks last, so the run starts on their words.
            assertEquals(id, lines.last().speaker)
        }
    }

    @Test fun tapFinishesTypingBeforeMovingOn() {
        val p = TalkPlayer(listOf(TalkLine("mint", "안녕"), TalkLine("player", "응")))
        assertTrue(p.isTyping)
        p.type(1)
        assertEquals(1, p.shown)
        p.tap()                       // completes the first line
        assertEquals(0, p.index); assertFalse(p.isTyping)
        p.tap()                       // next line
        assertEquals(1, p.index); assertEquals(0, p.shown)
        p.type(10)                    // typing never goes past the end
        assertEquals(1, p.shown)
        p.tap()
        assertTrue(p.isFinished)
        p.tap()                       // extra taps after the end are ignored
        assertTrue(p.isFinished)
    }

    @Test fun restoreShowsTheLineInFull() {
        val p = TalkPlayer(listOf(TalkLine("mint", "하나"), TalkLine("mint", "둘셋")))
        p.restore(1)
        assertEquals(1, p.index); assertFalse(p.isTyping)
        p.restore(99)
        assertTrue(p.isFinished)
    }
}
