package com.tinyrunners.school.stories

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class StoryTest {
    private val catalogue get() = StoryParser.parse(File("src/main/assets/content/stories.json").readText())
    private val first get() = catalogue.first { it.id == "mint_episode_1" }

    @Test fun sixFriendsHaveIntroductionEpisodeAndTwoOutlines() {
        val stories = catalogue
        assertEquals(24, stories.size)
        stories.groupBy { it.characterId }.values.forEach { own ->
            assertEquals(listOf("ready", "ready", "outline", "outline"), own.sortedBy { it.order }.map { it.status })
            assertTrue(own[0].pages.isNotEmpty()); assertTrue(own[1].pages.size > 1)
            assertTrue(own.drop(2).all { it.pages.isEmpty() })
        }
    }
    @Test fun introductionIsAvailableWithoutPlaying() {
        val intro = catalogue.first { it.id == "mint_intro" }
        assertEquals(StoryState.UNREAD, StoryRules.state(intro, 0, emptySet(), emptySet()))
    }
    @Test fun cumulativeDistanceBoundaryOpensEpisode() {
        assertEquals(StoryState.LOCKED, StoryRules.state(first, 99, emptySet(), emptySet()))
        assertEquals(StoryState.UNREAD, StoryRules.state(first, 100, emptySet(), emptySet()))
    }
    @Test fun appreciationModeOpensWithoutDistance() {
        assertEquals(StoryState.UNREAD, StoryRules.state(first, 0, setOf(first.id), emptySet()))
    }
    @Test fun anotherFriendsUnlockDoesNotOpenThisEpisode() {
        assertEquals(StoryState.LOCKED, StoryRules.state(first, 0, setOf("cheese_episode_1"), setOf("cheese_intro")))
    }
    @Test fun previouslyReadStoryStaysReadableOffline() {
        assertEquals(StoryState.READ, StoryRules.state(first, 0, emptySet(), setOf(first.id)))
    }
    @Test fun outlinesCannotBeUnlockedEvenWithLargeDistanceOrOldMetadata() {
        val outline = catalogue.first { it.status == "outline" }
        assertEquals(StoryState.COMING_SOON, StoryRules.state(outline, Long.MAX_VALUE, setOf(outline.id), setOf(outline.id)))
    }
    @Test fun openingOrSkippingDoesNotCountAsRead() {
        assertFalse(StoryRules.canFinish(first, 0, StoryState.UNREAD))
        assertFalse(StoryRules.canFinish(first, first.pages.lastIndex - 1, StoryState.UNREAD))
        assertTrue(StoryRules.canFinish(first, first.pages.lastIndex, StoryState.UNREAD))
        assertFalse(StoryRules.canFinish(first, first.pages.lastIndex, StoryState.LOCKED))
    }
    @Test fun staleBookmarksAreClampedForEditedStories() {
        assertEquals(0, StoryRules.bookmark(-99, 5))
        assertEquals(4, StoryRules.bookmark(500, 5))
        assertEquals(0, StoryRules.bookmark(0, 0))
    }
    @Test fun malformedCatalogueIsRejected() {
        val text = File("src/main/assets/content/stories.json").readText()
        assertThrows(IllegalArgumentException::class.java) { StoryParser.parse(text.replace("\"ready\"", "\"outline\"")) }
    }
}
