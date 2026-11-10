package com.tinyrunners.school.stories

import org.json.JSONArray

data class StoryBlock(val speaker: String?, val text: String)
data class StoryPage(val title: String, val blocks: List<StoryBlock>)
data class Story(
    val id: String, val characterId: String, val title: String, val body: String,
    val status: String, val kind: String, val order: Int, val unlockDistance: Long,
    val summary: String, val afterReading: String, val pages: List<StoryPage>,
)

enum class StoryState { COMING_SOON, LOCKED, UNREAD, READ }

object StoryRules {
    fun state(story: Story, meters: Long, unlocked: Set<String>, read: Set<String>): StoryState = when {
        story.status != "ready" -> StoryState.COMING_SOON
        story.id in read -> StoryState.READ
        story.id in unlocked || meters.coerceAtLeast(0) >= story.unlockDistance -> StoryState.UNREAD
        else -> StoryState.LOCKED
    }
    fun bookmark(page: Int, total: Int): Int = page.coerceIn(0, (total - 1).coerceAtLeast(0))
    fun canFinish(story: Story, page: Int, state: StoryState): Boolean =
        story.pages.isNotEmpty() && page == story.pages.lastIndex && state in setOf(StoryState.UNREAD, StoryState.READ)
}

object StoryParser {
    private val ids = setOf("mint", "cheese", "popo", "lumi", "nunu", "piko")
    fun parse(text: String): List<Story> {
        val a = JSONArray(text)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            val p = o.getJSONArray("pages")
            val pages = List(p.length()) { j ->
                val page = p.getJSONObject(j); val b = page.getJSONArray("blocks")
                StoryPage(page.getString("title"), List(b.length()) { k ->
                    val block = b.getJSONObject(k)
                    StoryBlock(if (block.isNull("speaker")) null else block.getString("speaker"), block.getString("text"))
                }.also { blocks -> require(blocks.isNotEmpty() && blocks.all { it.text.isNotBlank() }) })
            }
            Story(o.getString("id"), o.getString("characterId"), o.getString("title"), o.getString("body"),
                o.getString("status"), o.getString("kind"), o.getInt("order"), o.getLong("unlockDistance"),
                o.getString("summary"), o.optString("afterReading"), pages).also { s ->
                require(s.characterId in ids && s.id.startsWith(s.characterId + "_"))
                require(s.status in setOf("ready", "outline") && s.kind in setOf("background", "episode"))
                require(s.title.isNotBlank() && s.body.isNotBlank() && s.unlockDistance >= 0)
                require(if (s.status == "ready") s.pages.isNotEmpty() else s.pages.isEmpty())
            }
        }.also { stories ->
            require(stories.map { it.id }.distinct().size == stories.size)
            require(stories.map { it.characterId }.toSet() == ids)
        }
    }
}
