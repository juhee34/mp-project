package com.tinyrunners.school.talk

import org.json.JSONObject

/**
 * Content for the flow login → prologue → friend selection → talk → run (assets/content/talks.json).
 * A [TalkLine.speaker] is a character ID, [PLAYER] (the signed-in nickname) or [NARRATOR] (no name tag).
 */
data class TalkLine(val speaker: String, val text: String)

data class TalkScript(val prologue: List<String>, val talks: Map<String, List<TalkLine>>) {
    fun talkFor(characterId: String): List<TalkLine> = talks[characterId].orEmpty()

    companion object {
        const val PLAYER = "player"
        const val NARRATOR = "narrator"

        fun parse(json: String): TalkScript {
            val root = JSONObject(json)
            val prologue = root.getJSONArray("prologue").let { a -> List(a.length()) { a.getString(it) } }
            val talks = root.getJSONArray("talks").let { a ->
                List(a.length()) { a.getJSONObject(it) }.associate { o ->
                    val lines = o.getJSONArray("lines")
                    o.getString("characterId") to List(lines.length()) { i ->
                        lines.getJSONObject(i).let { l -> TalkLine(l.getString("speaker"), l.getString("text")) }
                    }
                }
            }
            return TalkScript(prologue, talks)
        }
    }
}

/** Which line is showing and how much of it has been "typed" so far. Pure, so it is unit tested. */
class TalkPlayer(private val lines: List<TalkLine>) {
    var index = 0
        private set
    /** Number of characters of the current line already revealed. */
    var shown = 0
        private set

    val current: TalkLine? get() = lines.getOrNull(index)
    val isTyping: Boolean get() = current?.let { shown < it.text.length } ?: false
    val isFinished: Boolean get() = index >= lines.size

    /** Reveals [count] more characters of the current line. */
    fun type(count: Int) {
        val line = current ?: return
        shown = (shown + count).coerceAtMost(line.text.length)
    }

    /** A tap: finish the line being typed, otherwise move to the next line. */
    fun tap() {
        if (isFinished) return
        if (isTyping) { shown = current!!.text.length; return }
        index++
        shown = 0
    }

    /** Restores a position after the screen is recreated (rotation); the restored line is shown in full. */
    fun restore(at: Int) {
        index = at.coerceIn(0, lines.size)
        shown = current?.text?.length ?: 0
    }
}
