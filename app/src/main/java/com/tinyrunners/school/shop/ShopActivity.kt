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
    private lateinit var pager: LinearLayout
    private var pages = 1
    private var wallet: Wallet? = null
    private var loadFailed = false
    /** The card whose purchase or wear request is being saved; its button shows "처리 중" and nothing else reacts. */
    private var busyId: String? = null
    private val busy get() = busyId != null
    /** Which shelf is shown. */
    private enum class Shelf { TRAIL, MUSIC, ITEMS, COINS }
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
        top.addView(com.tinyrunners.school.ui.HomeButton.create(this).apply {
            textSize = 14f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD)
            background = RippleDrawable(ColorStateList.valueOf(OWNED), GradientDrawable().apply {
                setColor(CARD); cornerRadius = dp(18).toFloat(); setStroke(dp(2), HONEY)
            }, null)
            minHeight = dp(38); minimumHeight = dp(38); setPadding(dp(14), 0, dp(16), 0)
        })
        top.addView(divider())
        top.addView(TextView(this).apply {
            setText(R.string.shop_title)
            textSize = 26f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD)
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        })
        top.addView(divider())
        top.addView(TextView(this).apply {
            setText(R.string.shop_intro); textSize = 15f; setTextColor(INK_SOFT); maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { weight = 0f; width = LinearLayout.LayoutParams.WRAP_CONTENT })
        top.addView(LavenderSprig(this), LinearLayout.LayoutParams(dp(26), dp(30)).apply { marginStart = dp(10) })
        top.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        coins = TextView(this).apply {
            setText(R.string.shop_loading)
            textSize = 17f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(12), dp(6), dp(16), dp(6))
            background = GradientDrawable().apply { setColor(COIN_BG); cornerRadius = dp(20).toFloat(); setStroke(dp(2), HONEY) }
            compoundDrawablePadding = dp(8)
            setCompoundDrawablesRelative(GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(COIN); setStroke(dp(2), PRICE); setBounds(0, 0, dp(20), dp(20))
            }, null, null, null)
        }
        top.addView(coins, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(10) })
        top.addView(com.tinyrunners.school.ui.BagButton.create(this))
        content.addView(top)

        // Guide line between two lavender rules: ── ◇ text ◇ ──
        val guideRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        guideRow.addView(rule(), LinearLayout.LayoutParams(0, dp(2), 1f))
        guide = TextView(this).apply { textSize = 14f; setTextColor(INK_SOFT); setPadding(dp(10), 0, dp(10), 0); gravity = Gravity.CENTER }
        guideRow.addView(guide)
        guideRow.addView(rule(), LinearLayout.LayoutParams(0, dp(2), 1f))
        content.addView(guideRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })

        // Tabs on the left, the latest message (and a retry after a failed load) on the right.
        val tabRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabRow.addView(tabs)
        message = TextView(this).apply {
            textSize = 14f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD); gravity = Gravity.END or Gravity.CENTER_VERTICAL
            maxLines = 2; setPadding(dp(12), 0, 0, 0)
        }
        tabRow.addView(message, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        retry = outlined(getString(R.string.shop_retry)) { load() }.apply { visibility = View.GONE }
        tabRow.addView(retry, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)).apply { marginStart = dp(8) })
        content.addView(tabRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })

        row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(2), 0, dp(2)) }
        scroller = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; clipToPadding = false; addView(row) }
        content.addView(scroller, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(6) })
        pager = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        content.addView(pager, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(30)).apply { topMargin = dp(3) })
        scroller.setOnScrollChangeListener { _, _, _, _, _ -> markPage() }
        // Cards are sized from the row's width (four across), so lay them out again once that width is known.
        scroller.addOnLayoutChangeListener { _, l, _, r, _, ol, _, or, _ -> if (r - l != or - ol) scroller.post { render() } }

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(FABRIC)
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
        load()
    }

    /** Loads the wallet (and the bag for the 아이템 shelf). Until it arrives, buttons say "불러오는 중". */
    private fun load() {
        loadFailed = false
        retry.visibility = View.GONE
        message.text = ""
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
        listOf(Shelf.TRAIL to R.string.shop_tab_trails, Shelf.MUSIC to R.string.shop_tab_music,
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
            Shelf.TRAIL -> add(ShopCatalog.of(ItemKind.TRAIL).map { card(it, w) })
            Shelf.MUSIC -> add(ShopCatalog.of(ItemKind.MUSIC).map { card(it, w) })
            Shelf.ITEMS -> add(com.tinyrunners.school.quest.QuestItems.all.map { itemCard(it, w) })
            Shelf.COINS -> add(ShopRules.COIN_PACKS.map { coinCard(it) })
        }
        // Pages of four: dots show where you are, ▶ moves on (and back to the start after the last page).
        val perPage = if (available > 0) maxOf(1, (available + gap) / (cardWidth + gap)) else 4
        pages = (row.childCount + perPage - 1) / perPage
        pager.removeAllViews()
        if (pages > 1) {
            repeat(pages) { i ->
                pager.addView(View(this).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO },
                    LinearLayout.LayoutParams(dp(12), dp(12)).apply { if (i > 0) marginStart = dp(12) })
            }
            pager.addView(Button(this).apply {
                text = "▶"; textSize = 13f; setTextColor(GREEN); stateListAnimator = null; setPadding(0, 0, 0, 0)
                minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
                contentDescription = getString(R.string.shop_next_page)
                background = RippleDrawable(ColorStateList.valueOf(OWNED), GradientDrawable().apply {
                    setColor(CARD); cornerRadius = dp(8).toFloat(); setStroke(dp(2), GREEN)
                }, null)
                setOnClickListener {
                    val page = currentPage()
                    scroller.smoothScrollTo(if (page + 1 >= pages) 0 else (page + 1) * pageWidth(), 0)
                }
            }, LinearLayout.LayoutParams(dp(30), dp(30)).apply { marginStart = dp(22) })
        }
        markPage()
    }

    private fun pageWidth() = scroller.width + dp(14)

    private fun currentPage(): Int {
        val max = (row.width - scroller.width).coerceAtLeast(0)
        if (pages <= 1) return 0
        // The last page may be partly filled, so being scrolled to the end counts as the last page.
        return if (scroller.scrollX >= max - dp(4)) pages - 1 else (scroller.scrollX + pageWidth() / 2) / pageWidth()
    }

    private fun markPage() {
        val page = currentPage()
        for (i in 0 until minOf(pages, pager.childCount)) {
            pager.getChildAt(i).background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (i == page) GREEN else CARD); setStroke(dp(2), if (i == page) GREEN else HONEY_LIGHT)
            }
        }
    }

    /** The lavender guide line for each shelf, between two small diamonds. */
    private fun guideText(): CharSequence {
        val body = when (shelf) {
            Shelf.TRAIL -> getString(R.string.shop_guide_trails)
            Shelf.MUSIC -> getString(R.string.shop_guide_music)
            Shelf.ITEMS -> getString(R.string.shop_guide_items, ShopRules.ITEM_PRICE)
            Shelf.COINS -> getString(R.string.shop_guide_coins, ShopRules.METERS_PER_COIN)
        }
        return SpannableStringBuilder().apply {
            append("◇", ForegroundColorSpan(LAVENDER), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append("  "); append(body); append("  ")
            append("◇", ForegroundColorSpan(LAVENDER), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun say(text: String, error: Boolean = false) {
        message.setTextColor(if (error) ERROR else GREEN)
        message.text = text
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
        val inUse = if (music) w?.state?.music == item.id else w?.state?.equipped == item.id
        // A song whose file has not been added to the app yet is on the shelf as "준비 중".
        val ready = !music || trackExists(item)
        val card = shelfCard(inUse)
        // Songs show their friend (the main theme shows everyone); decorations show the friend from the selection.
        val songFriend = if (music) item.id.removePrefix("music_").takeIf { it in FRIENDS } else null
        card.addView(ShopPreview(this, songFriend ?: friendId, if (music) TrailStyle.NOTES else item.style,
            everyone = music && songFriend == null).apply {
            if (!ready) alpha = 0.45f
            contentDescription = getString(R.string.shop_preview_description, getString(item.name))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        card.addView(nameRow(getString(item.name), if (owned) getString(R.string.shop_owned) else null))
        val sub = if (music) songFriend?.let { id ->
            getString(R.string.shop_song_of, com.tinyrunners.school.data.CharacterRepository(this).find(id)?.name.orEmpty())
        } ?: getString(R.string.shop_song_everyone) else getString(item.blurb)
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
            inUse -> stateButton(getString(R.string.shop_wearing), Look.IN_USE)
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
                    is ShopRules.Purchase.Bought -> say(getString(if (item.kind == ItemKind.MUSIC) R.string.shop_bought_music else R.string.shop_bought, getString(item.name)))
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
            if (shelf != kind) { shelf = kind; if (!loadFailed) message.text = ""; scroller.scrollTo(0, 0); render() }
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
        private val COIN_BG = Color.parseColor("#FCEFC8")

        fun intent(context: Context, friendId: String?) = Intent(context, ShopActivity::class.java).putExtra(EXTRA_FRIEND, friendId)
    }
}

/**
 * A picture in a card: the friend's home scene (the same art as their run) with a rounded edge. Subclasses draw what
 * is on sale over it.
 */
@SuppressLint("ViewConstructor")
private open class ScenePicture(context: Context, sceneFriend: String?) : View(context) {
    private val scene: Pair<Bitmap, Float>? = sceneFriend?.let { RoadThumbs.of(context, it) ?: BackgroundThumbs.of(context, it) }
    protected val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
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
        canvas.drawBitmap(bitmap, src, dst, pixel)
        floorY = (bitmap.height * floor - srcTop) * scale
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
        TrailPainter.draw(canvas, style, left + px * 10f, top + size * 0.55f, px, t * 1.5f)
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
        com.tinyrunners.school.quest.ItemArt.draw(canvas, item, (width - size) / 2f, floorY - size, size, withGlow = false)
    }
}

/**
 * The shop's backdrop from the approved design: cream fabric with a faint weave, a thin honey-wood frame, a potted
 * fern in the bottom-left corner and lavender and white flowers in the bottom-right. Drawn once (no animation);
 * everything with text is a real view on top.
 */
private class ShopBackground(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = android.graphics.Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val d = resources.displayMetrics.density
        // Fabric: cream with a very light weave, low enough not to disturb the text.
        paint.color = ShopActivity.FABRIC
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.color = Color.argb(14, 140, 100, 50)
        val weave = 4 * d
        var y = 0f
        while (y < h) { canvas.drawRect(0f, y, w, y + d * 0.6f, paint); y += weave }
        var x = 0f
        while (x < w) { canvas.drawRect(x, 0f, x + d * 0.6f, h, paint); x += weave }
        // Honey-wood frame: a darker outer line, the wood, and a light inner edge.
        val inset = 5 * d
        val r = 18 * d
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7 * d; paint.color = Color.rgb(214, 176, 124)
        canvas.drawRoundRect(inset, inset, w - inset, h - inset, r, r, paint)
        paint.strokeWidth = 1.5f * d; paint.color = ShopActivity.HONEY
        canvas.drawRoundRect(inset - 3.5f * d, inset - 3.5f * d, w - inset + 3.5f * d, h - inset + 3.5f * d, r + 3 * d, r + 3 * d, paint)
        canvas.drawRoundRect(inset + 3.5f * d, inset + 3.5f * d, w - inset - 3.5f * d, h - inset - 3.5f * d, r - 3 * d, r - 3 * d, paint)
        paint.style = Paint.Style.FILL
        // Corner plants.
        fern(canvas, 56 * d, h - 12 * d, d * 1.3f)
        flowers(canvas, w - 80 * d, h - 12 * d, d * 1.25f)
    }

    /** A terracotta pot with a fern of arching fronds. [cx] is the pot's centre, [base] its bottom. */
    private fun fern(canvas: Canvas, cx: Float, base: Float, d: Float) {
        // Fronds first, so the pot sits in front of them.
        paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND
        val top = base - 22 * d
        val fronds = listOf(-70f, -45f, -20f, 0f, 20f, 45f, 70f)
        for ((i, angle) in fronds.withIndex()) {
            val length = (if (i % 2 == 0) 40f else 34f) * d
            val rad = Math.toRadians((angle - 90).toDouble())
            val ex = cx + (kotlin.math.cos(rad) * length).toFloat()
            val ey = top + (kotlin.math.sin(rad) * length).toFloat() * 0.85f
            val mx = cx + (ex - cx) * 0.5f; val my = top + (ey - top) * 0.5f - 6 * d
            paint.color = Color.rgb(70, 128, 64); paint.strokeWidth = 2 * d
            path.reset(); path.moveTo(cx, top); path.quadTo(mx, my, ex, ey)
            canvas.drawPath(path, paint)
            // Leaflets along the frond.
            paint.color = if (i % 2 == 0) Color.rgb(104, 164, 84) else Color.rgb(84, 146, 72)
            paint.strokeWidth = 2.6f * d
            for (k in 1..5) {
                val f = k / 6f
                val px = (1 - f) * (1 - f) * cx + 2 * (1 - f) * f * mx + f * f * ex
                val py = (1 - f) * (1 - f) * top + 2 * (1 - f) * f * my + f * f * ey
                val leaf = (7 - k) * 1.3f * d
                canvas.drawLine(px, py, px - leaf * 0.7f, py - leaf * 0.5f, paint)
                canvas.drawLine(px, py, px + leaf * 0.7f, py - leaf * 0.5f, paint)
            }
        }
        paint.style = Paint.Style.FILL; paint.strokeCap = Paint.Cap.BUTT
        // The pot: a tapered body and a rim.
        path.reset()
        path.moveTo(cx - 16 * d, base - 20 * d); path.lineTo(cx + 16 * d, base - 20 * d)
        path.lineTo(cx + 12 * d, base); path.lineTo(cx - 12 * d, base); path.close()
        paint.color = Color.rgb(196, 112, 64); canvas.drawPath(path, paint)
        paint.color = Color.rgb(170, 90, 50); canvas.drawRect(cx - 14 * d, base - 10 * d, cx + 13 * d, base - 8 * d, paint)
        paint.color = Color.rgb(214, 132, 80); canvas.drawRoundRect(cx - 19 * d, base - 26 * d, cx + 19 * d, base - 19 * d, 2 * d, 2 * d, paint)
        // Grass and a daisy beside it.
        grass(canvas, cx + 26 * d, base, d, 22 * d)
        daisy(canvas, cx + 34 * d, base - 12 * d, d, Color.WHITE)
    }

    /** A clump of grass with lavender sprigs and two white daisies. */
    private fun flowers(canvas: Canvas, cx: Float, base: Float, d: Float) {
        grass(canvas, cx, base, d, 70 * d)
        paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND
        val sprigs = listOf(Triple(-14f, 34f, -6f), Triple(2f, 44f, 0f), Triple(18f, 38f, 8f), Triple(32f, 28f, 10f))
        for ((dx, height, lean) in sprigs) {
            val sx = cx + dx * d; val tx = sx + lean * d; val ty = base - height * d
            paint.color = Color.rgb(84, 140, 72); paint.strokeWidth = 1.8f * d
            canvas.drawLine(sx, base - 4 * d, tx, ty, paint)
            paint.style = Paint.Style.FILL
            for (k in 0..3) {
                paint.color = if (k % 2 == 0) ShopActivity.LAVENDER else Color.rgb(150, 116, 186)
                canvas.drawCircle(tx + (if (k % 2 == 0) -1.6f else 1.6f) * d, ty + k * 4.2f * d, 2.6f * d, paint)
            }
            paint.style = Paint.Style.STROKE
        }
        paint.style = Paint.Style.FILL; paint.strokeCap = Paint.Cap.BUTT
        daisy(canvas, cx - 26 * d, base - 10 * d, d, Color.WHITE)
        daisy(canvas, cx + 46 * d, base - 8 * d, d, Color.rgb(250, 246, 236))
    }

    private fun grass(canvas: Canvas, cx: Float, base: Float, d: Float, width: Float) {
        paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND; paint.strokeWidth = 2 * d
        var x = cx - width / 2
        var i = 0
        while (x <= cx + width / 2) {
            paint.color = if (i % 2 == 0) Color.rgb(104, 164, 84) else Color.rgb(132, 186, 100)
            val height = (6 + (i * 7) % 9) * d
            canvas.drawLine(x, base, x + (if (i % 3 == 0) -2 else 2) * d, base - height, paint)
            x += 4 * d; i++
        }
        paint.style = Paint.Style.FILL; paint.strokeCap = Paint.Cap.BUTT
    }

    private fun daisy(canvas: Canvas, x: Float, y: Float, d: Float, petal: Int) {
        paint.color = petal
        for (k in 0 until 5) {
            val a = Math.toRadians(k * 72.0)
            canvas.drawCircle(x + (kotlin.math.cos(a) * 2.6f * d).toFloat(), y + (kotlin.math.sin(a) * 2.6f * d).toFloat(), 2.2f * d, paint)
        }
        paint.color = Color.rgb(244, 196, 64); canvas.drawCircle(x, y, 1.8f * d, paint)
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
