package com.tinyrunners.school.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import kotlin.math.roundToInt

/**
 * The storybook "score board" shared by the run result and the friend profile (docs/research/SCORE_SCREEN_RESEARCH.md):
 * a cream page with a honey edge and a brown lip, a green ribbon carrying its title, the friend's round portrait
 * with a word in a speech bubble, big number tiles with stitched edges (the numbers count up), a 새 기록 stamp, a
 * "how close to your best" bar, small reward pills, and the story-collection button parts on the right.
 */
object ScoreBoard {
    val PAPER = Color.rgb(255, 248, 231)
    val TILE = Color.rgb(255, 252, 243)
    val HONEY = Color.rgb(212, 168, 104)
    val LIP = Color.rgb(150, 100, 50)
    val GREEN = Color.parseColor("#1E5A2A")
    val GREEN_SOFT = Color.rgb(225, 239, 218)
    val INK = Color.parseColor("#3E3A2E")
    val SOFT = Color.parseColor("#6B6250")
    val STAMP = Color.rgb(206, 92, 74)

    private fun dp(c: Context, v: Float) = (v * c.resources.displayMetrics.density).roundToInt()

    /** The page: lip underneath, paper with a honey edge on top. */
    fun pageBackground(c: Context, radius: Float = 24f): Drawable {
        val r = dp(c, radius).toFloat()
        val lip = GradientDrawable().apply { setColor(LIP); cornerRadius = r }
        val face = GradientDrawable().apply { setColor(PAPER); cornerRadius = r; setStroke(dp(c, 3f), HONEY) }
        return LayerDrawable(arrayOf(lip, face)).apply { setLayerInset(1, 0, 0, 0, dp(c, 4f)) }
    }

    /** A page with a ribbon title sitting on its top edge. Returns the wrapper and the page to fill. */
    fun page(c: Context, title: String): Pair<FrameLayout, LinearLayout> {
        val wrap = FrameLayout(c).apply { clipChildren = false; clipToPadding = false }
        val page = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            background = pageBackground(c)
            setPadding(dp(c, 18f), dp(c, 22f), dp(c, 18f), dp(c, 16f))
        }
        wrap.addView(page, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(c, 16f)
        })
        wrap.addView(Ribbon(c, title), FrameLayout.LayoutParams(dp(c, 220f), dp(c, 34f), Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        return wrap to page
    }

    /** The friend's head on a round tile in their colour, with a honey ring. */
    fun portrait(c: Context, characterId: String, tint: Int): View = Portrait(c, characterId, tint)

    /** A speech bubble with a small tail on its left, for a word from the friend. */
    fun bubble(c: Context, text: String): View = Bubble(c).apply { setText(text) }

    /** A number tile: a small label over a big value. Returns the tile and the value's TextView (for counting up). */
    fun tile(c: Context, label: String, value: String): Pair<LinearLayout, TextView> {
        val valueView = TextView(c).apply {
            text = value; textSize = 30f; setTextColor(INK); typeface = Typeface.DEFAULT_BOLD; includeFontPadding = false
        }
        val tile = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(c, 14f), dp(c, 8f), dp(c, 14f), dp(c, 10f))
            background = GradientDrawable().apply {
                setColor(TILE); cornerRadius = dp(c, 16f).toFloat()
                setStroke(dp(c, 2f), HONEY, dp(c, 6f).toFloat(), dp(c, 4f).toFloat())
            }
            addView(TextView(c).apply { text = label; textSize = 13f; setTextColor(SOFT); typeface = Typeface.DEFAULT_BOLD })
            addView(valueView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(c, 2f) })
        }
        return tile to valueView
    }

    /** A round "새 기록!" stamp, a little tilted. */
    fun stamp(c: Context, text: String): View = Stamp(c, text)

    /** A small pill: an optional icon and a short text (rewards, traits). */
    fun pill(c: Context, icon: View?, text: String, fill: Int = TILE): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(c, 10f), dp(c, 4f), dp(c, 12f), dp(c, 5f))
        background = GradientDrawable().apply { setColor(fill); cornerRadius = dp(c, 100f).toFloat(); setStroke(dp(c, 2f), HONEY) }
        icon?.let { addView(it, LinearLayout.LayoutParams(dp(c, 18f), dp(c, 18f)).apply { marginEnd = dp(c, 6f) }) }
        addView(TextView(c).apply { this.text = text; textSize = 13f; setTextColor(INK); typeface = Typeface.DEFAULT_BOLD; maxLines = 1 })
    }

    /** The story collection's green or outlined button part, for the column of actions. */
    fun button(c: Context, text: String, primary: Boolean, click: () -> Unit): Button = Button(c).apply {
        this.text = text; textSize = 16f; isAllCaps = false; stateListAnimator = null; typeface = Typeface.DEFAULT_BOLD
        minHeight = 0; minimumHeight = 0; minWidth = 0; minimumWidth = 0; setPadding(dp(c, 10f), 0, dp(c, 10f), 0)
        restyle(this, primary)
        setOnClickListener { click() }
    }

    fun restyle(button: Button, primary: Boolean) {
        button.setTextColor(if (primary) Color.parseColor("#FFF8E7") else GREEN)
        button.background = RippleDrawable(ColorStateList.valueOf(0x22000000),
            button.context.getDrawable(if (primary) R.drawable.ui_story_btn_primary else R.drawable.ui_story_btn_outline), null)
    }

    /** Counts [view] up from 0 to [target], writing it with [format]. */
    fun countUp(view: TextView, target: Int, format: (Int) -> String, delayMs: Long = 0L) {
        view.text = format(0)
        ValueAnimator.ofInt(0, target).apply {
            duration = (500L + target.coerceAtMost(2000) / 2L).coerceAtMost(1400L)
            startDelay = delayMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { view.text = format(it.animatedValue as Int) }
            start()
        }
    }
}

