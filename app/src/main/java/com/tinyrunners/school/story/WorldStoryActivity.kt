package com.tinyrunners.school.story

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.tinyrunners.school.MainActivity
import com.tinyrunners.school.R
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.characters.StoryPicker
import kotlin.math.roundToInt

/**
 * 세계 이야기: how the six homes are joined by little roads and who lives in each, as a book of short pages turned
 * with the ◀ ▶ arrows beside it (like the friend selection). Shown once, the first time 스토리 진행하기 is pressed on
 * this account, and then the friend selection opens; afterwards it is the first book in 이야기 모음.
 */
class WorldStoryActivity : Activity(), com.tinyrunners.school.audio.MusicScreen {
    override fun musicTrack(): Int = R.raw.main_theme

    /** One page: the picture behind it (a friend's home or the title meadow), who stands in it, and the words. */
    private class WorldPage(val home: String?, val picture: Int, val cast: List<String>, val text: Int)

    private val pages = listOf(
        WorldPage(null, R.drawable.bg_title_day, emptyList(), R.string.world_page_1),
        WorldPage("mint", 0, listOf("mint"), R.string.world_page_2),
        WorldPage("cheese", 0, listOf("cheese", "popo"), R.string.world_page_3),
        WorldPage("lumi", 0, listOf("lumi", "nunu"), R.string.world_page_4),
        WorldPage("piko", 0, listOf("piko"), R.string.world_page_5),
        WorldPage(null, R.drawable.bg_title_night, listOf("mint", "cheese", "popo", "lumi", "nunu", "piko"), R.string.world_page_6),
        WorldPage(null, R.drawable.bg_title_day, listOf("mint", "cheese", "popo", "lumi", "nunu", "piko"), R.string.world_page_7),
    )
    private var index = 0
    private var sliding = false
    private var firstTime = false

    private lateinit var page: View
    private lateinit var counter: TextView
    private lateinit var picture: ScenePicture
    private lateinit var words: TextView
    private lateinit var go: Button
    private lateinit var prev: Button
    private lateinit var next: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firstTime = intent.getBooleanExtra(EXTRA_FIRST_TIME, false)
        index = savedInstanceState?.getInt(KEY_PAGE)?.coerceIn(0, pages.lastIndex) ?: 0
        // Opening it the first time is what counts: it is not shown again on this account.
        if (firstTime) markSeen(this)

        val green = Color.rgb(62, 122, 78)
        val layout = StoryPicker.PageLayout(this)
        layout.add(View(this).apply { background = StoryPicker.BookPage(green, Color.rgb(255, 247, 228)) }, 0f, 0f, 1f, 1f)
        layout.add(text(0.040f, SOFT).apply { setText(R.string.world_book_title) }, L, 0.060f, 0.7f, 0.115f)
        counter = text(0.040f, SOFT).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        layout.add(counter, 0.55f, 0.060f, 0.76f, 0.115f)
        picture = ScenePicture(this)
        layout.add(picture, L, 0.130f, R_, 0.585f)
        words = text(0.046f, INK).apply { gravity = Gravity.TOP; maxLines = 4; setLineSpacing(0f, 1.15f) }
        layout.add(words, L, 0.610f, R_, 0.825f)
        go = Button(this).apply {
            isAllCaps = false; stateListAnimator = null; typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            tag = 0.046f
        }
        layout.add(go, 0.28f, 0.840f, 0.74f, 0.955f)
        layout.isClickable = true
        page = layout

