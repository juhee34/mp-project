package com.tinyrunners.school.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.hub.HubActivity
import com.tinyrunners.school.ui.MockupFrame
import com.tinyrunners.school.ui.Page
import kotlin.math.roundToInt

/**
 * Login, in A's approved design (docs/art/ui-screens/approved/mockup_login.png), as a popup: the opening already shows
 * the same meadow, so this window only dims it and raises the paper card (cut out of the approved picture,
 * res/drawable-nodpi/login_card.png) with the e-mail and password fields and the 로그인 / 회원가입 buttons at the
 * mockup's positions (layout.json). The card lands exactly on the opening's card.
 *
 * The flow is unchanged: when a session already exists the card first offers "이 계정으로 계속하기" (and
 * "다른 계정으로 로그인" shows the form), so returning players do not retype their password.
 */
class LoginActivity : Activity() {
    private lateinit var frame: MockupFrame
    private lateinit var dim: View
    private var closing = false
    private lateinit var error: TextView
    private lateinit var submit: Button
    private lateinit var second: Button
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var emailLabel: TextView
    private lateinit var passwordLabel: TextView
    private lateinit var account: TextView
    private var showingForm = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        frame = MockupFrame(this, 0)
        frame.place(android.widget.ImageView(this).apply {
            setImageResource(R.drawable.login_card); scaleType = android.widget.ImageView.ScaleType.FIT_XY
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, CARD[0], CARD[1], CARD[2], CARD[3])

        frame.place(com.tinyrunners.school.ui.HomeButton.create(this, getString(R.string.common_back_pill)) { close() }.apply {
            setTextColor(DEEP_GREEN); typeface = Typeface.DEFAULT_BOLD
        }.also { frame.onScaleText(it, 28f) }, 30, 26, 270, 92)

        emailLabel = label(R.string.auth_email).also { frame.place(it, 584, 340, 1089, 384) }
        email = field(R.string.auth_email, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
            .also { frame.place(it, 582, 389, 1089, 458) }
        passwordLabel = label(R.string.auth_password).also { frame.place(it, 584, 470, 1089, 514) }
        password = field(R.string.auth_password, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            .also { frame.place(it, 582, 519, 1089, 589) }
        // With a session: who is signed in, on the same paper strip as the fields.
        account = TextView(this).apply {
            setTextColor(INK); gravity = Gravity.CENTER_VERTICAL; maxLines = 2
            background = getDrawable(R.drawable.ui_login_input)
            setPadding(dp(14), 0, dp(14), 0)
        }.also { frame.onScaleText(it, 26f); frame.place(it, 582, 389, 1089, 470) }
        error = TextView(this).apply { setTextColor(Page.ERROR); gravity = Gravity.CENTER; maxLines = 2 }
            .also { frame.onScaleText(it, 22f); frame.place(it, 584, 592, 1089, 630) }

        submit = MockupFrame.partButton(this, frame, R.drawable.ui_login_btn_primary, "", CREAM, 38f) { primaryClick() }
            .also { frame.place(it, 583, 632, 1090, 712) }
        second = MockupFrame.partButton(this, frame, R.drawable.ui_login_btn_secondary, "", DEEP_GREEN, 38f) { secondClick() }
            .also { frame.place(it, 580, 731, 1091, 808) }

        dim = View(this).apply { setBackgroundColor(Color.argb(120, 30, 34, 24)); alpha = 0f; setOnClickListener { } }
        setContentView(android.widget.FrameLayout(this).apply {
            addView(dim, android.widget.FrameLayout.LayoutParams(-1, -1))
            addView(frame, android.widget.FrameLayout.LayoutParams(-1, -1))
        })
        hideSystemBars()
        // The card rises from below while the opening dims (first time only, not after rotation).
        if (savedInstanceState == null) {
            frame.visibility = View.INVISIBLE
            frame.post {
                frame.translationY = frame.height.toFloat()
                frame.visibility = View.VISIBLE
                frame.animate().translationY(0f).setDuration(RISE_MS).setInterpolator(android.view.animation.DecelerateInterpolator(2f)).start()
                dim.animate().alpha(1f).setDuration(RISE_MS).start()
            }
        } else dim.alpha = 1f
        showingForm = savedInstanceState?.getBoolean("form") ?: !AuthService.hasSession(this)
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("form", showingForm)
    }

    /** Session card (continue / other account) or the form (e-mail, password, 로그인, 회원가입). */
    private fun render() {
        val form = showingForm
        listOf<View>(emailLabel, email, passwordLabel, password).forEach { it.visibility = if (form) View.VISIBLE else View.GONE }
        account.visibility = if (form) View.GONE else View.VISIBLE
        if (!form) {
            emailLabel.visibility = View.VISIBLE
            emailLabel.setText(R.string.login_current_label)
            account.text = if (AuthService.isGuest(this)) getString(R.string.settings_guest)
                else getString(R.string.login_current, AuthService.currentName(this) ?: getString(R.string.settings_no_name),
                    AuthService.currentEmail(this).orEmpty())
            submit.setText(R.string.login_continue)
            second.setText(R.string.login_other_account_button)
        } else {
            emailLabel.setText(R.string.auth_email)
            submit.setText(R.string.login_submit)
            second.setText(R.string.login_to_signup)
            if (!AuthService.isConfigured(this)) {
                // No Firebase settings in this build: say so and offer the guest mode instead of 회원가입.
                submit.isEnabled = false; submit.alpha = 0.5f
                error.setText(R.string.auth_not_configured)
                second.setText(R.string.auth_guest)
            }
        }
    }

    private fun primaryClick() {
        if (!showingForm) { openMain(this); return }
        val e = email.text.toString().trim()
        val p = password.text.toString()
        AuthInput.loginError(e, p)?.let { error.text = getString(it); return }
        busy(true)
        AuthService.signIn(this, e, p) { message ->
            if (isDestroyed) return@signIn
            busy(false)
            if (message == null) openMain(this) else error.text = message
        }
    }

    private fun secondClick() {
        when {
            !showingForm -> { showingForm = true; error.text = ""; render(); email.requestFocus() }
            !AuthService.isConfigured(this) -> { AuthService.enterGuest(this); openMain(this) }
            else -> startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    /** The card sinks back down and the popup closes, leaving the opening as it was. */
    private fun close() {
        if (closing) return
        closing = true
        dim.animate().alpha(0f).setDuration(SINK_MS).start()
        frame.animate().translationY(frame.height.toFloat()).setDuration(SINK_MS)
            .setInterpolator(android.view.animation.AccelerateInterpolator(1.5f)).withEndAction {
                finish()
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }.start()
    }

    @Deprecated("Back closes the popup with the same animation.")
    override fun onBackPressed() {
        close()
    }

    private fun busy(on: Boolean) {
        submit.isEnabled = !on
        submit.setText(if (on) R.string.auth_working else R.string.login_submit)
        if (on) error.text = ""
    }

    private fun label(text: Int) = TextView(this).apply {
        setText(text); setTextColor(DEEP_GREEN); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.BOTTOM or Gravity.START
        includeFontPadding = false
    }.also { frame.onScaleText(it, 30f) }

    private fun field(hint: Int, type: Int) = EditText(this).apply {
        setHint(hint); inputType = type
        // In landscape, keep the card visible while typing instead of Android's full-screen text editor.
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_NEXT
        setTextColor(INK); setHintTextColor(Color.argb(140, 58, 58, 42))
        background = getDrawable(R.drawable.ui_login_input)
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), 0, dp(14), 0)
        isSingleLine = true
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
    }.also { frame.onScaleText(it, 26f) }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
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
        /** The card picture's place on the 1672×941 canvas (cut from bg_login.png at this rect). */
        private val CARD = intArrayOf(492, 40, 1180, 895)
        private const val RISE_MS = 340L
        private const val SINK_MS = 220L
        private val CREAM = Color.parseColor("#FFF8E7")
        private val DEEP_GREEN = Color.parseColor("#1E5A2A")
        private val INK = Color.parseColor("#3A3A2A")

        /** After login: the hub with 스토리 진행하기 / 게임하기 / 튜토리얼 하기. Closes the opening and login popups. */
        fun openMain(activity: Activity) {
            activity.startActivity(HubActivity.intent(activity))
            activity.finish()
        }

        /** Card-sized popup window: the opening stays visible (dimmed) around it. Used by the sign-up popup. */
        fun sizeAsPopup(activity: Activity, page: Page) {
            page.root.setBackgroundColor(Color.TRANSPARENT)
            val metrics = activity.resources.displayMetrics
            val width = kotlin.math.min((Page.MAX_COLUMN_DP * metrics.density).roundToInt(), metrics.widthPixels - dp(activity, 32))
            // A fixed height (not wrap) lets the form scroll down to the buttons on short landscape screens.
            val height = metrics.heightPixels - dp(activity, 24)
            activity.window.setLayout(width, height)
        }

        private fun dp(context: Context, v: Int) = (v * context.resources.displayMetrics.density).roundToInt()
    }
}
