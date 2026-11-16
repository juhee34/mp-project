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
    private lateinit var guest: Button
    private var showingForm = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        frame = MockupFrame(this, 0)
        val v = MockupFrame.Companion::v2
        // A's separated pack (reference/screen-1): a cream letter with a bookmark at the right of the opening.
        // The letter in the game's leaf green (the same page the story book uses): paper, spine and bookmark.
        frame.placeV2(View(this).apply {
            background = com.tinyrunners.school.characters.StoryPicker.BookPage(LEAF, Color.rgb(255, 247, 228))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, 420, 70, 860, 642)
        frame.placeV2(TextView(this).apply {
            setText(R.string.login_back_v2); setTextColor(CREAM); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER_VERTICAL
            setShadowLayer(6f, 0f, 1f, Color.argb(160, 40, 30, 60)); isClickable = true; isFocusable = true
            background = android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33FFFFFF), null,
                android.graphics.drawable.ColorDrawable(Color.WHITE))
            setOnClickListener { close() }
        }.also { frame.onScaleText(it, v(22).toFloat()) }, 28, 22, 240, 76)
        frame.placeV2(TextView(this).apply {
            setText(R.string.login_heading_v2); setTextColor(INK); typeface = Typeface.DEFAULT_BOLD; includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        }.also { frame.onScaleText(it, v(30).toFloat()) }, 477, 128, 816, 176)
        frame.placeV2(TextView(this).apply {
            setText(R.string.login_sub_v2); setTextColor(SOFT); includeFontPadding = false; gravity = Gravity.CENTER_VERTICAL
        }.also { frame.onScaleText(it, v(16).toFloat()) }, 477, 180, 816, 206)

        emailLabel = label(R.string.auth_email).also { frame.placeV2(it, 477, 220, 816, 250) }
        email = field(R.string.auth_email_hint_v2, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
            .also { frame.placeV2(it, 477, 253, 816, 298) }
        passwordLabel = label(R.string.auth_password).also { frame.placeV2(it, 477, 304, 816, 334) }
        password = field(R.string.auth_password, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            .also { frame.placeV2(it, 477, 337, 816, 382) }
        // With a session: who is signed in, in a box like the fields.
        account = TextView(this).apply {
            setTextColor(INK); gravity = Gravity.CENTER_VERTICAL; maxLines = 2
            background = box()
            setPadding(dp(14), 0, dp(14), 0)
        }.also { frame.onScaleText(it, v(18).toFloat()); frame.placeV2(it, 477, 253, 816, 312) }
        error = TextView(this).apply { setTextColor(Page.ERROR); gravity = Gravity.CENTER; maxLines = 2 }
            .also { frame.onScaleText(it, v(15).toFloat()); frame.placeV2(it, 477, 390, 816, 428) }

        submit = pillButton(filled = true) { primaryClick() }.also { frame.placeV2(it, 477, 432, 816, 486) }
        // 처음 오셨나요? 회원가입 (or 다른 계정으로 로그인 with a session): a quiet line under the main button.
        second = Button(this).apply {
            isAllCaps = false; stateListAnimator = null; setTextColor(SOFT); setPadding(0, 0, 0, 0)
            minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            background = android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x22000000), null,
                android.graphics.drawable.ColorDrawable(Color.WHITE))
            setOnClickListener { secondClick() }
        }.also { frame.onScaleText(it, v(17).toFloat()); frame.placeV2(it, 477, 498, 816, 536) }

        // 게스트로 둘러보기: the outlined button at the bottom; the guest can make an account later in 설정.
        guest = pillButton(filled = false) { guestClick() }.apply { setText(R.string.login_guest_start) }
            .also { frame.placeV2(it, 477, 547, 816, 599) }

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
        guest.visibility = if (form && AuthService.isConfigured(this)) View.VISIBLE else View.GONE
        if (!form) {
            emailLabel.visibility = View.VISIBLE
            emailLabel.setText(R.string.login_current_label)
            account.text = if (AuthService.isCloudGuest(this)) getString(R.string.login_guest_current)
                else if (AuthService.isGuest(this)) getString(R.string.settings_guest)
                else getString(R.string.login_current, AuthService.currentName(this) ?: getString(R.string.settings_no_name),
                    AuthService.currentEmail(this).orEmpty())
            submit.setText(R.string.login_continue)
            second.setText(R.string.login_other_account_button)
        } else {
            emailLabel.setText(R.string.auth_email)
            submit.setText(R.string.login_submit)
            second.setText(R.string.login_to_signup_v2)
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

    /** Plays at once as a guest (Firebase anonymous account). */
    private fun guestClick() {
        guest.isEnabled = false
        guest.setText(R.string.auth_working)
        error.text = ""
        AuthService.startGuest(this) { message ->
            if (isDestroyed) return@startGuest
            if (message == null) { openMain(this); return@startGuest }
            guest.isEnabled = true
            guest.setText(R.string.login_guest_start)
            error.text = message
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
        setText(text); setTextColor(INK); gravity = Gravity.BOTTOM or Gravity.START
        includeFontPadding = false
    }.also { frame.onScaleText(it, MockupFrame.v2(15).toFloat()) }

    /** The cream input box of the letter. */
    private fun box() = android.graphics.drawable.GradientDrawable().apply {
        setColor(Color.rgb(255, 250, 234)); cornerRadius = dp(8).toFloat(); setStroke(dp(1), Color.rgb(196, 182, 140))
    }

    /** A round button: dark green filled (로그인) or cream with a green edge (게스트로 둘러보기). */
    private fun pillButton(filled: Boolean, click: () -> Unit) = Button(this).apply {
        isAllCaps = false; stateListAnimator = null; typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (filled) CREAM else Color.rgb(46, 42, 54))
        setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
        fun pill(fill: Int) = android.graphics.drawable.GradientDrawable().apply {
            setColor(fill); cornerRadius = dp(16).toFloat(); if (!filled) setStroke(dp(2), LEAF)
        }
        background = android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pill(if (filled) Color.rgb(40, 86, 59) else Color.rgb(238, 236, 214)))
            addState(intArrayOf(), pill(if (filled) LEAF else Color.rgb(255, 253, 241)))
        }
        setOnClickListener { click() }
    }.also { frame.onScaleText(it, MockupFrame.v2(20).toFloat()) }

    private fun field(hint: Int, type: Int) = EditText(this).apply {
        setHint(hint); inputType = type
        // In landscape, keep the card visible while typing instead of Android's full-screen text editor.
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_NEXT
        setTextColor(INK); setHintTextColor(Color.argb(140, 58, 58, 42))
        background = box()
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), 0, dp(14), 0)
        isSingleLine = true
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
    }.also { frame.onScaleText(it, MockupFrame.v2(18).toFloat()) }

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
        private const val RISE_MS = 340L
        private const val SINK_MS = 220L
        private val CREAM = Color.parseColor("#FFF8E7")
        private val LEAF = Color.rgb(62, 122, 78)
        private val INK = Color.rgb(46, 52, 40)
        private val SOFT = Color.rgb(94, 98, 80)

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
