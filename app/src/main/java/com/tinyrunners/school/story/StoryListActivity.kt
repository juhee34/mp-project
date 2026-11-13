package com.tinyrunners.school.story

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.data.CharacterDefinition
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.talk.ShortLineRepository
import com.tinyrunners.school.ui.CardShelf
import com.tinyrunners.school.ui.HomeButton
import com.tinyrunners.school.ui.Page
import com.tinyrunners.school.ui.PaperBackdropView
import kotlin.math.roundToInt

/**
 * 마을 이야기 (스토리 진행하기), laid out like the shop: one small card per friend in a row that slides sideways
 * (CardShelf). Each card: the friend at home, their name and whether a new story waits, a line in their own voice
 * (A's short line pack, "select" lines), and "이 친구와 대화하기", which opens that friend's village story.
 * Top: "◀ 처음으로", the storybook button (이야기 모음) and the bag.
 */
class StoryListActivity : Activity(), com.tinyrunners.school.audio.MusicScreen {
    /** Story screens play the main theme. */
    override fun musicTrack(): Int = com.tinyrunners.school.R.raw.main_theme

    private val friends by lazy { CharacterRepository(this).characters }
    private val stories by lazy { StoryRepository.load(this) }
    private var read: Set<String> = emptySet()
    private var loaded = false
    private val quotes = mutableMapOf<String, String?>()
    /** The friend this screen was opened on (from a run result or the bag): shown first, with a green edge. */
    private var focus: String? = null
    private var firstShow = true

    private lateinit var shelf: CardShelf

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        focus = savedInstanceState?.getString(KEY_FOCUS) ?: intent.getStringExtra(EXTRA_FRIEND)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(6), dp(16), dp(10)) }

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(HomeButton.create(this))
        top.addView(TextView(this).apply {
            setText(R.string.story_list_title); textSize = 22f; setTextColor(Page.INK); setTypeface(typeface, Typeface.BOLD)
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            setPadding(dp(12), 0, dp(12), 0)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        // 이야기 모음: a small storybook button beside the bag.
        top.addView(com.tinyrunners.school.ui.BookButton.create(this) {
            startActivity(Intent(this, StoryCollectionActivity::class.java))
        })
        top.addView(com.tinyrunners.school.ui.TopPill.divider(this))
        top.addView(com.tinyrunners.school.ui.BagButton.create(this))
        content.addView(top)
        content.addView(TextView(this).apply {
            setText(R.string.village_hint); textSize = 14f; setTextColor(Page.MUTED); setPadding(dp(10), dp(3), dp(10), dp(3))
            // A soft paper strip so the hint stays readable over the tree in the picture.
            background = android.graphics.drawable.GradientDrawable().apply { setColor(android.graphics.Color.argb(215, 255, 249, 235)); cornerRadius = dp(12).toFloat() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })

        shelf = CardShelf(this)
        content.addView(shelf, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(8) })

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Page.BACKGROUND)
            addView(PaperBackdropView(this@StoryListActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(content, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            setOnApplyWindowInsetsListener { v, insets ->
                if (Build.VERSION.SDK_INT >= 30) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                } else {
                    @Suppress("DEPRECATION")
                    v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                }
                insets
            }
        })
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
        val cards = friends.map { card(it) }
        val start = if (firstShow) friends.indexOfFirst { it.id == focus } else -1
        firstShow = false
        shelf.setCards(cards, start)
    }

    private fun card(friend: CharacterDefinition): View {
        val (story, again) = storyFor(friend)
        val card = CardShelf.card(this, selected = friend.id == focus)
        card.addView(SceneThumb(this, friend.id, locked = false), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        val nameRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        nameRow.addView(CardShelf.title(this, friend.name), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        if (loaded && story != null) nameRow.addView(CardShelf.chip(this, getString(if (again) R.string.village_chip_read else R.string.village_chip_new),
            if (again) com.tinyrunners.school.shop.ShopActivity.DISABLED else com.tinyrunners.school.shop.ShopActivity.OWNED))
        card.addView(nameRow)
        val line = quotes.getOrPut(friend.id) { ShortLineRepository.pick(this, friend.id, "select") }
        card.addView(CardShelf.body(this, line?.let { getString(R.string.village_quote, it) } ?: friend.personality, lines = 2))
        card.addView(CardShelf.body(this, when {
            !loaded -> getString(R.string.records_loading)
            story == null -> getString(R.string.village_none)
            else -> story.title
        }, lines = 1).apply { setTextColor(CardShelf.GREEN) })
        card.addView(CardShelf.button(this, getString(if (again) R.string.village_talk_again else R.string.village_talk),
            primary = !again, enabled = story != null) { openTalk(friend) }, CardShelf.buttonParams(this, 6))
        card.contentDescription = getString(R.string.village_card_description_short, friend.name, friend.home, line ?: "")
        return card
    }

    private fun openTalk(friend: CharacterDefinition) {
        val (story, again) = storyFor(friend)
        if (story == null) return
        focus = friend.id
        // Told as an RPG-style talk on the friend's background. A first visit starts with the friend asking for their
        // items; seeing a story again skips the request.
        startActivity(com.tinyrunners.school.talk.TalkActivity.village(this, story.characterId, story.id, askItems = !again))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val KEY_FOCUS = "villageFocus"
        private const val EXTRA_FRIEND = "villageFriend"

        /** The village list, opened on [friendId]'s card. */
        fun intent(context: android.content.Context, friendId: String) =
            android.content.Intent(context, StoryListActivity::class.java).putExtra(EXTRA_FRIEND, friendId)
    }
}