/** "How close to your best": a rounded track with a green fill and a small flag at the end. */
@SuppressLint("ViewConstructor")
class BestBar(context: Context) : View(context) {
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(240, 228, 204) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(96, 150, 84) }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ScoreBoard.HONEY; style = Paint.Style.STROKE }
    private val flag = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ScoreBoard.STAMP }
    private val r = RectF()
    private var shown = 0f

    /** Fills to [fraction] (0..1) with a short slide. */
    fun set(fraction: Float) {
        ValueAnimator.ofFloat(shown, fraction.coerceIn(0f, 1f)).apply {
            duration = 700; interpolator = DecelerateInterpolator()
            addUpdateListener { shown = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val h = height.toFloat(); val w = width.toFloat() - 10 * d
        val rad = h / 2f
        r.set(0f, 0f, w, h); canvas.drawRoundRect(r, rad, rad, track)
        if (shown > 0f) { r.set(0f, 0f, maxOf(h, w * shown), h); canvas.drawRoundRect(r, rad, rad, fill) }
        edge.strokeWidth = 2 * d; r.set(d, d, w - d, h - d); canvas.drawRoundRect(r, rad, rad, edge)
        // The flag at the best (the bar's end).
        canvas.drawRect(w + 2 * d, 0f, w + 3.5f * d, h, edge.apply { style = Paint.Style.FILL; color = ScoreBoard.LIP })
        edge.style = Paint.Style.STROKE; edge.color = ScoreBoard.HONEY
        val p = Path(); p.moveTo(w + 3.5f * d, 0f); p.lineTo(w + 10 * d, h * 0.22f); p.lineTo(w + 3.5f * d, h * 0.44f); p.close()
        canvas.drawPath(p, flag)
    }
}

@SuppressLint("ViewConstructor")
private class Ribbon(context: Context, private val title: String) : View(context) {
    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ScoreBoard.GREEN }
    private val tail = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 66, 30) }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF8E7"); typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    private val path = Path()

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val d = resources.displayMetrics.density
        val notch = 10 * d; val fold = 14 * d
        // Tails with a notch, a little lower, then the band.
        path.reset(); path.moveTo(0f, h * 0.25f); path.lineTo(fold + 4 * d, h * 0.25f); path.lineTo(fold + 4 * d, h); path.lineTo(0f, h); path.lineTo(notch, h * 0.62f); path.close()
        canvas.drawPath(path, tail)
        path.reset(); path.moveTo(w, h * 0.25f); path.lineTo(w - fold - 4 * d, h * 0.25f); path.lineTo(w - fold - 4 * d, h); path.lineTo(w, h); path.lineTo(w - notch, h * 0.62f); path.close()
        canvas.drawPath(path, tail)
        canvas.drawRoundRect(fold, 0f, w - fold, h * 0.84f, 6 * d, 6 * d, body)
        text.textSize = h * 0.42f
        canvas.drawText(title, w / 2f, h * 0.42f + text.textSize * 0.36f, text)
    }
}

