package com.tinyrunners.school.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.story.BackgroundThumbs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The RPG talk box for the story's opening, climax and ending: a cream box at the bottom with the speaker's name on a
 * pill (none for the narrator), the words typed out letter by letter, ▼ when a tap goes on. A tap shows the whole line
 * at once, the next tap moves on; "건너뛰기" ends the talk. Sits over the run (dimmed) or over the ending picture.
 * With [friendId] the friend stands big in the middle of the screen, just above the box, while the talk goes on.
 */
@SuppressLint("ViewConstructor")
class StoryDialogView(
    context: Context,
    private val friendName: String,
    private val friendColor: Int,
    private val lines: List<RunLine>,
    dim: Boolean,
    friendId: String? = null,
    private val onDone: () -> Unit,
) : FrameLayout(context) {
    private val d = resources.displayMetrics.density
    private val handler = Handler(Looper.getMainLooper())
    private val nameTag = TextView(context)
    private val words = TextView(context)
    private val more = TextView(context)
    private var index = -1
    private var shown = 0
    private var typing = false
    private val charMs = com.tinyrunners.school.audio.AudioSettings(context).talkSpeed.charMs.toLong()

    init {
        if (dim) setBackgroundColor(Color.argb(110, 20, 24, 30))
        isClickable = true
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(26), dp(18), dp(26), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.argb(244, 255, 248, 236)); cornerRadius = dp(22).toFloat(); setStroke(dp(3), LAVENDER)
            }
        }
        words.apply { textSize = 19f; setTextColor(INK); setLineSpacing(0f, 1.2f); minLines = 2 }
        box.addView(words)
        more.apply { text = "▼"; textSize = 14f; setTextColor(LAVENDER); gravity = Gravity.END }
        box.addView(more, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        nameTag.apply {
            textSize = 16f; setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(18), dp(6), dp(18), dp(6))
            background = GradientDrawable().apply { setColor(friendColor); cornerRadius = dp(18).toFloat() }
            translationZ = dp(2).toFloat()
        }
        // The name pill sits on the box's top edge, as in the story talks.
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        // The friend in the middle, standing on the box (breathing like on the opening screen).
        if (friendId != null) {
            val size = (resources.displayMetrics.heightPixels * 0.42f).roundToInt()
            column.addView(com.tinyrunners.school.opening.OpeningFriend(context, friendId, 0),
                LinearLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = -dp(6) })
        }
        column.addView(nameTag, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            marginStart = dp(22); bottomMargin = -dp(14)
        })
        column.addView(box, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        addView(column, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
            leftMargin = dp(24); rightMargin = dp(24); bottomMargin = dp(18)
        })
        addView(TextView(context).apply {
            setText(R.string.story_skip); textSize = 15f; setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = GradientDrawable().apply { setColor(Color.argb(150, 40, 40, 40)); cornerRadius = dp(20).toFloat() }
            setOnClickListener { finish() }
        }, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END).apply { topMargin = dp(14); rightMargin = dp(14) })
        setOnClickListener { tap() }
        next()
    }

    private fun tap() {
        if (typing) { shown = lines[index].text.length; showTyped(); typing = false; handler.removeCallbacksAndMessages(null) } else next()
    }

    private fun next() {
        index++
        if (index >= lines.size) { finish(); return }
        val line = lines[index]
        nameTag.visibility = if (line.narrator) View.INVISIBLE else View.VISIBLE
        nameTag.text = friendName
        words.setTypeface(null, if (line.narrator) Typeface.ITALIC else Typeface.NORMAL)
        shown = 0; typing = true
        more.visibility = View.INVISIBLE
        val step = object : Runnable {
            override fun run() {
                shown++
                showTyped()
                if (shown < line.text.length) handler.postDelayed(this, charMs) else typing = false
            }
        }
        handler.post(step)
    }

    private fun showTyped() {
        val line = lines[index]
        words.text = line.text.take(shown)
        if (shown >= line.text.length) more.visibility = View.VISIBLE
    }

    private var done = false
    private fun finish() {
        if (done) return
        done = true
        handler.removeCallbacksAndMessages(null)
        (parent as? android.view.ViewGroup)?.removeView(this)
        onDone()
    }

    override fun onDetachedFromWindow() { handler.removeCallbacksAndMessages(null); super.onDetachedFromWindow() }

    private fun dp(v: Int) = (v * d).roundToInt()

    private companion object {
        val INK = Color.parseColor("#3D3550")
        val LAVENDER = Color.parseColor("#9B7BD0")
    }
}

