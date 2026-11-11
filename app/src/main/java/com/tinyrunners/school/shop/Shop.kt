package com.tinyrunners.school.shop

import com.tinyrunners.school.R

/**
 * Decorations from the shop only change how a run looks (a little trail behind the friend). They never change
 * jumping, speed or obstacles, so all six friends stay equal.
 */
enum class TrailStyle { NONE, STARS, HEARTS, PETALS, BUBBLES, NOTES, RAINBOW }

data class ShopItem(val id: String, val name: Int, val blurb: Int, val price: Int, val style: TrailStyle)

object ShopCatalog {
    const val NONE_ID = "none"
    val items = listOf(
        ShopItem(NONE_ID, R.string.shop_item_none, R.string.shop_blurb_none, 0, TrailStyle.NONE),
        ShopItem("stars", R.string.shop_item_stars, R.string.shop_blurb_stars, 30, TrailStyle.STARS),
        ShopItem("bubbles", R.string.shop_item_bubbles, R.string.shop_blurb_bubbles, 40, TrailStyle.BUBBLES),
        ShopItem("hearts", R.string.shop_item_hearts, R.string.shop_blurb_hearts, 50, TrailStyle.HEARTS),
        ShopItem("petals", R.string.shop_item_petals, R.string.shop_blurb_petals, 60, TrailStyle.PETALS),
        ShopItem("notes", R.string.shop_item_notes, R.string.shop_blurb_notes, 70, TrailStyle.NOTES),
        ShopItem("rainbow", R.string.shop_item_rainbow, R.string.shop_blurb_rainbow, 120, TrailStyle.RAINBOW),
    )

    fun find(id: String?): ShopItem? = items.firstOrNull { it.id == id }
    fun style(id: String?): TrailStyle = find(id)?.style ?: TrailStyle.NONE
}

/** What an account owns and wears. The free "none" item is always owned. */
data class ShopState(val owned: Set<String> = emptySet(), val equipped: String = ShopCatalog.NONE_ID, val spent: Int = 0) {
    fun owns(id: String) = id == ShopCatalog.NONE_ID || id in owned

    fun toMap(): Map<String, Any> = mapOf("owned" to owned.sorted(), "equipped" to equipped, "spent" to spent)

    companion object {
        fun fromMap(m: Map<String, Any?>?): ShopState {
            if (m == null) return ShopState()
            val owned = (m["owned"] as? List<*>)?.filterIsInstance<String>()?.filter { ShopCatalog.find(it) != null }?.toSet().orEmpty()
            val equipped = (m["equipped"] as? String)?.takeIf { it == ShopCatalog.NONE_ID || it in owned } ?: ShopCatalog.NONE_ID
            val spent = ((m["spent"] as? Number)?.toInt() ?: 0).coerceAtLeast(0)
            return ShopState(owned, equipped, spent)
        }
    }
}

/**
 * Coins come from distance: every [METERS_PER_COIN] metres run (all friends together, failed runs included) is one
 * coin. The balance is worked out from the saved total distance minus what was spent, so it cannot be counted twice.
 */
object ShopRules {
    const val METERS_PER_COIN = 10

    fun coinsForRun(meters: Int): Int = meters.coerceAtLeast(0) / METERS_PER_COIN
    fun earned(totalMeters: Long): Int = (totalMeters.coerceAtLeast(0) / METERS_PER_COIN).toInt()
    fun balance(totalMeters: Long, state: ShopState): Int = (earned(totalMeters) - state.spent).coerceAtLeast(0)

    sealed interface Purchase {
        data class Bought(val state: ShopState) : Purchase
        data object AlreadyOwned : Purchase
        data class NotEnough(val missing: Int) : Purchase
        data object Unknown : Purchase
    }

    /** Buying also puts the item on, which is what a player expects right after buying. */
    fun buy(state: ShopState, itemId: String, totalMeters: Long): Purchase {
        val item = ShopCatalog.find(itemId) ?: return Purchase.Unknown
        if (state.owns(item.id)) return Purchase.AlreadyOwned
        val balance = balance(totalMeters, state)
        if (balance < item.price) return Purchase.NotEnough(item.price - balance)
        return Purchase.Bought(state.copy(owned = state.owned + item.id, equipped = item.id, spent = state.spent + item.price))
    }

    /** Only owned items (or "none") can be worn; anything else leaves the state unchanged. */
    fun equip(state: ShopState, itemId: String): ShopState = if (state.owns(itemId)) state.copy(equipped = itemId) else state
}
