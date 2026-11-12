package com.tinyrunners.school.data

data class CharacterDefinition(
    val id: String, val name: String, val species: String, val home: String,
    val personality: String, val summary: String, val color: String,
    /** A's friendly introduction (docs/character-copy/), or null when the copy file has no entry for this friend. */
    val copy: CharacterCopy? = null,
)

/**
 * A's friendly selection and profile copy for one friend (assets/content/character_copy.json, a copy of
 * docs/character-copy/friendly-copy.ko.json). It sits beside the original personality and summary, which stay as
 * they are. The file's voice notes and source ids are for writers and are not read.
 */
data class CharacterCopy(
    val tagline: String,
    val selectionIntro: String,
    val personality: String,
    val traits: List<String>,
    val habits: List<String>,
    val cherishedItem: String,
    val runningReason: String,
    val storyTeaser: String,
    val greetings: List<String>,
)
