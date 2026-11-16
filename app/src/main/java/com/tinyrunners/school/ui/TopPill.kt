package com.tinyrunners.school.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * The top-bar pill from A's approved story-collection design (ui_story_back_button / ui_story_counter_pill): a warm
 * cream pill with a light brown edge and a darker brown lip along the bottom, deep green bold words. Every worded
 * button in a screen's top bar (◀ 처음으로, ◀ 돌아가기, the selection's 상점·스토리 진행·내 기록·설정, coin counters)
 * wears it; their words stay as they were. [divider] is the thin upright line that stands before the bag when
 * another button sits beside it, as in the design.
 */
object TopPill {
    val FILL = Color.rgb(252, 229, 186)
    val EDGE = Color.rgb(184, 142, 96)
    val LIP = Color.rgb(132, 82, 36)
    val INK = Color.parseColor("#1E5A2A")

    private fun dp(context: Context, v: Float) = (v * context.resources.displayMetrics.density).roundToInt()

    /** The pill picture: the lip underneath, the cream face on top of it. */
    fun background(context: Context, pressable: Boolean = true): Drawable {
        val r = dp(context, 100f).toFloat()
        val lip = GradientDrawable().apply { setColor(LIP); cornerRadius = r }
        val face = GradientDrawable().apply { setColor(FILL); cornerRadius = r; setStroke(dp(context, 2f), EDGE) }
        val layers = LayerDrawable(arrayOf(lip, face)).apply { setLayerInset(1, 0, 0, 0, dp(context, 2.5f)) }
        return if (pressable) RippleDrawable(ColorStateList.valueOf(Color.argb(40, 120, 80, 30)), layers, null) else layers
    }

    /** Dresses a worded top-bar button (or counter) as the pill; its text is left as it is. */
    fun style(view: TextView, pressable: Boolean = true, sizeSp: Float = 14f) {
        val c = view.context
        view.background = background(c, pressable)
        view.setTextColor(INK)
        view.setTypeface(Typeface.DEFAULT_BOLD)
        view.textSize = sizeSp
        view.isAllCaps = false
        view.stateListAnimator = null
        view.minHeight = dp(c, 38f); view.minimumHeight = dp(c, 38f)
        view.minWidth = 0; view.minimumWidth = 0
        view.setPadding(dp(c, 16f), dp(c, 2f), dp(c, 16f), dp(c, 4f))
        view.gravity = android.view.Gravity.CENTER
    }

    /** The upright line before the bag. */
    fun divider(context: Context): View = View(context).apply {
        setBackgroundColor(INK)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        layoutParams = LinearLayout.LayoutParams(dp(context, 2f), dp(context, 30f)).apply {
            marginStart = dp(context, 12f); marginEnd = dp(context, 2f)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
    }
}
