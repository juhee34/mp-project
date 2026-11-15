package com.tinyrunners.school.postcard

import android.content.Context
import com.tinyrunners.school.progress.CharacterProgress
import org.json.JSONObject

/** How a postcard is received (assets/content/postcards.json, docs/DATA_CONTRACT.md). */
enum class ConditionType { METERS, STORIES, WEATHER }

data class Condition(val type: ConditionType, val value: Int)

/** One friend's postcard: the picture side has [title]; the back has the friend's words. */
data class Postcard(val id: String, val characterId: String, val title: String, val back: String, val condition: Condition)

object PostcardParser {
    fun parse(json: String): List<Postcard> {
        val list = JSONObject(json).getJSONArray("postcards")
        return List(list.length()) { i ->
            val o = list.getJSONObject(i)
            val c = o.getJSONObject("condition")
            Postcard(
                id = o.getString("id"), characterId = o.getString("characterId"),
                title = o.getString("title"), back = o.getString("back"),
                condition = Condition(ConditionType.valueOf(c.getString("type").uppercase()), c.optInt("value", 0)),
            )
        }
    }
}

object PostcardRepository {
    private var cache: List<Postcard>? = null

    fun all(context: Context): List<Postcard> = cache ?: try {
        context.assets.open("content/postcards.json").bufferedReader().use { PostcardParser.parse(it.readText()) }
    } catch (e: Exception) {
        android.util.Log.w("Postcards", "postcards.json could not be read", e)
        emptyList()
    }.also { cache = it }
}

/**
 * Whether a postcard is earned and how far along it is. Pure, so it is unit tested. Nothing is lost by not having a
 * card; the album only says how to get it.
 */
object PostcardRules {
    /** (have, need) for [card]: metres together, stories read of the friend's [friendStories], or special-weather runs. */
    fun progress(card: Postcard, p: CharacterProgress?, friendStories: List<String>): Pair<Int, Int> = when (card.condition.type) {
        ConditionType.METERS -> (p?.totalMeters ?: 0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() to card.condition.value
        ConditionType.STORIES -> {
            val need = if (card.condition.value > 0) card.condition.value else friendStories.size
            friendStories.count { it in (p?.readStoryIds ?: emptyList()) } to need
        }
        ConditionType.WEATHER -> (p?.specialWeatherRuns ?: 0) to card.condition.value
    }

    fun earned(card: Postcard, p: CharacterProgress?, friendStories: List<String>): Boolean {
        val (have, need) = progress(card, p, friendStories)
        return need > 0 && have >= need
    }

    /** Every earned postcard id, given each friend's progress and village story ids. */
    fun earnedIds(cards: List<Postcard>, progress: Map<String, CharacterProgress>, stories: Map<String, List<String>>): Set<String> =
        cards.filter { earned(it, progress[it.characterId], stories[it.characterId].orEmpty()) }.map { it.id }.toSet()
}
