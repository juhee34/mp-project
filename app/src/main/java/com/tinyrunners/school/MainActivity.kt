package com.tinyrunners.school

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import com.tinyrunners.school.auth.AuthService
import com.tinyrunners.school.auth.LoginActivity
import com.tinyrunners.school.records.RecordsActivity
import com.tinyrunners.school.settings.SettingsActivity
import android.widget.FrameLayout
import android.widget.TextView
import com.tinyrunners.school.game.GameActivity
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        characterScreens?.save(outState)
        super.onSaveInstanceState(outState)
    }

    private fun onStartRun(characterId: String) {
        startActivity(GameActivity.intent(this, characterId))
    }

    @Deprecated("Uses the Activity back callback for the character screen stack.")
    override fun onBackPressed() {
        if (characterScreens?.goBack() != true) super.onBackPressed()
    }
}
