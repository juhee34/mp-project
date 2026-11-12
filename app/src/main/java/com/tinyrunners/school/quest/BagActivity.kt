package com.tinyrunners.school.quest

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.shop.ShopActivity
import com.tinyrunners.school.shop.ShopStore
import com.tinyrunners.school.story.Story
import com.tinyrunners.school.story.StoryListActivity
import com.tinyrunners.school.story.StoryRepository
import com.tinyrunners.school.talk.TalkActivity
import kotlin.math.roundToInt

/**
 * 가방, in A's approved design (docs/art/approved/bag-approved-20261112.png): the shop's cream fabric and honey-wood
 * frame, a coin badge, and six item cards in two rows — the item standing on a tuft of grass, then its name and count,
 * the friend it is for, and either how many more are needed (three dots) or "전할 준비 완료" with the next story
 * and a "전하러 가기" button. A meadow with a cottage runs along the bottom. All numbers come from the account.
 */
class BagActivity : Activity() {
    private val friends by lazy { CharacterRepository(this).characters }
    private lateinit var coins: TextView
    private lateinit var grid: LinearLayout
    private lateinit var message: TextView
    private var bag: ItemBag? = null
    private var read: Set<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(26), dp(8), dp(26), dp(44)) }

        // Top: 처음으로 | 가방 (with its subtitle) ............ coins
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(com.tinyrunners.school.ui.HomeButton.create(this).apply {
            textSize = 14f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD)
            background = RippleDrawable(ColorStateList.valueOf(ShopActivity.OWNED), GradientDrawable().apply {
                setColor(ShopActivity.CARD); cornerRadius = dp(18).toFloat(); setStroke(dp(2), ShopActivity.HONEY)
            }, null)
            minHeight = dp(38); minimumHeight = dp(38); setPadding(dp(14), 0, dp(16), 0)
        })
        top.addView(View(this).apply { setBackgroundColor(ShopActivity.HONEY) },
            LinearLayout.LayoutParams(dp(2), dp(40)).apply { marginStart = dp(14); marginEnd = dp(14) })
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(TextView(this).apply {
            setText(R.string.bag_title); textSize = 26f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD)
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            includeFontPadding = false
        })
        titles.addView(TextView(this).apply { setText(R.string.bag_subtitle); textSize = 14f; setTextColor(INK_SOFT) })
        top.addView(titles, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        coins = TextView(this).apply {
            text = "…"
            textSize = 20f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(14), dp(4), dp(20), dp(4))
            background = RippleDrawable(ColorStateList.valueOf(ShopActivity.OWNED), GradientDrawable().apply {
                setColor(ShopActivity.CARD); cornerRadius = dp(22).toFloat(); setStroke(dp(2), ShopActivity.HONEY)
            }, null)
            compoundDrawablePadding = dp(10)
            setCompoundDrawablesRelative(GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(COIN); setStroke(dp(2), ShopActivity.PRICE); setBounds(0, 0, dp(24), dp(24))
            }, null, null, null)
            contentDescription = getString(R.string.bag_coins_description)
            // Coins are spent in the shop.
            setOnClickListener { startActivity(ShopActivity.intent(this@BagActivity, null)) }
        }
        top.addView(coins)
        content.addView(top)

        // Guide line with a small lavender sprig.
        val guide = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        guide.addView(SprigIcon(this), LinearLayout.LayoutParams(dp(22), dp(22)))
        message = TextView(this).apply { setText(R.string.bag_guide); textSize = 14f; setTextColor(INK_SOFT); setPadding(dp(8), 0, 0, 0) }
        guide.addView(message)
        content.addView(guide, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2) })

        grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(grid, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(6) })

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(ShopActivity.FABRIC)
            addView(BagBackground(this@BagActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
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
        render()
        // Everything is re-read when the screen comes back: runs, purchases and hand-overs change it.
        ShopStore.forCurrentUser(this).load { result ->
            if (isDestroyed) return@load
            result.onSuccess { coins.text = it.coins.toString() }.onFailure { coins.text = "—" }
        }
        ItemBagStore.forCurrentUser(this).load { result ->
            if (isDestroyed) return@load
            result.onSuccess { bag = it; render() }.onFailure { message.setTextColor(ERROR); message.setText(R.string.quest_error) }
        }
        ProgressStore.forCurrentUser(this).loadAll { result ->
            if (isDestroyed) return@loadAll
            read = result.getOrNull().orEmpty().values.flatMap { it.readStoryIds }.toSet()
            render()
        }
    }

    /** Six item cards, three to a row. */
    private fun render() {
        grid.removeAllViews()
        val stories = StoryRepository.load(this)
        QuestItems.all.chunked(3).forEachIndexed { r, rowItems ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowItems.forEachIndexed { i, item ->
                row.addView(card(item, stories), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (i > 0) marginStart = dp(14)
                })
            }
            grid.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { if (r > 0) topMargin = dp(10) })
        }
    }

    private fun card(item: QuestItem, stories: List<Story>): View {
        val friend = friends.firstOrNull { it.id == item.friendId }
        val have = bag?.count(item.friendId)
        val next = read?.let { Story.nextUnread(stories, item.friendId, it) }
        val ready = have != null && have >= QuestRules.NEEDED && next != null
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(4), dp(12), dp(4))
            // The card whose items can be given now gets a lavender edge.
            background = GradientDrawable().apply {
                setColor(ShopActivity.CARD); cornerRadius = dp(16).toFloat()
                setStroke(dp(if (ready) 3 else 2), if (ready) ShopActivity.LAVENDER else ShopActivity.HONEY_LIGHT)
            }
        }
        card.addView(ItemOnGrass(this, item).apply { contentDescription = getString(item.name) },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.8f))
        card.addView(View(this).apply { setBackgroundColor(ShopActivity.HONEY_LIGHT) },
            LinearLayout.LayoutParams(dp(2), LinearLayout.LayoutParams.MATCH_PARENT).apply { marginStart = dp(8); marginEnd = dp(12); topMargin = dp(6); bottomMargin = dp(6) })

        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        // Name and count.
        val nameRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        nameRow.addView(TextView(this).apply {
            setText(item.name); textSize = 17f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD); maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        nameRow.addView(chip(if (have == null) "…" else getString(R.string.bag_count, have), CHIP))
        column.addView(nameRow)
        // Who it is for.
        val forRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        forRow.addView(FriendFace(this, item.friendId), LinearLayout.LayoutParams(dp(22), dp(22)))
        forRow.addView(TextView(this).apply {
            text = getString(R.string.bag_for, friend?.name.orEmpty()); textSize = 14f; setTextColor(INK); setPadding(dp(6), 0, 0, 0)
        })
        if (ready) forRow.addView(chip(getString(R.string.bag_ready_chip), ShopActivity.OWNED),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(8) })
        column.addView(forRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        if (ready && next != null) {
            // Ready: a chip, the story the items open, and the button.
            val readyRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            readyRow.addView(BookIcon(this), LinearLayout.LayoutParams(dp(18), dp(14)))
            readyRow.addView(TextView(this).apply {
                text = next.title; textSize = 13f; setTextColor(INK); maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(dp(4), 0, 0, 0)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            column.addView(readyRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(3) })
            column.addView(Button(this).apply {
                setText(R.string.bag_give)
                textSize = 14f; isAllCaps = false; stateListAnimator = null; setTypeface(typeface, Typeface.BOLD)
                minHeight = dp(30); minimumHeight = dp(30); setPadding(dp(18), 0, dp(18), 0)
                setTextColor(Color.WHITE)
                background = RippleDrawable(ColorStateList.valueOf(ShopActivity.OWNED), GradientDrawable().apply { setColor(GREEN); cornerRadius = dp(15).toFloat() }, null)
                setOnClickListener {
                    // The village list (on that friend) stays underneath, so the story ends there.
                    startActivities(arrayOf(StoryListActivity.intent(this@BagActivity, item.friendId),
                        TalkActivity.village(this@BagActivity, item.friendId, next.id, askItems = true)))
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(30)).apply { topMargin = dp(4) })
        } else {
            // Not yet: a thin rule, how many more are needed, and three dots filling up as items are found.
            column.addView(View(this).apply { setBackgroundColor(ShopActivity.HONEY_LIGHT) },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { topMargin = dp(4); bottomMargin = dp(3) })
            column.addView(TextView(this).apply {
                text = when {
                    have == null || read == null -> ""
                    next == null -> getString(R.string.bag_all_told)
                    else -> getString(R.string.bag_more, QuestRules.NEEDED - have)
                }
                textSize = 13f; setTextColor(INK); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            })
            if (next != null || read == null) {
                val dots = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_HORIZONTAL }
                repeat(QuestRules.NEEDED) { k ->
                    dots.addView(View(this).apply {
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(if (have != null && k < have) ShopActivity.OWNED else Color.TRANSPARENT)
                            setStroke(dp(2), ShopActivity.HONEY)
                        }
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }, LinearLayout.LayoutParams(dp(12), dp(12)).apply { if (k > 0) marginStart = dp(8) })
                }
                column.addView(dots, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
            }
        }
        card.addView(column, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.6f))
        return card
    }

    private fun chip(text: String, fill: Int) = TextView(this).apply {
        this.text = text; textSize = 12f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(10), dp(2), dp(10), dp(2))
        background = GradientDrawable().apply { setColor(fill); cornerRadius = dp(12).toFloat() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    companion object {
        private val GREEN = ShopActivity.GREEN
        private val INK = Color.parseColor("#3E3A2E")
        private val INK_SOFT = Color.parseColor("#5E5A4C")
        private val CHIP = Color.parseColor("#EFE6CF")
        private val ERROR = Color.parseColor("#B04A3A")
        private val COIN = Color.rgb(246, 200, 80)
        fun intent(context: Context) = Intent(context, BagActivity::class.java)
    }
}

