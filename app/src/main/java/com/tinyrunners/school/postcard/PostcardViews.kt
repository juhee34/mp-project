package com.tinyrunners.school.postcard

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.auth.AuthService
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.story.BackgroundThumbs
import com.tinyrunners.school.story.StoryRepository
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The picture side of a postcard, drawn from the app's own art until A's postcard pictures come: the friend's home,
 * the friend (standing on the first card, mid-run on the second, the scene a little further along), a cream border,
 * a stamp in the friend's colour and the title.
 */
@SuppressLint("ViewConstructor")
class PostcardArt(context: Context, private val card: Postcard, private val friendColor: Int) : View(context) {
    private val index = card.id.substringAfterLast('_').toIntOrNull() ?: 1
    private val scene = BackgroundThumbs.of(context, card.characterId)
    private val sprites = CharacterSprites.load(resources, card.characterId)
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD }
    private val clip = Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val r = h * 0.06f
        val border = h * 0.05f
        fill.color = Color.parseColor("#FFF8E7"); canvas.drawRoundRect(0f, 0f, w, h, r, r, fill)
        val band = h * 0.2f
        val pic = RectF(border, border, w - border, h - band)
        canvas.save()
        clip.reset(); clip.addRoundRect(pic, r * 0.6f, r * 0.6f, Path.Direction.CW); canvas.clipPath(clip)
        var floor = pic.bottom - pic.height() * 0.1f
        scene?.let { (bitmap, floorFraction) ->
            val scale = max(pic.height() / bitmap.height, pic.width() / bitmap.width) * 1.15f
            val visibleW = (pic.width() / scale).toInt().coerceAtMost(bitmap.width)
            val visibleH = (pic.height() / scale).coerceAtMost(bitmap.height.toFloat())
            val floorPx = bitmap.height * floorFraction
            val top = (floorPx - visibleH * 0.88f).coerceIn(0f, bitmap.height - visibleH)
            // The second card looks a little further down the road.
            val left = if (index >= 2) (bitmap.width - visibleW).coerceAtMost((visibleW * 0.6f).toInt()) else 0
            canvas.drawBitmap(bitmap, Rect(left, top.toInt(), left + visibleW, (top + visibleH).toInt()), pic, pixel)
            floor = pic.top + (floorPx - top) * scale
        } ?: run { fill.color = Color.parseColor("#DCEBCB"); canvas.drawRect(pic, fill) }
        sprites?.let { s ->
            val px = max(1, (pic.height() * 0.55f / CharacterSprites.SIZE).roundToInt()).toFloat()
            val size = CharacterSprites.SIZE * px
            val cx = pic.left + pic.width() * (if (index >= 2) 0.38f else 0.62f)
            val left = (cx - CharacterSprites.CENTER_X * px).roundToInt().toFloat()
            val top = (floor - CharacterSprites.FOOT_EDGE_Y * px).roundToInt().toFloat()
            canvas.drawBitmap(s[if (index >= 2) SpriteFrame.RUN_1 else SpriteFrame.IDLE_0], null, RectF(left, top, left + size, top + size), pixel)
        }
        canvas.restore()
        // Stamp: a scalloped square in the friend's colour with a small heart.
        val st = h * 0.2f
        val sx = pic.right - st - border * 0.6f; val sy = pic.top + border * 0.6f
        fill.color = Color.WHITE; canvas.drawRect(sx - 3f, sy - 3f, sx + st + 3f, sy + st * 1.15f + 3f, fill)
        fill.color = friendColor; canvas.drawRect(sx, sy, sx + st, sy + st * 1.15f, fill)
        fill.color = Color.argb(220, 255, 255, 255)
        val hx = sx + st / 2f; val hy = sy + st * 0.55f; val hr = st * 0.16f
        canvas.drawCircle(hx - hr * 0.7f, hy - hr * 0.3f, hr, fill); canvas.drawCircle(hx + hr * 0.7f, hy - hr * 0.3f, hr, fill)
        clip.reset(); clip.moveTo(hx - hr * 1.6f, hy - hr * 0.1f); clip.lineTo(hx + hr * 1.6f, hy - hr * 0.1f); clip.lineTo(hx, hy + hr * 1.7f); clip.close()
        canvas.drawPath(clip, fill)
        // Title on the cream band.
        text.color = Color.parseColor("#5E4594")
        text.textSize = band * 0.5f
        while (text.measureText(card.title) > w - border * 4 && text.textSize > 8f) text.textSize -= 1f
        canvas.drawText(card.title, (w - text.measureText(card.title)) / 2f, h - band / 2f - (text.descent() + text.ascent()) / 2f, text)
    }
}

