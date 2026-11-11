package com.tinyrunners.school.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.widget.Button
import com.tinyrunners.school.R
import com.tinyrunners.school.hub.HubActivity
import kotlin.math.roundToInt

/**
 * The small "◀ 처음으로" button at the top-left of the story list, friend selection, shop and settings. It goes
 * back to the menu (hub) the same way from every screen.
 */
object HomeButton {
    fun create(context: Context, label: String = context.getString(R.string.common_to_menu), click: () -> Unit = { goToMenu(context) }): Button =
        Button(context).apply {
            val d = context.resources.displayMetrics.density
            text = label
            textSize = 12f
            isAllCaps = false
            setTextColor(Page.LEAF)
            minHeight = (32 * d).roundToInt(); minimumHeight = (32 * d).roundToInt()
            minWidth = 0; minimumWidth = 0
            setPadding((10 * d).roundToInt(), 0, (10 * d).roundToInt(), 0)
            background = RippleDrawable(ColorStateList.valueOf(Page.MINT), GradientDrawable().apply {
                setColor(Page.PAPER); cornerRadius = 14 * d; setStroke((1 * d).roundToInt().coerceAtLeast(1), Page.LINE)
            }, null)
            stateListAnimator = null
            setOnClickListener { click() }
        }

    /** Back to the menu that is already open under this screen (screens above it close), or a fresh menu. */
    fun goToMenu(context: Context) {
        context.startActivity(Intent(context, HubActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        (context as? Activity)?.finish()
    }
}
