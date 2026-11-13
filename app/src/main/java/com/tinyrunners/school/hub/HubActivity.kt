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
import com.tinyrunners.school.MainActivity
import com.tinyrunners.school.R
import com.tinyrunners.school.game.GameActivity
import com.tinyrunners.school.ui.MockupFrame
import com.tinyrunners.school.talk.PrologueActivity
import kotlin.math.roundToInt

/**
 * First screen after login (the menu), in A's approved design: an open picture book on a pixel wooden desk with the
 * title, the subtitle, the six friends (game sprites, spread out on the meadow), and five buttons in the middle (스토리 진행하기 filled green, the rest cream).
 * 스토리 진행하기 → world story → talks with the village residents. 게임하기 → friend selection. 상점 → the shop.
 * 튜토리얼 → the game guide on a practice run. 설정 → settings.
 */
class HubActivity : Activity() {
    /** The login popup closes right where a button appears; a tap carried over from it must not open anything. */
    private val shownAt = SystemClock.uptimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A's final menu (docs/art/ui-screens/main-menu-final, 2026-11-12): the open picture book with the title, the six friends and a
        // cream gap in the middle where the five buttons sit at the mockup's own positions (layout.json).
        val frame = MockupFrame(this, R.drawable.bg_main_menu_wide)
        // The page's clouds drift slowly behind the tree and the title, fading in and out at the page's edges.
        frame.addView(com.tinyrunners.school.ui.CloudDrift(this, frame, R.drawable.ui_clouds_main_menu, R.drawable.ui_sky_main_menu,
            wrapLeft = 312, wrapRight = 2085, fade = 170))
        fun add(part: Int, label: Int, ink: Int, px: Float, rect: IntArray, click: () -> Unit) =
            frame.place(MockupFrame.partButton(this, frame, part, getString(label), ink, px) {
                if (SystemClock.uptimeMillis() - shownAt >= TAP_GUARD_MS) click()
            }, rect[0], rect[1], rect[2], rect[3])
        add(R.drawable.ui_menu_btn_primary, R.string.hub_story, CREAM, 44f, intArrayOf(610, 317, 1061, 401)) {
            startActivity(PrologueActivity.forStory(this))
        }
        add(R.drawable.ui_menu_btn_secondary, R.string.hub_game, DEEP_GREEN, 42f, intArrayOf(619, 410, 1053, 488)) {
            startActivity(MainActivity.selectionIntent(this))
        }
        add(R.drawable.ui_menu_btn_secondary, R.string.hub_shop, DEEP_GREEN, 42f, intArrayOf(618, 497, 1053, 575)) {
            startActivity(com.tinyrunners.school.shop.ShopActivity.intent(this, null))
        }
        add(R.drawable.ui_menu_btn_secondary, R.string.hub_tutorial, DEEP_GREEN, 42f, intArrayOf(619, 584, 1053, 663)) {
            // A practice run with 라일 that always starts with the game guide.
            startActivity(GameActivity.intent(this, "mint").putExtra(GameActivity.EXTRA_SHOW_GUIDE, true))
        }
        add(R.drawable.ui_menu_btn_secondary, R.string.hub_settings, DEEP_GREEN, 42f, intArrayOf(619, 672, 1053, 750)) {
            startActivity(android.content.Intent(this, com.tinyrunners.school.settings.SettingsActivity::class.java))
        }
        // The six friends from their game sprites (the picture's own drawings were taken out), three each side.
        val friends = com.tinyrunners.school.data.CharacterRepository(this).characters
        FRIEND_X.forEachIndexed { i, cx ->
            val id = friends.getOrNull(i)?.id ?: return@forEachIndexed
            frame.place(com.tinyrunners.school.opening.OpeningFriend(this, id, i), cx - 85, FRIEND_FEET - 170, cx + 85, FRIEND_FEET)
        }
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
        /** Friend centres and feet line on the 1672×941 canvas (close together as in A's picture, clear of the buttons). */
        val FRIEND_X = intArrayOf(183, 336, 497, 1176, 1350, 1524)
        const val FRIEND_FEET = 758
        /** Text colours from layout.json. */
        private val CREAM = Color.parseColor("#FFF8E7")
        private val DEEP_GREEN = Color.parseColor("#1E5A2A")
        private const val TAP_GUARD_MS = 600L

        /** Opens the hub as the only screen in the task (after login it replaces the opening and popups). */
        fun intent(context: Context) = Intent(context, HubActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