/** The item standing on a small tuft of grass with a daisy, as in the approved bag picture. */
@SuppressLint("ViewConstructor")
private class ItemOnGrass(context: Context, private val item: QuestItem) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val w = width.toFloat(); val h = height.toFloat()
        val ground = h * 0.84f
        val size = minOf(w * 0.86f, ground - 4 * d)
        ItemArt.draw(canvas, item, (w - size) / 2f, ground - size + 3 * d, size, withGlow = false)
        // Grass tuft in front of the item's base.
        paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND; paint.strokeWidth = 2 * d
        val half = size * 0.45f
        var x = w / 2f - half; var i = 0
        while (x <= w / 2f + half) {
            paint.color = if (i % 2 == 0) Color.rgb(132, 178, 92) else Color.rgb(168, 202, 110)
            val tall = (4 + (i * 5) % 6) * d
            canvas.drawLine(x, ground + 2 * d, x + (if (i % 3 == 0) -1.5f else 1.5f) * d, ground - tall, paint)
            x += 3.5f * d; i++
        }
        paint.style = Paint.Style.FILL; paint.strokeCap = Paint.Cap.BUTT
        paint.color = Color.rgb(168, 202, 110)
        canvas.drawRoundRect(w / 2f - half, ground, w / 2f + half, ground + 3 * d, 2 * d, 2 * d, paint)
        // A white daisy on each side.
        for (fx in floatArrayOf(w / 2f - half * 0.8f, w / 2f + half * 0.85f)) {
            paint.color = Color.WHITE
            for (k in 0 until 5) {
                val a = Math.toRadians(k * 72.0)
                canvas.drawCircle(fx + (kotlin.math.cos(a) * 2f * d).toFloat(), ground - 3 * d + (kotlin.math.sin(a) * 2f * d).toFloat(), 1.7f * d, paint)
            }
            paint.color = Color.rgb(244, 196, 64); canvas.drawCircle(fx, ground - 3 * d, 1.3f * d, paint)
        }
    }
}

