package com.tinyrunners.school.story

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.data.CharacterDefinition
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.opening.OpeningFriend
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.talk.ShortLineRepository
import com.tinyrunners.school.ui.HomeButton
import com.tinyrunners.school.ui.MockupFrame
import kotlin.math.roundToInt

/**
 * 스토리 진행 (from 스토리 진행하기), in A's final village design (TinyRunners-final-ui-assets, village): the village
 * picture under a cream header ("◀ 돌아가기", the title between two flowers, "이야기 모음" | bag), one friend standing
 * in the meadow, and a panel on the right with the friend's name, a short line in a speech bubble, where they live and
 * how far their stories have gone, and the talk button. ◀ ▶ beside the name and the dots under the button go through
 * the six friends; tapping the friend or the button opens that friend's next village story (or the last one again).
 *
 * The village picture is wider than 16:9 (the meadow behind the panel repeats); the header buttons and the panel keep
 * to the picture's frame on wide phones and to the screen's edges on 16:9 ones.
 */
class StoryListActivity : Activity(), com.tinyrunners.school.audio.MusicScreen {
    /** Story screens play the main theme. */
    override fun musicTrack(): Int = com.tinyrunners.school.R.raw.main_theme

    private val friends by lazy { CharacterRepository(this).characters }
    private val stories by lazy { StoryRepository.load(this) }
    private var read: Set<String> = emptySet()
    private var loaded = false
    private val quotes = mutableMapOf<String, String?>()
    /** The friend shown now. */
    private var index = 0

    private lateinit var frame: MockupFrame
    private lateinit var name: TextView
    private lateinit var line: TextView
    private lateinit var status: StatusLine
    private lateinit var talk: Button
    private lateinit var dots: LinearLayout
    private var friendView: View? = null

    /** Views that keep to the left or right edge of the picture's frame, with their rects on the 1672 canvas. */
    private class Anchored(val view: View, val x0: Int, val y0: Int, val x1: Int, val y1: Int, val right: Boolean)
    private val anchored = mutableListOf<Anchored>()
    private var shift = Float.NaN to Float.NaN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val focus = savedInstanceState?.getString(KEY_FOCUS) ?: intent.getStringExtra(EXTRA_FRIEND)
        index = friends.indexOfFirst { it.id == focus }.coerceAtLeast(0)
        frame = MockupFrame(this, R.drawable.bg_village_scene_wide)

