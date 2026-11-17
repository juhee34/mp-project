package com.tinyrunners.school.story

import org.json.JSONArray
import org.json.JSONObject

/** One step of a story script. See docs/DATA_CONTRACT.md "이야기 대사". */
sealed interface Step {
    /** [speaker] is a character ID, [PLAYER] (shown as the nickname) or [NARRATOR]. */
    data class Line(val speaker: String, val text: String) : Step
    /** Only the next reply changes; there are no right or wrong answers and no branching endings. */
    data class Choice(val options: List<Option>) : Step
    /** Run once with this story's friend. Any result continues the story. */
    data object Run : Step
}

data class Option(val text: String, val reply: List<Step.Line>)

data class Story(
    val id: String,
    val characterId: String,
    val title: String,
    val summary: String,
    val status: String,
    /** This story opens after the listed story has been read; null means open from the start. */
    val afterStory: String?,
    val steps: List<Step>,
    /** True when [steps] were written as dialogue rather than derived from the outline. */
    val scripted: Boolean,
) {
    fun isUnlocked(read: Set<String>) = afterStory == null || afterStory in read

    companion object {
        const val PLAYER = "player"
        const val NARRATOR = "narrator"

        /** A friend's next village story: the first unlocked one not read yet, or null when all are read. */
        fun nextUnread(stories: List<Story>, characterId: String, read: Set<String>): Story? =
            stories.firstOrNull { it.characterId == characterId && it.id !in read && it.isUnlocked(read) }

        fun parseAll(json: String): List<Story> {
            val array = JSONArray(json)
            return List(array.length()) { parse(array.getJSONObject(it)) }
        }

        fun parse(o: JSONObject): Story {
            val lines = o.optJSONArray("lines")
            val steps = if (lines != null && lines.length() > 0) parseSteps(lines) else fromOutline(o.optString("body"))
            return Story(
                id = o.getString("id"),
                characterId = o.getString("characterId"),
                title = o.getString("title"),
                summary = o.optString("body"),
                status = o.optString("status", "outline"),
                afterStory = o.optJSONObject("unlock")?.optString("afterStory")?.takeIf { it.isNotEmpty() && it != "null" },
                steps = steps,
                scripted = lines != null && lines.length() > 0,
            )
        }

        private fun parseSteps(a: JSONArray): List<Step> = List(a.length()) { i ->
            val s = a.getJSONObject(i)
            when {
                s.optBoolean("run") -> Step.Run
                s.has("choice") -> {
                    val opts = s.getJSONArray("choice")
                    Step.Choice(List(opts.length()) { j ->
                        val opt = opts.getJSONObject(j)
                        val reply = opt.optJSONArray("reply") ?: JSONArray()
                        Option(opt.getString("text"), List(reply.length()) { k -> line(reply.getJSONObject(k)) })
                    })
                }
                else -> line(s)
            }
        }

        private fun line(o: JSONObject) = Step.Line(o.getString("speaker"), o.getString("text"))

        /**
         * Stories that only have an outline still play: each sentence becomes a narrator line and the run comes
         * before the last sentence, so every friend has a playable story while A writes the dialogue.
         */
        fun fromOutline(body: String): List<Step> {
            val sentences = body.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
            if (sentences.isEmpty()) return listOf(Step.Run)
            val lines = sentences.map { Step.Line(NARRATOR, it) }
            return lines.dropLast(1) + Step.Run + lines.last()
        }
    }
}

