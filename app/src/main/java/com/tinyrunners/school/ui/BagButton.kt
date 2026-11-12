package com.tinyrunners.school.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.tinyrunners.school.R
import com.tinyrunners.school.quest.BagActivity
import kotlin.math.roundToInt

/**
 * The bag (가방) button: a small drawstring pouch at the top-right of most screens (not the menu or settings).
 * Opens [BagActivity] with the coins and the friends' items.
 */
object BagButton {
    fun create(context: Context): View = PouchButtonView(context).apply {
        val d = context.resources.displayMetrics.density
        contentDescription = context.getString(R.string.bag_open)
        isClickable = true; isFocusable = true
        background = GradientDrawable().apply {
            setColor(Color.rgb(255, 250, 238)); cornerRadius = 12 * d; setStroke((2 * d).roundToInt(), Color.rgb(190, 150, 100))
        }
        setOnClickListener { context.startActivity(BagActivity.intent(context)) }
        val size = (40 * d).roundToInt()
        // It goes last in a top row, at the right end.
        layoutParams = LinearLayout.LayoutParams(size, size).apply { marginStart = (10 * d).roundToInt() }
    }

    /** Floats the bag button in [frame]'s top-right corner, below the status bar. */
    fun floatIn(frame: android.widget.FrameLayout): View = create(frame.context).also { b ->
        val d = frame.resources.displayMetrics.density
        fun dp(v: Int) = (v * d).roundToInt()
        frame.addView(b, android.widget.FrameLayout.LayoutParams(dp(40), dp(40), android.view.Gravity.TOP or android.view.Gravity.END).apply {
            topMargin = dp(12); marginEnd = dp(16)
        })
        b.setOnApplyWindowInsetsListener { v, insets ->
            val (top, right) = if (android.os.Build.VERSION.SDK_INT >= 30) {
                insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout()).let { it.top to it.right }
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetTop to insets.systemWindowInsetRight
            }
            (v.layoutParams as android.widget.FrameLayout.LayoutParams).apply { topMargin = top + dp(12); marginEnd = right + dp(16) }
            v.requestLayout()
            insets
        }
        b.requestApplyInsets()
    }

    /** Puts the bag button at the right end of [row] (for rows built from XML). */
    fun appendTo(row: ViewGroup) {
        row.addView(create(row.context))
    }
}

@SuppressLint("ViewConstructor")
private class PouchButtonView(context: Context) : View(context) {
    override fun onDraw(canvas: Canvas) {
        val inset = width * 0.14f
        PouchIcon.draw(canvas, inset, inset, width - inset * 2)
    }
}

/**
 * A 16×16 pixel drawstring sack from the approved shop design (docs/art/shop/approved/shop-approved.png): a cream
 * cloth bag with a ruffled top, a brown tie at the neck and a small lavender flower on the front.
 */
object PouchIcon {
    private val paint = Paint().apply { isAntiAlias = false }
    private val ROWS = listOf(
        ".....bb..bb.....",
        "....bWWbbWWb....",
        ".....bWWWWb.....",
        "......tttt......",
        ".....btWWtb.....",
        "....bWWWWWWb....",
        "...bWWWWWWWWb...",
        "..bWWWWWWWWWSb..",
        ".bWWWWWpWWWWWSb.",
        ".bWWWWpPpWWWWSb.",
        ".bWWWWWpWWWWWSb.",
        ".bWWWWWWWWWWWSb.",
        ".bSWWWWWWWWWSSb.",
        "..bSSSSSSSSSSb..",
        "...bbbbbbbbbb...",
        "................",
    )
    private val PALETTE = mapOf(
        'b' to Color.rgb(138, 106, 74), 'W' to Color.rgb(247, 239, 222), 'S' to Color.rgb(224, 206, 174),
        't' to Color.rgb(150, 104, 62),
        'p' to Color.rgb(172, 146, 196), 'P' to Color.rgb(230, 218, 244),
    )

    fun draw(canvas: Canvas, left: Float, top: Float, size: Float) {
        val px = size / 16f
        ROWS.forEachIndexed { r, line ->
            line.forEachIndexed { c, ch ->
                paint.color = PALETTE[ch] ?: return@forEachIndexed
                canvas.drawRect(left + c * px, top + r * px, left + (c + 1) * px, top + (r + 1) * px, paint)
            }
        }
    }
}
