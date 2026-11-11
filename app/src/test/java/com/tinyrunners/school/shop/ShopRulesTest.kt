package com.tinyrunners.school.shop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopRulesTest {
    @Test fun tenMetresMakeOneCoin() {
        assertEquals(0, ShopRules.coinsForRun(9))
        assertEquals(1, ShopRules.coinsForRun(10))
        assertEquals(26, ShopRules.coinsForRun(263))
        assertEquals(0, ShopRules.coinsForRun(-5))
        assertEquals(120, ShopRules.earned(1205L))
        // The balance uses the saved total, so it is never less than the sum shown after each run.
        assertTrue(ShopRules.earned(15L + 15L) >= ShopRules.coinsForRun(15) + ShopRules.coinsForRun(15))
    }

    @Test fun buyingSpendsCoinsAndWearsTheItem() {
        val stars = ShopCatalog.find("stars")!!
        val bought = ShopRules.buy(ShopState(), stars.id, totalMeters = 400L) as ShopRules.Purchase.Bought
        assertTrue(bought.state.owns(stars.id))
        assertEquals(stars.id, bought.state.equipped)
        assertEquals(40 - stars.price, ShopRules.balance(400L, bought.state))
    }

    @Test fun cannotBuyWithoutEnoughCoinsOrTwice() {
        val rainbow = ShopCatalog.find("rainbow")!!
        val short = ShopRules.buy(ShopState(), rainbow.id, totalMeters = 500L)
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
