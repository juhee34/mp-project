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

    @Test fun decorationsDoNotChangeTheFreeStart() {
        assertEquals(0, ShopCatalog.find(ShopCatalog.NONE_ID)!!.price)
        assertTrue(ShopCatalog.items.drop(1).all { it.price > 0 })
        assertEquals(ShopCatalog.items.size, ShopCatalog.items.map { it.id }.toSet().size)
    }
}
