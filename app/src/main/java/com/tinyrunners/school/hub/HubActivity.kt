package com.tinyrunners.school.hub

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.MainActivity
import com.tinyrunners.school.R
import com.tinyrunners.school.game.GameActivity
import com.tinyrunners.school.ui.MockupFrame
import com.tinyrunners.school.talk.PrologueActivity
import kotlin.math.roundToInt

/**
 * First screen after login (the menu), in A's separated design pack: 라일's home under a hanging sign with the title,
 * the tagline, four menu buttons and the six friends in a row on the grass.
 * 스토리 진행하기 → friend selection (the story is told while running). 상점 → the shop.
 * 튜토리얼 → the game guide on a practice run. 설정 → settings.
 */
class HubActivity : Activity() {
    /** The login popup closes right where a button appears; a tap carried over from it must not open anything. */
    private val shownAt = SystemClock.uptimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A's separated design pack (2026-11-16, reference/screen-0): 라일's home under a hanging wooden sign with the
        // title, the tagline below it, the menu buttons in the middle and the six friends in a row on the grass.
        val frame = MockupFrame(this, R.drawable.bg_v2_main_wide)
        frame.placeV2(TextView(this).apply {
            setText(R.string.app_name); setTextColor(SIGN_INK); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            includeFontPadding = false; setShadowLayer(0f, 0f, 2f, Color.argb(90, 255, 240, 200))
        }.also { frame.onScaleText(it, MockupFrame.v2(60).toFloat()) }, 340, 78, 940, 164)
        frame.placeV2(TextView(this).apply {
            setText(R.string.hub_tagline_v2); setTextColor(TAGLINE); gravity = Gravity.CENTER; includeFontPadding = false
            setShadowLayer(4f, 0f, 0f, Color.argb(200, 255, 248, 230))
        }.also { frame.onScaleText(it, MockupFrame.v2(17).toFloat()) }, 380, 196, 900, 226)
        fun add(label: Int, top: Int, click: () -> Unit) = frame.placeV2(Button(this).apply {
            setText(label); isAllCaps = false; stateListAnimator = null; setTextColor(MENU_INK); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            background = android.graphics.drawable.StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), getDrawable(R.drawable.ui_v2_btn_menu_pressed))
                addState(intArrayOf(), getDrawable(R.drawable.ui_v2_btn_menu))
            }
            setOnClickListener { if (SystemClock.uptimeMillis() - shownAt >= TAP_GUARD_MS) click() }
        }.also { frame.onScaleText(it, MockupFrame.v2(18).toFloat()) }, 481, top - 4, 803, top + 54)
        // 스토리 진행하기 is the game now: choose a friend, then the story is told while running (게임하기 merged in).
        add(R.string.hub_story, 246) { startActivity(MainActivity.selectionIntent(this)) }
        add(R.string.hub_shop, 302) { startActivity(com.tinyrunners.school.shop.ShopActivity.intent(this, null)) }
        add(R.string.hub_tutorial, 358) {
            // A practice run with 라일 that always starts with the game guide.
            startActivity(GameActivity.intent(this, "mint").putExtra(GameActivity.EXTRA_SHOW_GUIDE, true))
        }
        add(R.string.hub_settings, 414) {
            startActivity(android.content.Intent(this, com.tinyrunners.school.settings.SettingsActivity::class.java))
        }
        // The six friends from their game sprites, in a row on the grass (reference/screen-0).
        val friends = com.tinyrunners.school.data.CharacterRepository(this).characters
        FRIEND_X.forEachIndexed { i, cx ->
            val id = friends.getOrNull(i)?.id ?: return@forEachIndexed
            frame.placeV2(com.tinyrunners.school.opening.OpeningFriend(this, id, i), cx - 40, FRIEND_FEET - 80, cx + 40, FRIEND_FEET)
        }
        // The line at the bottom on a soft green band.
        frame.placeV2(TextView(this).apply {
            setText(R.string.hub_footer_v2); setTextColor(Color.rgb(244, 236, 212)); gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(190, 52, 78, 52))
        }.also { frame.onScaleText(it, MockupFrame.v2(12).toFloat()) }, 0, 690, 1280, 720)
        setContentView(frame)
        hideSystemBars()
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    companion object {
        /** Friend centres and feet line in the pack's 1280×720 coordinates (reference/screen-0). */
        val FRIEND_X = intArrayOf(185, 365, 548, 722, 905, 1083)
        const val FRIEND_FEET = 670
        /** Colours from the pack's manifest. */
        private val SIGN_INK = Color.rgb(74, 92, 58)
        private val TAGLINE = Color.rgb(96, 84, 64)
        private val MENU_INK = Color.rgb(56, 82, 62)
        private const val TAP_GUARD_MS = 600L

        /** Opens the hub as the only screen in the task (after login it replaces the opening and popups). */
        fun intent(context: Context) = Intent(context, HubActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
