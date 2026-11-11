package com.tinyrunners.school.story

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import com.tinyrunners.school.R
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.talk.PrologueActivity
import com.tinyrunners.school.talk.TalkActivity
import com.tinyrunners.school.talk.TalkProgress
import com.tinyrunners.school.ui.Page

/**
 * 이야기 모음 (from the village story list, top-right): everything heard so far, in order, to watch again.
 * 1. the world story (the black screen), 2. each friend's long first talk once it has been seen,
 * 3. the village stories already finished. Nothing here changes progress; it only replays.
 */
class StoryCollectionActivity : Activity() {
    override fun onPause() {
        com.tinyrunners.school.audio.Music.pauseSoon()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        com.tinyrunners.school.audio.Music.playStory(this)
        val page = Page(this).show()
        page.backPill(getString(R.string.collection_back)) { finish() }
        page.eyebrow(getString(R.string.characters_brand))
        page.title(getString(R.string.collection_title))
        page.text(getString(R.string.collection_body))

        // 1. World story.
        val world = page.card(16)
        page.line(world, getString(R.string.collection_world), 17f, Page.INK, bold = true, top = 0)
        page.line(world, getString(R.string.collection_world_body))
        world.addView(replayButton(page) { startActivity(PrologueActivity.replay(this)) }, page.params(8))

        // 2. First talks with each friend (only the ones already seen).
        val friends = CharacterRepository(this).characters
        val seenTalks = TalkProgress(this)
        val talks = page.card(16)
        page.line(talks, getString(R.string.collection_talks), 17f, Page.INK, bold = true, top = 0)
        friends.forEach { friend ->
            if (seenTalks.shouldShowTalk(friend.id)) {
                page.line(talks, getString(R.string.collection_talk_locked, friend.name), 14f)
            } else {
                page.line(talks, getString(R.string.collection_talk, friend.name), 15f, Page.INK, top = 10)
                talks.addView(replayButton(page) { startActivity(TalkActivity.replay(this, friend.id)) }, page.params(4))
            }
        }

        // 3. Village stories already finished (saved with the account).
        val village = page.card(16)
        page.line(village, getString(R.string.collection_village), 17f, Page.INK, bold = true, top = 0)
        val status = page.line(village, getString(R.string.records_loading), 14f)
        val stories = StoryRepository.load(this)
        ProgressStore.forCurrentUser(this).loadAll { result ->
            if (isDestroyed) return@loadAll
            val read = result.getOrNull().orEmpty().values.flatMap { it.readStoryIds }.toSet()
            val finished = stories.filter { it.id in read }
            status.text = when {
                result.isFailure -> getString(R.string.records_load_failed)
                finished.isEmpty() -> getString(R.string.collection_village_empty)
                else -> ""
            }
            finished.forEach { story ->
                val name = friends.firstOrNull { it.id == story.characterId }?.name.orEmpty()
                page.line(village, getString(R.string.story_list_item, name, story.title), 15f, Page.INK, top = 10)
                village.addView(replayButton(page) { startActivity(StoryActivity.intent(this, story.id)) }, page.params(4))
            }
        }
    }

    private fun replayButton(page: Page, click: () -> Unit) = Button(this).apply {
        setText(R.string.collection_replay)
        isAllCaps = false
        setTextColor(Page.LEAF)
        minHeight = page.dp(44)
        background = page.shape(Page.MINT, 12)
        setOnClickListener { click() }
    }.also { it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
}
