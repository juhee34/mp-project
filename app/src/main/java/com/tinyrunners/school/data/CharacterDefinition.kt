package com.tinyrunners.school.data

data class CharacterDefinition(
    val id: String, val name: String, val species: String, val home: String,
    val personality: String, val summary: String, val color: String,
    /** Display name of this friend's skill. Same effect for everyone; only the look and name differ. */
    val skillName: String = "",
)
