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
import android.widget.ImageView
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
import com.tinyrunners.school.ui.MockupFrame
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 이야기 모음 (from the village story list, top-right), in A's approved design (docs/art/ui-screens/
 * approved/mockup_story_collection.png, layout.json): the paper-and-bench picture with the title, "◀ 스토리 진행으로" at
 * the top-left, the 본 이야기 counter and the bag at the top-right, and story cards (card frame, ribbon, title,
 * picture, count pill, button) standing on the bench, four across and sliding sideways for the rest.
 *
 * The first shelf has the world story and one card per friend; a friend's card opens that friend's shelf with every
 * story in order (first talk, the village stories). The ones not reached yet in 스토리 진행 stay locked. Nothing here
 * changes progress; it only replays. Friends always appear as their original sprites (A, 2026-11-12).
 */
class StoryCollectionActivity : Activity(), com.tinyrunners.school.audio.MusicScreen {
    /** Story screens play the main theme. */
    override fun musicTrack(): Int = com.tinyrunners.school.R.raw.main_theme

    private val friends by lazy { CharacterRepository(this).characters }
    private val stories by lazy { StoryRepository.load(this) }
    private var read: Set<String> = emptySet()
    private var loaded = false
    /** The friend whose shelf is open, or null for the first shelf. */
    private var shelf: String? = null

    private lateinit var frame: MockupFrame
    private lateinit var back: Button
    private lateinit var counter: TextView
    private lateinit var row: LinearLayout
    private lateinit var scroller: HorizontalScrollView
    private lateinit var pager: com.tinyrunners.school.ui.ShelfPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shelf = savedInstanceState?.getString(KEY_SHELF)
        frame = MockupFrame(this, R.drawable.bg_story_collection_wide)

