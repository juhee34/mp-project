package com.tinyrunners.school.talk

import android.content.Context
import org.json.JSONObject
import kotlin.random.Random

/**
 * One line from A's short dialogue pack (assets/content/short_lines.json, copied from docs/dialogue/short-lines.json).
 * Events used by the run flow: select (friend chosen), start (first departure), retry (one more run), stumble (crash).
 */
data class ShortLine(val id: String, val characterId: String, val event: String, val text: String)

object ShortLineParser {
    fun parse(json: String): List<ShortLine> {
        val lines = JSONObject(json).getJSONArray("lines")
        return List(lines.length()) { i ->
            lines.getJSONObject(i).let { ShortLine(it.getString("id"), it.getString("characterId"), it.getString("event"), it.getString("text")) }
        }
    }
}

/**
 * Picks lines for a friend and event so each of the three appears once before any repeats, and the same line is
 * never shown twice in a row (dialogue HANDOFF rule 2). Memory only; nothing is saved.
 */
class LineDeck(private val random: Random = Random.Default) {
    private val queues = mutableMapOf<String, ArrayDeque<ShortLine>>()
    private val last = mutableMapOf<String, String>()

    fun draw(all: List<ShortLine>, characterId: String, event: String): ShortLine? {
        val key = "$characterId/$event"
        val queue = queues.getOrPut(key) { ArrayDeque() }
        if (queue.isEmpty()) {
            val fresh = all.filter { it.characterId == characterId && it.event == event }.shuffled(random).toMutableList()
            if (fresh.isEmpty()) return null
            // A new round must not start with the line that ended the previous one.
            if (fresh.size > 1 && fresh.first().id == last[key]) fresh.add(fresh.removeAt(0))
            queue.addAll(fresh)
        }
        return queue.removeFirst().also { last[key] = it.id }
    }
}

object ShortLineRepository {
    private var cache: List<ShortLine>? = null
    private val deck = LineDeck()

    /** A friend's line for [event], or null when the pack is missing (the scene then skips that line). */
    fun pick(context: Context, characterId: String, event: String): String? {
        val all = cache ?: try {
            context.assets.open("content/short_lines.json").bufferedReader().use { ShortLineParser.parse(it.readText()) }
        } catch (e: Exception) {
            android.util.Log.w("ShortLines", "short_lines.json could not be read", e)
            emptyList()
        }.also { cache = it }
        return deck.draw(all, characterId, event)?.text
    }
}
