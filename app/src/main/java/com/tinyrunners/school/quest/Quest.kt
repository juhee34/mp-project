package com.tinyrunners.school.quest

import com.tinyrunners.school.R

/**
 * Helping a friend: each friend needs something from their hometown. Running with that friend scatters those items
 * along the road (they are never obstacles); in the village talk the friend asks for [QuestRules.NEEDED] of them
 * and the story goes on once they are handed over.
 */
data class QuestItem(
    val friendId: String,
    val name: Int,
    /** Friend's line asking for the items. */
    val request: Int,
    /** Friend's line after receiving them. */
    val thanks: Int,
    /** Pixel icon rows; see [ItemArt]. */
    val pixels: List<String>,
)

object QuestItems {
    val all = listOf(
        QuestItem("mint", R.string.item_mint, R.string.item_mint_request, R.string.item_mint_thanks,
            listOf("...g....", "..gGg...", ".gGgGg..", "gGg.gGg.", ".g..g.g.", "....g...", "....s...", "....s...")),
        QuestItem("cheese", R.string.item_cheese, R.string.item_cheese_request, R.string.item_cheese_thanks,
            listOf("...y....", ".y.Y.y..", "..YYY...", "yYYwYYy.", "..YYY...", ".y.Y.y..", "...y....", "........")),
        QuestItem("popo", R.string.item_popo, R.string.item_popo_request, R.string.item_popo_thanks,
            listOf("..pPPp..", ".pPpPpP.", "pPpPpPpP", "pPpPpPpP", ".pPpPpP.", "..p..p..", "...pp...", "........")),
        QuestItem("lumi", R.string.item_lumi, R.string.item_lumi_request, R.string.item_lumi_thanks,
            listOf("...gg...", "..bBb...", ".bBbBb..", ".BbBbB..", ".bBbBb..", "..BbB...", "...b....", "........")),
        QuestItem("nunu", R.string.item_nunu, R.string.item_nunu_request, R.string.item_nunu_thanks,
            listOf(".mmmmm..", ".mMMMm..", ".mMMMm..", ".mMrMm..", ".mMMMm..", ".mMMMm..", ".mm.mm..", ".m...m..")),
        QuestItem("piko", R.string.item_piko, R.string.item_piko_request, R.string.item_piko_thanks,
            listOf("...w....", "...wW...", "...wWW..", "...w....", "kkkkkkkk", ".kKKKKk.", "..kkkk..", "........")),
    )

    fun of(friendId: String): QuestItem? = all.firstOrNull { it.friendId == friendId }
}

/** What an account has collected and which stories already received their items. */
data class ItemBag(val have: Map<String, Int> = emptyMap(), val given: Set<String> = emptySet()) {
    fun count(friendId: String) = have[friendId] ?: 0

    fun toMap(): Map<String, Any> = mapOf("have" to have, "given" to given.sorted())

    companion object {
        fun fromMap(m: Map<String, Any?>?): ItemBag {
            if (m == null) return ItemBag()
            val have = (m["have"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                val id = k as? String ?: return@mapNotNull null
                if (QuestItems.of(id) == null) return@mapNotNull null
                id to ((v as? Number)?.toInt() ?: 0).coerceAtLeast(0)
            }?.toMap().orEmpty()
            val given = (m["given"] as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
            return ItemBag(have, given)
        }
    }
}

object QuestRules {
    /** Items a friend asks for in one story. */
    const val NEEDED = 3

    fun add(bag: ItemBag, friendId: String, count: Int): ItemBag =
        if (count <= 0 || QuestItems.of(friendId) == null) bag
        else bag.copy(have = bag.have + (friendId to bag.count(friendId) + count))

    /** Hand over the items for [storyId]; a story only ever takes them once. Null if there are not enough. */
    fun give(bag: ItemBag, friendId: String, storyId: String): ItemBag? {
        if (storyId in bag.given) return bag
        if (bag.count(friendId) < NEEDED) return null
        return bag.copy(have = bag.have + (friendId to bag.count(friendId) - NEEDED), given = bag.given + storyId)
    }
}
