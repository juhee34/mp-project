package com.tinyrunners.school.shop

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
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
import com.tinyrunners.school.story.BackgroundThumbs
import kotlin.math.roundToInt

/**
 * The shop, in A's approved design (docs/art/shop/): cream fabric inside a thin honey-wood frame, dark green text and
 * buttons, lavender accents and a fern and flowers in the bottom corners. Four shelves (꾸미기 · 음악 · 아이템 · 코인
 * 충전) share one card style; cards sit in a row that scrolls sideways, four across on a wide screen.
 *
 * Coins come from distance run (ShopRules.METERS_PER_COIN metres = 1 coin) and road coins; decorations only change
 * how the friend looks while running, never the physics. Coin packs are a demonstration: nothing is charged.
 */
class ShopActivity : Activity() {
    private lateinit var store: ShopStore
    private lateinit var coins: TextView
    private lateinit var guide: TextView
    private lateinit var message: TextView
    private lateinit var retry: Button
    private lateinit var row: LinearLayout
    private lateinit var scroller: HorizontalScrollView
    /** Page dots and the ▶ button under the cards (only when a shelf has more cards than fit). */
    private lateinit var prevArrow: Button
    private lateinit var nextArrow: Button
    private var wallet: Wallet? = null
    private var loadFailed = false
    /** The card whose purchase or wear request is being saved; its button shows "처리 중" and nothing else reacts. */
    private var busyId: String? = null
    private val busy get() = busyId != null
    /** Which shelf is shown. */
    private enum class Shelf { TRAIL, PROPS, MUSIC, ITEMS, COINS }
    private var shelf = Shelf.TRAIL
    /** Friends' items in the bag, for the 아이템 shelf. */
    private var bag: com.tinyrunners.school.quest.ItemBag? = null
    /** The song being previewed with "들어보기", if any. */
    private var previewing: String? = null
    private lateinit var tabs: LinearLayout
    private lateinit var friendId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        friendId = intent.getStringExtra(EXTRA_FRIEND) ?: "mint"
        if (intent.getBooleanExtra(EXTRA_MUSIC, false)) shelf = Shelf.MUSIC
        store = ShopStore.forCurrentUser(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Inside the wooden frame, with room at the bottom for the plants.
            setPadding(dp(26), dp(10), dp(26), dp(12))
        }

