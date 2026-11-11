package com.tinyrunners.school.quest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestRulesTest {
    @Test fun everyFriendHasOneItemWithAnEightByEightIcon() {
        val ids = listOf("mint", "cheese", "popo", "lumi", "nunu", "piko")
        assertEquals(ids, QuestItems.all.map { it.friendId })
        QuestItems.all.forEach { item ->
            assertEquals(item.friendId, 8, item.pixels.size)
            assertTrue(item.friendId, item.pixels.all { it.length == 8 })
        }
    }

    @Test fun collectingAddsOnlyForKnownFriends() {
        var bag = QuestRules.add(ItemBag(), "mint", 2)
        bag = QuestRules.add(bag, "mint", 1)
        assertEquals(3, bag.count("mint"))
        assertEquals(bag, QuestRules.add(bag, "nobody", 5))
        assertEquals(bag, QuestRules.add(bag, "mint", 0))
    }

    @Test fun givingNeedsThreeAndTakesThemOncePerStory() {
        val two = QuestRules.add(ItemBag(), "lumi", 2)
        assertNull(QuestRules.give(two, "lumi", "lumi-1"))
        val four = QuestRules.add(two, "lumi", 2)
        val given = QuestRules.give(four, "lumi", "lumi-1")!!
        assertEquals(1, given.count("lumi"))
        assertTrue("lumi-1" in given.given)
        // Coming back to the same story never takes more items.
        assertEquals(given, QuestRules.give(given, "lumi", "lumi-1"))
    }

    @Test fun storedMapRoundTripsAndDropsJunk() {
        val bag = ItemBag(mapOf("mint" to 4, "piko" to 1), setOf("mint-1"))
        assertEquals(bag, ItemBag.fromMap(bag.toMap()))
        val junk = ItemBag.fromMap(mapOf("have" to mapOf("mint" to -3L, "ghost" to 9L, 5 to 1), "given" to listOf("a", 7)))
        assertEquals(ItemBag(mapOf("mint" to 0), setOf("a")), junk)
        assertEquals(ItemBag(), ItemBag.fromMap(null))
    }
}
