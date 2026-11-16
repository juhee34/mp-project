package com.tinyrunners.school.characters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.story.Step
import com.tinyrunners.school.story.Story
import com.tinyrunners.school.story.StoryBookCard

/**
 * Before a run: the friend's stories as a little book, one page per episode, in the same cream letter as the login
 * card. The ◀ ▶ arrows (like on the selection screen) turn the page; each page has a picture (the friend's home with
 * the friends who appear in that episode), the episode's summary and a button that starts the run with it. Shown with
 * AlertDialog.Builder.setView (a course topic).
 */
internal class StoryPicker(
    private val context: Context,
    private val friendName: String,
    private val stories: List<Story>,
    private val read: Set<String>,
    /** The friend's own colour: the book's paper, spine, bookmark, button and arrows follow it. */
    private val tint: Int,
    private val onPick: (Story) -> Unit,
) {
    private val deep = blend(tint, Color.rgb(30, 26, 34), 0.38f)
    private val deeper = blend(tint, Color.rgb(30, 26, 34), 0.55f)
    private var index = stories.indexOfFirst { it.id !in read && it.isUnlocked(read) }.let { if (it < 0) 0 else it }
    private lateinit var dialog: AlertDialog
    private var flipping = false
    private val page = PageLayout(context)
    private val chapter = text(0.030f, SOFT)
    private val heading = text(0.050f, INK, bold = true).apply { maxLines = 1 }
    private val picture = EpisodePicture(context)
    private val summary = text(0.031f, INK).apply {
        maxLines = 3; ellipsize = android.text.TextUtils.TruncateAt.END; setLineSpacing(0f, 1.1f); gravity = Gravity.TOP
    }
    private val mark = text(0.030f, deep, bold = true)
    private val go = pill()
    private val counter = text(0.028f, SOFT).apply { gravity = Gravity.CENTER }
    private val close = text(0.032f, SOFT).apply {
        gravity = Gravity.CENTER; setText(R.string.choose_story_cancel); isClickable = true
        background = RippleDrawable(ColorStateList.valueOf(0x22000000), null, ColorDrawable(Color.WHITE))
        setOnClickListener { dialog.dismiss() }
    }
    private val prev = arrow("◀", R.string.choose_story_prev) { turn(-1) }
    private val next = arrow("▶", R.string.choose_story_next) { turn(+1) }

    fun show() {
        // The page: A's cream letter with the lavender spine and bookmark (the login card's picture).
        page.add(View(context).apply {
            background = BookPage(tint)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, 0f, 0f, 1f, 1f)
        page.add(chapter.apply { text = context.getString(R.string.choose_story_book, friendName) }, L, 0.085f, R_, 0.125f)
        page.add(counter, 0.62f, 0.085f, R_, 0.125f)
        page.add(heading, L, 0.130f, R_, 0.200f)
        page.add(picture, L, 0.212f, R_, 0.500f)
        page.add(summary, L, 0.518f, R_, 0.700f)
        page.add(mark, L, 0.705f, R_, 0.745f)
        page.add(go, L, 0.765f, R_, 0.855f)
        page.add(close, L, 0.870f, R_, 0.925f)

        // The page in the middle of the screen with an arrow on each side; a tap beside the book closes it.
        val root = object : FrameLayout(context) {
            override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
                val w = r - l; val h = b - t
                val ph = (h * 0.92f).toInt(); val pw = ph * 440 / 573
                val px = (w - pw) / 2; val py = (h - ph) / 2
                page.measure(MeasureSpec.makeMeasureSpec(pw, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ph, MeasureSpec.EXACTLY))
                page.layout(px, py, px + pw, py + ph)
                val a = (h * 0.13f).toInt(); val ay = (h - a) / 2; val gap = (h * 0.07f).toInt()
                for ((v, x) in listOf(prev to px - gap - a, next to px + pw + gap)) {
                    v.measure(MeasureSpec.makeMeasureSpec(a, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(a, MeasureSpec.EXACTLY))
                    v.layout(x, ay, x + a, ay + a)
                    v.setTextSize(TypedValue.COMPLEX_UNIT_PX, a * 0.36f)
                }
            }
        }.apply {
            setBackgroundColor(Color.argb(130, 30, 26, 40))
            addView(page); addView(prev); addView(next)
            setOnClickListener { dialog.dismiss() }
        }
        page.isClickable = true  // taps on the page itself do not close the book
        bind()
        dialog = AlertDialog.Builder(context).setView(root).create()
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setDimAmount(0f)
        }
    }

    /** Turns the page sideways, like the friends on the selection screen: it slides out and the next slides in. */
    private fun turn(step: Int) {
        val to = index + step
        // One more page after the last episode: more stories are on the way.
        if (to !in 0..stories.size || flipping) return
        flipping = true
        val shift = page.width * 0.35f
        page.animate().translationX(-step * shift).alpha(0f).setDuration(SLIDE_MS).withEndAction {
            index = to; bind()
            page.translationX = step * shift
            page.animate().translationX(0f).alpha(1f).setDuration(SLIDE_MS).withEndAction { flipping = false }.start()
        }.start()
    }

    private fun bind() {
        // A hidden arrow still takes the tap, so a tap there does not close the book.
        for ((arrow, shown) in listOf(prev to (index > 0), next to (index < stories.size))) {
            arrow.alpha = if (shown) 1f else 0f; arrow.isEnabled = shown
            arrow.importantForAccessibility = if (shown) View.IMPORTANT_FOR_ACCESSIBILITY_YES else View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        counter.text = context.getString(R.string.choose_story_page, index + 1, stories.size + 1)
        if (index == stories.size) { bindComing(); return }
        val story = stories[index]
        val seen = story.id in read
        val open = seen || story.isUnlocked(read)
        val episode = if (index == 0) context.getString(R.string.collection_intro) else context.getString(R.string.collection_episode, index)
        heading.text = context.getString(R.string.choose_story_heading, episode, story.title)
        summary.text = if (open) story.summary else context.getString(R.string.choose_story_locked_summary)
        mark.text = context.getString(when {
            seen -> R.string.choose_story_read
            open -> R.string.choose_story_open
            else -> R.string.choose_story_locked
        })
        picture.set(story, open)
        go.text = context.getString(if (open) R.string.choose_story_go else R.string.choose_story_locked_button)
        go.isEnabled = open
        go.setOnClickListener { dialog.dismiss(); onPick(story) }
        page.contentDescription = "${heading.text}. ${summary.text}. ${mark.text}"
        page.requestLayout()
    }

    /** The last page: the next stories are still being written. */
    private fun bindComing() {
        heading.text = context.getString(R.string.choose_story_coming_heading)
        summary.text = context.getString(R.string.choose_story_coming_summary, friendName)
        mark.text = context.getString(R.string.choose_story_coming_mark)
        picture.setComing(stories.first().characterId, deep)
        go.text = context.getString(R.string.choose_story_coming_button)
        go.isEnabled = false
        page.contentDescription = "${heading.text}. ${summary.text}"
        page.requestLayout()
    }

    private fun text(size: Float, color: Int, bold: Boolean = false) = TextView(context).apply {
        setTextColor(color); includeFontPadding = false; gravity = Gravity.CENTER_VERTICAL
        if (bold) typeface = Typeface.DEFAULT_BOLD
        tag = size  // text size as a share of the page height (PageLayout applies it)
    }

    /** The round purple button of the login card (pale and quiet when the episode is still locked). */
    private fun pill() = Button(context).apply {
        isAllCaps = false; stateListAnimator = null; typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
        tag = 0.036f
        fun face(fill: Int) = GradientDrawable().apply { setColor(fill); cornerRadius = 999f }
        background = android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(-android.R.attr.state_enabled), face(Color.rgb(214, 206, 222)))
            addState(intArrayOf(android.R.attr.state_pressed), face(deeper))
            addState(intArrayOf(), face(deep))
        }
        setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(Color.rgb(120, 110, 132), CREAM)))
    }

    /** A round cream arrow beside the book, like the ones on the selection screen. */
    private fun arrow(symbol: String, description: Int, click: () -> Unit) = Button(context).apply {
        text = symbol; setTextColor(deep); contentDescription = context.getString(description)
        setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0; stateListAnimator = null
        val d = context.resources.displayMetrics.density
        background = RippleDrawable(ColorStateList.valueOf(0x336E54A8), GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setColor(Color.argb(235, 255, 249, 235)); setStroke((2.5f * d).toInt(), deep)
        }, null)
        setOnClickListener { click() }
    }

    /** The book page: tinted cream paper with a soft edge, the spine on the left and a bookmark at the top right. */
    internal class BookPage(tint: Int, paperColor: Int? = null) : android.graphics.drawable.Drawable() {
        private val paper = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = paperColor ?: blend(tint, Color.rgb(255, 249, 235), 0.84f) }
        private val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 40, 30, 50) }
        private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = blend(tint, Color.rgb(30, 26, 34), 0.25f) }
        private val spine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = blend(tint, Color.WHITE, 0.15f) }
        private val ribbon = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = blend(tint, Color.rgb(30, 26, 34), 0.2f) }
        private val box = RectF()
        private val path = Path()

        override fun draw(canvas: Canvas) {
            val b = bounds; val w = b.width().toFloat(); val h = b.height().toFloat()
            val r = w * 0.03f
            box.set(b.left + w * 0.012f, b.top + h * 0.012f, b.right.toFloat(), b.bottom.toFloat())
            canvas.drawRoundRect(box, r, r, shade)
            box.set(b.left.toFloat(), b.top.toFloat(), b.right - w * 0.012f, b.bottom - h * 0.012f)
            canvas.drawRoundRect(box, r, r, paper)
            // The spine: a band down the left side.
            canvas.save(); canvas.clipRect(box.left, box.top, box.left + w * 0.045f, box.bottom)
            canvas.drawRoundRect(box, r, r, spine); canvas.restore()
            edge.strokeWidth = w * 0.006f
            canvas.drawRoundRect(box, r, r, edge)
            // The bookmark hanging from the top edge, with a notch at the bottom.
            val x0 = box.right - w * 0.13f; val x1 = box.right - w * 0.065f; val y1 = box.top + h * 0.11f
            path.reset(); path.moveTo(x0, box.top); path.lineTo(x1, box.top); path.lineTo(x1, y1)
            path.lineTo((x0 + x1) / 2, y1 - h * 0.025f); path.lineTo(x0, y1); path.close()
            canvas.drawPath(path, ribbon)
        }
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
        @Deprecated("Deprecated in Java")
        override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
    }

    /** Places children by shares of its own size, and sizes their text by a share of its height (the view's tag). */
    private class PageLayout(context: Context) : ViewGroup(context) {
        private val boxes = mutableMapOf<View, RectF>()
        fun add(v: View, x0: Float, y0: Float, x1: Float, y1: Float) { boxes[v] = RectF(x0, y0, x1, y1); addView(v) }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val w = MeasureSpec.getSize(widthMeasureSpec); val h = MeasureSpec.getSize(heightMeasureSpec)
            for ((v, r) in boxes) {
                (v.tag as? Float)?.let { share ->
                    if (v is TextView) {
                        var size = h * share
                        // A long title gets smaller so it stays on one line.
                        if (v.maxLines == 1 || v.typeface == Typeface.DEFAULT_BOLD) {
                            val room = r.width() * w
                            while (size > h * 0.026f && v.paint.apply { textSize = size }.measureText(v.text.toString()) > room) size *= 0.94f
                        }
                        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
                    }
                }
                v.measure(MeasureSpec.makeMeasureSpec((r.width() * w).toInt(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec((r.height() * h).toInt(), MeasureSpec.EXACTLY))
            }
            setMeasuredDimension(w, h)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            val w = r - l; val h = b - t
            for ((v, box) in boxes) v.layout((box.left * w).toInt(), (box.top * h).toInt(), (box.right * w).toInt(), (box.bottom * h).toInt())
        }
    }

    /**
     * The episode's picture: the friend's home (from the selection backdrops) with the friends who speak in this
     * episode standing in front, the main friend in the middle. Locked pages show only a soft silhouette.
     */
    @SuppressLint("ViewConstructor")
    private class EpisodePicture(context: Context) : View(context) {
        private var home: Bitmap? = null
        private var cast: List<CharacterSprites> = emptyList()
        private var open = true
        private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
        private val smooth = Paint(Paint.FILTER_BITMAP_FLAG)
        private val veil = Paint(Paint.ANTI_ALIAS_FLAG)
        private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.rgb(196, 182, 140) }
        private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(70, 40, 30, 50) }
        private val clip = Path()
        private val box = RectF()
        private val src = android.graphics.Rect()
        private val dst = RectF()

        init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

        fun set(story: Story, open: Boolean) {
            this.open = open; coming = false
            home = homes.getOrPut(story.characterId) {
                val res = StoryBookCard.HOMES[story.characterId] ?: return@getOrPut null
                val full = BitmapFactory.decodeResource(resources, res, BitmapFactory.Options().apply { inSampleSize = 4; inScaled = false })
                    ?: return@getOrPut null
                // The home part of the widened picture: the middle 80 %, under the sky band.
                val x0 = full.width / 10; val y0 = full.height * 20 / 100
                Bitmap.createBitmap(full, x0, y0, full.width - 2 * x0, full.height - y0).also { if (it != full) full.recycle() }
            }
            val speakers = story.steps.filterIsInstance<Step.Line>().map { it.speaker }.filter { it in FRIENDS }.distinct()
            val others = speakers.filter { it != story.characterId }.take(2)
            // Main friend in the middle, the others beside.
            val order = when (others.size) { 0 -> listOf(story.characterId); 1 -> listOf(story.characterId, others[0]); else -> listOf(others[0], story.characterId, others[1]) }
            cast = order.mapNotNull { CharacterSprites.load(resources, it) }
            invalidate()
        }

        /** The "coming soon" page: the friend alone at home, waiting for the next story. */
        fun setComing(friendId: String, mark: Int) {
            set(Story(friendId, friendId, "", "", "", null, emptyList(), false), true)
            markColor = mark
            coming = true
            invalidate()
        }

        private var coming = false
        private var markColor = Color.GRAY

        override fun onDraw(canvas: Canvas) {
            val d = resources.displayMetrics.density
            val radius = 10 * d
            box.set(0f, 0f, width.toFloat(), height.toFloat())
            clip.reset(); clip.addRoundRect(box, radius, radius, Path.Direction.CW)
            canvas.save(); canvas.clipPath(clip)
            home?.let { b ->
                // Center crop.
                val scale = maxOf(width.toFloat() / b.width, height.toFloat() / b.height)
                val sw = (width / scale).toInt(); val sh = (height / scale).toInt()
                src.set((b.width - sw) / 2, (b.height - sh) / 2, (b.width + sw) / 2, (b.height + sh) / 2)
                canvas.drawBitmap(b, src, box, smooth)
            } ?: canvas.drawColor(Color.rgb(236, 228, 210))
            val s = height * 0.5f
            val floor = height * 0.93f
            val step = s * 0.95f
            val start = width / 2f - step * (cast.size - 1) / 2f
            cast.forEachIndexed { i, sprites ->
                val cx = start + i * step
                canvas.drawOval(cx - s * 0.3f, floor - s * 0.05f, cx + s * 0.3f, floor + s * 0.04f, shadow)
                dst.set(cx - s / 2, floor - s * CharacterSprites.FOOT_EDGE_Y / CharacterSprites.SIZE, cx + s / 2,
                    floor + s * (CharacterSprites.SIZE - CharacterSprites.FOOT_EDGE_Y) / CharacterSprites.SIZE)
                canvas.drawBitmap(sprites[SpriteFrame.IDLE_0], null, dst, pixel)
            }
            if (coming) {
                veil.color = Color.argb(110, 248, 242, 228)
                canvas.drawRect(box, veil)
                veil.color = markColor; veil.textAlign = Paint.Align.CENTER; veil.textSize = height * 0.2f
                canvas.drawText("✦ · · ·", width / 2f, height * 0.3f, veil)
            }
            if (!open) {
                veil.color = Color.argb(185, 248, 242, 228)
                canvas.drawRect(box, veil)
                veil.color = Color.rgb(150, 136, 168); veil.textAlign = Paint.Align.CENTER; veil.textSize = height * 0.22f
                canvas.drawText("🔒", width / 2f, height * 0.6f, veil)
            }
            canvas.restore()
            edge.strokeWidth = 1.5f * d
            box.inset(edge.strokeWidth / 2, edge.strokeWidth / 2)
            canvas.drawRoundRect(box, radius, radius, edge)
        }

        companion object {
            private val FRIENDS = setOf("mint", "cheese", "popo", "lumi", "nunu", "piko")
            private val homes = mutableMapOf<String, Bitmap?>()
        }
    }

    companion object {
        fun blend(a: Int, b: Int, t: Float): Int = Color.rgb(
            (Color.red(a) * (1 - t) + Color.red(b) * t).toInt(),
            (Color.green(a) * (1 - t) + Color.green(b) * t).toInt(),
            (Color.blue(a) * (1 - t) + Color.blue(b) * t).toInt())
        private const val L = 0.135f
        private const val R_ = 0.895f
        private const val SLIDE_MS = 150L
        private val INK = Color.rgb(46, 42, 54)
        private val SOFT = Color.rgb(100, 92, 116)
        private val CREAM = Color.parseColor("#FFF8E7")
    }
}