        // Header: ◀ 돌아가기 | title between the flowers | 이야기 모음 | bag.
        val back = Button(this).apply {
            text = getString(R.string.common_back_pill).removePrefix("◀ ")
            isAllCaps = false; stateListAnimator = null; setTextColor(StoryBookCard.DEEP_GREEN); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            maxLines = 1; isSingleLine = true
            background = RippleDrawable(ColorStateList.valueOf(0x22000000), getDrawable(R.drawable.ui_story_back_button), null)
            setOnClickListener { HomeButton.goBack(this@StoryListActivity) }
        }.also { frame.onScaleText(it, 32f) }
        anchor(back, 42, 33, 328, 104, right = false)
        frame.place(ImageView(this).apply {
            setImageResource(R.drawable.ui_village_title); scaleType = ImageView.ScaleType.FIT_XY
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, 520, 18, 1150, 130)
        frame.place(TextView(this).apply {
            text = getString(R.string.story_list_title); setTextColor(StoryBookCard.DEEP_GREEN); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; maxLines = 1; includeFontPadding = false
        }.also { frame.onScaleText(it, 62f) }, 615, 18, 1060, 100)
        // A's own 이야기 모음 button (its words are part of the picture).
        anchor(ImageButton(this).apply {
            setImageDrawable(null); contentDescription = getString(R.string.collection_open)
            background = RippleDrawable(ColorStateList.valueOf(0x22000000), getDrawable(R.drawable.ui_village_collection_button), null)
            setOnClickListener { startActivity(Intent(this@StoryListActivity, StoryCollectionActivity::class.java)) }
        }, 1253, 33, 1512, 103, right = true)
        anchor(View(this).apply { setBackgroundColor(StoryBookCard.DEEP_GREEN) }, 1529, 42, 1533, 96, right = true)
        anchor(ImageButton(this).apply {
            setImageDrawable(null); contentDescription = getString(R.string.bag_open)
            background = RippleDrawable(ColorStateList.valueOf(0x22000000), getDrawable(R.drawable.ui_story_bag_button), null)
            setOnClickListener { startActivity(com.tinyrunners.school.quest.BagActivity.intent(this@StoryListActivity)) }
        }, 1556, 28, 1639, 109, right = true)

        // The panel: name between ◀ ▶, the line in the bubble, the status, the button and the friend dots.
        anchor(ImageView(this).apply {
            setImageResource(R.drawable.ui_village_panel); scaleType = ImageView.ScaleType.FIT_XY
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, 1010, 196, 1632, 868, right = true)
        name = TextView(this).apply {
            setTextColor(StoryBookCard.DEEP_GREEN); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            maxLines = 1; includeFontPadding = false
        }.also { frame.onScaleText(it, 66f) }
        anchor(name, 1208, 236, 1436, 330, right = true)
        anchor(arrow("◀", R.string.village_prev_friend) { show(index - 1) }, 1040, 248, 1108, 316, right = true)
        anchor(arrow("▶", R.string.village_next_friend) { show(index + 1) }, 1534, 248, 1602, 316, right = true)
        line = TextView(this).apply {
            setTextColor(StoryBookCard.DEEP_GREEN); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER_VERTICAL
            maxLines = 3; ellipsize = android.text.TextUtils.TruncateAt.END; setLineSpacing(0f, 1.1f)
            // Korean lines break between words, not inside one.
            if (Build.VERSION.SDK_INT >= 33) lineBreakWordStyle = android.graphics.text.LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE
        }.also { frame.onScaleText(it, 40f) }
        anchor(line, 1108, 396, 1572, 572, right = true)
        status = StatusLine(this, frame)
        anchor(status, 1062, 610, 1598, 670, right = true)
        talk = Button(this).apply {
            isAllCaps = false; stateListAnimator = null; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            setOnClickListener { openTalk(friends[index]) }
        }.also { frame.onScaleText(it, 46f) }
        anchor(talk, 1052, 697, 1590, 809, right = true)
        dots = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        anchor(dots, 1062, 818, 1580, 858, right = true)

        frame.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            // The words right of the part's arrow, never cut at the top or bottom.
            back.setPadding((86 * frame.scale).roundToInt(), 0, (18 * frame.scale).roundToInt(), 0)
            keepToFrame()
        }
        setContentView(frame)
        hideSystemBars()
        show(index)
    }

