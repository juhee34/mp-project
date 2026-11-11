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
    /** Which shelf is shown. */
    private enum class Shelf { TRAIL, MUSIC, ITEMS, COINS }
    private var shelf = Shelf.TRAIL
    /** Friends' items in the bag, for the 물품 shelf. */
    private var bag: com.tinyrunners.school.quest.ItemBag? = null
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
            // Clear of the curtains on both sides.
            setPadding(dp(52), dp(20), dp(52), dp(12))
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(com.tinyrunners.school.ui.HomeButton.create(this))
        top.addView(TextView(this).apply {
            setText(R.string.shop_title)
            textSize = 24f; setTextColor(TITLE); setTypeface(typeface, Typeface.BOLD)
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
            text = getString(R.string.shop_body, ShopRules.METERS_PER_COIN); textSize = 15f; setTextColor(SUBTITLE); setPadding(0, dp(6), 0, 0)
        })
        tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        if (intent.getBooleanExtra(EXTRA_MUSIC, false)) shelf = Shelf.MUSIC
        content.addView(tabs, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        message = TextView(this).apply { textSize = 15f; setTextColor(MESSAGE); setTypeface(typeface, Typeface.BOLD) }
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
        loadBag()
        store.load { result ->
            if (isDestroyed) return@load
            result.onSuccess { wallet = it; render() }.onFailure { coins.text = "—"; message.setTextColor(MESSAGE_ERROR); message.setText(R.string.shop_load_failed) }
        }
    }

    private fun render() {
        val w = wallet
        if (w != null) coins.text = getString(R.string.shop_coins, w.coins)
        tabs.removeAllViews()
        listOf(Shelf.TRAIL to R.string.shop_tab_trails, Shelf.MUSIC to R.string.shop_tab_music,
            Shelf.ITEMS to R.string.shop_tab_items, Shelf.COINS to R.string.shop_tab_coins).forEachIndexed { i, (s, label) ->
            tabs.addView(tab(label, s), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { if (i > 0) marginStart = dp(8) })
        }
        row.removeAllViews()
        val params = { LinearLayout.LayoutParams(dp(178), LinearLayout.LayoutParams.MATCH_PARENT).apply { marginEnd = dp(10) } }
        when (shelf) {
            Shelf.TRAIL -> ShopCatalog.of(ItemKind.TRAIL).forEach { row.addView(card(it, w), params()) }
            Shelf.MUSIC -> ShopCatalog.of(ItemKind.MUSIC).forEach { row.addView(card(it, w), params()) }
            Shelf.ITEMS -> com.tinyrunners.school.quest.QuestItems.all.forEach { row.addView(itemCard(it, w), params()) }
            Shelf.COINS -> ShopRules.COIN_PACKS.forEach { row.addView(coinCard(it), params()) }
        }
    }

    private fun loadBag() {
        com.tinyrunners.school.quest.ItemBagStore.forCurrentUser(this).load { result ->
            if (isDestroyed) return@load
            result.onSuccess { bag = it; if (shelf == Shelf.ITEMS) render() }
        }
    }

    /** One of a friend's items for coins; it goes straight to the bag. */
    private fun itemCard(item: com.tinyrunners.school.quest.QuestItem, w: Wallet?): View {
        val friend = com.tinyrunners.school.data.CharacterRepository(this).find(item.friendId)
        val card = shelfCard()
        card.addView(com.tinyrunners.school.talk.ItemIconView(this, item), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        card.addView(TextView(this).apply { setText(item.name); textSize = 17f; setTextColor(Page.INK); setTypeface(typeface, Typeface.BOLD) })
        card.addView(TextView(this).apply {
            text = getString(R.string.shop_item_owner, friend?.name.orEmpty(), bag?.count(item.friendId)?.toString() ?: "…")
            textSize = 13f; setTextColor(Page.MUTED); maxLines = 2
        })
        card.addView(priceText(getString(R.string.shop_price, ShopRules.ITEM_PRICE)))
        card.addView(actionButton(getString(R.string.shop_buy), primary = true, enabled = w != null) { buyItem(item) }, actionParams())
        return card
    }

    private fun buyItem(item: com.tinyrunners.school.quest.QuestItem) {
        if (busy) return
        busy = true
        store.buyItem(item.friendId) { result ->
            busy = false
            if (isDestroyed) return@buyItem
            result.onSuccess { (ok, w) ->
                wallet = w
                if (ok) { message.setTextColor(MESSAGE); message.text = getString(R.string.shop_item_bought, getString(item.name)) }
                else { message.setTextColor(MESSAGE_ERROR); message.text = getString(R.string.shop_not_enough, ShopRules.ITEM_PRICE - w.coins) }
                loadBag()
                render()
            }.onFailure { message.setTextColor(MESSAGE_ERROR); message.setText(R.string.shop_failed) }
        }
    }

    /** A coin pack. Buying asks first and makes clear that this build charges nothing (demonstration). */
    private fun coinCard(pack: ShopRules.CoinPack): View {
        val card = shelfCard()
        card.addView(CoinPile(this, pack.coins), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        card.addView(TextView(this).apply { text = getString(R.string.shop_pack_name, pack.coins); textSize = 17f; setTextColor(Page.INK); setTypeface(typeface, Typeface.BOLD) })
        card.addView(TextView(this).apply { setText(R.string.shop_pack_demo); textSize = 13f; setTextColor(Page.MUTED); maxLines = 2 })
        card.addView(priceText(getString(R.string.shop_pack_price, String.format(java.util.Locale.KOREA, "%,d", pack.won))))
        card.addView(actionButton(getString(R.string.shop_pack_buy), primary = true, enabled = true) { confirmPack(pack) }, actionParams())
        return card
    }

    private fun confirmPack(pack: ShopRules.CoinPack) {
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.shop_pack_confirm_title, pack.coins))
            .setMessage(getString(R.string.shop_pack_confirm_body, String.format(java.util.Locale.KOREA, "%,d", pack.won), pack.coins))
            .setPositiveButton(R.string.shop_pack_buy) { _, _ -> buyPack(pack) }
            .setNegativeButton(R.string.shop_pack_cancel, null)
            .show()
    }

    private fun buyPack(pack: ShopRules.CoinPack) {
        if (busy) return
        busy = true
        store.buyCoins(pack) { result ->
            busy = false
            if (isDestroyed) return@buyCoins
            result.onSuccess { wallet = it; message.setTextColor(MESSAGE); message.text = getString(R.string.shop_pack_done, pack.coins); render() }
                .onFailure { message.setTextColor(MESSAGE_ERROR); message.setText(R.string.shop_failed) }
        }
    }

    private fun shelfCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = GradientDrawable().apply { setColor(Page.PAPER); cornerRadius = dp(20).toFloat(); setStroke(dp(1), Page.LINE) }
    }

    private fun priceText(text: String) = TextView(this).apply {
        this.text = text; textSize = 14f; setTextColor(COIN_INK); setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(4), 0, 0)
    }

    private fun actionParams() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) }

    private fun actionButton(text: String, primary: Boolean, enabled: Boolean, click: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 15f; isAllCaps = false; stateListAnimator = null
        minHeight = dp(44); minimumHeight = dp(44); setPadding(0, 0, 0, 0)
        setTextColor(if (primary) Color.WHITE else Page.LEAF)
        background = RippleDrawable(ColorStateList.valueOf(Page.MINT), GradientDrawable().apply {
            setColor(if (primary) Page.LEAF else Page.MINT); cornerRadius = dp(14).toFloat()
        }, null)
        isEnabled = enabled
        alpha = if (enabled) 1f else 0.6f
        setOnClickListener { click() }
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
                message.setTextColor(MESSAGE)
                message.text = when (outcome) {
                    is ShopRules.Purchase.Bought -> getString(if (item.kind == ItemKind.MUSIC) R.string.shop_bought_music else R.string.shop_bought, getString(item.name))
                    is ShopRules.Purchase.NotEnough -> { message.setTextColor(MESSAGE_ERROR); getString(R.string.shop_not_enough, outcome.missing) }
                    else -> ""
                }
                render()
            }.onFailure { message.setTextColor(MESSAGE_ERROR); message.setText(R.string.shop_failed) }
        }
    }

    private fun wear(item: ShopItem) {
        if (busy) return
        busy = true
        store.equip(item.id) { result ->
            busy = false
            if (isDestroyed) return@equip
            result.onSuccess { wallet = it; message.text = ""; render() }
                .onFailure { message.setTextColor(MESSAGE_ERROR); message.setText(R.string.shop_failed) }
        }
    }

    /** Shelf switch: the chosen shelf is filled. */
    private fun tab(label: Int, kind: Shelf) = pill(getString(label), if (shelf == kind) MAGIC_TAB else Page.PAPER,
        if (shelf == kind) Color.WHITE else MAGIC_TAB) { if (shelf != kind) { shelf = kind; message.text = ""; render() } }

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
        /** Text and the selected tab on the light wooden shop. */
        private val TITLE = Color.rgb(78, 48, 26)
        private val SUBTITLE = Color.rgb(112, 80, 52)
        private val MESSAGE = Page.LEAF
        private val MESSAGE_ERROR = Page.ERROR
        private val MAGIC_TAB = Color.rgb(150, 88, 50)

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
 * A cosy shop with a touch of magic: light wooden plank walls with a few gold sparkles, caramel velvet curtains with
 * gold tassels on both sides and a fringed valance, and a wooden shop counter along the bottom that the item cards
 * stand in front of. Drawn once (no animation).
 */
