package com.tinyrunners.school.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.ui.MockupFrame

/**
 * The rest card shown when the player taps 정지, from A's separated pack (panels/pause-1 over the paused run): the
 * road dims, and a cream card says 천천히, 숨을 골라요 / 잠깐 쉬어가요 with the friend breathing in the middle, then
 * 계속 달리기 (resume) and two small links, 친구 바꾸기 and 집으로 돌아가기.
 */
@SuppressLint("ViewConstructor")
class PausePanel(
    context: Context,
    friendId: String,
    private val onResume: () -> Unit,
    onChangeFriend: () -> Unit,
    onHome: () -> Unit,
) : MockupFrame(context, 0) {
    init {
        setBackgroundColor(Color.argb(150, 34, 28, 44))
        isClickable = true   // taps on the dimmed road do not reach the run
        val v = MockupFrame.Companion::v2
        placeV2(ImageView(context).apply {
            setImageResource(R.drawable.ui_v2_pause_panel); scaleType = ImageView.ScaleType.FIT_XY
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, 408, 96, 872, 591)
        fun text(px: Int, color: Int, bold: Boolean = false) = TextView(context).apply {
            setTextColor(color); gravity = Gravity.CENTER; includeFontPadding = false
            if (bold) typeface = Typeface.DEFAULT_BOLD
            onScaleText(this, v(px).toFloat())
        }
        placeV2(text(13, SOFT).apply { setText(R.string.pause_eyebrow); letterSpacing = 0.1f }, 440, 140, 840, 160)
        placeV2(text(30, INK, bold = true).apply {
            setText(R.string.pause_title)
            if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        }, 440, 168, 840, 212)
        placeV2(com.tinyrunners.school.opening.OpeningFriend(context, friendId, 0), 590, 226, 690, 326)
        placeV2(text(15, INK).apply { setText(R.string.pause_body); setLineSpacing(0f, 1.35f) }, 440, 340, 840, 410)
        placeV2(Button(context).apply {
            setText(R.string.pause_continue); isAllCaps = false; stateListAnimator = null; setTextColor(CREAM)
            typeface = Typeface.DEFAULT_BOLD; setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), pill(GREEN_DARK))
                addState(intArrayOf(), pill(GREEN))
            }
            setOnClickListener { close() }
            onScaleText(this, v(18).toFloat())
        }, 460, 447, 820, 501)
        fun link(label: Int, click: () -> Unit) = Button(context).apply {
            setText(label); isAllCaps = false; stateListAnimator = null; setTextColor(LINK); setPadding(0, 0, 0, 0)
            minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            background = android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x22000000), null,
                android.graphics.drawable.ColorDrawable(Color.WHITE))
            setOnClickListener { click() }
            onScaleText(this, v(14).toFloat())
        }
        placeV2(link(R.string.pause_change_friend, onChangeFriend), 512, 512, 632, 556)
        placeV2(text(14, LINK).apply { text = "·" }, 632, 512, 648, 556)
        placeV2(link(R.string.pause_home, onHome), 648, 512, 778, 556)
    }

    /** Back to the run (also the system back key while the card is open). */
    fun close() {
        (parent as? android.view.ViewGroup)?.removeView(this)
        onResume()
    }

    private fun pill(color: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = 18f * resources.displayMetrics.density
    }

    private companion object {
        val INK = Color.rgb(68, 60, 49)
        val SOFT = Color.rgb(120, 110, 88)
        val LINK = Color.rgb(112, 99, 71)
        val CREAM = Color.rgb(255, 253, 241)
        val GREEN = Color.rgb(52, 90, 62)
        val GREEN_DARK = Color.rgb(38, 70, 48)
    }
}