    override fun onResume() {
        super.onResume()
        // Re-read progress each time: finishing a story marks it read.
        ProgressStore.forCurrentUser(this).loadAll { result ->
            if (isDestroyed) return@loadAll
            read = result.getOrNull().orEmpty().values.flatMap { it.readStoryIds }.toSet()
            loaded = true
            show(index)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        friends.getOrNull(index)?.let { outState.putString(KEY_FOCUS, it.id) }
        super.onSaveInstanceState(outState)
    }

    private fun anchor(view: View, x0: Int, y0: Int, x1: Int, y1: Int, right: Boolean) {
        anchored += Anchored(view, x0, y0, x1, y1, right)
        frame.place(view, x0, y0, x1, y1)
    }

    /**
     * The picture is centred and reaches PICTURE_LEFT..PICTURE_RIGHT on the 1672 canvas; what keeps to its frame moves
     * to the frame (wide phones) or to the screen's edge (16:9, where the frame is cut off).
     */
    private fun keepToFrame() {
        val left = maxOf(frame.visibleLeft, PICTURE_LEFT)
        val right = minOf(frame.visibleRight, PICTURE_RIGHT)
        if (left == shift.first && right == shift.second) return
        shift = left to right
        frame.post {
            anchored.forEach { a ->
                val dx = (if (a.right) right - MockupFrame.REF_W else left).roundToInt()
                frame.move(a.view, a.x0 + dx, a.y0, a.x1 + dx, a.y1)
            }
        }
    }

    /** The story to open: the first unlocked one not read yet, or the last read one to see again. */
    private fun storyFor(friend: CharacterDefinition): Pair<Story?, Boolean> {
        Story.nextUnread(stories, friend.id, read)?.let { return it to false }
        return stories.lastOrNull { it.characterId == friend.id && it.id in read } to true
    }

    /** Shows friend [i] (wrapping round the six). */
    private fun show(i: Int) {
        if (friends.isEmpty()) return
        index = ((i % friends.size) + friends.size) % friends.size
        val friend = friends[index]
        val (story, again) = storyFor(friend)

        // The friend in the meadow where A's picture has them, in front of the homes (the picture's own place, so it
        // does not move with the screen's edges). A fresh view each time so the idle steps start over.
        friendView?.let { frame.removeView(it) }
        val left = FRIEND_X - FRIEND_BOX / 2 + PICTURE_LEFT.roundToInt()
        friendView = OpeningFriend(this, friend.id, 0).apply {
            setOnClickListener { openTalk(friend) }
            contentDescription = friend.name
        }.also { frame.place(it, left, FRIEND_FEET - FRIEND_BOX, left + FRIEND_BOX, FRIEND_FEET) }

        name.text = friend.name
        line.text = quotes.getOrPut(friend.id) { ShortLineRepository.pick(this, friend.id, "select") } ?: friend.home
        status.set(friend.home, when {
            !loaded -> StatusLine.Mark.LOADING to getString(R.string.records_loading)
            story == null -> StatusLine.Mark.LOCKED to getString(R.string.collection_locked)
            again -> StatusLine.Mark.READ to getString(R.string.village_chip_read)
            else -> StatusLine.Mark.NEW to getString(R.string.village_chip_new)
        })
        val (part, ink) = when {
            story == null -> R.drawable.ui_story_btn_disabled to Color.parseColor("#8A8478")
            again -> R.drawable.ui_story_btn_outline to StoryBookCard.DEEP_GREEN
            else -> R.drawable.ui_story_btn_primary to StoryBookCard.CREAM
        }
        talk.text = getString(when {
            story == null -> R.string.collection_locked
            again -> R.string.village_talk_again
            else -> R.string.village_talk
        })
        talk.setTextColor(ink)
        talk.background = RippleDrawable(ColorStateList.valueOf(0x22000000), getDrawable(part), null)
        talk.isEnabled = story != null
        talk.contentDescription = getString(R.string.village_card_description_short, friend.name, friend.home, line.text)

        dots.removeAllViews()
        val size = (11 * resources.displayMetrics.density).roundToInt()
        friends.forEachIndexed { k, f ->
            dots.addView(View(this).apply {
                contentDescription = f.name
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (k == index) StoryBookCard.DEEP_GREEN else Color.parseColor("#F4E6C8"))
                    setStroke((2 * resources.displayMetrics.density).roundToInt(), if (k == index) StoryBookCard.DEEP_GREEN else Color.parseColor("#C9A26A"))
                }
                setOnClickListener { show(k) }
            }, LinearLayout.LayoutParams(size, size).apply { if (k > 0) marginStart = size })
        }
    }

    private fun arrow(symbol: String, description: Int, click: () -> Unit) = Button(this).apply {
        text = symbol; setTextColor(StoryBookCard.DEEP_GREEN); stateListAnimator = null; setPadding(0, 0, 0, 0)
        minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
        contentDescription = getString(description)
        background = RippleDrawable(ColorStateList.valueOf(0x22000000), GradientDrawable().apply {
            setColor(Color.parseColor("#FFF8E7")); cornerRadius = 1000f
            setStroke(resources.displayMetrics.density.roundToInt() * 2, StoryBookCard.DEEP_GREEN)
        }, null)
        setOnClickListener { click() }
    }.also { frame.onScaleText(it, 30f) }

    private fun openTalk(friend: CharacterDefinition) {
        val (story, again) = storyFor(friend)
        if (story == null) return
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
        /** The village picture (2091 reference px wide) is centred on the 1672 canvas. */
        private const val PICTURE_LEFT = -209.5f
        private const val PICTURE_RIGHT = 1672f + 209.5f
        /** Where A's mockup stands the friend, in the picture's own 1672 coordinates: centre, feet and box height. */
        private const val FRIEND_X = 345
        private const val FRIEND_FEET = 858
        private const val FRIEND_BOX = 350

        /** The village, opened on [friendId]. */
        fun intent(context: Context, friendId: String) =
            Intent(context, StoryListActivity::class.java).putExtra(EXTRA_FRIEND, friendId)
    }
}

