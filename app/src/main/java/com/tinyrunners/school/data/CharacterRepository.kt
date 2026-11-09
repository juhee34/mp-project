package com.tinyrunners.school.data

import android.content.Context
import org.json.JSONArray

class CharacterRepository(context: Context) {
    val characters: List<CharacterDefinition> = context.assets.open("content/characters.json").bufferedReader().use { reader ->
        val array = JSONArray(reader.readText())
        List(array.length()) { i ->
            val value = array.getJSONObject(i)
            CharacterDefinition(value.getString("id"), value.getString("name"), value.getString("species"), value.getString("home"), value.getString("personality"), value.getString("summary"), value.getString("color"))
        }
    }.also { list ->
        require(list.size == 10) { "캐릭터 10명이 필요합니다." }
        require(list.map { it.id }.distinct().size == list.size) { "중복 캐릭터 ID" }
    }
    fun find(id: String): CharacterDefinition? = characters.firstOrNull { it.id == id }
}