        // Top: 처음으로 | 상점 | short intro ........ coins  bag
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(com.tinyrunners.school.ui.HomeButton.create(this))
        top.addView(divider())
        top.addView(TextView(this).apply {
            setText(R.string.shop_title)
            // Cream with a soft dark shadow, so it reads on the wooden shop interior (A's separated pack).
            textSize = 24f; setTypeface(typeface, Typeface.BOLD); onPlaque(this)
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        })
        top.addView(divider())
        top.addView(TextView(this).apply {
            setText(R.string.shop_intro); textSize = 15f; maxLines = 1; onPlaque(this)
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { weight = 0f; width = LinearLayout.LayoutParams.WRAP_CONTENT })
        top.addView(LavenderSprig(this), LinearLayout.LayoutParams(dp(26), dp(30)).apply { marginStart = dp(10) })
        top.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        coins = TextView(this).apply {
            setText(R.string.shop_loading)
            textSize = 17f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(12), dp(6), dp(16), dp(6))
            compoundDrawablePadding = dp(8)
            setCompoundDrawablesRelative(GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(COIN); setStroke(dp(2), PRICE); setBounds(0, 0, dp(20), dp(20))
            }, null, null, null)
        }
        com.tinyrunners.school.ui.TopPill.style(coins, pressable = false, sizeSp = 16f)
        coins.setPadding(dp(12), dp(2), dp(16), dp(4))
        top.addView(coins, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(10) })
        top.addView(com.tinyrunners.school.ui.TopPill.divider(this))
        top.addView(com.tinyrunners.school.ui.BagButton.create(this))
        content.addView(top)

        // Guide line between two lavender rules: ── ◇ text ◇ ──
        val guideRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        guideRow.addView(rule(), LinearLayout.LayoutParams(0, dp(2), 1f))
        guide = TextView(this).apply { textSize = 14f; gravity = Gravity.CENTER; onPlaque(this) }
        guideRow.addView(guide)
        guideRow.addView(rule(), LinearLayout.LayoutParams(0, dp(2), 1f))
        content.addView(guideRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })

        // Tabs on the left, the latest message (and a retry after a failed load) on the right.
        val tabRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabRow.addView(tabs)
        message = TextView(this).apply {
            textSize = 14f; setTypeface(typeface, Typeface.BOLD); gravity = Gravity.CENTER_VERTICAL
            maxLines = 2; onPlaque(this); visibility = View.GONE
        }
        tabRow.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        tabRow.addView(message, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(12) })
        retry = outlined(getString(R.string.shop_retry)) { load() }.apply { visibility = View.GONE }
        tabRow.addView(retry, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)).apply { marginStart = dp(8) })
        content.addView(tabRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })

        row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(2), 0, dp(2)) }
        scroller = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; clipToPadding = false; addView(row) }
        // The cards between a round ◀ and ▶ (like the friend selection); each slides the shelf by one card.
        prevArrow = sideArrow("◀", R.string.shop_prev_card) { slide(-1) }
        nextArrow = sideArrow("▶", R.string.shop_next_page) { slide(+1) }
        val shelfRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        shelfRow.addView(prevArrow, LinearLayout.LayoutParams(dp(46), dp(46)).apply { marginEnd = dp(10) })
        shelfRow.addView(scroller, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        shelfRow.addView(nextArrow, LinearLayout.LayoutParams(dp(46), dp(46)).apply { marginStart = dp(10) })
        content.addView(shelfRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(6); bottomMargin = dp(6) })
        scroller.setOnScrollChangeListener { _, _, _, _, _ -> updateArrows() }
        // Cards are sized from the row's width (four across), so lay them out again once that width is known.
        scroller.addOnLayoutChangeListener { _, l, _, r, _, ol, _, or, _ -> if (r - l != or - ol) scroller.post { render() } }

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(FABRIC)
            addView(ShopBackground(this@ShopActivity, interior = true), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
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
        load()
    }

    /** Loads the wallet (and the bag for the 아이템 shelf). Until it arrives, buttons say "불러오는 중". */
    private fun load() {
        loadFailed = false
        retry.visibility = View.GONE
        say("")
        coins.setText(R.string.shop_loading)
        render()
        loadBag()
        store.load { result ->
            if (isDestroyed) return@load
            result.onSuccess { wallet = it; loadFailed = false; render() }
                .onFailure {
                    // Never show a failed load as an empty or owned shelf: say so, and offer to try again.
                    loadFailed = true
                    coins.text = "—"
                    say(getString(R.string.shop_load_failed), error = true)
                    retry.visibility = View.VISIBLE
                    render()
                }
        }
    }

    private fun render() {
        val w = wallet
        if (w != null) coins.text = getString(R.string.shop_coins, w.coins)
        guide.text = guideText()
        tabs.removeAllViews()
        listOf(Shelf.TRAIL to R.string.shop_tab_trails, Shelf.PROPS to R.string.shop_tab_props, Shelf.MUSIC to R.string.shop_tab_music,
            Shelf.ITEMS to R.string.shop_tab_items, Shelf.COINS to R.string.shop_tab_coins).forEachIndexed { i, (s, label) ->
            tabs.addView(tab(label, s), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40)).apply { if (i > 0) marginStart = dp(14) })
        }
        row.removeAllViews()
        // Four cards across on a wide screen; on a narrow one keep a readable minimum width and scroll sideways.
        val gap = dp(14)
        val available = scroller.width
        val cardWidth = if (available > 0) maxOf(dp(176), (available - gap * 3) / 4) else dp(196)
        fun params(last: Boolean) = LinearLayout.LayoutParams(cardWidth, LinearLayout.LayoutParams.MATCH_PARENT).apply { if (!last) marginEnd = gap }
        fun add(views: List<View>) = views.forEachIndexed { i, v -> row.addView(v, params(i == views.lastIndex)) }
        when (shelf) {
            Shelf.TRAIL -> add(ShopCatalog.onShelf(ItemKind.TRAIL, Season.now()) { w?.state?.owns(it) == true }.map { card(it, w) })
            Shelf.MUSIC -> add(ShopCatalog.of(ItemKind.MUSIC).map { card(it, w) })
            Shelf.PROPS -> add(ShopCatalog.of(ItemKind.PROP).map { card(it, w) })
            Shelf.ITEMS -> add(com.tinyrunners.school.quest.QuestItems.all.map { itemCard(it, w) })
            Shelf.COINS -> add(ShopRules.COIN_PACKS.map { coinCard(it) })
        }
        scroller.post { updateArrows() }
    }

    /** One card further (or back). */
    private fun slide(step: Int) {
        val first = row.getChildAt(0) ?: return
        scroller.smoothScrollBy(step * (first.width + dp(14)), 0)
    }

    /** An arrow is pale and does nothing at its end of the shelf. */
    private fun updateArrows() {
        for ((arrow, more) in listOf(prevArrow to scroller.canScrollHorizontally(-1), nextArrow to scroller.canScrollHorizontally(1))) {
            arrow.isEnabled = more
            arrow.alpha = if (more) 1f else 0.35f
        }
    }

    /** A round cream arrow with a green edge, the same as on the friend selection screen. */
    private fun sideArrow(symbol: String, description: Int, click: () -> Unit) = Button(this).apply {
        text = symbol; textSize = 17f; setTextColor(GREEN); stateListAnimator = null; setPadding(0, 0, 0, 0)
        minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
        contentDescription = getString(description)
        background = RippleDrawable(ColorStateList.valueOf(OWNED), GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setColor(Color.argb(240, 255, 249, 235)); setStroke(dp(2), GREEN)
        }, null)
        setOnClickListener { click() }
    }

    /** The lavender guide line for each shelf, between two small diamonds. */
    private fun guideText(): CharSequence {
        val body = when (shelf) {
            Shelf.TRAIL -> getString(R.string.shop_guide_trails)
            Shelf.MUSIC -> getString(R.string.shop_guide_music)
            Shelf.PROPS -> getString(R.string.shop_guide_props)
            Shelf.ITEMS -> getString(R.string.shop_guide_items, ShopRules.ITEM_PRICE)
            Shelf.COINS -> getString(R.string.shop_guide_coins, ShopRules.METERS_PER_COIN)
        }
        return SpannableStringBuilder().apply {
            append("◇", ForegroundColorSpan(LAVENDER), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append("  "); append(body); append("  ")
            append("◇", ForegroundColorSpan(LAVENDER), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /**
     * Words on the wooden shop picture sit on a cream rounded plaque with a honey edge (like the buttons), in dark
     * green, so they read clearly instead of cream letters straight on the wood.
     */
    private fun onPlaque(t: TextView) {
        t.setTextColor(GREEN); t.setShadowLayer(0f, 0f, 0f, 0)
        t.background = GradientDrawable().apply {
            setColor(Color.argb(240, 255, 249, 235)); cornerRadius = dp(100).toFloat(); setStroke(dp(2), HONEY_LIGHT)
        }
        t.setPadding(dp(14), dp(4), dp(14), dp(5))
    }

    private fun say(text: String, error: Boolean = false) {
        // On its cream plaque (onPlaque); errors stay red; hidden while there is nothing to say.
        message.setTextColor(if (error) ERROR else GREEN)
        message.text = text
        message.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun loadBag() {
        com.tinyrunners.school.quest.ItemBagStore.forCurrentUser(this).load { result ->
            if (isDestroyed) return@load
            result.onSuccess { bag = it; if (shelf == Shelf.ITEMS) render() }
        }
    }

    // ---- Cards -------------------------------------------------------------------------------------------------

    /** The shared card frame: cream card, honey border; the decoration or song in use gets a green border. */
    private fun shelfCard(inUse: Boolean = false) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(10), dp(8), dp(10), dp(8))
        background = GradientDrawable().apply {
            setColor(CARD); cornerRadius = dp(16).toFloat()
            setStroke(dp(if (inUse) 3 else 2), if (inUse) GREEN else HONEY_LIGHT)
        }
    }

    /** Name with an optional small chip (보유 / 가방 N개) at its right. */
    private fun nameRow(name: String, chip: String?) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4), dp(4), 0, 0)
        addView(TextView(context).apply {
            text = name; textSize = 17f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD); maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        if (chip != null) addView(TextView(context).apply {
            text = chip; textSize = 12f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), dp(2), dp(8), dp(2))
            background = GradientDrawable().apply { setColor(OWNED); cornerRadius = dp(10).toFloat() }
        })
    }

    private fun blurb(text: String) = TextView(this).apply {
        this.text = text; textSize = 13f; setTextColor(INK_SOFT); maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(dp(4), dp(2), 0, 0)
    }

    private fun priceText(text: String) = TextView(this).apply {
        this.text = text; textSize = 16f; setTextColor(PRICE); setTypeface(typeface, Typeface.BOLD); setPadding(dp(4), 0, 0, 0)
    }

    private fun actionParams() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(36)).apply { topMargin = dp(4) }

    /** Button states from the design: filled green (사기), outlined (쓰기), light green (쓰는 중), beige (부족·대기). */
    private enum class Look { BUY, USE, IN_USE, WAIT }

    private fun stateButton(text: String, look: Look, click: (() -> Unit)? = null) = Button(this).apply {
        this.text = text
        textSize = 15f; isAllCaps = false; stateListAnimator = null; setPadding(0, 0, 0, 0)
        setTypeface(typeface, Typeface.BOLD)
        val (fill, ink, stroke) = when (look) {
            Look.BUY -> Triple(GREEN, Color.WHITE, GREEN)
            Look.USE -> Triple(CARD, GREEN, GREEN)
            Look.IN_USE -> Triple(OWNED, GREEN, OWNED)
            Look.WAIT -> Triple(DISABLED, DISABLED_INK, DISABLED)
        }
        setTextColor(ink)
        background = RippleDrawable(ColorStateList.valueOf(OWNED), GradientDrawable().apply {
            setColor(fill); cornerRadius = dp(14).toFloat(); setStroke(dp(2), stroke)
        }, null)
        isEnabled = click != null
        if (click != null) setOnClickListener { click() }
    }

    /** The button shown while nothing can be done yet: loading, a failed load, or another request being saved. */
    private fun waitingButton(id: String): Button? = when {
        busyId == id -> stateButton(getString(R.string.shop_processing), Look.WAIT)
        loadFailed -> stateButton(getString(R.string.shop_unavailable), Look.WAIT)
        wallet == null -> stateButton(getString(R.string.shop_loading_short), Look.WAIT)
        else -> null
    }

    private fun card(item: ShopItem, w: Wallet?): View {
        val owned = w?.state?.owns(item.id) == true
        val music = item.kind == ItemKind.MUSIC
        val prop = item.kind == ItemKind.PROP
        val inUse = when (item.kind) {
            ItemKind.MUSIC -> w?.state?.music == item.id
            ItemKind.TRAIL -> w?.state?.equipped == item.id
            // A bought road prop is simply on the road; it has no "쓰기".
            ItemKind.PROP -> owned
        }
        // A song whose file has not been added to the app yet is on the shelf as "준비 중".
        val ready = !music || trackExists(item)
        val card = shelfCard(inUse)
        // Songs show their friend (the main theme shows everyone); decorations show the friend from the selection.
        val songFriend = if (music) item.id.removePrefix("music_").takeIf { it in FRIENDS } else null
        val picture: View = if (prop) PropPreview(this, item) else ShopPreview(this, songFriend ?: friendId,
            if (music) TrailStyle.NOTES else item.style, everyone = music && songFriend == null)
        card.addView(picture.apply {
            if (!ready) alpha = 0.45f
            contentDescription = getString(R.string.shop_preview_description, getString(item.name))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        // A seasonal decoration says so ("겨울 한정") until it is owned.
        card.addView(nameRow(getString(item.name), when {
            owned -> getString(R.string.shop_owned)
            item.season != null -> getString(R.string.shop_season_only, getString(item.season.label))
            else -> null
        }))
        val sub = if (music) songFriend?.let { id ->
            getString(R.string.shop_song_of, com.tinyrunners.school.data.CharacterRepository(this).find(id)?.name.orEmpty())
        } ?: getString(R.string.shop_song_everyone) else if (prop) getString(R.string.shop_prop_from,
            com.tinyrunners.school.data.CharacterRepository(this).find(item.friend.orEmpty())?.name.orEmpty()) else getString(item.blurb)
        card.addView(blurb(sub))
        val priceRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        priceRow.addView(priceText(when {
            item.price == 0 -> getString(R.string.shop_free)
            !ready -> getString(R.string.shop_coming_soon)
            else -> getString(R.string.shop_price, item.price)
        }), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        if (music && ready) priceRow.addView(outlined(getString(if (previewing == item.id) R.string.shop_preview_stop else R.string.shop_preview_play), small = true) {
            preview(item)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(30)))
        card.addView(priceRow)
        val button = waitingButton(item.id) ?: when {
            inUse -> stateButton(getString(if (prop) R.string.shop_prop_on else R.string.shop_wearing), Look.IN_USE)
            owned -> stateButton(getString(R.string.shop_wear), Look.USE) { wear(item) }
            !ready -> stateButton(getString(R.string.shop_coming_soon), Look.WAIT)
            w!!.coins >= item.price -> stateButton(getString(R.string.shop_buy), Look.BUY) { buy(item) }
            else -> stateButton(getString(R.string.shop_short, item.price - w.coins), Look.WAIT)
        }
        card.addView(button, actionParams())
        return card
    }

    /** One of a friend's items for coins; it goes straight to the bag. Items are used up, so there is no "쓰기". */
    private fun itemCard(item: com.tinyrunners.school.quest.QuestItem, w: Wallet?): View {
        val friend = com.tinyrunners.school.data.CharacterRepository(this).find(item.friendId)
        val card = shelfCard()
        card.addView(ItemPreview(this, item).apply { contentDescription = getString(item.name) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        val have = bag?.count(item.friendId)
        card.addView(nameRow(getString(item.name), have?.takeIf { it > 0 }?.let { getString(R.string.shop_in_bag, it) }))
        card.addView(blurb(getString(R.string.shop_item_for, friend?.name.orEmpty())))
        card.addView(priceText(getString(R.string.shop_price, ShopRules.ITEM_PRICE)))
        val button = waitingButton("item_" + item.friendId) ?: if (w!!.coins >= ShopRules.ITEM_PRICE) {
            stateButton(getString(R.string.shop_buy), Look.BUY) { buyItem(item) }
        } else stateButton(getString(R.string.shop_short, ShopRules.ITEM_PRICE - w.coins), Look.WAIT)
        card.addView(button, actionParams())
        return card
    }

    /** A coin pack. Buying asks first and makes clear that this build charges nothing (demonstration). */
    private fun coinCard(pack: ShopRules.CoinPack): View {
        val card = shelfCard()
        card.addView(CoinPile(this, pack.coins), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        card.addView(nameRow(getString(R.string.shop_pack_name, pack.coins), null))
        card.addView(blurb(getString(R.string.shop_pack_demo)))
        card.addView(priceText(getString(R.string.shop_pack_price, String.format(java.util.Locale.KOREA, "%,d", pack.won))))
        val id = "pack_" + pack.coins
        val button = if (busyId == id) stateButton(getString(R.string.shop_processing), Look.WAIT)
            else stateButton(getString(R.string.shop_pack_buy), Look.BUY) { confirmPack(pack) }
        card.addView(button, actionParams())
        return card
    }

    // ---- Actions -----------------------------------------------------------------------------------------------

    private fun buyItem(item: com.tinyrunners.school.quest.QuestItem) {
        if (busy) return
        busyId = "item_" + item.friendId; render()
        store.buyItem(item.friendId) { result ->
            busyId = null
            if (isDestroyed) return@buyItem
            result.onSuccess { (ok, w) ->
                wallet = w
                if (ok) say(getString(R.string.shop_item_bought, getString(item.name)))
                else say(getString(R.string.shop_not_enough, ShopRules.ITEM_PRICE - w.coins), error = true)
                loadBag()
            }.onFailure { say(getString(R.string.shop_failed), error = true) }
            render()
        }
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
        busyId = "pack_" + pack.coins; render()
        store.buyCoins(pack) { result ->
            busyId = null
            if (isDestroyed) return@buyCoins
            result.onSuccess { wallet = it; say(getString(R.string.shop_pack_done, pack.coins)) }
                .onFailure { say(getString(R.string.shop_failed), error = true) }
            render()
        }
    }

    override fun onPause() {
        // Leaving the shop ends the preview.
        previewing = null
        super.onPause()
    }

    /** Plays a song from the music shelf (or stops it when it is already playing). */
    @SuppressLint("DiscouragedApi")
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

    private fun buy(item: ShopItem) {
        if (busy) return
        busyId = item.id; render()
        store.buy(item.id) { result ->
            busyId = null
            if (isDestroyed) return@buy
            result.onSuccess { (outcome, w) ->
                wallet = w
                when (outcome) {
                    is ShopRules.Purchase.Bought -> say(getString(when (item.kind) {
                        ItemKind.MUSIC -> R.string.shop_bought_music
                        ItemKind.PROP -> R.string.shop_bought_prop
                        ItemKind.TRAIL -> R.string.shop_bought
                    }, getString(item.name)))
                    is ShopRules.Purchase.NotEnough -> say(getString(R.string.shop_not_enough, outcome.missing), error = true)
                    else -> say("")
                }
            }.onFailure { say(getString(R.string.shop_failed), error = true) }
            render()
        }
    }

    private fun wear(item: ShopItem) {
        if (busy) return
        busyId = item.id; render()
        store.equip(item.id) { result ->
            busyId = null
            if (isDestroyed) return@equip
            result.onSuccess { wallet = it; say("") }
                .onFailure { say(getString(R.string.shop_failed), error = true) }
            render()
        }
    }

    // ---- Small parts -------------------------------------------------------------------------------------------

    /** Shelf switch: the chosen shelf is filled green, the others are cream with a thin honey line. */
    private fun tab(label: Int, kind: Shelf) = Button(this).apply {
        val on = shelf == kind
        setText(label)
        textSize = 16f; isAllCaps = false; stateListAnimator = null; setTypeface(typeface, Typeface.BOLD)
        setTextColor(if (on) Color.WHITE else GREEN)
        minWidth = dp(128); minimumWidth = dp(128); minHeight = dp(40); minimumHeight = dp(40)
        setPadding(dp(18), 0, dp(18), 0)
        background = RippleDrawable(ColorStateList.valueOf(OWNED), GradientDrawable().apply {
            setColor(if (on) GREEN else CARD); cornerRadius = dp(20).toFloat(); setStroke(dp(2), if (on) GREEN else HONEY_LIGHT)
        }, null)
        if (on) isSelected = true
        setOnClickListener {
            if (shelf != kind) { shelf = kind; if (!loadFailed) say(""); scroller.scrollTo(0, 0); render() }
        }
    }

    private fun outlined(text: String, small: Boolean = false, click: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = if (small) 13f else 14f; isAllCaps = false; stateListAnimator = null; setTypeface(typeface, Typeface.BOLD)
        minHeight = dp(if (small) 30 else 38); minimumHeight = minHeight; minWidth = 0; minimumWidth = 0
        setPadding(dp(10), 0, dp(10), 0)
        setTextColor(GREEN)
        background = RippleDrawable(ColorStateList.valueOf(OWNED), GradientDrawable().apply {
            setColor(CARD); cornerRadius = dp(14).toFloat(); setStroke(dp(2), GREEN)
        }, null)
        setOnClickListener { click() }
    }

    private fun divider() = View(this).apply { setBackgroundColor(HONEY) }.also {
        it.layoutParams = LinearLayout.LayoutParams(dp(2), dp(28)).apply { marginStart = dp(14); marginEnd = dp(14) }
    }

    private fun rule() = View(this).apply { setBackgroundColor(LAVENDER) }

    @SuppressLint("DiscouragedApi")
    private fun trackExists(item: ShopItem) =
        item.track != null && resources.getIdentifier(item.track, "raw", packageName) != 0

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()


    companion object {
        const val EXTRA_FRIEND = "friendId"
        const val EXTRA_MUSIC = "openMusic"
        private val FRIENDS = setOf("mint", "cheese", "popo", "lumi", "nunu", "piko")

        // Colours from docs/art/shop/DESIGN_SPEC.md.
        internal val FABRIC = Color.parseColor("#F8F0DE")
        internal val CARD = Color.parseColor("#FFF9EB")
        internal val GREEN = Color.parseColor("#28563B")
        internal val HONEY = Color.parseColor("#BE9664")
        internal val HONEY_LIGHT = Color.parseColor("#DCC09A")
        internal val PRICE = Color.parseColor("#8C5D22")
        internal val OWNED = Color.parseColor("#CFDCA6")
        internal val DISABLED = Color.parseColor("#E9DEC7")
        internal val DISABLED_INK = Color.parseColor("#8A7B66")
        internal val LAVENDER = Color.parseColor("#AD92C2")
        private val INK_SOFT = Color.parseColor("#5E5A4C")
        private val ERROR = Color.parseColor("#B04A3A")
        private val COIN = Color.rgb(246, 200, 80)

        fun intent(context: Context, friendId: String?) = Intent(context, ShopActivity::class.java).putExtra(EXTRA_FRIEND, friendId)
    }
}

/**
 * A picture in a card: the friend's home scene (the same art as their run) with a rounded edge. Subclasses draw what
 * is on sale over it. The scene is kept quiet (paler and less colourful, as if seen through a little mist) and a soft
 * light sits behind what is on sale ([spotlight]), so the decoration or item is what the eye finds first.
 */
@SuppressLint("ViewConstructor")
private open class ScenePicture(context: Context, sceneFriend: String?) : View(context) {
    private val scene: Pair<Bitmap, Float>? = sceneFriend?.let { RoadThumbs.of(context, it) ?: BackgroundThumbs.of(context, it) }
    protected val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val scenePaint = Paint().apply {
        isFilterBitmap = false; isAntiAlias = false
        colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(0.3f) })
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val src = Rect()
    private val dst = RectF()
    private val clip = android.graphics.Path()
    /** Where the ground is in this view, after [drawScene]. */
    protected var floorY = 0f

    protected fun drawScene(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val r = 12 * resources.displayMetrics.density
        clip.reset(); clip.addRoundRect(0f, 0f, w, h, r, r, android.graphics.Path.Direction.CW)
        canvas.clipPath(clip)
        val art = scene
        if (art == null) {
            fill.shader = android.graphics.LinearGradient(0f, 0f, 0f, h, Color.rgb(250, 238, 222), Color.rgb(222, 236, 204), android.graphics.Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, fill); fill.shader = null
            fill.color = Color.rgb(196, 220, 168); canvas.drawRect(0f, h * 0.8f, w, h, fill)
            floorY = h * 0.84f
            return
        }
        val (bitmap, floor) = art
        // Fill the height; keep the floor a little above the bottom edge so the friend stands on it.
        val scale = h / (bitmap.height * 0.62f)
        val srcW = (w / scale).coerceAtMost(bitmap.width.toFloat())
        val srcTop = (bitmap.height * floor - (h * 0.86f) / scale).coerceIn(0f, bitmap.height - h / scale)
        src.set(0, srcTop.toInt(), srcW.toInt(), (srcTop + h / scale).toInt().coerceAtMost(bitmap.height))
        dst.set(0f, 0f, w, h)
        canvas.drawBitmap(bitmap, src, dst, scenePaint)
        floorY = (bitmap.height * floor - srcTop) * scale
        // A light cream wash over the whole scene.
        fill.color = Color.argb(130, 255, 248, 234); canvas.drawRect(0f, 0f, w, h, fill)
    }

    /** A soft warm light around ([cx], [cy]) and a small shadow on the ground under what is on sale. */
    protected fun spotlight(canvas: Canvas, cx: Float, cy: Float, radius: Float, shadowWidth: Float) {
        fill.shader = android.graphics.RadialGradient(cx, cy, radius,
            intArrayOf(Color.argb(200, 255, 252, 240), Color.argb(90, 255, 250, 236), Color.argb(0, 255, 250, 236)),
            floatArrayOf(0f, 0.55f, 1f), android.graphics.Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius, fill); fill.shader = null
        fill.color = Color.argb(70, 60, 50, 30)
        canvas.drawOval(cx - shadowWidth / 2f, floorY - shadowWidth * 0.08f, cx + shadowWidth / 2f, floorY + shadowWidth * 0.08f, fill)
    }
}

/** The start of each friend's running loop (the road outside home), for the shop's pictures. */
internal object RoadThumbs {
    private val cache = mutableMapOf<String, Pair<Bitmap, Float>?>()

    fun of(context: Context, characterId: String): Pair<Bitmap, Float>? = cache.getOrPut(characterId) {
        try {
            val manifest = context.assets.open("backgrounds/manifest.json").bufferedReader().use { org.json.JSONObject(it.readText()) }
            val entries = manifest.getJSONArray("characters")
            val entry = (0 until entries.length()).map { entries.getJSONObject(it) }.firstOrNull { it.getString("id") == characterId }
                ?: return@getOrPut null
            val loop = entry.optString("loopSource").ifEmpty { return@getOrPut null }
            val r = entry.getJSONArray("rect")
            val height = r.getInt(3)
            val bitmap = context.assets.open("backgrounds/" + loop).use { stream ->
                @Suppress("DEPRECATION")
                val decoder = if (Build.VERSION.SDK_INT >= 31) android.graphics.BitmapRegionDecoder.newInstance(stream)
                    else android.graphics.BitmapRegionDecoder.newInstance(stream, false)
                val width = minOf(decoder?.width ?: 0, height * 2)
                decoder?.decodeRegion(Rect(0, 0, width, minOf(height, decoder.height)), null).also { decoder?.recycle() }
            } ?: return@getOrPut null
            bitmap to entry.getInt("floorY") / height.toFloat()
        } catch (e: Exception) {
            android.util.Log.w("RoadThumbs", "No road picture for $characterId", e)
            null
        }
    }
}

/** A sprig of lavender after the shop's short introduction. */
private class LavenderSprig(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val d = resources.displayMetrics.density
        paint.color = Color.rgb(104, 150, 84); paint.strokeWidth = 1.5f * d
        val tips = listOf(w * 0.3f to h * 0.12f, w * 0.62f to h * 0.05f, w * 0.85f to h * 0.3f)
        for ((x, y) in tips) canvas.drawLine(w * 0.45f, h, x, y + 8 * d, paint)
        for ((x, y) in tips) {
            for (k in 0..3) {
                paint.color = if (k % 2 == 0) ShopActivity.LAVENDER else Color.rgb(150, 116, 186)
                canvas.drawCircle(x + (if (k % 2 == 0) -1.2f else 1.2f) * d, y + k * 2.8f * d, 2f * d, paint)
            }
            paint.color = Color.rgb(104, 150, 84)
        }
    }
}

/** The friend running on their home ground with the decoration behind them (or six friends for the main theme). */
@SuppressLint("ViewConstructor")
private class ShopPreview(context: Context, friendId: String, private val style: TrailStyle, everyone: Boolean = false) :
    ScenePicture(context, if (everyone) null else friendId) {
    private val sprites = CharacterSprites.load(resources, friendId)
    private val all = if (everyone) listOf("mint", "cheese", "popo", "lumi", "nunu", "piko").mapNotNull { CharacterSprites.load(resources, it) } else emptyList()
    private val box = RectF()
    private val start = SystemClock.uptimeMillis()

    override fun onDraw(canvas: Canvas) {
        drawScene(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val t = (SystemClock.uptimeMillis() - start) / 1000f
        if (all.isNotEmpty()) {
            // The main theme belongs to everyone: all six friends bob in a row with notes above.
            val px = minOf(h * 0.55f, w / 6.4f) / CharacterSprites.SIZE
            val size = CharacterSprites.SIZE * px
            all.forEachIndexed { i, s ->
                val left = w / 2f + (i - 2.5f) * size * 0.98f - size / 2f
                val bob = if (((t * 2).toInt() + i) % 2 == 0) 0f else px * 2
                box.set(left, floorY - size - bob, left + size, floorY - bob)
                canvas.drawBitmap(s[SpriteFrame.IDLE_0], null, box, pixel)
            }
            TrailPainter.draw(canvas, style, w * 0.15f, h * 0.22f, px, t * 1.5f)
            postInvalidateDelayed(120)
            return
        }
        val art = sprites ?: return
        // The friend about half the picture's height, feet on the home ground.
        val px = (h * 0.8f / CharacterSprites.SIZE).coerceAtLeast(1f)
        val size = CharacterSprites.SIZE * px
        val left = w * 0.6f - size / 2f
        val top = floorY - CharacterSprites.FOOT_EDGE_Y * px
        // The decoration drawn bigger than it is in a run (the picture is small), in a soft light behind the friend.
        spotlight(canvas, left - size * 0.05f, top + size * 0.58f, minOf(w * 0.45f, size * 1.1f), size * 0.5f)
        TrailPainter.draw(canvas, style, left + px * 12f, top + size * 0.55f, px * 1.8f, t * 1.5f)
        val frames = arrayOf(SpriteFrame.RUN_0, SpriteFrame.RUN_1, SpriteFrame.RUN_2, SpriteFrame.RUN_3)
        box.set(left, top, left + size, top + size)
        canvas.drawBitmap(art[frames[((t * 8).toInt()) % 4]], null, box, pixel)
        postInvalidateDelayed(66)
    }
}

/** A friend's item standing on their home ground, using the approved item art when there is one. */
@SuppressLint("ViewConstructor")
private class ItemPreview(context: Context, private val item: com.tinyrunners.school.quest.QuestItem) : ScenePicture(context, item.friendId) {
    override fun onDraw(canvas: Canvas) {
        drawScene(canvas)
        val size = minOf(height * 0.72f, width * 0.5f)
        spotlight(canvas, width / 2f, floorY - size / 2f, size * 0.95f, size * 0.7f)
        com.tinyrunners.school.quest.ItemArt.draw(canvas, item, (width - size) / 2f, floorY - size, size, withGlow = false)
    }
}

/** A road prop for sale standing on its friend's road, in a soft light, drawn pixel-sharp like in the run. */
@SuppressLint("ViewConstructor")
private class PropPreview(context: Context, item: ShopItem) : ScenePicture(context, item.friend) {
    private val art = android.graphics.BitmapFactory.decodeResource(resources, item.prop,
        android.graphics.BitmapFactory.Options().apply { inScaled = false })
    /** Only the drawn part of the 48×80 sprite (it has empty space around the prop). */
    private val opaque = art?.let { com.tinyrunners.school.game.ObstacleArt.opaqueBounds(it) }
    private val box = RectF()

    override fun onDraw(canvas: Canvas) {
        drawScene(canvas)
        val bitmap = art ?: return
        val part = opaque ?: return
        // As big as the picture allows: most of the height above the floor, at most two thirds of the width.
        val k = minOf(floorY * 0.78f / part.height(), width * 0.66f / part.width())
        val w = part.width() * k; val h = part.height() * k
        spotlight(canvas, width / 2f, floorY - h * 0.45f, maxOf(w, h) * 0.85f, w * 0.9f)
        box.set((width - w) / 2f, floorY - h, (width + w) / 2f, floorY)
        canvas.drawBitmap(bitmap, part, box, pixel)
    }
}

/**
 * The shop's (and settings') backdrop: A's final frame picture (TinyRunners-final-ui-assets, shop_background) —
 * cream fabric, a thin honey-wood frame, a potted fern in the bottom-left corner and flowers in the bottom-right — drawn
 * as nine pieces: the corners keep their shape at the screen's height, and only the plain edges and middle stretch.
 * Drawn once (no animation); everything with text is a real view on top.
 */
internal class ShopBackground(context: Context, private val interior: Boolean = false) : View(context) {
    /** The wooden shop interior of A's separated pack (backgrounds/shop, widened): clear in the shop, veiled elsewhere. */
    private val art = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.bg_v2_shop_wide)
    /** Settings, the bag and the postcards: the same room under a cream veil, so their dark text and cards read calmly. */
    private val veil = Paint().apply { color = Color.argb(190, 250, 241, 222) }
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val src = Rect()
    private val dst = RectF()

    override fun onDraw(canvas: Canvas) {
        val art = art
        if (art != null && width > 0 && height > 0) {
            // Cover: fill the screen, cut evenly from the long side.
            // Only the original picture (the widened file's middle 80 %, no blurred side bands), cut evenly to fill.
            val aw = art.width * 0.8f; val left = art.width * 0.1f
            val k = maxOf(width / aw, height / art.height.toFloat())
            val sw = width / k; val sh = height / k
            src.set((left + (aw - sw) / 2).toInt(), ((art.height - sh) / 2).toInt(), (left + (aw + sw) / 2).toInt(), ((art.height + sh) / 2).toInt())
            dst.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawBitmap(art, src, dst, paint)
            if (!interior) canvas.drawRect(dst, veil)
            return
        }
        if (art == null || width <= 0 || height <= 0) { canvas.drawColor(ShopActivity.FABRIC); return }
        drawFrame(canvas, art, width.toFloat(), height.toFloat(), paint)
    }

    companion object {
        /** Corner sizes in the 1919×1080 picture: wide enough for the fern and the flowers, tall enough for the board. */
        private const val SIDE = 330
        private const val TOP = 70
        private const val BOTTOM = 180

        /** Draws the frame picture [art] over [w]×[h] in nine pieces (also used for the share cards). */
        fun drawFrame(canvas: Canvas, art: Bitmap, w: Float, h: Float, paint: Paint) {
            val k = h / art.height
            // Source cuts (picture pixels) and the same cuts on screen; the corners never grow wider than half the screen.
            val sx = intArrayOf(0, SIDE, art.width - SIDE, art.width)
            val sy = intArrayOf(0, TOP, art.height - BOTTOM, art.height)
            val side = minOf(SIDE * k, w / 2f)
            val dx = floatArrayOf(0f, side, w - side, w)
            val dy = floatArrayOf(0f, TOP * k, h - BOTTOM * k, h)
            val src = Rect(); val dst = RectF()
            for (row in 0..2) for (col in 0..2) {
                src.set(sx[col], sy[row], sx[col + 1], sy[row + 1])
                dst.set(dx[col], dy[row], dx[col + 1], dy[row + 1])
                canvas.drawBitmap(art, src, dst, paint)
            }
        }
    }
}

/** A small heap of coins for a coin pack card; bigger packs show more coins. */
@SuppressLint("ViewConstructor")
private class CoinPile(context: Context, private val coins: Int) : View(context) {
    private val back = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FCEFC8") }

    override fun onDraw(canvas: Canvas) {
        val r0 = 12 * resources.displayMetrics.density
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), r0, r0, back)
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
