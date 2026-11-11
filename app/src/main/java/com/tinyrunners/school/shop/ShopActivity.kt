package com.tinyrunners.school.shop

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
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.ui.Page
import kotlin.math.roundToInt

/**
 * Decoration shop. Coins come from distance run (ShopRules.METERS_PER_COIN metres = 1 coin); items only change the trail drawn behind the
 * friend while running. Cards sit in one row that scrolls sideways, so the whole shop fits a landscape screen.
 */
class ShopActivity : Activity() {
    private lateinit var store: ShopStore
    private lateinit var coins: TextView
    private lateinit var message: TextView
    private lateinit var row: LinearLayout
    private var wallet: Wallet? = null
    private var busy = false
    /** Which shelf is shown: trails behind the runner, or music for runs. */
    private var shelf = ItemKind.TRAIL
    /** The song being previewed with "들어보기", if any. */
    private var previewing: String? = null
    private lateinit var tabs: LinearLayout
    private lateinit var friendId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        friendId = intent.getStringExtra(EXTRA_FRIEND) ?: "mint"
        store = ShopStore.forCurrentUser(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(12))
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(com.tinyrunners.school.ui.HomeButton.create(this))
        top.addView(TextView(this).apply {
            setText(R.string.shop_title)
            textSize = 24f; setTextColor(Page.INK); setTypeface(typeface, Typeface.BOLD)
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
        }
        top.addView(coins)
        top.addView(com.tinyrunners.school.ui.BagButton.create(this))
        content.addView(top)
        content.addView(TextView(this).apply {
            text = getString(R.string.shop_body, ShopRules.METERS_PER_COIN); textSize = 15f; setTextColor(Page.MUTED); setPadding(0, dp(6), 0, 0)
        })
        tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        if (intent.getBooleanExtra(EXTRA_MUSIC, false)) shelf = ItemKind.MUSIC
        content.addView(tabs, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        message = TextView(this).apply { textSize = 15f; setTextColor(Page.LEAF); setTypeface(typeface, Typeface.BOLD) }
        content.addView(message, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })

        row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), dp(8), dp(4)) }
        val scroller = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(row) }
        content.addView(scroller, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(6) })

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Page.BACKGROUND)
            addView(ShopBackground(this@ShopActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
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
        render()
        store.load { result ->
            if (isDestroyed) return@load
            result.onSuccess { wallet = it; render() }.onFailure { coins.text = "—"; message.setTextColor(Page.ERROR); message.setText(R.string.shop_load_failed) }
        }
    }

    private fun render() {
        val w = wallet
        if (w != null) coins.text = getString(R.string.shop_coins, w.coins)
        tabs.removeAllViews()
        tabs.addView(tab(R.string.shop_tab_trails, ItemKind.TRAIL))
        tabs.addView(tab(R.string.shop_tab_music, ItemKind.MUSIC), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(8) })
        row.removeAllViews()
        for (item in ShopCatalog.of(shelf)) row.addView(card(item, w), LinearLayout.LayoutParams(dp(178), LinearLayout.LayoutParams.MATCH_PARENT).apply { marginEnd = dp(10) })
    }

    override fun onPause() {
        // Leaving the shop ends the preview.
        previewing = null
        super.onPause()
    }

    /** Plays a song from the music shelf (or stops it when it is already playing). */
    private fun preview(item: ShopItem) {
        val id = item.track?.let { resources.getIdentifier(it, "raw", packageName) } ?: 0
        if (id == 0) return
        if (previewing == item.id) {
            // Stop the preview: back to the song chosen for the app.
            com.tinyrunners.school.audio.Music.playGame(this); previewing = null
        } else {
            com.tinyrunners.school.audio.Music.play(this, id, force = true); previewing = item.id
        }
        render()
    }

    private fun card(item: ShopItem, w: Wallet?): View {
        val owned = w?.state?.owns(item.id) == true
        val music = item.kind == ItemKind.MUSIC
        val wearing = if (music) w?.state?.music == item.id else w?.state?.equipped == item.id
        // A song whose file has not been added to the app yet is on the shelf as "준비 중".
        val ready = !music || trackExists(item)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(Page.PAPER); cornerRadius = dp(20).toFloat()
                setStroke(dp(if (wearing) 3 else 1), if (wearing) Page.LEAF else Page.LINE)
            }
        }
        val previewFriend = if (music) item.id.removePrefix("music_").takeIf { it in FRIENDS } ?: friendId else friendId
        card.addView(TrailPreview(this, previewFriend, if (music) TrailStyle.NOTES else item.style).apply {
            if (!ready) alpha = 0.45f
            contentDescription = getString(R.string.shop_preview_description, getString(item.name))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        card.addView(TextView(this).apply { setText(item.name); textSize = 17f; setTextColor(Page.INK); setTypeface(typeface, Typeface.BOLD) })
        card.addView(TextView(this).apply { setText(item.blurb); textSize = 13f; setTextColor(Page.MUTED); maxLines = 2 })
        card.addView(TextView(this).apply {
            text = if (item.price == 0) getString(R.string.shop_free) else if (!ready) getString(R.string.shop_coming_soon) else getString(R.string.shop_price, item.price)
            textSize = 14f; setTextColor(COIN_INK); setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(4), 0, 0)
        })
        if (music && ready) card.addView(Button(this).apply {
            // Hear the song before buying or wearing it.
            setText(if (previewing == item.id) R.string.shop_preview_stop else R.string.shop_preview_play)
            textSize = 14f; isAllCaps = false; stateListAnimator = null
            minHeight = dp(36); minimumHeight = dp(36); setPadding(0, 0, 0, 0)
            setTextColor(Page.LEAF)
            background = RippleDrawable(ColorStateList.valueOf(Page.MINT), GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(14).toFloat(); setStroke(dp(2), Page.LEAF)
            }, null)
            setOnClickListener { preview(item) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
        val label = when { wearing -> R.string.shop_wearing; owned -> R.string.shop_wear; !ready -> R.string.shop_coming_soon; else -> R.string.shop_buy }
        card.addView(Button(this).apply {
            setText(label)
            textSize = 15f; isAllCaps = false; stateListAnimator = null
            minHeight = dp(44); minimumHeight = dp(44); setPadding(0, 0, 0, 0)
            val primary = !owned
            setTextColor(if (primary) Color.WHITE else Page.LEAF)
            background = RippleDrawable(ColorStateList.valueOf(Page.MINT), GradientDrawable().apply {
                setColor(if (primary) Page.LEAF else Page.MINT); cornerRadius = dp(14).toFloat()
            }, null)
            isEnabled = w != null && !wearing && (owned || ready)
            alpha = if (isEnabled) 1f else 0.6f
            setOnClickListener { if (owned) wear(item) else buy(item) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
        return card
    }

    private fun buy(item: ShopItem) {
        if (busy) return
        busy = true
        store.buy(item.id) { result ->
            busy = false
            if (isDestroyed) return@buy
            result.onSuccess { (outcome, w) ->
                wallet = w
                message.setTextColor(Page.LEAF)
                message.text = when (outcome) {
                    is ShopRules.Purchase.Bought -> getString(if (item.kind == ItemKind.MUSIC) R.string.shop_bought_music else R.string.shop_bought, getString(item.name))
                    is ShopRules.Purchase.NotEnough -> { message.setTextColor(Page.ERROR); getString(R.string.shop_not_enough, outcome.missing) }
                    else -> ""
                }
                render()
            }.onFailure { message.setTextColor(Page.ERROR); message.setText(R.string.shop_failed) }
        }
    }

    private fun wear(item: ShopItem) {
        if (busy) return
        busy = true
        store.equip(item.id) { result ->
            busy = false
            if (isDestroyed) return@equip
            result.onSuccess { wallet = it; message.text = ""; render() }
                .onFailure { message.setTextColor(Page.ERROR); message.setText(R.string.shop_failed) }
        }
    }

    /** Shelf switch: the chosen shelf is filled green. */
    private fun tab(label: Int, kind: ItemKind) = pill(getString(label), if (shelf == kind) Page.LEAF else Page.PAPER,
        if (shelf == kind) Color.WHITE else Page.LEAF) { if (shelf != kind) { shelf = kind; message.text = ""; render() } }

    @android.annotation.SuppressLint("DiscouragedApi")
    private fun trackExists(item: ShopItem) =
        item.track != null && resources.getIdentifier(item.track, "raw", packageName) != 0

    private fun pill(text: String, fill: Int, ink: Int, click: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 14f; isAllCaps = false; stateListAnimator = null
        setTextColor(ink)
        minHeight = dp(40); minimumHeight = dp(40)
        setPadding(dp(12), 0, dp(12), 0)
        background = RippleDrawable(ColorStateList.valueOf(Page.MINT), GradientDrawable().apply {
            setColor(fill); cornerRadius = dp(16).toFloat(); setStroke(dp(1), Page.LINE)
        }, null)
        setOnClickListener { click() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    companion object {
        const val EXTRA_FRIEND = "friendId"
        const val EXTRA_MUSIC = "openMusic"
        private val FRIENDS = setOf("mint", "cheese", "popo", "lumi", "nunu", "piko")
        private val COIN = Color.rgb(246, 200, 80)
        private val COIN_BG = Color.rgb(255, 247, 220)
        private val COIN_INK = Color.rgb(140, 96, 20)

        fun intent(context: Context, friendId: String?) = Intent(context, ShopActivity::class.java).putExtra(EXTRA_FRIEND, friendId)
    }
}

/** A friend running in place with the item's trail behind them, so players see the decoration before buying. */
@SuppressLint("ViewConstructor")
private class TrailPreview(context: Context, friendId: String, private val style: TrailStyle) : View(context) {
    private val sprites = CharacterSprites.load(resources, friendId)
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val ground = Paint().apply { color = Color.rgb(221, 232, 214) }
    private val box = RectF()
    private val start = SystemClock.uptimeMillis()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRoundRect(0f, h * 0.55f, w, h, 12f, 12f, ground)
        val art = sprites ?: return
        val px = (h * 0.8f / CharacterSprites.SIZE).toInt().coerceAtLeast(1).toFloat()
        val size = CharacterSprites.SIZE * px
        val left = w * 0.62f - size / 2f
        val top = h - size - px
        val t = (SystemClock.uptimeMillis() - start) / 1000f
        TrailPainter.draw(canvas, style, left + px * 10f, top + size * 0.55f, px, t * 1.5f)
        val frames = arrayOf(SpriteFrame.RUN_0, SpriteFrame.RUN_1, SpriteFrame.RUN_2, SpriteFrame.RUN_3)
        box.set(left, top, left + size, top + size)
        canvas.drawBitmap(art[frames[((t * 8).toInt()) % 4]], null, box, pixel)
        postInvalidateDelayed(66)
    }
}

/**
 * Inside a little village shop: a thin striped awning along the top, warm wooden plank walls with shelves of jars,
 * boxes and plants, and a wooden counter along the bottom the item cards stand on.
 */
private class ShopBackground(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val d = resources.displayMetrics.density
        // Wall: vertical planks in two warm tones with dark seams and a few nail dots.
        val plank = 38 * d
        var x = 0f; var i = 0
        while (x < w) {
            paint.color = if (i % 2 == 0) Color.rgb(236, 214, 178) else Color.rgb(228, 204, 166)
            canvas.drawRect(x, 0f, x + plank, h, paint)
            paint.color = Color.rgb(196, 160, 116); canvas.drawRect(x + plank - 1.5f * d, 0f, x + plank, h, paint)
            paint.color = Color.rgb(170, 132, 90)
            canvas.drawCircle(x + plank / 2f, h * 0.18f, 1.6f * d, paint); canvas.drawCircle(x + plank / 2f, h * 0.62f, 1.6f * d, paint)
            x += plank; i++
        }
        // Two long shelves with jars, boxes and little pots.
        shelf(canvas, w, h * 0.36f, d, seed = 1)
        shelf(canvas, w, h * 0.6f, d, seed = 7)
        // Awning: red and cream stripes with a scalloped edge.
        // Kept thin so the title and buttons below it stay easy to read.
        val awning = 7 * d; val stripe = 22 * d
        x = 0f; i = 0
        while (x < w) {
            paint.color = if (i % 2 == 0) Color.rgb(214, 92, 76) else Color.rgb(255, 246, 228)
            canvas.drawRect(x, 0f, x + stripe, awning, paint)
            canvas.drawCircle(x + stripe / 2f, awning, stripe * 0.32f, paint)
            x += stripe; i++
        }
        // Counter along the bottom: a top board and the front panels.
        val top = h * 0.84f
        paint.color = Color.rgb(150, 98, 58); canvas.drawRect(0f, top, w, h, paint)
        paint.color = Color.rgb(176, 120, 72); canvas.drawRect(0f, top, w, top + 10 * d, paint)
        paint.color = Color.rgb(126, 80, 46)
        x = 0f; while (x < w) { canvas.drawRect(x, top + 16 * d, x + 2 * d, h, paint); x += 90 * d }
    }

    private fun shelf(canvas: Canvas, w: Float, y: Float, d: Float, seed: Int) {
        // Things sitting on the board, in a repeating but uneven order.
        var x = 14 * d; var n = seed
        while (x < w - 30 * d) {
            when (n % 4) {
                0 -> { // glass jar with a coloured inside and a lid
                    paint.color = JARS[n % JARS.size]; rect.set(x, y - 26 * d, x + 20 * d, y); canvas.drawRoundRect(rect, 5 * d, 5 * d, paint)
                    paint.color = Color.argb(110, 255, 255, 255); rect.set(x + 3 * d, y - 22 * d, x + 7 * d, y - 6 * d); canvas.drawRoundRect(rect, 2 * d, 2 * d, paint)
                    paint.color = Color.rgb(120, 84, 50); rect.set(x - 1 * d, y - 30 * d, x + 21 * d, y - 25 * d); canvas.drawRoundRect(rect, 2 * d, 2 * d, paint)
                    x += 30 * d
                }
                1 -> { // small wooden box
                    paint.color = Color.rgb(184, 132, 82); canvas.drawRect(x, y - 18 * d, x + 28 * d, y, paint)
                    paint.color = Color.rgb(150, 104, 62); canvas.drawRect(x, y - 18 * d, x + 28 * d, y - 15 * d, paint)
                    x += 38 * d
                }
                2 -> { // potted sprout
                    paint.color = Color.rgb(196, 112, 76); rect.set(x, y - 14 * d, x + 18 * d, y); canvas.drawRoundRect(rect, 3 * d, 3 * d, paint)
                    paint.color = Color.rgb(104, 160, 88)
                    canvas.drawOval(x + 1 * d, y - 28 * d, x + 10 * d, y - 14 * d, paint); canvas.drawOval(x + 8 * d, y - 30 * d, x + 17 * d, y - 15 * d, paint)
                    x += 28 * d
                }
                else -> { // stack of little books
                    for (b in 0 until 3) { paint.color = BOOKS[(n + b) % BOOKS.size]; canvas.drawRect(x, y - (b + 1) * 7 * d, x + 26 * d - b * 3 * d, y - b * 7 * d, paint) }
                    x += 36 * d
                }
            }
            n += 3
        }
        // The board itself and its shadow.
        paint.color = Color.rgb(160, 110, 66); canvas.drawRect(0f, y, w, y + 7 * d, paint)
        paint.color = Color.argb(50, 90, 50, 20); canvas.drawRect(0f, y + 7 * d, w, y + 11 * d, paint)
    }

    private companion object {
        val JARS = intArrayOf(Color.rgb(240, 190, 80), Color.rgb(150, 200, 120), Color.rgb(230, 140, 150), Color.rgb(140, 180, 220), Color.rgb(200, 160, 220))
        val BOOKS = intArrayOf(Color.rgb(120, 150, 200), Color.rgb(214, 120, 100), Color.rgb(110, 170, 120), Color.rgb(230, 190, 90))
    }
}
