package com.tinyrunners.school.story

import android.app.Activity
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
import com.tinyrunners.school.ui.MockupFrame
import kotlin.math.roundToInt

/**
 * The bookshelf screen shared by 이야기 모음 and 이야기 도감 (A's separated pack, reference/screen-4): the forest
 * picture, "← back | title" at the top-left and the bag at the top-right, a big line and a small one with a counter,
 * the books in a row on the shelf, and a round ◀ ▶ arrow on each side (like the friend selection) that slides the
 * shelf by one book.
 */
internal class BookShelf(private val activity: Activity, backLabel: String, title: String, onBack: () -> Unit) {
    val frame = MockupFrame(activity, 0)
    val headline: TextView
    val subline: TextView
    val counter: TextView
    val footer: TextView
    private val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
    private val scroller = HorizontalScrollView(activity).apply { isHorizontalScrollBarEnabled = false; clipToPadding = false; addView(row) }
    private val prev = arrow("◀", R.string.shelf_prev) { slide(-1) }
    private val next = arrow("▶", R.string.shelf_next) { slide(+1) }
    private var shelfLeft = 0
    private var shelfRight = 1672
    private var books: List<View> = emptyList()

    init {
        val v2 = MockupFrame.Companion::v2
        frame.addView(com.tinyrunners.school.ui.CoverPicture(activity, R.drawable.bg_v2_stories_wide, sideInset = 0.1f))
        frame.placeV2(Button(activity).apply {
            text = backLabel; isAllCaps = false; stateListAnimator = null; setTextColor(LIGHT)
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0; setPadding(0, 0, 0, 0)
            background = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), null, android.graphics.drawable.ColorDrawable(Color.WHITE))
            maxLines = 1; isSingleLine = true
            setOnClickListener { onBack() }
        }.also { frame.onScaleText(it, v2(26).toFloat()) }, 30, 26, 210, 76)
        frame.placeV2(text(32, CREAM_TEXT, bold = true).apply { text = title }, 216, 26, 640, 76)
        headline = text(30, CREAM_TEXT, bold = true).apply { setShadowLayer(6f, 0f, 2f, Color.argb(120, 0, 0, 0)) }
        frame.placeV2(headline, 76, 108, 860, 158)
        subline = text(24, LIGHT)
        frame.placeV2(subline, 76, 160, 1000, 196)
        counter = text(22, LIGHT).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        frame.placeV2(counter, 860, 118, 1200, 150)
        frame.place(android.widget.ImageButton(activity).apply {
            setImageDrawable(null)
            background = RippleDrawable(ColorStateList.valueOf(0x22000000), activity.getDrawable(R.drawable.ui_story_bag_button), null)
            contentDescription = activity.getString(R.string.bag_open)
            setOnClickListener { activity.startActivity(com.tinyrunners.school.quest.BagActivity.intent(activity)) }
        }, v2(1198), v2(24), v2(1248), v2(74))

        frame.placeV2(scroller, 0, SHELF_TOP, 1280, SHELF_TOP + 392)
        frame.placeV2(prev, 10, ARROW_TOP, 86, ARROW_TOP + 76)
        frame.placeV2(next, 1194, ARROW_TOP, 1270, ARROW_TOP + 76)
        frame.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            // The shelf runs from screen edge to screen edge on phones wider than 16:9; the arrows sit at those edges.
            val left = frame.visibleLeft.roundToInt(); val right = frame.visibleRight.roundToInt()
            if (left != shelfLeft || right != shelfRight) {
                shelfLeft = left; shelfRight = right
                frame.post {
                    // The books stay between the two arrows, so an arrow never covers a book.
                    val a = v2(76); val gap = v2(12)
                    frame.move(scroller, left + gap * 2 + a, v2(SHELF_TOP), right - gap * 2 - a, v2(SHELF_TOP + 392))
                    frame.move(prev, left + gap, v2(ARROW_TOP), left + gap + a, v2(ARROW_TOP) + a)
                    frame.move(next, right - gap - a, v2(ARROW_TOP), right - gap, v2(ARROW_TOP) + a)
                    setBooks(books)
                }
            }
        }
        footer = text(22, LIGHT)
        frame.placeV2(footer, 80, 662, 1200, 698)
        scroller.setOnScrollChangeListener { _, _, _, _, _ -> updateArrows() }
    }

    /** Puts [views] on the shelf, the first one where the mockup's first book stands. */
    fun setBooks(views: List<View>) {
        books = views
        row.removeAllViews()
        row.addView(RefBox(activity, frame, MockupFrame.v2(8), 1))
        views.forEach { (it.parent as? android.view.ViewGroup)?.removeView(it); row.addView(it) }
        row.addView(RefBox(activity, frame, MockupFrame.v2(8), 1))
        scroller.post { updateArrows() }
    }

    fun scrollToStart() = scroller.post { scroller.scrollTo(0, 0) }

    /** One book further (or back), sliding sideways like the friends on the selection screen. */
    private fun slide(step: Int) {
        scroller.smoothScrollBy((step * MockupFrame.v2(276) * frame.scale).roundToInt(), 0)
    }

    /** An arrow is pale and does nothing at its end of the shelf (and both hide when every book fits). */
    private fun updateArrows() {
        for ((arrow, more) in listOf(prev to scroller.canScrollHorizontally(-1), next to scroller.canScrollHorizontally(1))) {
            arrow.isEnabled = more
            arrow.alpha = if (more) 1f else 0.35f
        }
    }

    private fun text(px: Int, color: Int, bold: Boolean = false) = TextView(activity).apply {
        setTextColor(color); includeFontPadding = false; maxLines = 1; gravity = Gravity.CENTER_VERTICAL
        ellipsize = android.text.TextUtils.TruncateAt.END
        if (bold) typeface = Typeface.DEFAULT_BOLD
        frame.onScaleText(this, MockupFrame.v2(px).toFloat())
    }

    /** A round cream arrow with a green edge, the same as on the friend selection screen. */
    private fun arrow(symbol: String, description: Int, click: () -> Unit) = Button(activity).apply {
        text = symbol; setTextColor(GREEN); contentDescription = activity.getString(description)
        setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0; stateListAnimator = null
        val d = activity.resources.displayMetrics.density
        background = RippleDrawable(ColorStateList.valueOf(0x33466F53), GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setColor(Color.argb(235, 255, 249, 235)); setStroke((2.5f * d).roundToInt(), GREEN)
        }, null)
        frame.onScaleText(this, MockupFrame.v2(30).toFloat())
        setOnClickListener { click() }
    }

    companion object {
        /** Top of the book shelf in the pack's 1280×720 coordinates. */
        const val SHELF_TOP = 225
        /** The arrows sit beside the middle of the books. */
        private const val ARROW_TOP = SHELF_TOP + 158
        val LIGHT = Color.rgb(236, 230, 206)
        val CREAM_TEXT = Color.rgb(255, 246, 222)
        private val GREEN = com.tinyrunners.school.ui.CardShelf.GREEN
    }
}
