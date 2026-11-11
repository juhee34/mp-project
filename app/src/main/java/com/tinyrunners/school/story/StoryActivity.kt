package com.tinyrunners.school.story

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import com.tinyrunners.school.R
import com.tinyrunners.school.auth.AuthService
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.game.GameActivity
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.ui.Page

/** Loads the story scripts from assets. */
object StoryRepository {
    fun load(context: Context): List<Story> =
        Story.parseAll(context.assets.open("content/village_stories.json").bufferedReader().use { it.readText() })
}

/** Set by the result screen when a run that a story asked for has finished; the story then moves on. */
object StoryRunSignal {
    var finishedFor: String? = null
}

/**
 * Dialogue screen: tap to continue, a skip button, at most a couple of choices, and one run with the friend.
 * The player has no picture; their lines are shown with their nickname.
 */
class StoryActivity : Activity() {
    override fun onPause() {
        com.tinyrunners.school.audio.Music.pauseSoon()
        super.onPause()
    }

    private lateinit var player: StoryPlayer
    private lateinit var page: Page
    private var waitingForRun = false
    private var markedRead = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val story = StoryRepository.load(this).firstOrNull { it.id == intent.getStringExtra(EXTRA_STORY_ID) }
        val friend = story?.let { CharacterRepository(this).find(it.characterId) }
        if (story == null || friend == null) { finish(); return }
        player = StoryPlayer(story).apply { restore(savedInstanceState?.getIntArray(STATE_POSITION)) }
        waitingForRun = savedInstanceState?.getBoolean(STATE_WAITING) ?: false
        markedRead = savedInstanceState?.getBoolean(STATE_READ) ?: false
        page = Page(this).show()
        page.root.setOnClickListener { onTap() }
        render()
    }

    override fun onResume() {
        super.onResume()
        com.tinyrunners.school.audio.Music.playStory(this)
        if (!::player.isInitialized) return
        if (waitingForRun && StoryRunSignal.finishedFor == player.story.id) {
            StoryRunSignal.finishedFor = null
            waitingForRun = false
            player.completeRun()
            render()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!::player.isInitialized) return
        outState.putIntArray(STATE_POSITION, player.save())
        outState.putBoolean(STATE_WAITING, waitingForRun)
        outState.putBoolean(STATE_READ, markedRead)
    }

    private fun onTap() {
        if (player.current is Step.Line) { player.advance(); render() }
    }

    private fun render() {
        val story = player.story
        val friends = CharacterRepository(this)
        val friend = friends.find(story.characterId) ?: return
        page.body.removeAllViews()
        page.eyebrow(getString(R.string.story_eyebrow, friend.name))
        page.title(story.title)
        if (!story.scripted) page.text(getString(R.string.story_outline_notice), 13f)

        when (val step = player.current) {
            is Step.Line -> {
                val card = page.card(16)
                card.setOnClickListener { onTap() }
                val who = when (step.speaker) {
                    Story.PLAYER -> AuthService.currentName(this) ?: getString(R.string.story_player_default)
                    Story.NARRATOR -> null
                    else -> friends.find(step.speaker)?.name ?: step.speaker
                }
                if (who != null) page.line(card, who, 14f, Page.LEAF, bold = true, top = 0)
                page.line(card, step.text, 18f, Page.INK, top = if (who == null) 0 else 6)
                page.text(getString(R.string.story_tap_hint), 13f)
            }
            is Step.Choice -> {
                page.text(getString(R.string.story_choice_hint), 15f, Page.INK, top = 16)
                step.options.forEachIndexed { i, option ->
                    page.button(option.text) { player.choose(i); render() }
                }
            }
            Step.Run -> {
                page.text(getString(R.string.story_run_hint, friend.name), 16f, Page.INK, top = 16)
                page.button(getString(R.string.story_run_button), primary = true) {
                    waitingForRun = true
                    startActivity(GameActivity.intent(this, friend.id).putExtra(GameActivity.EXTRA_STORY_ID, story.id))
                }
            }
            null -> renderEnd(friend.name)
        }
        if (!player.finished) {
            page.button(getString(R.string.story_skip)) { player.skip(); render() }
        }
    }

    private fun renderEnd(friendName: String) {
        val card = page.card(16)
        page.line(card, getString(R.string.story_end, friendName), 18f, Page.INK, bold = true, top = 0)
        val status = page.text(getString(R.string.result_saving), 14f)
        if (!markedRead) {
            ProgressStore.forCurrentUser(this).markStoryRead(player.story.characterId, player.story.id) { result ->
                if (isDestroyed) return@markStoryRead
                markedRead = result.isSuccess
                status.text = getString(if (result.isSuccess) R.string.story_saved else R.string.story_save_failed)
            }
        } else {
            status.text = getString(R.string.story_saved)
        }
        page.button(getString(R.string.story_back_to_list), primary = true) { finish() }
    }

    companion object {
        const val EXTRA_STORY_ID = "storyId"
        private const val STATE_POSITION = "position"
        private const val STATE_WAITING = "waitingForRun"
        private const val STATE_READ = "markedRead"

        fun intent(context: Context, storyId: String) =
            Intent(context, StoryActivity::class.java).putExtra(EXTRA_STORY_ID, storyId)
    }
}

/** Every friend's stories with read / new / locked state. A's collection screen can replace this later. */
class StoryListActivity : Activity() {
    override fun onPause() {
        com.tinyrunners.school.audio.Music.pauseSoon()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        com.tinyrunners.school.audio.Music.playStory(this)
        val page = Page(this).show()
        page.eyebrow(getString(R.string.characters_brand))
        page.title(getString(R.string.story_list_title))
        page.text(getString(R.string.story_list_body))
        val status = page.text(getString(R.string.records_loading), 14f)
        val stories = StoryRepository.load(this)
        val friends = CharacterRepository(this).characters
        ProgressStore.forCurrentUser(this).loadAll { result ->
            if (isDestroyed) return@loadAll
            val all = result.getOrNull().orEmpty()
            status.text = if (result.isSuccess) "" else getString(R.string.records_load_failed)
            friends.forEach { friend ->
                val read = all[friend.id]?.readStoryIds.orEmpty().toSet()
                val card: LinearLayout = page.card()
                page.line(card, friend.name, 18f, Page.INK, bold = true, top = 0)
                stories.filter { it.characterId == friend.id }.forEach { story ->
                    val state = when {
                        story.id in read -> R.string.story_state_read
                        story.isUnlocked(read) -> R.string.story_state_new
                        else -> R.string.story_state_locked
                    }
                    page.line(card, getString(R.string.story_list_item, story.title, getString(state)))
                    if (story.isUnlocked(read)) {
                        card.addView(android.widget.Button(this).apply {
                            text = getString(if (story.id in read) R.string.story_open_again else R.string.story_open)
                            isAllCaps = false
                            setTextColor(Page.LEAF)
                            minHeight = page.dp(48)
                            background = page.shape(Page.MINT, 12)
                            setOnClickListener { startActivity(StoryActivity.intent(this@StoryListActivity, story.id)) }
                        }, page.params(6))
                    }
                }
            }
            page.button(getString(R.string.common_back)) { finish() }
        }
    }
}