/**
 * The ending picture: the friend at home, framed like a storybook page, with the light of the ending — a warm glow for
 * happy, a starry night for hidden, a grey dusk for bad, plain day for an ordinary story — the treasure chest at a
 * finale (open and shining, or closed), and the ending's name on a ribbon. The talk box sits under it.
 */
@SuppressLint("ViewConstructor")
class EndingPicture(context: Context, private val friendId: String, private val ending: String, private val title: String) : View(context) {
    private val scene = BackgroundThumbs.of(context, friendId)
    private val sprites = CharacterSprites.load(resources, friendId)
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    private val clip = Path()
    private val d = resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawColor(Color.argb(235, 34, 30, 38))
        // The picture: upper part of the screen, leaving room for the talk box below.
        val pw = minOf(w * 0.62f, (h * 0.56f) * 2.1f); val ph = pw / 2.1f
        val box = RectF((w - pw) / 2f, h * 0.1f, (w + pw) / 2f, h * 0.1f + ph)
        fill.color = Color.parseColor("#FFF8E7")
        canvas.drawRoundRect(box.left - 10 * d, box.top - 10 * d, box.right + 10 * d, box.bottom + 10 * d, 18 * d, 18 * d, fill)
        canvas.save()
        clip.reset(); clip.addRoundRect(box, 12 * d, 12 * d, Path.Direction.CW); canvas.clipPath(clip)
        var floor = box.bottom - ph * 0.1f
        scene?.let { (bitmap, floorFraction) ->
            val scale = max(box.height() / bitmap.height, box.width() / bitmap.width)
            val vw = (box.width() / scale).toInt().coerceAtMost(bitmap.width)
            val vh = (box.height() / scale).coerceAtMost(bitmap.height.toFloat())
            val floorPx = bitmap.height * floorFraction
            val top = (floorPx - vh * 0.88f).coerceIn(0f, bitmap.height - vh)
            canvas.drawBitmap(bitmap, Rect(0, top.toInt(), vw, (top + vh).toInt()), box, pixel)
            floor = box.top + (floorPx - top) * scale
        }
        when (ending) {
            EndingRule.HIDDEN -> {
                fill.color = Color.argb(150, 24, 30, 80); canvas.drawRect(box, fill)
                fill.color = Color.rgb(255, 246, 200)
                for (i in 0 until 26) canvas.drawCircle(box.left + (i * 137 % 100) / 100f * box.width(), box.top + (i * 53 % 45) / 100f * box.height(), 2.2f * d, fill)
                canvas.drawCircle(box.right - box.width() * 0.14f, box.top + box.height() * 0.18f, box.height() * 0.07f, fill)
            }
            EndingRule.HAPPY -> { fill.color = Color.argb(70, 255, 214, 120); canvas.drawRect(box, fill) }
            EndingRule.BAD -> { fill.color = Color.argb(110, 90, 90, 100); canvas.drawRect(box, fill) }
        }
        val px = max(1, (box.height() * 0.42f / CharacterSprites.SIZE).roundToInt()).toFloat()
        val cx = box.centerX() - box.width() * 0.08f
        sprites?.let { s ->
            val size = CharacterSprites.SIZE * px
            val left = (cx - CharacterSprites.CENTER_X * px).roundToInt().toFloat()
            val top = (floor - CharacterSprites.FOOT_EDGE_Y * px).roundToInt().toFloat()
            canvas.drawBitmap(s[if (ending == EndingRule.BAD) SpriteFrame.IDLE_1 else SpriteFrame.IDLE_0], null, RectF(left, top, left + size, top + size), pixel)
        }
        if (ending == EndingRule.HAPPY || ending == EndingRule.HIDDEN || ending == EndingRule.BAD) chest(canvas, cx + 26 * px, floor, px, open = ending != EndingRule.BAD)
        canvas.restore()
        // Ribbon with the ending's name.
        val label = when (ending) {
            EndingRule.HAPPY -> context.getString(R.string.ending_happy)
            EndingRule.HIDDEN -> context.getString(R.string.ending_hidden)
            EndingRule.BAD -> context.getString(R.string.ending_bad)
            else -> context.getString(R.string.ending_story, title)
        }
        text.textSize = 22 * d
        val rw = text.measureText(label) + 60 * d
        val ribbon = RectF(w / 2f - rw / 2f, box.top - 30 * d, w / 2f + rw / 2f, box.top + 10 * d)
        fill.color = when (ending) { EndingRule.HIDDEN -> Color.rgb(84, 76, 150); EndingRule.BAD -> Color.rgb(110, 104, 112); else -> Color.rgb(30, 90, 42) }
        canvas.drawRoundRect(ribbon, 10 * d, 10 * d, fill)
        text.color = Color.WHITE
        canvas.drawText(label, ribbon.centerX(), ribbon.centerY() - (text.descent() + text.ascent()) / 2f, text)
    }

    /** A small pixel treasure chest; open with a glow, or shut. */
    private fun chest(canvas: Canvas, cx: Float, floor: Float, px: Float, open: Boolean) {
        val w = 16 * px; val h = 11 * px
        val l = cx - w / 2f; val t = floor - h
        if (open) {
            fill.color = Color.argb(150, 255, 236, 150); canvas.drawCircle(cx, t - 2 * px, 12 * px, fill)
        }
        fill.color = Color.rgb(120, 70, 36); canvas.drawRect(l, t, l + w, floor, fill)
        fill.color = Color.rgb(176, 110, 54); canvas.drawRect(l + px, t + px, l + w - px, floor - px, fill)
        fill.color = Color.rgb(236, 196, 84); canvas.drawRect(l, t + 3 * px, l + w, t + 4 * px, fill)
        canvas.drawRect(cx - px, t + 3 * px, cx + px, t + 6 * px, fill)
        fill.color = Color.rgb(120, 70, 36)
        if (open) canvas.drawRect(l, t - 6 * px, l + w, t - 4 * px, fill) else canvas.drawRect(l, t - 3 * px, l + w, t, fill)
    }
}

