package com.tinyrunners.school.shop

import com.tinyrunners.school.R

/**
 * Decorations from the shop only change how a run looks (a little trail behind the friend) or sounds (the music
 * during runs). They never change jumping, speed or obstacles, so all six friends stay equal.
 */
enum class TrailStyle { NONE, STARS, HEARTS, PETALS, BUBBLES, NOTES, RAINBOW }

/** Shop shelves: trails behind the runner, and background music for runs. */
enum class ItemKind { TRAIL, MUSIC }

/**
 * [track] is the name of a res/raw file for MUSIC items (any audio Android plays: mp3, m4a, ogg, mp4). A music
 * item whose file is not in the app yet is shown as "준비 중" and cannot be bought.
 */
data class ShopItem(
    val id: String,
    val name: Int,
    val blurb: Int,
    val price: Int,
    val style: TrailStyle = TrailStyle.NONE,
    val kind: ItemKind = ItemKind.TRAIL,
    val track: String? = null,
)

object ShopCatalog {
    const val NONE_ID = "none"
    const val MAIN_MUSIC_ID = "music_main"
    const val MUSIC_PRICE = 80

    val items = listOf(
        ShopItem(NONE_ID, R.string.shop_item_none, R.string.shop_blurb_none, 0, TrailStyle.NONE),
        ShopItem("stars", R.string.shop_item_stars, R.string.shop_blurb_stars, 30, TrailStyle.STARS),
        ShopItem("bubbles", R.string.shop_item_bubbles, R.string.shop_blurb_bubbles, 40, TrailStyle.BUBBLES),
        ShopItem("hearts", R.string.shop_item_hearts, R.string.shop_blurb_hearts, 50, TrailStyle.HEARTS),
        ShopItem("petals", R.string.shop_item_petals, R.string.shop_blurb_petals, 60, TrailStyle.PETALS),
        ShopItem("notes", R.string.shop_item_notes, R.string.shop_blurb_notes, 70, TrailStyle.NOTES),
        ShopItem("rainbow", R.string.shop_item_rainbow, R.string.shop_blurb_rainbow, 120, TrailStyle.RAINBOW),
        // Music: the main theme is free; six songs (2026-11-11) are res/raw/music_<id>.mp3. The friend in each id only picks
        // the sprite in the shop preview; the shop shows the song's own title (Korean, with the original English title).
        ShopItem(MAIN_MUSIC_ID, R.string.shop_music_main, R.string.shop_music_main_blurb, 0, kind = ItemKind.MUSIC, track = "main_theme"),
        ShopItem("music_mint", R.string.shop_music_mint, R.string.shop_music_mint_blurb, MUSIC_PRICE, kind = ItemKind.MUSIC, track = "music_mint"),
        ShopItem("music_cheese", R.string.shop_music_cheese, R.string.shop_music_cheese_blurb, MUSIC_PRICE, kind = ItemKind.MUSIC, track = "music_cheese"),
        ShopItem("music_popo", R.string.shop_music_popo, R.string.shop_music_popo_blurb, MUSIC_PRICE, kind = ItemKind.MUSIC, track = "music_popo"),
        ShopItem("music_lumi", R.string.shop_music_lumi, R.string.shop_music_lumi_blurb, MUSIC_PRICE, kind = ItemKind.MUSIC, track = "music_lumi"),
        ShopItem("music_nunu", R.string.shop_music_nunu, R.string.shop_music_nunu_blurb, MUSIC_PRICE, kind = ItemKind.MUSIC, track = "music_nunu"),
        ShopItem("music_piko", R.string.shop_music_piko, R.string.shop_music_piko_blurb, MUSIC_PRICE, kind = ItemKind.MUSIC, track = "music_piko"),
    )

    fun find(id: String?): ShopItem? = items.firstOrNull { it.id == id }
    fun style(id: String?): TrailStyle = find(id)?.style ?: TrailStyle.NONE
    fun of(kind: ItemKind) = items.filter { it.kind == kind }
}

/** What an account owns and wears. Free items are always owned. [equipped] is the trail, [music] the run music. */
data class ShopState(
    val owned: Set<String> = emptySet(),
    val equipped: String = ShopCatalog.NONE_ID,
    val spent: Int = 0,
    val music: String = ShopCatalog.MAIN_MUSIC_ID,
) {
    fun owns(id: String) = ShopCatalog.find(id)?.price == 0 || id in owned

    fun toMap(): Map<String, Any> = mapOf("owned" to owned.sorted(), "equipped" to equipped, "spent" to spent, "music" to music)

    companion object {
        fun fromMap(m: Map<String, Any?>?): ShopState {
            if (m == null) return ShopState()
            val owned = (m["owned"] as? List<*>)?.filterIsInstance<String>()?.filter { ShopCatalog.find(it) != null }?.toSet().orEmpty()
            fun wearable(id: String?, kind: ItemKind, default: String) =
                id?.takeIf { ShopCatalog.find(it)?.kind == kind && (ShopCatalog.find(it)?.price == 0 || it in owned) } ?: default
            val equipped = wearable(m["equipped"] as? String, ItemKind.TRAIL, ShopCatalog.NONE_ID)
            val music = wearable(m["music"] as? String, ItemKind.MUSIC, ShopCatalog.MAIN_MUSIC_ID)
            val spent = ((m["spent"] as? Number)?.toInt() ?: 0).coerceAtLeast(0)
            return ShopState(owned, equipped, spent, music)
        }
    }
}

/**
 * Coins come from distance: every [METERS_PER_COIN] metres run (all friends together, failed runs included) is one
 * coin. The balance is worked out from the saved total distance minus what was spent, so it cannot be counted twice.
 */
object ShopRules {
    const val METERS_PER_COIN = 25

    fun coinsForRun(meters: Int): Int = meters.coerceAtLeast(0) / METERS_PER_COIN
    fun earned(totalMeters: Long): Int = (totalMeters.coerceAtLeast(0) / METERS_PER_COIN).toInt()
    fun balance(totalMeters: Long, state: ShopState): Int = (earned(totalMeters) - state.spent).coerceAtLeast(0)

    sealed interface Purchase {
        data class Bought(val state: ShopState) : Purchase
        data object AlreadyOwned : Purchase
        data class NotEnough(val missing: Int) : Purchase
        data object Unknown : Purchase
    }

    /** Buying also puts the item on (trail) or chooses it for runs (music), which is what a player expects. */
    fun buy(state: ShopState, itemId: String, totalMeters: Long): Purchase {
        val item = ShopCatalog.find(itemId) ?: return Purchase.Unknown
        if (state.owns(item.id)) return Purchase.AlreadyOwned
        val balance = balance(totalMeters, state)
        if (balance < item.price) return Purchase.NotEnough(item.price - balance)
        val bought = state.copy(owned = state.owned + item.id, spent = state.spent + item.price)
        return Purchase.Bought(wear(bought, item))
    }

    /** Only owned items can be worn; anything else leaves the state unchanged. */
    fun equip(state: ShopState, itemId: String): ShopState {
        val item = ShopCatalog.find(itemId) ?: return state
        return if (state.owns(item.id)) wear(state, item) else state
    }

    private fun wear(state: ShopState, item: ShopItem) =
        if (item.kind == ItemKind.MUSIC) state.copy(music = item.id) else state.copy(equipped = item.id)
}
