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
    private var shelfLeft = 0
    private var shelfRight = 1672

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shelf = savedInstanceState?.getString(KEY_SHELF)
        // The picture fills the screen behind the parts (only its original middle: no blurred side bands).
        frame = MockupFrame(this, 0)
        frame.addView(com.tinyrunners.school.ui.CoverPicture(this, R.drawable.bg_v2_stories_wide, sideInset = 0.1f))
        val v2 = MockupFrame.Companion::v2
        fun text(px: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
            setTextColor(color); includeFontPadding = false; maxLines = 1
            if (bold) typeface = Typeface.DEFAULT_BOLD
            frame.onScaleText(this, v2(px).toFloat())
        }

        // A's separated pack (reference/screen-4): "← 처음으로 | 이야기 모음" at the top-left, the bag at the top-right,
        // a big line and a small one, then the friends' books on a sideways shelf.
        back = Button(this).apply {
            isAllCaps = false; stateListAnimator = null; setTextColor(LIGHT); gravity = Gravity.CENTER_VERTICAL or Gravity.START
            minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0; setPadding(0, 0, 0, 0)
            background = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), null, android.graphics.drawable.ColorDrawable(Color.WHITE))
            setOnClickListener { goBack() }
            maxLines = 1; isSingleLine = true
        }.also { frame.onScaleText(it, v2(14).toFloat()) }
        frame.placeV2(back, 30, 30, 132, 72)
        frame.placeV2(text(24, CREAM_TEXT, bold = true).apply {
            text = getString(R.string.collection_open); gravity = Gravity.CENTER_VERTICAL
        }, 136, 30, 460, 72)
        frame.placeV2(text(30, CREAM_TEXT, bold = true).apply {
            text = getString(R.string.collection_headline); gravity = Gravity.CENTER_VERTICAL
            setShadowLayer(6f, 0f, 2f, Color.argb(120, 0, 0, 0))
        }, 76, 108, 760, 158)
        frame.placeV2(text(12, LIGHT).apply { text = getString(R.string.collection_subline); gravity = Gravity.CENTER_VERTICAL }, 76, 164, 760, 186)
        counter = text(12, LIGHT).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        frame.placeV2(counter, 860, 124, 1200, 146)
        frame.place(android.widget.ImageButton(this).apply {
            setImageDrawable(null)
            background = RippleDrawable(ColorStateList.valueOf(0x22000000), getDrawable(R.drawable.ui_story_bag_button), null)
            contentDescription = getString(R.string.bag_open)
            setOnClickListener { startActivity(com.tinyrunners.school.quest.BagActivity.intent(this@StoryCollectionActivity)) }
        }, v2(1198), v2(24), v2(1248), v2(74))

        // Books on the shelf; they slide sideways when there are more than fit.
        row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        scroller = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; clipToPadding = false; addView(row) }
        frame.placeV2(scroller, 0, SHELF_TOP, 1280, SHELF_TOP + 392)
        frame.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            // The shelf runs from screen edge to screen edge on phones wider than 16:9.
            val left = frame.visibleLeft.roundToInt(); val right = frame.visibleRight.roundToInt()
            if (left != shelfLeft || right != shelfRight) {
                shelfLeft = left; shelfRight = right
                frame.post { frame.move(scroller, left, v2(SHELF_TOP), right, v2(SHELF_TOP + 392)); render() }
            }
        }
        frame.placeV2(text(12, LIGHT).apply { text = getString(R.string.collection_footer); gravity = Gravity.CENTER_VERTICAL }, 80, 668, 600, 694)
        // Page dots and ◀ ▶ at the bottom right ("책장 둘러보기").
        pager = com.tinyrunners.school.ui.ShelfPager(this, scroller)
        frame.placeV2(pager, 900, 650, 1230, 708)

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
        // The first book starts where the mockup's does (x 66), wherever the shelf begins on this phone.
        row.addView(RefBox(this, frame, (MockupFrame.v2(56) - shelfLeft).coerceAtLeast(MockupFrame.v2(16)), 1))
        val friend = shelf?.let { id -> friends.firstOrNull { it.id == id } }
        if (friend == null) {
            shelf = null
            back.text = getString(R.string.collection_back_home)
            val total = friends.sumOf { entriesOf(it).size } + 1
            val open = friends.sumOf { f -> entriesOf(f).count { it.open } } + if (prologueSeen()) 1 else 0
            counter.text = if (loaded) getString(R.string.collection_seen, open, total) else getString(R.string.records_loading)
            row.addView(worldCard())
            friends.forEach { row.addView(friendCard(it)) }
        } else {
            back.text = getString(R.string.collection_back_books)
            val entries = entriesOf(friend)
            counter.text = getString(R.string.collection_seen_friend, friend.name, entries.count { it.open }, entries.size)
            entries.forEach { row.addView(storyCard(friend, it)) }
        }
        row.addView(RefBox(this, frame, MockupFrame.v2(40), 1))
        scroller.post { pager.refresh() }
    }

    private fun prologueSeen() = !TalkProgress(this).shouldShowPrologue

    private fun worldCard(): View {
        val open = prologueSeen()
        return StoryBookCard.cover(this, frame, R.drawable.ui_v2_book_nunu, getString(R.string.collection_world),
            getString(R.string.collection_world_sub), ImageView(this).apply {
                setImageResource(R.drawable.story_thumb_world); scaleType = ImageView.ScaleType.CENTER_CROP
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, if (open) getString(R.string.collection_replay) else getString(R.string.collection_locked), open) {
            startActivity(PrologueActivity.replay(this))
        }
    }

    private fun friendCard(friend: CharacterDefinition): View {
        val entries = entriesOf(friend)
        val open = entries.count { it.open }
        return StoryBookCard.cover(this, frame, StoryBookCard.book(friend.id), getString(R.string.collection_friend_title, friend.name),
            friend.home + " · " + getString(R.string.collection_seen, open, entries.size), StoryBookCard.homePicture(this, friend.id),
            if (open > 0) getString(R.string.collection_open_book) else getString(R.string.collection_not_open), open > 0,
        ) { shelf = friend.id; render(); scroller.post { scroller.scrollTo(0, 0) } }
    }

    private fun storyCard(friend: CharacterDefinition, entry: CollectionEntry): View =
        // Locked stories show their title only, never what happens in them.
        StoryBookCard.cover(this, frame, StoryBookCard.book(friend.id), entry.title, entry.label,
            SceneThumb(this, friend.id, locked = !entry.open),
            if (entry.open) getString(R.string.collection_replay) else getString(R.string.collection_locked), entry.open) { entry.play() }

    private enum class Look { PRIMARY, OUTLINE, LOCKED }

    /** One storybook card (StoryBookCard, shared with 스토리 진행). */
    private fun card(ribbon: Int, title: String, thumbPart: Int?, thumbView: View?, count: String?, button: String,
                     look: Look, click: () -> Unit): View {
        val picture = thumbView ?: ImageView(this).apply {
            setImageResource(thumbPart!!); scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        return StoryBookCard.build(this, frame, ribbon, title, picture, count, button, StoryBookCard.Look.valueOf(look.name), CARD_ZOOM, click)
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
        /** The cards stand inside the shelf of A's final picture (boards at about 205 and 765), a little smaller. */
        const val CARD_ZOOM = 0.9f
        /** Top of the book shelf in the pack's 1280×720 coordinates (books 237..604 with their 10 px shadow edge). */
        const val SHELF_TOP = 225
        val LIGHT = Color.rgb(236, 230, 206)
        val CREAM_TEXT = Color.rgb(255, 246, 222)
        val DEEP_GREEN = Color.parseColor("#5E4594")
        val CREAM = Color.parseColor("#FFF8E7")
        /** Ribbons and thumbnails A has drawn so far (세계·라일·치즈·포포); the others use the world ribbon and the home picture. */
        val RIBBONS = mapOf("mint" to R.drawable.ui_story_ribbon_mint, "cheese" to R.drawable.ui_story_ribbon_cheese, "popo" to R.drawable.ui_story_ribbon_popo)
        val THUMBS = mapOf("mint" to R.drawable.story_thumb_mint, "cheese" to R.drawable.story_thumb_cheese, "popo" to R.drawable.story_thumb_popo)
    }
}

/**
 * A fixed-size box in the mockup's reference pixels (scaled by [frame]'s current scale), with children placed by
 * reference rects relative to the box, all shrunk or grown by [zoom]. Used for the story cards inside the sideways scroller.
 */
@SuppressLint("ViewConstructor")
internal class RefBox(context: Context, private val frame: MockupFrame, private val refW: Int, private val refH: Int,
                      private val zoom: Float = 1f) : android.view.ViewGroup(context) {
    private class Ref(val x0: Int, val y0: Int, val x1: Int, val y1: Int)

    fun add(view: View, x0: Int, y0: Int, x1: Int, y1: Int) {
        view.setTag(R.id.mockup_rect, Ref(x0, y0, x1, y1))
        addView(view)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = frame.scale * zoom
        for (i in 0 until childCount) {
            val c = getChildAt(i); val r = c.getTag(R.id.mockup_rect) as Ref
            c.measure(MeasureSpec.makeMeasureSpec(((r.x1 - r.x0) * s).roundToInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(((r.y1 - r.y0) * s).roundToInt(), MeasureSpec.EXACTLY))
        }
        setMeasuredDimension((refW * s).roundToInt(), (refH * s).roundToInt())
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val s = frame.scale * zoom
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
        paint.color = Color.rgb(94, 69, 148)
        canvas.drawCircle(cx, cy + size * 0.1f, size * 0.16f, paint)
    }
}
