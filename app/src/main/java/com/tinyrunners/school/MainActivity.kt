package com.tinyrunners.school

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import com.tinyrunners.school.auth.AuthService
import com.tinyrunners.school.auth.LoginActivity
import com.tinyrunners.school.records.RecordsActivity
import com.tinyrunners.school.settings.SettingsActivity
import com.tinyrunners.school.story.StoryListActivity
import android.widget.FrameLayout
import android.widget.TextView
import com.tinyrunners.school.talk.TalkActivity
import com.tinyrunners.school.characters.CharacterScreens
import com.tinyrunners.school.data.CharacterRepository

class MainActivity : Activity() {
    private var characterScreens: CharacterScreens? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Login gate: the launcher stays MainActivity so the existing screen flow is unchanged.
        if (!AuthService.hasSession(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.menu_shop).setOnClickListener {
            startActivity(com.tinyrunners.school.shop.ShopActivity.intent(this, characterScreens?.currentFriendId))
        }
        findViewById<TextView>(R.id.coin_badge).apply {
            background = coinBackground()
            setCompoundDrawablesRelative(coinIcon(), null, null, null)
            compoundDrawablePadding = (6 * resources.displayMetrics.density).toInt()
            setOnClickListener { startActivity(com.tinyrunners.school.shop.ShopActivity.intent(this@MainActivity, characterScreens?.currentFriendId)) }
        }
        findViewById<View>(R.id.menu_stories).setOnClickListener { startActivity(Intent(this, StoryListActivity::class.java)) }
        findViewById<View>(R.id.menu_records).setOnClickListener { startActivity(Intent(this, RecordsActivity::class.java)) }
        findViewById<View>(R.id.menu_settings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        val host = findViewById<FrameLayout>(R.id.screen_host)
        findViewById<View>(R.id.main_root).setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
            }
            insets
        }
        val repository = try {
            CharacterRepository(this)
        } catch (_: Exception) {
            host.addView(TextView(this).apply { setText(R.string.characters_load_error) })
            return
        }
        characterScreens = CharacterScreens(host, repository, ::onStartRun).also {
            it.restore(savedInstanceState)
        }
        openRequestedSelection()
    }

    override fun onResume() {
        super.onResume()
        refreshCoins()
    }

    /** Coins change after runs and purchases, so the badge is re-read every time this screen comes back. */
    private fun refreshCoins() {
        val badge = findViewById<TextView>(R.id.coin_badge) ?: return
        // The coins live in the bag together with the friends' items.
        badge.setOnClickListener { startActivity(com.tinyrunners.school.quest.BagActivity.intent(this)) }
        com.tinyrunners.school.shop.ShopStore.forCurrentUser(this).load { result ->
            if (isDestroyed) return@load
            result.onSuccess { wallet ->
                badge.text = getString(R.string.shop_coins, wallet.coins)
                badge.contentDescription = getString(R.string.coin_badge_description, wallet.coins)
            }
        }
    }

    private fun coinBackground() = android.graphics.drawable.GradientDrawable().apply {
        val d = resources.displayMetrics.density
        setColor(android.graphics.Color.rgb(255, 247, 220))
        setStroke((2 * d).toInt(), android.graphics.Color.rgb(246, 200, 80))
        cornerRadius = 20 * d
    }

    private fun coinIcon() = android.graphics.drawable.GradientDrawable().apply {
        val d = resources.displayMetrics.density
        shape = android.graphics.drawable.GradientDrawable.OVAL
        setColor(android.graphics.Color.rgb(246, 200, 80))
        setStroke((2 * d).toInt(), android.graphics.Color.rgb(140, 96, 20))
        setBounds(0, 0, (16 * d).toInt(), (16 * d).toInt())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openRequestedSelection()
    }

    private fun openRequestedSelection() {
        if (intent.getBooleanExtra(EXTRA_SHOW_SELECTION, false)) {
            intent.removeExtra(EXTRA_SHOW_SELECTION)
            characterScreens?.showSelection()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        characterScreens?.save(outState)
        super.onSaveInstanceState(outState)
    }

    private fun onStartRun(characterId: String) {
        // Every run opens with a talk: the long one the first time with a friend, short lines afterwards.
        startActivity(TalkActivity.intent(this, characterId))
    }

    @Deprecated("Uses the Activity back callback for the character screen stack.")
    override fun onBackPressed() {
        if (characterScreens?.goBack() != true) super.onBackPressed()
    }

    companion object {
        private const val EXTRA_SHOW_SELECTION = "showCharacterSelection"

        fun selectionIntent(context: Context) = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_SHOW_SELECTION, true)
    }
}