/** The friend's own sprite, small, for "라일에게". */
@SuppressLint("ViewConstructor")
private class FriendFace(context: Context, characterId: String) : View(context) {
    private val sprites = CharacterSprites.load(resources, characterId)
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val box = RectF()

    override fun onDraw(canvas: Canvas) {
        val art = sprites ?: return
        val s = minOf(width, height).toFloat()
        // The upper part of the sprite (head and shoulders) fills the icon.
        val src = android.graphics.Rect(4, 0, CharacterSprites.SIZE - 4, CharacterSprites.SIZE - 8)
        box.set(0f, 0f, s, s)
        canvas.drawBitmap(art[SpriteFrame.IDLE_0], src, box, pixel)
    }
}

/** A tiny open book in front of the story title. */
private class BookIcon(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val d = resources.displayMetrics.density
        paint.style = Paint.Style.FILL; paint.color = Color.rgb(252, 248, 236)
        canvas.drawRect(1 * d, 1 * d, w - 1 * d, h - 1 * d, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.4f * d; paint.color = Color.rgb(80, 70, 52)
        canvas.drawRect(1 * d, 1 * d, w / 2f, h - 1 * d, paint)
        canvas.drawRect(w / 2f, 1 * d, w - 1 * d, h - 1 * d, paint)
        paint.strokeWidth = 1f * d
        for (k in 1..2) {
            val y = h * k / 3.2f + 1 * d
            canvas.drawLine(3 * d, y, w / 2f - 2 * d, y, paint); canvas.drawLine(w / 2f + 2 * d, y, w - 3 * d, y, paint)
        }
    }
}

