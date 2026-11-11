package com.tinyrunners.school.shop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopRulesTest {
    @Test fun twentyFiveMetresMakeOneCoin() {
        assertEquals(25, ShopRules.METERS_PER_COIN)
        assertEquals(0, ShopRules.coinsForRun(24))
        assertEquals(1, ShopRules.coinsForRun(25))
        assertEquals(10, ShopRules.coinsForRun(263))
        assertEquals(0, ShopRules.coinsForRun(-5))
        assertEquals(48, ShopRules.earned(1205L))
        // The balance uses the saved total, so it is never less than the sum shown after each run.
        assertTrue(ShopRules.earned(15L + 15L) >= ShopRules.coinsForRun(15) + ShopRules.coinsForRun(15))
    }

    @Test fun spendingMoreThanANewRateAllowsNeverGoesBelowZero() {
        // Coins spent under an older, kinder rate cannot push the balance negative.
        assertEquals(0, ShopRules.balance(500L, ShopState(spent = 30)))
    }

    @Test fun buyingSpendsCoinsAndWearsTheItem() {
        val stars = ShopCatalog.find("stars")!!
        val bought = ShopRules.buy(ShopState(), stars.id, totalMeters = 1000L) as ShopRules.Purchase.Bought
        assertTrue(bought.state.owns(stars.id))
        assertEquals(stars.id, bought.state.equipped)
        assertEquals(40 - stars.price, ShopRules.balance(1000L, bought.state))
    }

    @Test fun cannotBuyWithoutEnoughCoinsOrTwice() {
        val rainbow = ShopCatalog.find("rainbow")!!
        val short = ShopRules.buy(ShopState(), rainbow.id, totalMeters = 1250L)
        assertEquals(ShopRules.Purchase.NotEnough(rainbow.price - 50), short)
        val owned = ShopState(owned = setOf(rainbow.id))
        assertEquals(ShopRules.Purchase.AlreadyOwned, ShopRules.buy(owned, rainbow.id, 99_999L))
        assertEquals(ShopRules.Purchase.Unknown, ShopRules.buy(ShopState(), "crown", 99_999L))
    }

    @Test fun onlyOwnedItemsCanBeWorn() {
        val state = ShopState(owned = setOf("hearts"), equipped = "hearts")
        assertEquals("hearts", ShopRules.equip(state, "petals").equipped)
        assertEquals(ShopCatalog.NONE_ID, ShopRules.equip(state, ShopCatalog.NONE_ID).equipped)
        assertEquals("hearts", ShopRules.equip(state.copy(equipped = ShopCatalog.NONE_ID), "hearts").equipped)
    }

    @Test fun savedStateIsCheckedWhenRead() {
        val read = ShopState.fromMap(mapOf("owned" to listOf("stars", "made-up"), "equipped" to "rainbow", "spent" to -3))
        assertEquals(setOf("stars"), read.owned)
        assertEquals(ShopCatalog.NONE_ID, read.equipped)   // cannot wear something not owned
        assertEquals(0, read.spent)
        assertEquals(read, ShopState.fromMap(read.toMap()))
    }

    @Test fun eachShelfStartsWithOneFreeItemAndIdsAreUnique() {
        for (kind in ItemKind.entries) {
            val shelf = ShopCatalog.of(kind)
            assertEquals(1, shelf.count { it.price == 0 })
            assertTrue(shelf.filter { it.price > 0 }.isNotEmpty())
        }
        assertEquals(6, ShopCatalog.of(ItemKind.MUSIC).count { it.price > 0 })
        assertEquals(ShopCatalog.items.size, ShopCatalog.items.map { it.id }.toSet().size)
    }

    @Test fun musicIsBoughtAndChosenSeparatelyFromTrails() {
        val start = ShopState(owned = setOf("stars"), equipped = "stars")
        val bought = ShopRules.buy(start, "music_lumi", totalMeters = 5000L) as ShopRules.Purchase.Bought
        assertEquals("music_lumi", bought.state.music)
        assertEquals("stars", bought.state.equipped)   // the trail stays on
        assertEquals(ShopCatalog.MUSIC_PRICE, bought.state.spent)
        // Back to the free main theme at any time.
        assertEquals(ShopCatalog.MAIN_MUSIC_ID, ShopRules.equip(bought.state, ShopCatalog.MAIN_MUSIC_ID).music)
        // A trail id never ends up as the music, and the other way round.
        assertEquals("music_lumi", ShopRules.equip(bought.state, "stars").music)
    }

    @Test fun savedMusicMustBeOwned() {
        val read = ShopState.fromMap(mapOf("owned" to listOf("stars"), "music" to "music_piko", "equipped" to "stars"))
        assertEquals(ShopCatalog.MAIN_MUSIC_ID, read.music)
        val ok = ShopState.fromMap(mapOf("owned" to listOf("music_piko"), "music" to "music_piko"))
        assertEquals("music_piko", ok.music)
        assertEquals(ok, ShopState.fromMap(ok.toMap()))
    }

    @Test fun roadCoinsAddToTheDistanceCoinsAndAreSaved() {
        val state = ShopRules.addFound(ShopState(spent = 10), 7)
        assertEquals(7, state.found)
        assertEquals(500 / ShopRules.METERS_PER_COIN + 7 - 10, ShopRules.balance(500L, state))
        assertEquals(state, ShopRules.addFound(state, 0))
        assertEquals(state, ShopState.fromMap(state.toMap()))
        assertEquals(0, ShopState.fromMap(mapOf("found" to -3L)).found)
    }
}