/** "📍 where they live · ✓ how far": a pin, the home, a dot, a mark and the state, shrunk to fit one line. */
@SuppressLint("ViewConstructor")
internal class StatusLine(context: Context, private val frame: MockupFrame) : View(context) {
    enum class Mark { READ, NEW, LOCKED, LOADING }

    private var home = ""
    private var mark = Mark.LOADING
    private var state = ""
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3D3550"); typeface = Typeface.DEFAULT_BOLD }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = StoryBookCard.DEEP_GREEN; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    fun set(home: String, state: Pair<Mark, String>) {
        this.home = home; mark = state.first; this.state = state.second
        contentDescription = "$home, ${state.second}"
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val h = height.toFloat()
        val icon = h * 0.62f
        val gap = h * 0.18f
        var size = 30f * frame.scale
        // Shrink until everything fits on one line.
        fun widthAt(s: Float): Float { text.textSize = s; return icon * 2 + gap * 6 + text.measureText(home) + text.measureText(state) + s * 0.4f }
        while (size > 12f && widthAt(size) > width) size -= 1f
        text.textSize = size
        val total = widthAt(size)
        var x = (width - total) / 2f
        val cy = h / 2f
        val base = cy - (text.descent() + text.ascent()) / 2f
        ink.strokeWidth = icon * 0.11f
        pin(canvas, x + icon / 2f, cy, icon); x += icon + gap
        canvas.drawText(home, x, base, text); x += text.measureText(home) + gap * 1.5f
        fill.color = Color.parseColor("#3D3550"); canvas.drawCircle(x + size * 0.2f, cy, size * 0.09f, fill); x += size * 0.4f + gap * 1.5f
        markIcon(canvas, x + icon / 2f, cy, icon); x += icon + gap
        canvas.drawText(state, x, base, text)
    }

    /** A map pin: a round head with a hole, pointing down. */
    private fun pin(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        val r = s * 0.32f
        val top = cy - s * 0.18f
        path.reset()
        path.addCircle(cx, top, r, Path.Direction.CW)
        canvas.drawPath(path, ink)
        path.reset(); path.moveTo(cx - r * 0.92f, top + r * 0.4f); path.lineTo(cx, cy + s * 0.48f); path.lineTo(cx + r * 0.92f, top + r * 0.4f)
        canvas.drawPath(path, ink)
        canvas.drawCircle(cx, top, r * 0.32f, ink)
    }

    private fun markIcon(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        val r = s * 0.46f
        when (mark) {
            Mark.READ -> {
                canvas.drawCircle(cx, cy, r, ink)
                path.reset(); path.moveTo(cx - r * 0.45f, cy); path.lineTo(cx - r * 0.1f, cy + r * 0.38f); path.lineTo(cx + r * 0.5f, cy - r * 0.35f)
                canvas.drawPath(path, ink)
            }
            Mark.NEW -> {
                // A small four-point sparkle.
                fill.color = Color.parseColor("#9B7BD0")
                path.reset()
                path.moveTo(cx, cy - r); path.quadTo(cx, cy, cx + r, cy); path.quadTo(cx, cy, cx, cy + r)
                path.quadTo(cx, cy, cx - r, cy); path.quadTo(cx, cy, cx, cy - r); path.close()
                canvas.drawPath(path, fill)
            }
            Mark.LOCKED -> {
                canvas.drawRoundRect(cx - r * 0.7f, cy - r * 0.1f, cx + r * 0.7f, cy + r * 0.9f, r * 0.15f, r * 0.15f, ink)
                canvas.drawArc(cx - r * 0.42f, cy - r * 0.85f, cx + r * 0.42f, cy + r * 0.05f, 180f, 180f, false, ink)
            }
            Mark.LOADING -> canvas.drawCircle(cx, cy, r * 0.3f, ink)
        }
    }
}
