package com.tinyrunners.school.postcard

import com.tinyrunners.school.progress.CharacterProgress
import com.tinyrunners.school.progress.ProgressRules
import com.tinyrunners.school.progress.RunResult
import com.tinyrunners.school.shop.ItemKind
import com.tinyrunners.school.shop.Season
import com.tinyrunners.school.shop.ShopCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PostcardRulesTest {
    private val meters = Postcard("mint_1", "mint", "t", "b", Condition(ConditionType.METERS, 300))
    private val stories = Postcard("mint_2", "mint", "t", "b", Condition(ConditionType.STORIES, 0))
    private val weather = Postcard("cheese_2", "cheese", "t", "b", Condition(ConditionType.WEATHER, 1))

    @Test fun metersTogether() {
        assertFalse(PostcardRules.earned(meters, null, emptyList()))
        assertEquals(120 to 300, PostcardRules.progress(meters, CharacterProgress("mint", totalMeters = 120), emptyList()))
        assertTrue(PostcardRules.earned(meters, CharacterProgress("mint", totalMeters = 300), emptyList()))
    }

    @Test fun allStoriesOfTheFriend() {
        val friendStories = listOf("m1", "m2", "m3")
        val some = CharacterProgress("mint", readStoryIds = listOf("m1", "other"))
        assertEquals(1 to 3, PostcardRules.progress(stories, some, friendStories))
        assertFalse(PostcardRules.earned(stories, some, friendStories))
        assertTrue(PostcardRules.earned(stories, some.copy(readStoryIds = listOf("m3", "m1", "m2")), friendStories))
        assertFalse("no stories known: never earned by accident", PostcardRules.earned(stories, some, emptyList()))
    }

    @Test fun specialWeatherRunsAreCounted() {
        val run = RunResult("r1", "cheese", 40, 40, 1, specialWeather = true)
        val after = ProgressRules.apply(null, run)
        assertEquals(1, after.specialWeatherRuns)
        assertEquals(1, ProgressRules.apply(after, run.copy(runId = "r2", specialWeather = false)).specialWeatherRuns)
        assertTrue(PostcardRules.earned(weather, after, emptyList()))
        assertEquals("older saves read as 0", 0, CharacterProgress.fromMap("cheese", mapOf("runs" to 3)).specialWeatherRuns)
    }

    @Test fun earnedIdsAcrossFriends() {
        val progress = mapOf("mint" to CharacterProgress("mint", totalMeters = 500))
        assertEquals(setOf("mint_1"), PostcardRules.earnedIds(listOf(meters, stories, weather), progress, mapOf("mint" to listOf("m1"))))
    }

    @Test fun theAppPackParses() {
        val file = listOf("src/main/assets/content/postcards.json", "app/src/main/assets/content/postcards.json").map(::File).first { it.exists() }
        val cards = PostcardParser.parse(file.readText())
        assertEquals(12, cards.size)
        assertEquals(cards.size, cards.map { it.id }.toSet().size)
        assertEquals(setOf("mint", "cheese", "popo", "lumi", "nunu", "piko"), cards.map { it.characterId }.toSet())
    }

    @Test fun seasonsByMonth() {
        assertEquals(Season.WINTER, Season.of(11))
        assertEquals(Season.WINTER, Season.of(1))
        assertEquals(Season.SPRING, Season.of(3))
        assertEquals(Season.SUMMER, Season.of(8))
        assertEquals(Season.AUTUMN, Season.of(10))
    }

    @Test fun seasonalDecorationsShowOnlyInTheirSeasonUnlessOwned() {
        val winter = ShopCatalog.onShelf(ItemKind.TRAIL, Season.WINTER) { false }.map { it.id }
        assertTrue("snowflakes" in winter && "yarn" in winter && "stars" in winter)
        assertFalse("maple" in winter)
        val ownedMaple = ShopCatalog.onShelf(ItemKind.TRAIL, Season.WINTER) { it == "maple" }.map { it.id }
        assertTrue("an owned autumn decoration stays", "maple" in ownedMaple)
        assertFalse("snowflakes" in ShopCatalog.onShelf(ItemKind.TRAIL, Season.SUMMER) { false }.map { it.id })
    }
}