        // ◀ 스토리 진행으로: the part has the arrow; the words sit in its text area.
        back = Button(this).apply {
            isAllCaps = false; stateListAnimator = null; setTextColor(DEEP_GREEN); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            background = RippleDrawable(ColorStateList.valueOf(0x22000000), getDrawable(R.drawable.ui_story_back_button), null)
            setOnClickListener { goBack() }
        }.also { frame.onScaleText(it, 34f) }
        frame.place(back, 47, 41, 371, 112)
        // Keep the label right of the arrow (text_rect 128–350 of 47–371).
        frame.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val s = frame.scale
            back.setPadding(((128 - 47) * s).roundToInt(), 0, ((371 - 350) * s).roundToInt(), 0)
        }
        counter = TextView(this).apply {
            setTextColor(DEEP_GREEN); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            background = getDrawable(R.drawable.ui_story_counter_pill)
        }.also { frame.onScaleText(it, 31f) }
        frame.place(counter, 1233, 41, 1526, 112)
        frame.place(android.widget.ImageButton(this).apply {
            setImageDrawable(null)
            background = RippleDrawable(ColorStateList.valueOf(0x22000000), getDrawable(R.drawable.ui_story_bag_button), null)
            contentDescription = getString(R.string.bag_open)
            setOnClickListener { startActivity(com.tinyrunners.school.quest.BagActivity.intent(this@StoryCollectionActivity)) }
        }, 1562, 34, 1637, 120)

        // Cards on the bench; they slide sideways when there are more than four.
        row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        scroller = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; clipToPadding = false; addView(row) }
        frame.place(scroller, 40, 205, 1640, 812)
        // Page dots and ◀ ▶ on the grass under the bench, as in 스토리 진행.
        pager = com.tinyrunners.school.ui.ShelfPager(this, scroller)
        frame.place(pager, 436, 842, 1236, 912)

        setContentView(frame)
        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        render()
        // Re-read progress each time: a story finished in 스토리 진행 opens here.
        ProgressStore.forCurrentUser(this).loadAll { result ->
            if (isDestroyed) return@loadAll
            read = result.getOrNull().orEmpty().values.flatMap { it.readStoryIds }.toSet()
            loaded = result.isSuccess
            render()
        }
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
            open = talkSeen,
        ) { startActivity(TalkActivity.replay(this, friend.id)) }
        val village = stories.filter { it.characterId == friend.id }.mapIndexed { i, story ->
            CollectionEntry(
                label = if (i == 0) getString(R.string.collection_intro) else getString(R.string.collection_episode, i),
                title = story.title,
                open = story.id in read,
            ) { startActivity(TalkActivity.village(this, friend.id, story.id)) }
        }
        return listOf(first) + village
    }

    private fun render() {
        row.removeAllViews()
        row.addView(RefBox(this, frame, 25, 1))   // the first card starts where the mockup's does (x 65)
        val friend = shelf?.let { id -> friends.firstOrNull { it.id == id } }
        if (friend == null) {
            shelf = null
            back.text = getString(R.string.collection_back).removePrefix("◀ ")
            val total = friends.sumOf { entriesOf(it).size } + 1
            val open = friends.sumOf { f -> entriesOf(f).count { it.open } } + if (prologueSeen()) 1 else 0
            counter.text = if (loaded) getString(R.string.collection_seen, open, total) else getString(R.string.records_loading)
            row.addView(worldCard())
            friends.forEach { row.addView(friendCard(it)) }
        } else {
            back.text = getString(R.string.collection_back_all).removePrefix("◀ ")
            val entries = entriesOf(friend)
            counter.text = getString(R.string.collection_seen_friend, friend.name, entries.count { it.open }, entries.size)
            entries.forEach { row.addView(storyCard(friend, it)) }
        }
        row.addView(RefBox(this, frame, 25, 1))
        scroller.post { pager.refresh() }
    }

    private fun prologueSeen() = !TalkProgress(this).shouldShowPrologue

    private fun worldCard(): View {
        val open = prologueSeen()
        return card(R.drawable.ui_story_ribbon_world, getString(R.string.collection_world),
            thumbPart = R.drawable.story_thumb_world, thumbView = null, count = null,
            button = if (open) getString(R.string.collection_replay) else getString(R.string.collection_locked),
            look = if (open) Look.PRIMARY else Look.LOCKED) { startActivity(PrologueActivity.replay(this)) }
    }

    private fun friendCard(friend: CharacterDefinition): View {
        val entries = entriesOf(friend)
        val open = entries.count { it.open }
        return card(RIBBONS[friend.id] ?: R.drawable.ui_story_ribbon_world, getString(R.string.collection_friend_title, friend.name),
            thumbPart = THUMBS[friend.id], thumbView = if (THUMBS[friend.id] == null) SceneThumb(this, friend.id, locked = false) else null,
            count = getString(R.string.collection_seen, open, entries.size),
            button = if (open > 0) getString(R.string.collection_open_friend) else getString(R.string.collection_not_open),
            look = if (open > 0) Look.OUTLINE else Look.LOCKED,
        ) { shelf = friend.id; render(); scroller.post { scroller.scrollTo(0, 0) } }
    }

    private fun storyCard(friend: CharacterDefinition, entry: CollectionEntry): View =
        // Locked stories show their title only, never what happens in them.
        card(RIBBONS[friend.id] ?: R.drawable.ui_story_ribbon_world, entry.title,
            thumbPart = null, thumbView = SceneThumb(this, friend.id, locked = !entry.open), count = entry.label,
            button = if (entry.open) getString(R.string.collection_replay) else getString(R.string.collection_locked),
            look = if (entry.open) Look.PRIMARY else Look.LOCKED) { entry.play() }

    private enum class Look { PRIMARY, OUTLINE, LOCKED }

    /** One storybook card (StoryBookCard, shared with 스토리 진행). */
    private fun card(ribbon: Int, title: String, thumbPart: Int?, thumbView: View?, count: String?, button: String,
                     look: Look, click: () -> Unit): View {
        val picture = thumbView ?: ImageView(this).apply {
            setImageResource(thumbPart!!); scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        return StoryBookCard.build(this, frame, ribbon, title, picture, count, button, StoryBookCard.Look.valueOf(look.name), click)
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

    private class CollectionEntry(val label: String, val title: String, val open: Boolean, val play: () -> Unit)

    private companion object {
        const val KEY_SHELF = "collectionShelf"
        val DEEP_GREEN = Color.parseColor("#1E5A2A")
        val CREAM = Color.parseColor("#FFF8E7")
        /** Ribbons and thumbnails A has drawn so far (세계·라일·치즈·포포); the others use the world ribbon and the home picture. */
        val RIBBONS = mapOf("mint" to R.drawable.ui_story_ribbon_mint, "cheese" to R.drawable.ui_story_ribbon_cheese, "popo" to R.drawable.ui_story_ribbon_popo)
        val THUMBS = mapOf("mint" to R.drawable.story_thumb_mint, "cheese" to R.drawable.story_thumb_cheese, "popo" to R.drawable.story_thumb_popo)
    }
}

/**
 * A fixed-size box in the mockup's reference pixels (scaled by [frame]'s current scale), with children placed by
 * reference rects relative to the box. Used for the story cards inside the sideways scroller.
 */
@SuppressLint("ViewConstructor")
internal class RefBox(context: Context, private val frame: MockupFrame, private val refW: Int, private val refH: Int) : android.view.ViewGroup(context) {
    private class Ref(val x0: Int, val y0: Int, val x1: Int, val y1: Int)

    fun add(view: View, x0: Int, y0: Int, x1: Int, y1: Int) {
        view.setTag(R.id.mockup_rect, Ref(x0, y0, x1, y1))
        addView(view)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = frame.scale
        for (i in 0 until childCount) {
            val c = getChildAt(i); val r = c.getTag(R.id.mockup_rect) as Ref
            c.measure(MeasureSpec.makeMeasureSpec(((r.x1 - r.x0) * s).roundToInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(((r.y1 - r.y0) * s).roundToInt(), MeasureSpec.EXACTLY))
        }
        setMeasuredDimension((refW * s).roundToInt(), (refH * s).roundToInt())
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val s = frame.scale
        for (i in 0 until childCount) {
            val c = getChildAt(i); val ref = c.getTag(R.id.mockup_rect) as Ref
            val x = (ref.x0 * s).roundToInt(); val y = (ref.y0 * s).roundToInt()
            c.layout(x, y, x + c.measuredWidth, y + c.measuredHeight)
        }
    }
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
            // Fill the whole picture from the left edge (the home is at the start of the road); on a wide card the
            // picture is cropped top and bottom around the floor instead of leaving an empty strip at the right.
            val scale = max(h / bitmap.height, w / bitmap.width)
            val visibleW = (w / scale).toInt().coerceAtMost(bitmap.width)
            val visibleH = (h / scale).coerceAtMost(bitmap.height.toFloat())
            val floorPx = bitmap.height * floorFraction
            val top = (floorPx - visibleH * 0.9f).coerceIn(0f, bitmap.height - visibleH)
            src.set(0, top.toInt(), visibleW, (top + visibleH).toInt())
            dst.set(0f, 0f, w, h)
            canvas.drawBitmap(bitmap, src, dst, pixel)
            floor = (floorPx - top) * scale
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
