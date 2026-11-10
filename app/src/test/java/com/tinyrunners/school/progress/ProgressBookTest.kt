package com.tinyrunners.school.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressBookTest {
    private fun run(id: String, character: String = "cheese", meters: Int = 100, score: Int = 120) =
        RunResult(id, character, meters, score, cleared = 4)

    @Test fun firstRunCreatesProgressAndIsBest() {
        val out = ProgressBook().record(run("a"))
        assertEquals(CharacterProgress("cheese", runs = 1, totalMeters = 100, bestMeters = 100, bestScore = 120), out.progress)
        assertTrue(out.newBest)
        assertFalse(out.duplicate)
    }

    @Test fun sameRunIdIsCountedOnce() {
        val book = ProgressBook()
        book.record(run("a"))
        val again = book.record(run("a"))
        assertTrue(again.duplicate)
        assertEquals(1, book.all.getValue("cheese").runs)
        assertEquals(100L, book.all.getValue("cheese").totalMeters)
    }

    @Test fun failedShortRunsStillAddDistanceButKeepBest() {
        val book = ProgressBook()
        book.record(run("a", meters = 300, score = 340))
        val out = book.record(run("b", meters = 12, score = 12))
        assertFalse(out.newBest)
        assertEquals(2, out.progress.runs)
        assertEquals(312L, out.progress.totalMeters)
        assertEquals(300, out.progress.bestMeters)
        assertEquals(340, out.progress.bestScore)
    }

    @Test fun charactersAreKeptSeparate() {
        val book = ProgressBook()
        book.record(run("a", character = "mint"))
        book.record(run("b", character = "piko", meters = 50))
        assertEquals(setOf("mint", "piko"), book.all.keys)
        assertEquals(50L, book.all.getValue("piko").totalMeters)
    }

    @Test fun jsonRoundTripKeepsProgressAndDuplicateProtection() {
        val book = ProgressBook()
        book.record(run("a"))
        val restored = ProgressBook.fromJson(book.toJson())
        assertEquals(book.all, restored.all)
        assertTrue(restored.record(run("a")).duplicate)
    }

    @Test fun corruptOrMissingDataStartsFresh() {
        assertTrue(ProgressBook.fromJson(null).all.isEmpty())
        assertTrue(ProgressBook.fromJson("{not json").all.isEmpty())
        assertTrue(ProgressBook.fromJson("[]").all.isEmpty())
    }

    @Test fun rememberedRunIdsAreBounded() {
        val book = ProgressBook()
        repeat(ProgressBook.MAX_REMEMBERED_RUNS + 10) { book.record(run("r$it", meters = 1, score = 1)) }
        assertEquals(ProgressBook.MAX_REMEMBERED_RUNS + 10, book.all.getValue("cheese").runs)
        assertTrue("newest id still protected", book.record(run("r${ProgressBook.MAX_REMEMBERED_RUNS + 9}")).duplicate)
    }
}