/** Postcards already shown as new, per account (so each one is announced once). */
class PostcardSeen(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("postcards_" + (AuthService.currentUid(context) ?: "guest"), Context.MODE_PRIVATE)
    var ids: Set<String>
        get() = prefs.getStringSet("seen", emptySet()).orEmpty()
        set(value) { prefs.edit().putStringSet("seen", value).apply() }
}

/**
 * After a run is saved or a story ends: reads the progress, and when a postcard was earned that has not been
 * announced yet, shows a small card at the top of [activity] ("새 엽서를 받았어요 · 라일 「고사리 언덕의 아침」",
 * 엽서첩 보기) for a few seconds.
 */
object PostcardNotice {
    fun checkNew(activity: Activity) {
        ProgressStore.forCurrentUser(activity).loadAll { result ->
            if (activity.isDestroyed) return@loadAll
            val progress = result.getOrNull() ?: return@loadAll
            val cards = PostcardRepository.all(activity)
            val stories = StoryRepository.load(activity).groupBy({ it.characterId }, { it.id })
            val earned = PostcardRules.earnedIds(cards, progress, stories)
            val seen = PostcardSeen(activity)
            val fresh = cards.filter { it.id in earned && it.id !in seen.ids }
            if (fresh.isEmpty()) return@loadAll
            seen.ids = seen.ids + fresh.map { it.id }
            show(activity, fresh)
        }
    }

    private fun show(activity: Activity, fresh: List<Postcard>) {
        val root = activity.window.decorView as? ViewGroup ?: return
        val d = activity.resources.displayMetrics.density
        val first = fresh.first()
        val friend = com.tinyrunners.school.data.CharacterRepository(activity).find(first.characterId)
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * d).toInt(), (10 * d).toInt(), (10 * d).toInt(), (10 * d).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FFF8E7")); cornerRadius = 22 * d; setStroke((2 * d).toInt(), Color.parseColor("#C9A26A"))
            }
            elevation = 8 * d
        }
        val color = friend?.color?.let { runCatching { Color.parseColor(it) }.getOrNull() } ?: Color.parseColor("#9B7BD0")
        card.addView(PostcardArt(activity, first, color), LinearLayout.LayoutParams((84 * d).toInt(), (58 * d).toInt()))
        card.addView(TextView(activity).apply {
            text = if (fresh.size == 1) activity.getString(R.string.postcard_new_one, friend?.name.orEmpty(), first.title)
                else activity.getString(R.string.postcard_new_many, fresh.size)
            textSize = 15f; setTextColor(Color.parseColor("#3D3550")); setTypeface(typeface, Typeface.BOLD)
            setPadding((12 * d).toInt(), 0, (12 * d).toInt(), 0)
        })
        card.addView(TextView(activity).apply {
            setText(R.string.postcard_open_album); textSize = 14f; setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER; setPadding((14 * d).toInt(), (8 * d).toInt(), (14 * d).toInt(), (8 * d).toInt())
            background = GradientDrawable().apply { setColor(Color.parseColor("#5E4594")); cornerRadius = 18 * d }
            setOnClickListener { activity.startActivity(PostcardActivity.intent(activity)) }
        })
        root.addView(card, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = (18 * d).toInt() })
        card.alpha = 0f; card.translationY = -40 * d
        card.animate().alpha(1f).translationY(0f).setDuration(260).start()
        card.postDelayed({
            card.animate().alpha(0f).translationY(-40 * d).setDuration(260).withEndAction { root.removeView(card) }.start()
        }, SHOW_MS)
    }

    private const val SHOW_MS = 4500L
}
