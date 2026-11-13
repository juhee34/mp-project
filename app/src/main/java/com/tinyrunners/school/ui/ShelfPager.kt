package com.tinyrunners.school.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.tinyrunners.school.R
import com.tinyrunners.school.shop.ShopActivity
import kotlin.math.roundToInt

/**
 * Page dots with a ▶ (right of the dots, next page) that turns into a ◀ (left of the dots, back) on the last page,
 * for any sideways [scroller] — the same look and behaviour as [CardShelf]'s, for screens that lay their cards out
 * themselves (the story collection). One page is the scroller's width. Hidden when everything fits.
 */
class ShelfPager(context: Context, private val scroller: HorizontalScrollView) : LinearLayout(context) {
    private var pages = 1
    private val back = arrow("◀", R.string.shelf_prev_page) { go(-1) }
    private val next = arrow("▶", R.string.shelf_next_page) { go(1) }
    private val dots = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER }

    init {
        orientation = HORIZONTAL; gravity = Gravity.CENTER
        addView(back, LayoutParams(dp(30), dp(30)).apply { marginEnd = dp(22) })
        addView(dots)
        addView(next, LayoutParams(dp(30), dp(30)).apply { marginStart = dp(22) })
        scroller.setOnScrollChangeListener { _, _, _, _, _ -> mark() }
        scroller.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> post { refresh() } }
    }

    /** Counts the pages again (call after the cards change). */
    fun refresh() {
        val content = scroller.getChildAt(0)?.width ?: 0
        val view = scroller.width
        val count = if (view <= 0) 1 else maxOf(1, (content + view - dp(8)) / view)
        if (count != pages || dots.childCount != count) {
            pages = count
            dots.removeAllViews()
            repeat(pages) { i ->
                dots.addView(View(context).apply { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO },
                    LayoutParams(dp(12), dp(12)).apply { if (i > 0) marginStart = dp(12) })
            }
        }
        visibility = if (pages > 1) View.VISIBLE else View.INVISIBLE
        mark()
    }

    private fun page(): Int {
        if (pages <= 1) return 0
        val max = ((scroller.getChildAt(0)?.width ?: 0) - scroller.width).coerceAtLeast(0)
        return if (scroller.scrollX >= max - dp(4)) pages - 1 else ((scroller.scrollX + scroller.width / 2) / scroller.width.coerceAtLeast(1))
    }

    private fun go(step: Int) {
        val target = (page() + step).coerceIn(0, pages - 1)
        scroller.smoothScrollTo(target * scroller.width, 0)
    }

    private fun mark() {
        val page = page()
        val last = pages > 1 && page >= pages - 1
        next.visibility = if (last) View.GONE else View.VISIBLE
        back.visibility = if (last) View.VISIBLE else View.GONE
        for (i in 0 until dots.childCount) {
            dots.getChildAt(i).background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (i == page) CardShelf.GREEN else ShopActivity.CARD)
                setStroke(dp(2), if (i == page) CardShelf.GREEN else ShopActivity.HONEY_LIGHT)
            }
        }
    }

    private fun arrow(symbol: String, description: Int, click: () -> Unit) = Button(context).apply {
        text = symbol; textSize = 13f; setTextColor(CardShelf.GREEN); stateListAnimator = null; setPadding(0, 0, 0, 0)
        minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
        contentDescription = context.getString(description)
        background = RippleDrawable(ColorStateList.valueOf(ShopActivity.OWNED), GradientDrawable().apply {
            setColor(ShopActivity.CARD); cornerRadius = dp(8).toFloat(); setStroke(dp(2), CardShelf.GREEN)
        }, null)
        setOnClickListener { click() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
}
