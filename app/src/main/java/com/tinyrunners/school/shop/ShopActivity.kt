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
import com.tinyrunners.school.ui.SceneryView
import kotlin.math.roundToInt

/**
 * Decoration shop. Coins come from distance run (10 m = 1 coin); items only change the trail drawn behind the
 * friend while running. Cards sit in one row that scrolls sideways, so the whole shop fits a landscape screen.
 */
class ShopActivity : Activity() {
    private lateinit var store: ShopStore
    private lateinit var coins: TextView
    private lateinit var message: TextView
    private lateinit var row: LinearLayout
    private var wallet: Wallet? = null
    private var busy = false
    private lateinit var friendId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        friendId = intent.getStringExtra(EXTRA_FRIEND) ?: "mint"
        store = ShopStore.forCurrentUser(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(12))
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(pill(getString(R.string.shop_back), Page.PAPER, Page.LEAF) { finish() })
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
        content.addView(top)
        content.addView(TextView(this).apply {
            setText(R.string.shop_body); textSize = 15f; setTextColor(Page.MUTED); setPadding(0, dp(6), 0, 0)
        })
        message = TextView(this).apply { textSize = 15f; setTextColor(Page.LEAF); setTypeface(typeface, Typeface.BOLD) }
        content.addView(message, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })

        row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), dp(8), dp(4)) }
        val scroller = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(row) }
        content.addView(scroller, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(6) })

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Page.BACKGROUND)
            addView(SceneryView(this@ShopActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
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
        row.removeAllViews()
        for (item in ShopCatalog.items) row.addView(card(item, w), LinearLayout.LayoutParams(dp(178), LinearLayout.LayoutParams.MATCH_PARENT).apply { marginEnd = dp(10) })
    }

    private fun card(item: ShopItem, w: Wallet?): View {
        val owned = w?.state?.owns(item.id) == true
        val wearing = w?.state?.equipped == item.id
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(Page.PAPER); cornerRadius = dp(20).toFloat()
                setStroke(dp(if (wearing) 3 else 1), if (wearing) Page.LEAF else Page.LINE)
            }
        }
        card.addView(TrailPreview(this, friendId, item.style).apply {
            contentDescription = getString(R.string.shop_preview_description, getString(item.name))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        card.addView(TextView(this).apply { setText(item.name); textSize = 17f; setTextColor(Page.INK); setTypeface(typeface, Typeface.BOLD) })
        card.addView(TextView(this).apply { setText(item.blurb); textSize = 13f; setTextColor(Page.MUTED); maxLines = 2 })
        card.addView(TextView(this).apply {
            text = if (item.price == 0) getString(R.string.shop_free) else getString(R.string.shop_price, item.price)
            textSize = 14f; setTextColor(COIN_INK); setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(4), 0, 0)
        })
        val label = when { wearing -> R.string.shop_wearing; owned -> R.string.shop_wear; else -> R.string.shop_buy }
        card.addView(Button(this).apply {
            setText(label)
            textSize = 15f; isAllCaps = false; stateListAnimator = null
            minHeight = dp(44); minimumHeight = dp(44); setPadding(0, 0, 0, 0)
            val primary = !owned
            setTextColor(if (primary) Color.WHITE else Page.LEAF)
            background = RippleDrawable(ColorStateList.valueOf(Page.MINT), GradientDrawable().apply {
                setColor(if (primary) Page.LEAF else Page.MINT); cornerRadius = dp(14).toFloat()
            }, null)
            isEnabled = w != null && !wearing
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
                    is ShopRules.Purchase.Bought -> getString(R.string.shop_bought, getString(item.name))
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
