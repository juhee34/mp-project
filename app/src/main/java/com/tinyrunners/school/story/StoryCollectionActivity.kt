package com.tinyrunners.school.story

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.data.CharacterDefinition
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.talk.PrologueActivity
import com.tinyrunners.school.talk.TalkActivity
import com.tinyrunners.school.talk.TalkProgress
import com.tinyrunners.school.ui.Page
import com.tinyrunners.school.ui.SceneryView
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 이야기 모음 (from the village story list, top-right), laid out like the shop: a row of cards to swipe through.
 * The first shelf has the world story and one card per friend; a friend's card opens that friend's shelf with every
 * story in order (first talk, the village stories). All are listed from the start; the ones not reached yet in
 * 스토리 진행 stay locked. Nothing here changes progress; it only replays.
 */
class StoryCollectionActivity : Activity() {
    private val friends by lazy { CharacterRepository(this).characters }
    private val stories by lazy { StoryRepository.load(this) }
    private var read: Set<String> = emptySet()
    private var loaded = false
    /** The friend whose shelf is open, or null for the first shelf. */
    private var shelf: String? = null

    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var back: Button
    private lateinit var row: LinearLayout
    private lateinit var scroller: HorizontalScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shelf = savedInstanceState?.getString(KEY_SHELF)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(12)) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        back = com.tinyrunners.school.ui.HomeButton.create(this, getString(R.string.collection_back)) { goBack() }
        top.addView(back)
        title = TextView(this).apply {
            textSize = 24f; setTextColor(Page.INK); setTypeface(typeface, Typeface.BOLD)
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            setPadding(dp(12), 0, 0, 0)
        }
        top.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(com.tinyrunners.school.ui.BagButton.create(this))
        content.addView(top)
        subtitle = TextView(this).apply { textSize = 15f; setTextColor(Page.MUTED); setPadding(0, dp(6), 0, 0) }
        content.addView(subtitle)
        row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), dp(8), dp(4)) }
        scroller = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(row) }
        content.addView(scroller, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(8) })

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Page.BACKGROUND)
            addView(SceneryView(this@StoryCollectionActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
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
    }

    override fun onResume() {
        super.onResume()
        com.tinyrunners.school.audio.Music.playStory(this)
        render()
        // Re-read progress each time: a story finished in 스토리 진행 opens here.
        ProgressStore.forCurrentUser(this).loadAll { result ->
            if (isDestroyed) return@loadAll
            read = result.getOrNull().orEmpty().values.flatMap { it.readStoryIds }.toSet()
            loaded = result.isSuccess
            render()
        }
    }

    override fun onPause() {
        com.tinyrunners.school.audio.Music.pauseSoon()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_SHELF, shelf)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Back from a friend's shelf returns to the first shelf.")
    override fun onBackPressed() {
        if (shelf != null) goBack() else @Suppress("DEPRECATION") super.onBackPressed()
    }

    private fun goBack() {
        if (shelf == null) { finish(); return }
        shelf = null
        render()
        scroller.post { scroller.scrollTo(0, 0) }
    }

    /** Everything one friend has, in story order, and whether each is open. */
    private fun entriesOf(friend: CharacterDefinition): List<CollectionEntry> {
        val talkSeen = !TalkProgress(this).shouldShowTalk(friend.id)
        val first = CollectionEntry(
            label = getString(R.string.collection_first_talk),
            title = getString(R.string.collection_first_talk_title),
            hint = getString(R.string.collection_first_talk_hint),
            open = talkSeen,
            summary = getString(R.string.collection_first_talk_body, friend.name),
        ) { startActivity(TalkActivity.replay(this, friend.id)) }
        val village = stories.filter { it.characterId == friend.id }.mapIndexed { i, story ->
            CollectionEntry(
                label = if (i == 0) getString(R.string.collection_intro) else getString(R.string.collection_episode, i),
                title = story.title,
                hint = getString(R.string.collection_locked_hint),
                open = story.id in read,
                summary = story.summary,
            ) { startActivity(TalkActivity.village(this, friend.id, story.id)) }
        }
        return listOf(first) + village
    }

    private fun render() {
        row.removeAllViews()
        val friend = shelf?.let { id -> friends.firstOrNull { it.id == id } }
        if (friend == null) {
            shelf = null
            title.setText(R.string.collection_title)
            back.text = getString(R.string.collection_back)
            val total = friends.sumOf { entriesOf(it).size } + 1
            val open = friends.sumOf { f -> entriesOf(f).count { it.open } } + if (prologueSeen()) 1 else 0
            subtitle.text = if (loaded) getString(R.string.collection_progress, open, total) else getString(R.string.records_loading)
            row.addView(worldCard(), cardParams())
            friends.forEach { row.addView(friendCard(it), cardParams()) }
        } else {
            title.text = getString(R.string.collection_friend_title, friend.name)
            back.text = getString(R.string.collection_back_all)
            val entries = entriesOf(friend)
            subtitle.text = getString(R.string.collection_friend_progress, friend.home, entries.count { it.open }, entries.size)
            entries.forEach { row.addView(storyCard(friend, it), cardParams()) }
        }
    }

    private fun prologueSeen() = !TalkProgress(this).shouldShowPrologue

    private fun worldCard(): View {
        val open = prologueSeen()
        return card(
            thumb = WorldThumb(this, friends.map { it.id }, locked = !open),
            label = getString(R.string.collection_world_label),
            name = getString(R.string.collection_world),
            body = getString(R.string.collection_world_body),
            button = if (open) getString(R.string.collection_replay) else getString(R.string.collection_locked),
            enabled = open, primary = true,
        ) { startActivity(PrologueActivity.replay(this)) }
    }

    private fun friendCard(friend: CharacterDefinition): View {
        val entries = entriesOf(friend)
        val open = entries.count { it.open }
        return card(
            thumb = SceneThumb(this, friend.id, locked = false),
            label = friend.home,
            name = getString(R.string.collection_friend_title, friend.name),
            body = getString(R.string.collection_friend_count, open, entries.size),
            button = getString(R.string.collection_open_friend), enabled = true, primary = open > 0,
        ) { shelf = friend.id; render(); scroller.post { scroller.scrollTo(0, 0) } }
    }

    private fun storyCard(friend: CharacterDefinition, entry: CollectionEntry): View = card(
        thumb = SceneThumb(this, friend.id, locked = !entry.open),
        label = entry.label,
        name = entry.title,
        // Locked stories show their title only, never what happens in them.
        body = if (entry.open) entry.summary else entry.hint,
        button = if (entry.open) getString(R.string.collection_replay) else getString(R.string.collection_locked),
        enabled = entry.open, primary = true,
    ) { entry.play() }

    private fun card(thumb: View, label: String, name: String, body: String, button: String, enabled: Boolean,
                     primary: Boolean, click: () -> Unit): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply { setColor(Page.PAPER); cornerRadius = dp(20).toFloat(); setStroke(dp(1), Page.LINE) }
        }
        card.addView(thumb, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        card.addView(TextView(this).apply {
            text = label; textSize = 12f; setTextColor(Page.LEAF); setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(6), 0, 0)
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        })
        card.addView(TextView(this).apply {
            text = name; textSize = 17f; setTextColor(if (enabled) Page.INK else Page.MUTED); setTypeface(typeface, Typeface.BOLD)
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        })
        card.addView(TextView(this).apply {
            text = body; textSize = 13f; setTextColor(Page.MUTED); maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
        })
        card.addView(Button(this).apply {
            text = button
            textSize = 15f; isAllCaps = false; stateListAnimator = null
            minHeight = dp(44); minimumHeight = dp(44); setPadding(0, 0, 0, 0)
            val filled = enabled && primary
            setTextColor(if (filled) Color.WHITE else Page.LEAF)
            background = RippleDrawable(ColorStateList.valueOf(Page.MINT), GradientDrawable().apply {
                setColor(if (filled) Page.LEAF else Page.MINT); cornerRadius = dp(14).toFloat()
            }, null)
            isEnabled = enabled
            alpha = if (enabled) 1f else 0.6f
            setOnClickListener { click() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
        if (enabled) { card.isClickable = true; card.setOnClickListener { click() } }
        return card
    }

    private fun cardParams() = LinearLayout.LayoutParams(dp(210), LinearLayout.LayoutParams.MATCH_PARENT).apply { marginEnd = dp(12) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    private class CollectionEntry(
        val label: String, val title: String, val hint: String, val open: Boolean, val summary: String, val play: () -> Unit,
    )

    private companion object { const val KEY_SHELF = "collectionShelf" }
}

/**
 * Small copies of the first frame of each friend's running background (the home at the start of the road), read
 * once with a region decoder so the cards never decode the full panoramas.
 */
internal object BackgroundThumbs {
    private val cache = mutableMapOf<String, Pair<Bitmap, Float>?>()

    /** The friend's home scene and where its floor is (0..1 from the top), or null without art. */
    fun of(context: Context, characterId: String): Pair<Bitmap, Float>? = cache.getOrPut(characterId) {
        try {
            val manifest = context.assets.open("backgrounds/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
            val entries = manifest.getJSONArray("characters")
            val entry = (0 until entries.length()).map { entries.getJSONObject(it) }.firstOrNull { it.getString("id") == characterId }
                ?: return@getOrPut null
            val r = entry.getJSONArray("rect")
            val height = r.getInt(3)
            // About two screens of the home: wide enough for a card, small enough to keep in memory.
            val width = minOf(r.getInt(2), height * 2)
            val bitmap = context.assets.open("backgrounds/" + entry.getString("source")).use { stream ->
                @Suppress("DEPRECATION")
                val decoder = if (Build.VERSION.SDK_INT >= 31) BitmapRegionDecoder.newInstance(stream) else BitmapRegionDecoder.newInstance(stream, false)
                decoder?.decodeRegion(Rect(r.getInt(0), r.getInt(1), r.getInt(0) + width, r.getInt(1) + height), null).also { decoder?.recycle() }
            } ?: return@getOrPut null
            bitmap to entry.getInt("floorY") / height.toFloat()
        } catch (e: Exception) {
            android.util.Log.w("BackgroundThumbs", "No thumbnail for $characterId", e)
            null
        }
    }
}

/** A friend standing at home on their own background; locked cards are darkened with a small lock. */
@SuppressLint("ViewConstructor")
internal class SceneThumb(context: Context, private val characterId: String, private val locked: Boolean) : View(context) {
    private val scene = BackgroundThumbs.of(context, characterId)
    private val sprites = CharacterSprites.load(resources, characterId)
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val src = Rect()
    private val dst = RectF()
    private val clip = android.graphics.Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val radius = 14 * resources.displayMetrics.density
        clip.reset(); clip.addRoundRect(0f, 0f, w, h, radius, radius, android.graphics.Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clip)
        var floor = h * 0.9f
        val art = scene
        if (art != null) {
            val (bitmap, floorFraction) = art
            // Fill the height and start from the left edge: the home is at the start of the road.
            val scale = h / bitmap.height
            val visible = (w / scale).toInt().coerceAtMost(bitmap.width)
            src.set(0, 0, visible, bitmap.height)
            dst.set(0f, 0f, visible * scale, h)
            canvas.drawBitmap(bitmap, src, dst, pixel)
            floor = h * floorFraction
        } else {
            fill.color = Page.MINT; canvas.drawRect(0f, 0f, w, h, fill)
        }
        sprites?.let {
            val px = max(1, (h * 0.5f / CharacterSprites.SIZE).roundToInt()).toFloat()
            val left = (w * 0.5f - CharacterSprites.CENTER_X * px).roundToInt().toFloat()
            val top = (floor - CharacterSprites.FOOT_EDGE_Y * px).roundToInt().toFloat()
            dst.set(left, top, left + CharacterSprites.SIZE * px, top + CharacterSprites.SIZE * px)
            canvas.drawBitmap(it[SpriteFrame.IDLE_0], null, dst, pixel)
        }
        if (locked) {
            fill.color = Color.argb(150, 30, 38, 33); canvas.drawRect(0f, 0f, w, h, fill)
            LockIcon.draw(canvas, w / 2f, h / 2f, h * 0.16f, fill)
        }
        canvas.restore()
    }
}

/** The world story: all six friends side by side on the hill. */
@SuppressLint("ViewConstructor")
internal class WorldThumb(context: Context, private val ids: List<String>, private val locked: Boolean) : View(context) {
    private val sprites = ids.map { CharacterSprites.load(resources, it) }
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dst = RectF()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val radius = 14 * resources.displayMetrics.density
        canvas.save()
        canvas.clipPath(android.graphics.Path().apply { addRoundRect(0f, 0f, w, h, radius, radius, android.graphics.Path.Direction.CW) })
        // Night sky with a little moon, then a green hill: the world before the first morning.
        fill.color = Color.rgb(46, 58, 92); canvas.drawRect(0f, 0f, w, h, fill)
        fill.color = Color.rgb(250, 238, 190); canvas.drawCircle(w * 0.82f, h * 0.2f, h * 0.08f, fill)
        fill.color = Color.rgb(250, 246, 220)
        for (i in 0 until 14) canvas.drawCircle((i * 73 % 100) / 100f * w, (i * 37 % 45) / 100f * h, 2.2f, fill)
        val ground = h * 0.86f
        fill.color = Color.rgb(116, 166, 98); canvas.drawOval(-w * 0.2f, ground - h * 0.12f, w * 1.2f, h * 1.4f, fill)
        val px = max(1, (h * 0.32f / CharacterSprites.SIZE).roundToInt()).toFloat()
        val step = w / (ids.size + 1)
        sprites.forEachIndexed { i, s ->
            s ?: return@forEachIndexed
            val left = (step * (i + 1) - CharacterSprites.CENTER_X * px).roundToInt().toFloat()
            val top = (ground - CharacterSprites.FOOT_EDGE_Y * px).roundToInt().toFloat()
            dst.set(left, top, left + CharacterSprites.SIZE * px, top + CharacterSprites.SIZE * px)
            canvas.drawBitmap(s[SpriteFrame.IDLE_0], null, dst, pixel)
        }
        if (locked) {
            fill.color = Color.argb(150, 30, 38, 33); canvas.drawRect(0f, 0f, w, h, fill)
            LockIcon.draw(canvas, w / 2f, h / 2f, h * 0.16f, fill)
        }
        canvas.restore()
    }
}

/** A plain padlock: rounded body and a shackle. */
internal object LockIcon {
    fun draw(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        paint.color = Color.rgb(255, 250, 236)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = size * 0.22f
        canvas.drawArc(cx - size * 0.45f, cy - size * 1.05f, cx + size * 0.45f, cy - size * 0.05f, 180f, 180f, false, paint)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cx - size * 0.75f, cy - size * 0.45f, cx + size * 0.75f, cy + size * 0.75f, size * 0.18f, size * 0.18f, paint)
        paint.color = Color.rgb(48, 83, 65)
        canvas.drawCircle(cx, cy + size * 0.1f, size * 0.16f, paint)
    }
}