/** A sprig of lavender before the guide line. */
private class SprigIcon(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val d = resources.displayMetrics.density
        paint.color = Color.rgb(104, 150, 84); paint.strokeWidth = 1.5f * d
        canvas.drawLine(w * 0.5f, h, w * 0.35f, h * 0.25f, paint)
        canvas.drawLine(w * 0.5f, h, w * 0.75f, h * 0.35f, paint)
        for ((x, y) in listOf(w * 0.35f to h * 0.25f, w * 0.75f to h * 0.35f)) {
            paint.color = ShopActivity.LAVENDER
            for (k in 0..2) canvas.drawCircle(x, y + k * 3 * d, 2.2f * d, paint)
        }
    }
}

/**
 * The bag's backdrop: the shop's cream fabric and honey-wood frame, with a soft meadow along the bottom — rolling
 * hills, bushes and flowers on the left, trees, a fence and a little red-roofed cottage on the right. Drawn once.
 */
private class BagBackground(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = android.graphics.Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val d = resources.displayMetrics.density
        paint.color = ShopActivity.FABRIC; canvas.drawRect(0f, 0f, w, h, paint)
        paint.color = Color.argb(14, 140, 100, 50)
        val weave = 4 * d
        var y = 0f; while (y < h) { canvas.drawRect(0f, y, w, y + d * 0.6f, paint); y += weave }
        var x = 0f; while (x < w) { canvas.drawRect(x, 0f, x + d * 0.6f, h, paint); x += weave }

        // Meadow along the bottom, inside the frame.
        val base = h - 10 * d
        canvas.save()
        canvas.clipRect(8 * d, 0f, w - 8 * d, h - 8 * d)
        hill(canvas, w, base - 28 * d, 16 * d, 0.0011f / d, Color.rgb(214, 226, 196))
        hill(canvas, w, base - 22 * d, 16 * d, 0.0017f / d, Color.rgb(188, 212, 160))
        hill(canvas, w, base - 12 * d, 10 * d, 0.0023f / d, Color.rgb(160, 196, 128))
        // Bushes on the left.
        for ((cx, r) in listOf(30f to 15f, 54f to 11f, 74f to 8f)) {
            paint.color = Color.rgb(118, 160, 96); canvas.drawCircle(cx * d, base - r * 0.6f * d, r * d, paint)
            paint.color = Color.rgb(140, 182, 112); canvas.drawCircle(cx * d - r * 0.3f * d, base - r * 0.85f * d, r * 0.6f * d, paint)
        }
        // Flowers scattered along the meadow.
        var fx = 40 * d; var k = 0
        while (fx < w * 0.7f) {
            paint.color = if (k % 3 == 0) Color.WHITE else ShopActivity.LAVENDER
            canvas.drawCircle(fx, base - (4 + (k * 7) % 9) * d, 2.2f * d, paint)
            fx += (26 + (k * 13) % 30) * d; k++
        }
        // Trees, a fence and the cottage on the right.
        tree(canvas, w - 200 * d, base - 2 * d, d, 0.62f)
        tree(canvas, w - 36 * d, base - 1 * d, d, 0.72f)
        fence(canvas, w - 240 * d, w - 170 * d, base - 1 * d, d)
        cottage(canvas, w - 120 * d, base - 1 * d, d * 0.62f)
        canvas.restore()

        // Honey-wood frame.
        val inset = 5 * d; val r = 18 * d
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7 * d; paint.color = Color.rgb(214, 176, 124)
        canvas.drawRoundRect(inset, inset, w - inset, h - inset, r, r, paint)
        paint.strokeWidth = 1.5f * d; paint.color = ShopActivity.HONEY
        canvas.drawRoundRect(inset - 3.5f * d, inset - 3.5f * d, w - inset + 3.5f * d, h - inset + 3.5f * d, r + 3 * d, r + 3 * d, paint)
        canvas.drawRoundRect(inset + 3.5f * d, inset + 3.5f * d, w - inset - 3.5f * d, h - inset - 3.5f * d, r - 3 * d, r - 3 * d, paint)
        paint.style = Paint.Style.FILL
    }

    private fun hill(canvas: Canvas, w: Float, top: Float, amp: Float, freq: Float, color: Int) {
        path.reset(); path.moveTo(0f, height.toFloat())
        var x = 0f
        while (x <= w) { path.lineTo(x, top + (kotlin.math.sin(x * freq * 6.28f) * amp * 0.5f).toFloat()); x += 6f }
        path.lineTo(w, height.toFloat()); path.close()
        paint.color = color; canvas.drawPath(path, paint)
    }

    private fun tree(canvas: Canvas, cx: Float, base: Float, d: Float, s: Float) {
        paint.color = Color.rgb(132, 94, 60); canvas.drawRect(cx - 3 * d * s, base - 22 * d * s, cx + 3 * d * s, base, paint)
        paint.color = Color.rgb(108, 152, 88); canvas.drawCircle(cx, base - 30 * d * s, 15 * d * s, paint)
        paint.color = Color.rgb(132, 176, 104); canvas.drawCircle(cx - 5 * d * s, base - 34 * d * s, 9 * d * s, paint)
    }

    private fun fence(canvas: Canvas, from: Float, to: Float, base: Float, d: Float) {
        paint.color = Color.rgb(176, 128, 82)
        canvas.drawRect(from, base - 10 * d, to, base - 8 * d, paint)
        canvas.drawRect(from, base - 5 * d, to, base - 3 * d, paint)
        var x = from; while (x <= to) { canvas.drawRect(x, base - 13 * d, x + 3 * d, base, paint); x += 16 * d }
    }

    private fun cottage(canvas: Canvas, left: Float, base: Float, d: Float) {
        val wid = 62 * d; val wall = 26 * d
        paint.color = Color.rgb(244, 230, 204); canvas.drawRect(left, base - wall, left + wid, base, paint)
        // Roof.
        path.reset(); path.moveTo(left - 6 * d, base - wall); path.lineTo(left + wid * 0.3f, base - wall - 24 * d)
        path.lineTo(left + wid + 6 * d, base - wall - 24 * d); path.lineTo(left + wid + 10 * d, base - wall); path.close()
        paint.color = Color.rgb(184, 86, 64); canvas.drawPath(path, paint)
        paint.color = Color.rgb(150, 66, 50); canvas.drawRect(left - 6 * d, base - wall - 2 * d, left + wid + 10 * d, base - wall, paint)
        // Door and a lit window.
        paint.color = Color.rgb(132, 90, 56); canvas.drawRoundRect(left + 10 * d, base - 16 * d, left + 20 * d, base, 2 * d, 2 * d, paint)
        paint.color = Color.rgb(250, 214, 120); canvas.drawRect(left + 34 * d, base - 18 * d, left + 46 * d, base - 9 * d, paint)
        paint.color = Color.rgb(132, 90, 56); canvas.drawRect(left + 39.5f * d, base - 18 * d, left + 40.5f * d, base - 9 * d, paint)
    }
}
