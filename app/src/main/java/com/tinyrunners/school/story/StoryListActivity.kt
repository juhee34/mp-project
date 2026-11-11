package com.tinyrunners.school.story

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.data.CharacterDefinition
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.talk.ShortLineRepository
import com.tinyrunners.school.ui.HomeButton
import com.tinyrunners.school.ui.Page
import com.tinyrunners.school.ui.SceneryView
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 마을 이야기 (스토리 진행하기): like the friend selection, one friend at a time on a stage that swipes sideways.
 * Next to the friend: name, home and one line in their own voice (A's short line pack, "select" lines), then
 * "이 친구와 대화하기" opens that friend's village story. Top: "◀ 처음으로" and "이야기 모음 ▶".
 */
class StoryListActivity : Activity() {
    private val friends by lazy { CharacterRepository(this).characters }
    private val stories by lazy { StoryRepository.load(this) }
    private var index = 0
    private var read: Set<String> = emptySet()
    private var loaded = false
    private val quotes = mutableMapOf<String, String?>()

    private lateinit var card: LinearLayout
    private lateinit var sprite: ImageView
    private lateinit var spot: FrameLayout
    private lateinit var name: TextView
    private lateinit var tag: TextView
    private lateinit var quote: TextView
    private lateinit var status: TextView
    private lateinit var talk: Button
    private lateinit var dots: LinearLayout
    private var frame = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        index = savedInstanceState?.getInt(KEY_INDEX)
            ?: friends.indexOfFirst { it.id == intent.getStringExtra(EXTRA_FRIEND) }.coerceAtLeast(0)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(6), dp(16), dp(12)) }

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(com.tinyrunners.school.ui.BagButton.create(this))
        top.addView(HomeButton.create(this))
        top.addView(TextView(this).apply {
            setText(R.string.story_list_title); textSize = 22f; setTextColor(Page.INK); setTypeface(typeface, Typeface.BOLD)
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            setPadding(dp(12), 0, dp(12), 0)
        })
        dots = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(dots, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(HomeButton.create(this, getString(R.string.collection_open)) {
            startActivity(Intent(this, StoryCollectionActivity::class.java))
        })
        content.addView(top)

        // Stage: ◀ [friend + their line] ▶
        val stage = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        card = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(12), dp(22), dp(12)) }
        spot = FrameLayout(this)
        sprite = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        spot.addView(sprite, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        card.addView(spot, LinearLayout.LayoutParams(dp(150), dp(150)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), 0, 0, 0) }
        name = TextView(this).apply { textSize = 26f; setTextColor(Page.INK); setTypeface(typeface, Typeface.BOLD) }
        tag = TextView(this).apply { textSize = 13f; setTextColor(Page.LEAF); setTypeface(typeface, Typeface.BOLD); setPadding(dp(10), dp(3), dp(10), dp(3)) }
        // The friend's own line, in a speech bubble, introduces them.
        quote = TextView(this).apply { textSize = 18f; setTextColor(Page.INK); setLineSpacing(dp(2).toFloat(), 1f); setPadding(dp(16), dp(12), dp(16), dp(12)) }
        status = TextView(this).apply { textSize = 13f; setTextColor(Page.MUTED) }
        info.addView(name)
        info.addView(tag, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
        info.addView(quote, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        info.addView(status, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        card.addView(info, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        stage.addView(arrow("◀", R.string.characters_prev) { go(-1) }, LinearLayout.LayoutParams(dp(48), dp(48)))
        stage.addView(card, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { marginStart = dp(8); marginEnd = dp(8) })
        stage.addView(arrow("▶", R.string.characters_next) { go(1) }, LinearLayout.LayoutParams(dp(48), dp(48)))
        content.addView(stage, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(6) })

        talk = Button(this).apply {
            textSize = 16f; isAllCaps = false; setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD)
            minHeight = dp(52)
            background = RippleDrawable(ColorStateList.valueOf(Page.MINT), GradientDrawable().apply { setColor(Page.LEAF); cornerRadius = dp(16).toFloat() }, null)
            stateListAnimator = null
            setOnClickListener { openTalk() }
        }
        content.addView(talk, LinearLayout.LayoutParams(dp(360), LinearLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(8) })

        // Sideways swipe on the stage: compare where the finger went down and lifted.
        var downX = 0f; var downY = 0f
        @Suppress("ClickableViewAccessibility")
        card.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = e.x; downY = e.y; v.parent.requestDisallowInterceptTouchEvent(true) }
                MotionEvent.ACTION_UP -> { val dx = e.x - downX; if (abs(dx) > dp(40) && abs(dx) > abs(e.y - downY)) go(if (dx < 0) 1 else -1) }
            }
            true
        }

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Page.BACKGROUND)
            addView(SceneryView(this@StoryListActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
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
        bind()
        idle()
    }

    override fun onResume() {
        super.onResume()
        com.tinyrunners.school.audio.Music.playStory(this)
        // Re-read progress each time: finishing a story marks it read.
        ProgressStore.forCurrentUser(this).loadAll { result ->
            if (isDestroyed) return@loadAll
            read = result.getOrNull().orEmpty().values.flatMap { it.readStoryIds }.toSet()
            loaded = true
            bind()
        }
    }

    override fun onPause() {
        com.tinyrunners.school.audio.Music.pauseSoon()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_INDEX, index)
        super.onSaveInstanceState(outState)
    }

    /** The story to open: the first unlocked one not read yet, or the last read one to see again. */
    private fun storyFor(friend: CharacterDefinition): Pair<Story?, Boolean> {
        Story.nextUnread(stories, friend.id, read)?.let { return it to false }
        return stories.lastOrNull { it.characterId == friend.id && it.id in read } to true
    }

    private fun bind() {
        if (friends.isEmpty()) return
        index = index.coerceIn(0, friends.lastIndex)
        val friend = friends[index]
        val tint = try { Color.parseColor(friend.color) } catch (_: IllegalArgumentException) { Page.LEAF }
        card.background = GradientDrawable().apply { setColor(Page.PAPER); cornerRadius = dp(24).toFloat(); setStroke(dp(3), tint) }
        spot.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(blend(tint, Page.PAPER, 0.55f)) }
        sprite.setImageDrawable(spriteFrame(friend))
        name.text = friend.name
        tag.text = getString(R.string.characters_species_home, friend.species, friend.home)
        tag.background = GradientDrawable().apply { setColor(blend(tint, Page.PAPER, 0.7f)); cornerRadius = dp(12).toFloat() }
        val line = quotes.getOrPut(friend.id) { ShortLineRepository.pick(this, friend.id, "select") }
        quote.text = line?.let { getString(R.string.village_quote, it) } ?: friend.personality
        quote.background = GradientDrawable().apply { setColor(blend(tint, Page.PAPER, 0.82f)); cornerRadius = dp(18).toFloat() }
        val (story, again) = storyFor(friend)
        status.text = when {
            !loaded -> getString(R.string.records_loading)
            story == null -> getString(R.string.village_none)
            again -> getString(R.string.village_status_read, story.title)
            else -> getString(R.string.village_status_new, story.title)
        }
        talk.text = getString(if (again) R.string.village_talk_again else R.string.village_talk)
        talk.isEnabled = story != null
        talk.alpha = if (story != null) 1f else 0.5f
        card.contentDescription = getString(R.string.village_card_description, index + 1, friends.size, friend.name, friend.home, quote.text)
        dots.removeAllViews()
        friends.forEachIndexed { i, f ->
            val c = try { Color.parseColor(f.color) } catch (_: IllegalArgumentException) { Page.LINE }
            dots.addView(View(this).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(if (i == index) c else Page.LINE) }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(if (i == index) 14 else 10), dp(if (i == index) 14 else 10)).apply { marginStart = dp(6) })
        }
    }

    private fun go(step: Int) {
        val out = if (step > 0) -dp(60).toFloat() else dp(60).toFloat()
        card.animate().translationX(out).alpha(0f).setDuration(120).withEndAction {
            index = (index + step + friends.size) % friends.size
            bind()
            card.translationX = -out
            card.animate().translationX(0f).alpha(1f).setDuration(160).start()
            card.announceForAccessibility(card.contentDescription)
        }.start()
    }

    private fun openTalk() {
        val (story, again) = storyFor(friends[index])
        if (story == null) return
        // Told as an RPG-style talk on the friend's background. A first visit starts with the friend asking for their
        // items; seeing a story again skips the request.
        startActivity(com.tinyrunners.school.talk.TalkActivity.village(this, story.characterId, story.id, askItems = !again))
    }

    /** The friend breathes between the two idle frames and bobs a little. */
    private fun idle() {
        sprite.postDelayed(object : Runnable {
            override fun run() {
                if (isDestroyed) return
                frame++
                if (friends.isNotEmpty()) sprite.setImageDrawable(spriteFrame(friends[index]))
                sprite.translationY = if (frame % 2 == 0) 0f else -dp(4).toFloat()
                sprite.postDelayed(this, 520)
            }
        }, 520)
    }

    private fun spriteFrame(friend: CharacterDefinition) = CharacterSprites.load(resources, friend.id)?.let {
        BitmapDrawable(resources, it[if (frame % 2 == 0) SpriteFrame.IDLE_0 else SpriteFrame.IDLE_1]).apply { isFilterBitmap = false; setAntiAlias(false) }
    }

    @SuppressLint("SetTextI18n")
    private fun arrow(symbol: String, description: Int, click: () -> Unit) = Button(this).apply {
        text = symbol; textSize = 18f; setTextColor(Page.LEAF)
        contentDescription = getString(description)
        setPadding(0, 0, 0, 0)
        background = RippleDrawable(ColorStateList.valueOf(Page.MINT), GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setColor(Page.PAPER); setStroke(dp(1), Page.LINE)
        }, null)
        stateListAnimator = null
        setOnClickListener { click() }
    }

    private fun blend(a: Int, b: Int, t: Float) = Color.rgb(
        (Color.red(a) * (1 - t) + Color.red(b) * t).roundToInt(),
        (Color.green(a) * (1 - t) + Color.green(b) * t).roundToInt(),
        (Color.blue(a) * (1 - t) + Color.blue(b) * t).roundToInt())

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val KEY_INDEX = "friendIndex"
        private const val EXTRA_FRIEND = "villageFriend"

        /** The village list, opened on [friendId]'s page. */
        fun intent(context: android.content.Context, friendId: String) =
            android.content.Intent(context, StoryListActivity::class.java).putExtra(EXTRA_FRIEND, friendId)
    }
}
