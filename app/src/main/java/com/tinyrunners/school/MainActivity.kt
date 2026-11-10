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
        // Talk with the chosen friend first; the talk screen starts the run.
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