/**
 * The crossroads question: the run stops, everything behind goes dark, and the question shows with two route buttons
 * (the raised path or the ground). A tap on one picks it and the run goes on.
 */
@SuppressLint("ViewConstructor")
class ForkChoiceView(context: Context, fork: RunFork, private val onPick: (Boolean) -> Unit) : FrameLayout(context) {
    private val d = resources.displayMetrics.density

    init {
        setBackgroundColor(Color.argb(170, 16, 18, 26))
        isClickable = true
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(22), dp(28), dp(24))
            background = GradientDrawable().apply { setColor(Color.parseColor("#FFF8E7")); cornerRadius = dp(24).toFloat(); setStroke(dp(3), LAVENDER) }
        }
        card.addView(TextView(context).apply {
            setText(R.string.fork_choice_eyebrow); textSize = 14f; setTextColor(LAVENDER); setTypeface(typeface, Typeface.BOLD); gravity = Gravity.CENTER
        })
        card.addView(TextView(context).apply {
            text = fork.question; textSize = 24f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD); gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(16))
        })
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(route(context.getString(R.string.fork_choice_up), fork.up, filled = true) { pick(true) },
            LinearLayout.LayoutParams(dp(280), LinearLayout.LayoutParams.WRAP_CONTENT))
        row.addView(route(context.getString(R.string.fork_choice_down), fork.down, filled = false) { pick(false) },
            LinearLayout.LayoutParams(dp(280), LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(16) })
        card.addView(row)
        addView(card, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    }

    /** One route: its name in bold, what lies on it below. */
    private fun route(name: String, about: String, filled: Boolean, click: () -> Unit) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(Color.argb(60, 255, 255, 255)),
            GradientDrawable().apply { setColor(if (filled) GREEN else Color.parseColor("#FFF8E7")); cornerRadius = dp(18).toFloat(); setStroke(dp(2), GREEN) }, null)
        isClickable = true
        setOnClickListener { click() }
        addView(TextView(context).apply { text = name; textSize = 19f; setTypeface(typeface, Typeface.BOLD); gravity = Gravity.CENTER; setTextColor(if (filled) Color.WHITE else GREEN) })
        addView(TextView(context).apply {
            text = about.substringAfter(": ", about); textSize = 14f; gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0)
            setTextColor(if (filled) Color.argb(230, 255, 255, 255) else Color.parseColor("#5E5A4C"))
        })
    }

    private var picked = false
    private fun pick(up: Boolean) {
        if (picked) return
        picked = true
        (parent as? android.view.ViewGroup)?.removeView(this)
        onPick(up)
    }

    private fun dp(v: Int) = (v * d).roundToInt()

    private companion object {
        val GREEN = Color.parseColor("#1E5A2A")
        val LAVENDER = Color.parseColor("#9B7BD0")
    }
}
