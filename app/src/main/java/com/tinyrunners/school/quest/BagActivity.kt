package com.tinyrunners.school.quest

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.shop.ShopStore
import com.tinyrunners.school.story.Story
import com.tinyrunners.school.story.StoryListActivity
import com.tinyrunners.school.story.StoryRepository
import com.tinyrunners.school.talk.ItemIconView
import com.tinyrunners.school.talk.TalkActivity
import com.tinyrunners.school.ui.Page
import kotlin.math.roundToInt

/**
 * 가방: what the account has gathered. Coins (from distance run, spent in the shop) and each friend's items picked
 * up on the road. A friend whose next village story can take the items gets a "전하러 가기" button.
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
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(30), dp(22), dp(16)) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(com.tinyrunners.school.ui.HomeButton.create(this))
        top.addView(TextView(this).apply {
            setText(R.string.bag_title); textSize = 24f; setTextColor(CREAM); setTypeface(typeface, Typeface.BOLD)
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            setPadding(dp(12), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        coins = TextView(this).apply {
            setText(R.string.shop_loading)
            textSize = 18f; setTextColor(COIN_INK); setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(14), dp(8), dp(16), dp(8))
            background = GradientDrawable().apply { setColor(COIN_BG); cornerRadius = dp(20).toFloat(); setStroke(dp(2), COIN) }
            compoundDrawablePadding = dp(8)
            setCompoundDrawablesRelative(GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(COIN); setStroke(dp(2), COIN_INK); setBounds(0, 0, dp(18), dp(18))
            }, null, null, null)
            // Coins are spent in the shop.
            setOnClickListener { startActivity(com.tinyrunners.school.shop.ShopActivity.intent(this@BagActivity, null)) }
        }
        top.addView(coins)
        content.addView(top)
        message = TextView(this).apply { setText(R.string.bag_body); textSize = 15f; setTextColor(CREAM_SOFT); setPadding(0, dp(6), 0, 0) }
        content.addView(message)
        grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(grid, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(8) })

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(LEATHER)
            // Inside the pouch: leather with a drawstring at the top and stitching around the edge.
            addView(PouchBackground(this@BagActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
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
        com.tinyrunners.school.audio.Music.playGame(this)
        render()
        // Everything is re-read when the screen comes back: runs, purchases and hand-overs change it.
        ShopStore.forCurrentUser(this).load { result ->
            if (isDestroyed) return@load
            result.onSuccess { coins.text = getString(R.string.shop_coins, it.coins) }.onFailure { coins.text = "—" }
        }
        ItemBagStore.forCurrentUser(this).load { result ->
            if (isDestroyed) return@load
            result.onSuccess { bag = it; render() }.onFailure { message.setTextColor(Color.rgb(255, 200, 190)); message.setText(R.string.quest_error) }
        }
        ProgressStore.forCurrentUser(this).loadAll { result ->
            if (isDestroyed) return@loadAll
            read = result.getOrNull().orEmpty().values.flatMap { it.readStoryIds }.toSet()
            render()
        }
    }

    override fun onPause() {
        com.tinyrunners.school.audio.Music.pauseSoon()
        super.onPause()
    }

    /** Six item cards, three to a row. */
    private fun render() {
        grid.removeAllViews()
        val stories = StoryRepository.load(this)
        QuestItems.all.chunked(3).forEachIndexed { r, rowItems ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowItems.forEachIndexed { i, item ->
                row.addView(card(item, stories), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (i > 0) marginStart = dp(10)
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
        val tint = try { Color.parseColor(friend?.color) } catch (_: Exception) { Page.LEAF }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            // A canvas pocket sewn into the pouch: dashed stitches, in the friend's colour once the items can be given.
            background = GradientDrawable().apply {
                setColor(CANVAS); cornerRadius = dp(16).toFloat()
                setStroke(dp(if (ready) 3 else 2), if (ready) tint else STITCH, dp(7).toFloat(), dp(4).toFloat())
            }
        }
        card.addView(ItemIconView(this, item), LinearLayout.LayoutParams(dp(52), dp(52)))
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        column.addView(TextView(this).apply {
            text = getString(R.string.bag_item_title, getString(item.name), have?.toString() ?: "…")
            textSize = 17f; setTextColor(BROWN_INK); setTypeface(typeface, Typeface.BOLD); maxLines = 1
        })
        column.addView(TextView(this).apply {
            text = getString(R.string.bag_item_owner, friend?.name.orEmpty())
            // With the give button showing there is no room for this line, and the button says it anyway.
            visibility = if (ready) View.GONE else View.VISIBLE
            textSize = 13f; setTextColor(BROWN_SOFT); maxLines = 1
        })
        column.addView(TextView(this).apply {
            text = when {
                have == null || read == null -> ""
                next == null -> getString(R.string.bag_all_told)
                ready -> getString(R.string.bag_ready, next.title)
                else -> getString(R.string.bag_need, QuestRules.NEEDED - have)
            }
            textSize = 13f; setTextColor(if (ready) Page.LEAF else BROWN_SOFT); maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            if (ready) setTypeface(typeface, Typeface.BOLD)
        })
        if (ready && next != null) column.addView(Button(this).apply {
            setText(R.string.bag_give)
            textSize = 14f; isAllCaps = false; stateListAnimator = null
            minHeight = dp(36); minimumHeight = dp(36); setPadding(dp(12), 0, dp(12), 0)
            setTextColor(Color.WHITE)
            background = RippleDrawable(ColorStateList.valueOf(Page.MINT), GradientDrawable().apply { setColor(Page.LEAF); cornerRadius = dp(14).toFloat() }, null)
            setOnClickListener {
                // The village list (on that friend) stays underneath, so the story ends there.
                startActivities(arrayOf(StoryListActivity.intent(this@BagActivity, item.friendId),
                    TalkActivity.village(this@BagActivity, item.friendId, next.id, askItems = true)))
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(36)).apply { topMargin = dp(4) })
        card.addView(column, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        return card
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    companion object {
        private val COIN = Color.rgb(246, 200, 80)
        private val COIN_BG = Color.rgb(255, 247, 220)
        private val COIN_INK = Color.rgb(140, 96, 20)
        private val LEATHER = Color.rgb(170, 112, 64)
        private val CANVAS = Color.rgb(246, 232, 204)
        private val STITCH = Color.rgb(140, 92, 50)
        private val CREAM = Color.rgb(255, 246, 226)
        private val CREAM_SOFT = Color.rgb(246, 226, 196)
        private val BROWN_INK = Color.rgb(78, 48, 26)
        private val BROWN_SOFT = Color.rgb(128, 90, 58)
        fun intent(context: Context) = Intent(context, BagActivity::class.java)
    }
}

/**
 * The inside of a leather pouch: a woven texture, the gathered neck with a drawstring and bow along the top, and
 * a row of stitches around the edge.
 */
private class PouchBackground(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = android.graphics.Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val d = resources.displayMetrics.density
        // Leather with a soft lighter middle.
        paint.shader = android.graphics.RadialGradient(w / 2f, h * 0.55f, maxOf(w, h) * 0.7f,
            Color.rgb(196, 138, 86), Color.rgb(150, 96, 52), android.graphics.Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        // Grain: faint crossing threads.
        paint.color = Color.argb(22, 60, 30, 10); paint.strokeWidth = d
        var x = 0f; while (x < w) { canvas.drawLine(x, 0f, x, h, paint); x += 6 * d }
        var y = 0f; while (y < h) { canvas.drawLine(0f, y, w, y, paint); y += 6 * d }
        // Gathered neck: a darker band with folds.
        val neck = 22 * d
        paint.color = Color.rgb(120, 74, 40); canvas.drawRect(0f, 0f, w, neck, paint)
        paint.color = Color.argb(70, 40, 20, 8); paint.strokeWidth = 2 * d
        x = 10 * d; while (x < w) { canvas.drawLine(x, 0f, x + 4 * d, neck, paint); x += 18 * d }
        // Drawstring: a red cord along the neck, tied in a bow in the middle with two loose ends.
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 4 * d; paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(176, 64, 54)
        path.reset(); path.moveTo(0f, neck * 0.7f)
        var px = 0f
        while (px < w) { path.quadTo(px + 20 * d, neck * 0.35f, px + 40 * d, neck * 0.7f); px += 40 * d }
        canvas.drawPath(path, paint)
        val cx = w / 2f
        paint.strokeWidth = 5 * d
        canvas.drawOval(cx - 26 * d, neck * 0.2f, cx - 2 * d, neck * 1.1f, paint)
        canvas.drawOval(cx + 2 * d, neck * 0.2f, cx + 26 * d, neck * 1.1f, paint)
        canvas.drawLine(cx - 2 * d, neck, cx - 12 * d, neck + 22 * d, paint)
        canvas.drawLine(cx + 2 * d, neck, cx + 14 * d, neck + 20 * d, paint)
        paint.style = Paint.Style.FILL; paint.color = Color.rgb(150, 50, 44)
        canvas.drawCircle(cx, neck * 0.7f, 5 * d, paint)
        // Stitches around the edge, below the neck.
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2 * d; paint.strokeCap = Paint.Cap.BUTT
        paint.color = Color.argb(200, 250, 232, 196)
        paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(8 * d, 6 * d), 0f)
        canvas.drawRoundRect(8 * d, neck + 4 * d, w - 8 * d, h - 8 * d, 18 * d, 18 * d, paint)
        paint.pathEffect = null; paint.style = Paint.Style.FILL
    }
}