@SuppressLint("ViewConstructor")
private class Portrait(context: Context, characterId: String, private val tint: Int) : View(context) {
    private val sprites = com.tinyrunners.school.characters.CharacterSprites.load(resources, characterId)
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onDraw(canvas: Canvas) {
        val s = minOf(width, height).toFloat(); val d = resources.displayMetrics.density
        val cx = width / 2f; val cy = height / 2f
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb((Color.red(tint) * 0.35f + 255 * 0.65f).toInt(), (Color.green(tint) * 0.35f + 255 * 0.65f).toInt(), (Color.blue(tint) * 0.35f + 255 * 0.65f).toInt())
        canvas.drawCircle(cx, cy, s / 2f - 2 * d, paint)
        sprites?.let { canvas.drawBitmap(it[com.tinyrunners.school.characters.SpriteFrame.IDLE_0], null, RectF(cx - s * 0.46f, cy - s * 0.46f, cx + s * 0.46f, cy + s * 0.46f), pixel) }
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 3 * d; paint.color = ScoreBoard.HONEY
        canvas.drawCircle(cx, cy, s / 2f - 2 * d, paint)
    }
}

private class Bubble(context: Context) : TextView(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    init {
        val d = resources.displayMetrics.density
        textSize = 15f; setTextColor(ScoreBoard.INK); maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding((22 * d).roundToInt(), (8 * d).roundToInt(), (14 * d).roundToInt(), (8 * d).roundToInt())
        setLineSpacing(0f, 1.1f)
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val w = width.toFloat(); val h = height.toFloat(); val tail = 9 * d
        paint.style = Paint.Style.FILL; paint.color = ScoreBoard.GREEN_SOFT
        canvas.drawRoundRect(tail, 0f, w, h, 16 * d, 16 * d, paint)
        path.reset(); path.moveTo(0f, h / 2f); path.lineTo(tail + 2 * d, h / 2f - 7 * d); path.lineTo(tail + 2 * d, h / 2f + 7 * d); path.close()
        canvas.drawPath(path, paint)
        super.onDraw(canvas)
    }
}

@SuppressLint("ViewConstructor")
private class Stamp(context: Context, private val label: String) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init { rotation = -12f; contentDescription = label }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val s = minOf(width, height).toFloat(); val cx = width / 2f; val cy = height / 2f
        paint.style = Paint.Style.FILL; paint.color = Color.argb(235, 255, 244, 236)
        canvas.drawCircle(cx, cy, s / 2f - 2 * d, paint)
        paint.style = Paint.Style.STROKE; paint.color = ScoreBoard.STAMP
        paint.strokeWidth = 3 * d; canvas.drawCircle(cx, cy, s / 2f - 3 * d, paint)
        paint.strokeWidth = 1.5f * d; canvas.drawCircle(cx, cy, s / 2f - 8 * d, paint)
        paint.style = Paint.Style.FILL; paint.typeface = Typeface.DEFAULT_BOLD; paint.textAlign = Paint.Align.CENTER
        paint.textSize = s * 0.22f
        canvas.drawText(label, cx, cy + paint.textSize * 0.36f, paint)
    }
}
