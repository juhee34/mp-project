package com.tinyrunners.school.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterCopyParserTest {
    private fun appCopy(): String {
        // Unit tests run from the app module directory.
        val file = listOf(File("src/main/assets/content/character_copy.json"), File("app/src/main/assets/content/character_copy.json"))
            .first { it.exists() }
        return file.readText()
    }

    @Test
    fun everyFriendHasCopyMatchedById() {
        val copies = CharacterCopyParser.parse(appCopy())
        assertEquals(setOf("mint", "cheese", "popo", "lumi", "nunu", "piko"), copies.keys)
        copies.values.forEach { c ->
            assertTrue(c.tagline.isNotBlank() && c.selectionIntro.isNotBlank() && c.storyTeaser.isNotBlank())
            assertTrue(c.traits.isNotEmpty() && c.habits.isNotEmpty())
            assertEquals(2, c.greetings.size)
        }
    }

    @Test
    fun matchesByIdNotByPosition() {
        val json = """{"characters":[
            {"id":"piko","tagline":"P","selectionIntro":"pi","personality":"pp","traits":["a"],"habits":["h"],
             "cherishedItem":"c","runningReason":"r","storyTeaser":"s","greetings":["g1","g2"]},
            {"id":"mint","tagline":"M","selectionIntro":"mi","personality":"mp","traits":[],"habits":[],
             "cherishedItem":"c","runningReason":"r","storyTeaser":"s"}]}"""
        val copies = CharacterCopyParser.parse(json)
        assertEquals("P", copies.getValue("piko").tagline)
        assertEquals("M", copies.getValue("mint").tagline)
        // A missing list reads as empty rather than failing.
        assertEquals(emptyList<String>(), copies.getValue("mint").greetings)
    }
}
