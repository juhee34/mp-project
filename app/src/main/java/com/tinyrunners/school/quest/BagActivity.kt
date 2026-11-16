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
import com.tinyrunners.school.story.StoryRepository
import kotlin.math.roundToInt

/**
 * 가방, in A's approved design (docs/art/approved/bag-approved-20261112.png): the shop's cream fabric and honey-wood
 * frame, a coin badge, and six item cards in two rows — the item standing on a tuft of grass, then its name and count,
 * the friend it is for, and either how many more are needed (three dots) or "전할 준비 완료" (the next story's title only
 * in the description read aloud)
 * and a "전하러 가기" button. The backdrop is A's final frame picture, the same as the shop's and settings' (the bag opens from the shop). All numbers come from the account.
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

        // Top: 돌아가기 | 가방 (with its subtitle) ............ coins
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        // Back to whatever screen opened the bag (the shop, a run result, the village...), not straight to the menu.
        top.addView(com.tinyrunners.school.ui.HomeButton.create(this, getString(R.string.common_back_pill)) { finish() })
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
        // 엽서첩 lives in the bag (guide ⑤, B's default until A and B decide otherwise).
        top.addView(TextView(this).apply {
            setText(R.string.postcard_open); textSize = 18f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(18), dp(6), dp(18), dp(6))
            background = RippleDrawable(ColorStateList.valueOf(ShopActivity.OWNED), GradientDrawable().apply {
                setColor(ShopActivity.CARD); cornerRadius = dp(22).toFloat(); setStroke(dp(2), ShopActivity.HONEY)
            }, null)
            setOnClickListener { startActivity(com.tinyrunners.school.postcard.PostcardActivity.intent(this@BagActivity)) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(12) })
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
            addView(com.tinyrunners.school.shop.ShopBackground(this@BagActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
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
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(4), dp(12), dp(4))
            background = GradientDrawable().apply {
                setColor(ShopActivity.CARD); cornerRadius = dp(16).toFloat(); setStroke(dp(2), ShopActivity.HONEY_LIGHT)
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
        column.addView(forRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        // A thin rule and a line about the items (they are gifts picked up on the friend's road).
        column.addView(View(this).apply { setBackgroundColor(ShopActivity.HONEY_LIGHT) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { topMargin = dp(4); bottomMargin = dp(3) })
        column.addView(TextView(this).apply {
            text = if (have == null) "" else getString(R.string.bag_collected)
            textSize = 13f; setTextColor(INK); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        })
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
