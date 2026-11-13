package com.tinyrunners.school.story

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.tinyrunners.school.R
import com.tinyrunners.school.data.CharacterDefinition
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.talk.ShortLineRepository
import com.tinyrunners.school.ui.HomeButton
import com.tinyrunners.school.ui.MockupFrame
import kotlin.math.roundToInt

/**
 * 마을 이야기 (스토리 진행하기), built like 이야기 모음 so the two story screens read as one place: A's paper picture
 * with the frame, the trees and the bench filling the screen (bg_village_stories_wide), the title drawn as the
 * collection's is, "◀ 처음으로" top-left, the storybook (이야기 모음) | bag top-right, one storybook card per friend
 * on the bench (StoryBookCard: the friend's home, "새 이야기" or "다 봤어요", and "이 친구와 대화하기", which opens
 * that friend's village story), sliding sideways with page dots and ◀ ▶ on the grass.
 */
class StoryListActivity : Activity(), com.tinyrunners.school.audio.MusicScreen {
    /** Story screens play the main theme. */
    override fun musicTrack(): Int = com.tinyrunners.school.R.raw.main_theme

    private val friends by lazy { CharacterRepository(this).characters }
    private val stories by lazy { StoryRepository.load(this) }
    private var read: Set<String> = emptySet()
    private var loaded = false
    private val quotes = mutableMapOf<String, String?>()
    /** The friend this screen was opened on (from a run result or the bag): its card is brought into view first. */
    private var focus: String? = null
    private var firstShow = true

    private lateinit var frame: MockupFrame
    private lateinit var row: LinearLayout
    private lateinit var scroller: HorizontalScrollView
    private lateinit var pager: com.tinyrunners.school.ui.ShelfPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        focus = savedInstanceState?.getString(KEY_FOCUS) ?: intent.getStringExtra(EXTRA_FRIEND)
        frame = MockupFrame(this, R.drawable.bg_village_stories_wide)

        // ◀ 처음으로 in the collection's back part (same place as its ◀ 마을 이야기로).
        val back = Button(this).apply {
            text = getString(R.string.common_to_menu).removePrefix("◀ ")
            isAllCaps = false; stateListAnimator = null; setTextColor(StoryBookCard.DEEP_GREEN); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            background = RippleDrawable(ColorStateList.valueOf(0x22000000), getDrawable(R.drawable.ui_story_back_button), null)
            setOnClickListener { HomeButton.goToMenu(this@StoryListActivity) }
        }.also { frame.onScaleText(it, 34f) }
        frame.place(back, 47, 41, 371, 112)
        frame.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val sc = frame.scale
            back.setPadding(((128 - 47) * sc).roundToInt(), 0, ((371 - 350) * sc).roundToInt(), 0)
        }
        frame.place(TitleBanner(this, frame, getString(R.string.story_list_title)), 436, 20, 1236, 150)
        // 이야기 모음 (storybook) | bag, where the collection has its counter | bag.
        frame.place(com.tinyrunners.school.ui.BookButton.create(this) {
            startActivity(Intent(this, StoryCollectionActivity::class.java))
        }, 1451, 34, 1526, 120)
        frame.place(android.widget.ImageButton(this).apply {
            setImageDrawable(null)
            background = RippleDrawable(ColorStateList.valueOf(0x22000000), getDrawable(R.drawable.ui_story_bag_button), null)
            contentDescription = getString(R.string.bag_open)
            setOnClickListener { startActivity(com.tinyrunners.school.quest.BagActivity.intent(this@StoryListActivity)) }
        }, 1562, 34, 1637, 120)

        // Cards on the bench; they slide sideways.
        row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        scroller = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; clipToPadding = false; addView(row) }
        frame.place(scroller, 40, 205, 1640, 812)
        pager = com.tinyrunners.school.ui.ShelfPager(this, scroller)
        frame.place(pager, 436, 842, 1236, 912)

        setContentView(frame)
        hideSystemBars()
        render()
    }

    override fun onResume() {
        super.onResume()
        // Re-read progress each time: finishing a story marks it read.
        ProgressStore.forCurrentUser(this).loadAll { result ->
            if (isDestroyed) return@loadAll
            read = result.getOrNull().orEmpty().values.flatMap { it.readStoryIds }.toSet()
            loaded = true
            render()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_FOCUS, focus)
        super.onSaveInstanceState(outState)
    }

    /** The story to open: the first unlocked one not read yet, or the last read one to see again. */
    private fun storyFor(friend: CharacterDefinition): Pair<Story?, Boolean> {
        Story.nextUnread(stories, friend.id, read)?.let { return it to false }
        return stories.lastOrNull { it.characterId == friend.id && it.id in read } to true
    }

    private fun render() {
        row.removeAllViews()
        row.addView(RefBox(this, frame, 25, 1))   // the first card starts where the collection's does
        friends.forEach { row.addView(card(it)) }
        row.addView(RefBox(this, frame, 25, 1))
        val start = if (firstShow) friends.indexOfFirst { it.id == focus } else -1
        firstShow = false
        scroller.post {
            if (start > 0) row.getChildAt(start + 1)?.let { scroller.scrollTo(it.left - (25 * frame.scale).roundToInt(), 0) }
            pager.refresh()
        }
    }

    private fun card(friend: CharacterDefinition): View {
        val (story, again) = storyFor(friend)
        val pill = when {
            !loaded -> getString(R.string.records_loading)
            story == null -> null
            again -> getString(R.string.village_chip_read)
            else -> getString(R.string.village_chip_new)
        }
        val look = when {
            story == null -> StoryBookCard.Look.LOCKED
            again -> StoryBookCard.Look.OUTLINE
            else -> StoryBookCard.Look.PRIMARY
        }
        val button = getString(when {
            story == null -> R.string.collection_locked
            again -> R.string.village_talk_again
            else -> R.string.village_talk
        })
        val line = quotes.getOrPut(friend.id) { ShortLineRepository.pick(this, friend.id, "select") }
        return StoryBookCard.build(this, frame, StoryBookCard.ribbon(friend.id), friend.name, StoryBookCard.picture(this, friend.id),
            pill, button, look) { openTalk(friend) }.apply {
            contentDescription = getString(R.string.village_card_description_short, friend.name, friend.home, line ?: "")
        }
    }

    private fun openTalk(friend: CharacterDefinition) {
        val (story, again) = storyFor(friend)
        if (story == null) return
        focus = friend.id
        // Told as an RPG-style talk on the friend's background. A first visit starts with the friend asking for their
        // items; seeing a story again skips the request.
        startActivity(com.tinyrunners.school.talk.TalkActivity.village(this, story.characterId, story.id, askItems = !again))
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    companion object {
        private const val KEY_FOCUS = "villageFocus"
        private const val EXTRA_FRIEND = "villageFriend"

        /** The village list, opened on [friendId]'s card. */
        fun intent(context: android.content.Context, friendId: String) =
            android.content.Intent(context, StoryListActivity::class.java).putExtra(EXTRA_FRIEND, friendId)
    }
}
