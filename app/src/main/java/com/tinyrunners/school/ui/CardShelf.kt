package com.tinyrunners.school.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.shop.ShopActivity
import kotlin.math.roundToInt

/**
 * A row of small cards that slides sideways, as in the shop: four across on a wide screen (each at least 176 dp),
 * with page dots and a ▶ button under them when there are more cards than fit. Used by the village stories, the
 * friend selection and the records so the three read like the shop. Screens fill it with [setCards] and build each
 * card with [card] and the small parts below.
 */
class CardShelf(context: Context) : LinearLayout(context) {
    private val row = LinearLayout(context).apply { orientation = HORIZONTAL; setPadding(0, dp(2), 0, dp(2)) }
    private val scroller = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false; clipToPadding = false; addView(row) }
    private val pager = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }
    private var cards: List<View> = emptyList()
    private var pages = 1
    private var pendingIndex = -1

    init {
        orientation = VERTICAL
        addView(scroller, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        addView(pager, LayoutParams(LayoutParams.MATCH_PARENT, dp(30)).apply { topMargin = dp(6) })
        scroller.setOnScrollChangeListener { _, _, _, _, _ -> markPage() }
        // Card widths depend on the shelf width: lay them out again when it is known or changes.
        scroller.addOnLayoutChangeListener { _, l, _, r, _, ol, _, or, _ -> if (r - l != or - ol) scroller.post { layoutCards() } }
    }

    /** Shows [views] as the cards; [showIndex] scrolls that card into view. */
    fun setCards(views: List<View>, showIndex: Int = -1) {
        cards = views
        pendingIndex = showIndex
        layoutCards()
    }

    private fun layoutCards() {
        val keep = scroller.scrollX
        row.removeAllViews()
        val gap = dp(14)
        val available = scroller.width
        val width = if (available > 0) maxOf(dp(176), (available - gap * 3) / 4) else dp(196)
        cards.forEachIndexed { i, v ->
            (v.parent as? android.view.ViewGroup)?.removeView(v)
            row.addView(v, LayoutParams(width, LayoutParams.MATCH_PARENT).apply { if (i < cards.lastIndex) marginEnd = gap })
        }
        val perPage = if (available > 0) maxOf(1, (available + gap) / (width + gap)) else 4
        pages = (cards.size + perPage - 1) / perPage
        pager.removeAllViews()
        if (pages > 1) {
            repeat(pages) { i ->
                pager.addView(View(context).apply { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO },
                    LayoutParams(dp(12), dp(12)).apply { if (i > 0) marginStart = dp(12) })
            }
            pager.addView(Button(context).apply {
                text = "▶"; textSize = 13f; setTextColor(GREEN); stateListAnimator = null; setPadding(0, 0, 0, 0)
                minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
                contentDescription = context.getString(R.string.shelf_next_page)
                background = RippleDrawable(ColorStateList.valueOf(ShopActivity.OWNED), GradientDrawable().apply {
                    setColor(ShopActivity.CARD); cornerRadius = dp(8).toFloat(); setStroke(dp(2), GREEN)
                }, null)
                setOnClickListener {
                    val page = currentPage()
                    scroller.smoothScrollTo(if (page + 1 >= pages) 0 else (page + 1) * pageWidth(), 0)
                }
            }, LayoutParams(dp(30), dp(30)).apply { marginStart = dp(22) })
        }
        pager.visibility = if (pages > 1) View.VISIBLE else View.INVISIBLE
        scroller.post {
            if (pendingIndex >= 0 && available > 0) {
                // Bring the asked-for card onto the screen once.
                val target = (pendingIndex / perPage) * pageWidth()
                scroller.scrollTo(target, 0); pendingIndex = -1
            } else scroller.scrollTo(keep, 0)
            markPage()
        }
    }

    private fun pageWidth() = scroller.width + dp(14)

    private fun currentPage(): Int {
        if (pages <= 1) return 0
        val max = (row.width - scroller.width).coerceAtLeast(0)
        return if (scroller.scrollX >= max - dp(4)) pages - 1 else (scroller.scrollX + pageWidth() / 2) / pageWidth()
    }

    private fun markPage() {
        val page = currentPage()
        for (i in 0 until minOf(pages, pager.childCount)) {
            pager.getChildAt(i).background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (i == page) GREEN else ShopActivity.CARD); setStroke(dp(2), if (i == page) GREEN else ShopActivity.HONEY_LIGHT)
            }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    companion object {
        val GREEN = ShopActivity.GREEN
        val INK = Color.parseColor("#3E3A2E")
        val INK_SOFT = Color.parseColor("#5E5A4C")

        private fun dp(context: Context, v: Int) = (v * context.resources.displayMetrics.density).roundToInt()

        /** The shop's card: cream, honey edge; [selected] gives it the green edge. */
        fun card(context: Context, selected: Boolean = false, accent: Int? = null) = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8))
            background = GradientDrawable().apply {
                setColor(ShopActivity.CARD); cornerRadius = dp(context, 16).toFloat()
                setStroke(dp(context, if (selected) 3 else 2), if (selected) GREEN else accent ?: ShopActivity.HONEY_LIGHT)
            }
        }

        fun title(context: Context, text: String) = TextView(context).apply {
            this.text = text; textSize = 17f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD); maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(dp(context, 4), dp(context, 6), 0, 0)
        }

        fun chip(context: Context, text: String, fill: Int = ShopActivity.OWNED) = TextView(context).apply {
            this.text = text; textSize = 12f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD); maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(context, 8), dp(context, 2), dp(context, 8), dp(context, 2))
            background = GradientDrawable().apply { setColor(fill); cornerRadius = dp(context, 10).toFloat() }
        }

        fun body(context: Context, text: String, lines: Int = 2) = TextView(context).apply {
            this.text = text; textSize = 13f; setTextColor(INK_SOFT); maxLines = lines
            ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(dp(context, 4), dp(context, 3), dp(context, 2), 0)
            setLineSpacing(0f, 1.1f)
        }

        /** Filled green (main action), outlined (other actions) or beige and disabled. */
        fun button(context: Context, text: String, primary: Boolean, enabled: Boolean = true, click: () -> Unit) = Button(context).apply {
            this.text = text; textSize = 14f; isAllCaps = false; stateListAnimator = null; setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(context, 4), 0, dp(context, 4), 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            val (fill, ink, stroke) = when {
                !enabled -> Triple(ShopActivity.DISABLED, ShopActivity.DISABLED_INK, ShopActivity.DISABLED)
                primary -> Triple(GREEN, Color.WHITE, GREEN)
                else -> Triple(ShopActivity.CARD, GREEN, GREEN)
            }
            setTextColor(ink)
            background = RippleDrawable(ColorStateList.valueOf(ShopActivity.OWNED), GradientDrawable().apply {
                setColor(fill); cornerRadius = dp(context, 14).toFloat(); setStroke(dp(context, 2), stroke)
            }, null)
            isEnabled = enabled
            setOnClickListener { click() }
        }

        fun buttonParams(context: Context, top: Int = 4) =
            LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 38)).apply { topMargin = dp(context, top) }
    }
}
