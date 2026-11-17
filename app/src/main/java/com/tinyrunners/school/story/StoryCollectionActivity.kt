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

    private lateinit var shelfView: BookShelf

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The shared bookshelf (BookShelf): the world story first, then one book per friend; ◀ ▶ slide the shelf.
        shelfView = BookShelf(this, getString(R.string.collection_back_home), getString(R.string.collection_open)) { finish() }
        shelfView.headline.text = getString(R.string.collection_headline)
        shelfView.subline.text = getString(R.string.collection_subline)
        shelfView.footer.text = getString(R.string.collection_footer)
        setContentView(shelfView.frame)
        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        render()
        // Re-read progress each time: a story finished in a run opens here.
        ProgressStore.forCurrentUser(this).loadAll { result ->
            if (isDestroyed) return@loadAll
            read = result.getOrNull().orEmpty().values.flatMap { it.readStoryIds }.toSet()
            loaded = result.isSuccess
            render()
        }
    }

    /** A friend's stories, in order, and whether each has been read in a run. */
    private fun entriesOf(friend: CharacterDefinition): List<Boolean> =
        stories.filter { it.characterId == friend.id }.map { it.id in read }

    private fun render() {
        val total = friends.sumOf { entriesOf(it).size }
        val open = friends.sumOf { f -> entriesOf(f).count { it } }
        shelfView.counter.text = if (loaded) getString(R.string.collection_seen, open, total) else getString(R.string.records_loading)
        shelfView.setBooks(listOf(worldCard()) + friends.map { friendCard(it) })
    }

    /** The first book: the world story (how the six homes and friends are joined by little roads). */
    private fun worldCard(): View = StoryBookCard.cover(this, shelfView.frame, R.drawable.ui_v2_book_mint, getString(R.string.world_book_title),
        getString(R.string.world_book_sub), WorldStoryActivity.ScenePicture(this).apply { set(null, R.drawable.bg_title_day, friends.map { it.id }) },
        getString(R.string.collection_open_book), true,
    ) { startActivity(WorldStoryActivity.intent(this, firstTime = false)) }

    /** A friend's book: how many of their stories were read; it opens their 이야기 도감. */
    private fun friendCard(friend: CharacterDefinition): View {
        val entries = entriesOf(friend)
        return StoryBookCard.cover(this, shelfView.frame, StoryBookCard.book(friend.id), getString(R.string.collection_friend_title, friend.name),
            friend.home + " · " + getString(R.string.collection_seen, entries.count { it }, entries.size),
            StoryBookCard.homePicture(this, friend.id), getString(R.string.collection_open_book), true,
        ) { startActivity(com.tinyrunners.school.stories.StoryActivity.intent(this, friend.id)) }
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