private class ShopBackground(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = android.graphics.Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val d = resources.displayMetrics.density
        // Wall: light vertical planks in two warm tones with soft seams.
        val plank = 38 * d
        var x = 0f; var i = 0
        while (x < w) {
            paint.color = if (i % 2 == 0) Color.rgb(238, 218, 184) else Color.rgb(230, 208, 172)
            canvas.drawRect(x, 0f, x + plank, h, paint)
            paint.color = Color.rgb(204, 172, 130); canvas.drawRect(x + plank - 1.5f * d, 0f, x + plank, h, paint)
            x += plank; i++
        }
        // A few gold sparkles: the shop's bit of magic.
        for ((fx, fy, size) in listOf(Triple(0.22f, 0.3f, 7f), Triple(0.6f, 0.07f, 8f), Triple(0.84f, 0.42f, 6f), Triple(0.42f, 0.4f, 5f), Triple(0.13f, 0.6f, 6f))) {
            sparkle(canvas, w * fx, h * fy, size * d)
        }
        // Shop counter along the bottom: a thick top board, then the front with carved panels.
        val top = h * 0.82f
        paint.color = Color.rgb(140, 92, 54); canvas.drawRect(0f, top, w, h, paint)
        paint.color = Color.rgb(176, 120, 72); canvas.drawRect(0f, top, w, top + 12 * d, paint)
        paint.color = Color.rgb(200, 146, 92); canvas.drawRect(0f, top, w, top + 3 * d, paint)
        paint.color = Color.argb(60, 60, 30, 10); canvas.drawRect(0f, top + 12 * d, w, top + 16 * d, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2 * d; paint.color = Color.rgb(118, 76, 42)
        val panel = 120 * d
        x = 10 * d
        while (x < w) { canvas.drawRoundRect(x, top + 22 * d, x + panel - 20 * d, h - 8 * d, 6 * d, 6 * d, paint); x += panel }
        paint.style = Paint.Style.FILL
        // Curtains with gold tassels on both sides, and the valance with its fringe.
        curtain(canvas, 0f, w * 0.07f, h, d, left = true)
        curtain(canvas, w, w * 0.07f, h, d, left = false)
        paint.color = CURTAIN_DARK; canvas.drawRect(0f, 0f, w, 6 * d, paint)
        paint.color = Color.rgb(232, 186, 90)
        x = 0f; while (x < w) { canvas.drawCircle(x + 5 * d, 7 * d, 2.2f * d, paint); x += 10 * d }
    }

    private fun sparkle(canvas: Canvas, x: Float, y: Float, s: Float) {
        paint.color = Color.argb(200, 236, 186, 80)
        path.reset()
        path.moveTo(x, y - s); path.quadTo(x, y, x + s, y); path.quadTo(x, y, x, y + s); path.quadTo(x, y, x - s, y); path.quadTo(x, y, x, y - s)
        canvas.drawPath(path, paint)
    }

    private fun curtain(canvas: Canvas, edge: Float, width: Float, h: Float, d: Float, left: Boolean) {
        val inner = if (left) edge + width else edge - width
        path.reset()
        path.moveTo(edge, 0f); path.lineTo(inner, 0f)
        path.quadTo(if (left) inner - width * 0.6f else inner + width * 0.6f, h * 0.45f, inner, h * 0.85f)
        path.lineTo(edge, h * 0.85f); path.close()
        paint.color = CURTAIN; canvas.drawPath(path, paint)
        // Folds.
        paint.color = Color.argb(60, 60, 24, 8); paint.strokeWidth = 3 * d; paint.style = Paint.Style.STROKE
        for (k in 1..2) { val fx = if (left) edge + width * k / 3f else edge - width * k / 3f; canvas.drawLine(fx, 0f, fx, h * 0.8f, paint) }
        paint.style = Paint.Style.FILL
        // Gold tie and tassel.
        paint.color = Color.rgb(232, 186, 90)
        val tx = if (left) edge + width * 0.7f else edge - width * 0.7f
        canvas.drawRect(tx - width * 0.35f, h * 0.48f, tx + width * 0.35f, h * 0.48f + 5 * d, paint)
        canvas.drawCircle(tx, h * 0.48f + 10 * d, 5 * d, paint)
        canvas.drawRect(tx - 3 * d, h * 0.48f + 12 * d, tx + 3 * d, h * 0.48f + 26 * d, paint)
    }

    private companion object {
        /** Caramel velvet that goes with the light wood. */
        val CURTAIN = Color.rgb(170, 96, 56)
        val CURTAIN_DARK = Color.rgb(132, 70, 38)
    }
}

/** A small heap of coins for a coin pack card; bigger packs show more coins. */
@SuppressLint("ViewConstructor")
private class CoinPile(context: Context, private val coins: Int) : View(context) {
    override fun onDraw(canvas: Canvas) {
        val n = when { coins >= 1000 -> 6; coins >= 500 -> 4; else -> 2 }
        val r = minOf(width / 8f, height / 3.2f)
        val baseY = height - r * 1.1f
        for (i in 0 until n) {
            val x = width / 2f + (i - (n - 1) / 2f) * r * 1.1f
            val y = baseY - (i % 2) * r * 0.5f
            com.tinyrunners.school.game.CoinArt.draw(canvas, x, y, r, spin = 0f)
        }
    }
}
