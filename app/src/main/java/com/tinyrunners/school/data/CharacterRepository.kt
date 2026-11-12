package com.tinyrunners.school.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class CharacterRepository(context: Context) {
    val characters: List<CharacterDefinition> = context.assets.open("content/characters.json").bufferedReader().use { reader ->
        val copies = try {
            context.assets.open("content/character_copy.json").bufferedReader().use { CharacterCopyParser.parse(it.readText()) }
        } catch (e: Exception) {
            // Without the copy the screens fall back to the original personality and summary.
            android.util.Log.w("CharacterRepository", "character_copy.json could not be read", e)
            emptyMap()
        }
        val array = JSONArray(reader.readText())
        List(array.length()) { i ->
            val value = array.getJSONObject(i)
            val id = value.getString("id")
            CharacterDefinition(id, value.getString("name"), value.getString("species"), value.getString("home"),
                value.getString("personality"), value.getString("summary"), value.getString("color"), copies[id])
        }
    }.also { list ->
        require(list.size == 6) { "캐릭터 6명이 필요합니다." }
        require(list.map { it.id }.distinct().size == list.size) { "중복 캐릭터 ID" }
    }
    fun find(id: String): CharacterDefinition? = characters.firstOrNull { it.id == id }
}

/** Reads character_copy.json into copies keyed by character id (never by position in the array). */
object CharacterCopyParser {
    fun parse(text: String): Map<String, CharacterCopy> {
        val array = JSONObject(text).getJSONArray("characters")
        return (0 until array.length()).associate { i ->
            val c = array.getJSONObject(i)
            c.getString("id") to CharacterCopy(
                tagline = c.getString("tagline"),
                selectionIntro = c.getString("selectionIntro"),
                personality = c.getString("personality"),
                traits = c.strings("traits"),
                habits = c.strings("habits"),
                cherishedItem = c.getString("cherishedItem"),
                runningReason = c.getString("runningReason"),
                storyTeaser = c.getString("storyTeaser"),
                greetings = c.strings("greetings"),
            )
        }
    }

    private fun JSONObject.strings(key: String): List<String> {
        val a = optJSONArray(key) ?: return emptyList()
        return (0 until a.length()).map { a.getString(it) }
    }
}
