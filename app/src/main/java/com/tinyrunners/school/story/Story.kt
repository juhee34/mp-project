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

/**
 * Walks through a story. The whole position is three integers, so it survives rotation and the trip to the
 * game and back via [save]/[restore].
 */
class StoryPlayer(val story: Story) {
    var index = 0; private set
    /** Option picked at the current Choice step, or -1 before choosing. */
    var chosen = -1; private set
    /** Position inside the chosen option's reply. */
    var replyIndex = 0; private set

    val finished: Boolean get() = index >= story.steps.size

    /** What to show now: a line, a choice to pick, a run to start, or null at the end. */
    val current: Step?
        get() {
            val step = story.steps.getOrNull(index) ?: return null
            if (step is Step.Choice && chosen >= 0) return step.options[chosen].reply.getOrNull(replyIndex)
            return step
        }

    /** Moves past a line. Choices need [choose] and runs need [completeRun]. */
    fun advance() {
        val step = story.steps.getOrNull(index) ?: return
        when {
            step is Step.Choice && chosen >= 0 -> {
                replyIndex++
                if (replyIndex >= step.options[chosen].reply.size) next()
            }
            step is Step.Line -> next()
        }
    }

    fun choose(option: Int) {
        val step = story.steps.getOrNull(index) as? Step.Choice ?: return
        if (chosen >= 0 || option !in step.options.indices) return
        chosen = option
        replyIndex = 0
        if (step.options[option].reply.isEmpty()) next()
    }

    fun completeRun() { if (story.steps.getOrNull(index) == Step.Run) next() }

    /** Jumps to the end; the story still counts as read. */
    fun skip() { index = story.steps.size; chosen = -1; replyIndex = 0 }

    fun save(): IntArray = intArrayOf(index, chosen, replyIndex)

    fun restore(state: IntArray?) {
        if (state == null || state.size != 3) return
        index = state[0].coerceIn(0, story.steps.size)
        val step = story.steps.getOrNull(index) as? Step.Choice
        chosen = if (step != null && state[1] in step.options.indices) state[1] else -1
        replyIndex = if (chosen >= 0) state[2].coerceIn(0, step!!.options[chosen].reply.size) else 0
    }

    private fun next() { index++; chosen = -1; replyIndex = 0 }
}