        prev = arrow("◀", R.string.shelf_prev) { turn(-1) }
        next = arrow("▶", R.string.shelf_next) { turn(+1) }
        // 건너뛰기 the first time (straight to the friends), ← 이야기 모음 when opened from the shelf.
        val leave = TextView(this).apply {
            setText(if (firstTime) R.string.world_skip else R.string.world_back)
            setTextColor(Color.rgb(255, 246, 222)); typeface = Typeface.DEFAULT_BOLD; textSize = 16f
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), 0, dp(18), 0); isClickable = true
            setShadowLayer(6f, 0f, 1f, Color.argb(160, 30, 24, 16))
            background = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), null, android.graphics.drawable.ColorDrawable(Color.WHITE))
            setOnClickListener { finishStory() }
        }

        val root = object : FrameLayout(this) {
            override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
                super.onLayout(changed, l, t, r, b)
                val w = r - l; val h = b - t
                val ph = (h * 0.9f).toInt(); val pw = minOf((ph * 1.3f).toInt(), (w * 0.7f).toInt())
                val px = (w - pw) / 2; val py = (h - ph) / 2
                page.measure(MeasureSpec.makeMeasureSpec(pw, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ph, MeasureSpec.EXACTLY))
                page.layout(px, py, px + pw, py + ph)
                val a = (h * 0.13f).toInt(); val ay = (h - a) / 2; val gap = (h * 0.06f).toInt()
                for ((v, x) in listOf(prev to px - gap - a, next to px + pw + gap)) {
                    v.measure(MeasureSpec.makeMeasureSpec(a, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(a, MeasureSpec.EXACTLY))
                    v.layout(x, ay, x + a, ay + a)
                    v.setTextSize(TypedValue.COMPLEX_UNIT_PX, a * 0.36f)
                }
            }
        }.apply {
            setBackgroundColor(Color.rgb(46, 52, 40))
            addView(com.tinyrunners.school.ui.CoverPicture(this@WorldStoryActivity, R.drawable.bg_v2_stories_wide, sideInset = 0.1f))
            addView(View(this@WorldStoryActivity).apply { setBackgroundColor(Color.argb(90, 20, 24, 16)) })
            addView(page); addView(prev); addView(next)
            addView(leave, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, dp(52)).apply { leftMargin = dp(12); topMargin = dp(8) })
        }
        setContentView(root)
        hideSystemBars()
        bind()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_PAGE, index)
        super.onSaveInstanceState(outState)
    }

    /** The next or previous page slides in sideways, like the friends on the selection screen. */
    private fun turn(step: Int) {
        val to = index + step
        if (to !in pages.indices || sliding) return
        sliding = true
        val shift = page.width * 0.3f
        page.animate().translationX(-step * shift).alpha(0f).setDuration(SLIDE_MS).withEndAction {
            index = to; bind()
            page.translationX = step * shift
            page.animate().translationX(0f).alpha(1f).setDuration(SLIDE_MS).withEndAction { sliding = false }.start()
        }.start()
    }

    private fun bind() {
        val p = pages[index]
        counter.text = getString(R.string.world_page, index + 1, pages.size)
        picture.set(p.home, p.picture, p.cast)
        words.setText(p.text)
        val last = index == pages.lastIndex
        go.text = getString(when {
            !last -> R.string.world_next_page
            firstTime -> R.string.world_go_select
            else -> R.string.world_back
        })
        styleButton(go, primary = last)
        go.setOnClickListener { if (last) finishStory() else turn(+1) }
        for ((arrow, shown) in listOf(prev to (index > 0), next to (index < pages.lastIndex))) {
            arrow.alpha = if (shown) 1f else 0f; arrow.isEnabled = shown
            arrow.importantForAccessibility = if (shown) View.IMPORTANT_FOR_ACCESSIBILITY_YES else View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        page.contentDescription = "${getString(R.string.world_book_title)} ${counter.text}. ${words.text}"
        page.requestLayout()
    }

    /** The first time it goes on to choosing a friend; from the shelf it just closes. */
    private fun finishStory() {
        if (firstTime) startActivity(MainActivity.selectionIntent(this))
        finish()
    }

    private fun styleButton(b: Button, primary: Boolean) {
        val green = Color.rgb(62, 122, 78)
        fun face(fill: Int) = GradientDrawable().apply {
            setColor(fill); cornerRadius = 999f; if (!primary) setStroke(dp(2), green)
        }
        b.background = android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), face(if (primary) Color.rgb(40, 86, 59) else Color.rgb(238, 232, 210)))
            addState(intArrayOf(), face(if (primary) green else Color.rgb(255, 252, 240)))
        }
        b.setTextColor(if (primary) Color.rgb(255, 248, 231) else green)
    }

    private fun text(size: Float, color: Int) = TextView(this).apply {
        setTextColor(color); includeFontPadding = false; gravity = Gravity.CENTER_VERTICAL
        tag = size  // a share of the page height; PageLayout applies it
    }

    private fun arrow(symbol: String, description: Int, click: () -> Unit) = Button(this).apply {
        text = symbol; contentDescription = getString(description); setTextColor(GREEN)
        setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0; stateListAnimator = null
        background = RippleDrawable(ColorStateList.valueOf(0x33466F53), GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setColor(Color.argb(235, 255, 249, 235)); setStroke(dp(2), GREEN)
        }, null)
        setOnClickListener { click() }
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

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    /** A page's picture: a friend's home (or the title meadow) with friends standing in front, rounded at the corners. */
    @SuppressLint("ViewConstructor")
    internal class ScenePicture(context: Context) : View(context) {
        private var art: Bitmap? = null
        private var cast: List<CharacterSprites> = emptyList()
        private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
        private val smooth = Paint(Paint.FILTER_BITMAP_FLAG)
        private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(70, 40, 30, 50) }
        private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.rgb(196, 182, 140) }
        private val clip = Path()
        private val box = RectF()
        private val src = Rect()
        private val dst = RectF()

        init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

        fun set(home: String?, pictureRes: Int, ids: List<String>) {
            art = if (home != null) homes.getOrPut(home) {
                val res = StoryBookCard.HOMES[home] ?: return@getOrPut null
                val full = BitmapFactory.decodeResource(resources, res, BitmapFactory.Options().apply { inSampleSize = 4; inScaled = false })
                    ?: return@getOrPut null
                // The home part of the widened picture: the middle 80 %, under the sky band.
                val x0 = full.width / 10; val y0 = full.height * 20 / 100
                Bitmap.createBitmap(full, x0, y0, full.width - 2 * x0, full.height - y0).also { if (it != full) full.recycle() }
            } else pictures.getOrPut(pictureRes) {
                BitmapFactory.decodeResource(resources, pictureRes, BitmapFactory.Options().apply { inSampleSize = 2; inScaled = false })
            }
            cast = ids.mapNotNull { CharacterSprites.load(resources, it) }
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val d = resources.displayMetrics.density
            val radius = 10 * d
            box.set(0f, 0f, width.toFloat(), height.toFloat())
            clip.reset(); clip.addRoundRect(box, radius, radius, Path.Direction.CW)
            canvas.save(); canvas.clipPath(clip)
            art?.let { b ->
                // Center crop, keeping the bottom (the ground) in view.
                val scale = maxOf(width.toFloat() / b.width, height.toFloat() / b.height)
                val sw = (width / scale).toInt(); val sh = (height / scale).toInt()
                src.set((b.width - sw) / 2, b.height - sh, (b.width + sw) / 2, b.height)
                canvas.drawBitmap(b, src, box, smooth)
            } ?: canvas.drawColor(Color.rgb(236, 228, 210))
            if (cast.isNotEmpty()) {
                val s = minOf(height * 0.42f, width * 0.85f / cast.size)
                val floor = height * 0.95f
                val step = s * 1.02f
                val start = width / 2f - step * (cast.size - 1) / 2f
                cast.forEachIndexed { i, sprites ->
                    val cx = start + i * step
                    canvas.drawOval(cx - s * 0.3f, floor - s * 0.05f, cx + s * 0.3f, floor + s * 0.04f, shadow)
                    dst.set(cx - s / 2, floor - s * CharacterSprites.FOOT_EDGE_Y / CharacterSprites.SIZE, cx + s / 2,
                        floor + s * (CharacterSprites.SIZE - CharacterSprites.FOOT_EDGE_Y) / CharacterSprites.SIZE)
                    canvas.drawBitmap(sprites[SpriteFrame.IDLE_0], null, dst, pixel)
                }
            }
            canvas.restore()
            edge.strokeWidth = 1.5f * d
            box.inset(edge.strokeWidth / 2, edge.strokeWidth / 2)
            canvas.drawRoundRect(box, radius, radius, edge)
        }

        companion object {
            private val homes = mutableMapOf<String, Bitmap?>()
            private val pictures = mutableMapOf<Int, Bitmap?>()
        }
    }

    companion object {
        private const val EXTRA_FIRST_TIME = "firstTime"
        private const val KEY_PAGE = "worldPage"
        private const val SLIDE_MS = 150L
        private const val L = 0.10f
        private const val R_ = 0.90f
        private val INK = Color.rgb(46, 52, 40)
        private val SOFT = Color.rgb(94, 98, 80)
        private val GREEN = com.tinyrunners.school.ui.CardShelf.GREEN

        fun intent(context: Context, firstTime: Boolean) = Intent(context, WorldStoryActivity::class.java)
            .putExtra(EXTRA_FIRST_TIME, firstTime)

        private fun prefs(context: Context) = context.getSharedPreferences("world_story", Context.MODE_PRIVATE)
        private fun key(context: Context) = "seen_" + (com.tinyrunners.school.auth.AuthService.currentUid(context) ?: "guest")

        /** Whether this account has already been shown the world story (it opens by itself only once). */
        fun seen(context: Context): Boolean = prefs(context).getBoolean(key(context), false)

        fun markSeen(context: Context) { prefs(context).edit().putBoolean(key(context), true).apply() }
    }
}
