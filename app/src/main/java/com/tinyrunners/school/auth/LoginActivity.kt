package com.tinyrunners.school.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.TextView
import com.tinyrunners.school.MainActivity
import com.tinyrunners.school.R
import com.tinyrunners.school.ui.Page
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Login popup shown over the opening every time "시작하기" is pressed. When a session already exists it offers
 * "continue with this account" first, so returning players do not retype their password, and the form below
 * still lets them switch accounts.
 */
class LoginActivity : Activity() {
    private lateinit var error: TextView
    private lateinit var submit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = Page(this).show(scenery = false)
        sizeAsPopup(this, page)
        page.eyebrow(getString(R.string.characters_brand))
        page.title(getString(R.string.login_title))

        if (AuthService.hasSession(this)) {
            val current = page.card(10)
            val who = if (AuthService.isGuest(this)) getString(R.string.settings_guest)
                else getString(R.string.login_current, AuthService.currentName(this) ?: getString(R.string.settings_no_name),
                    AuthService.currentEmail(this).orEmpty())
            page.line(current, getString(R.string.login_current_label), 13f, Page.LEAF, bold = true, top = 0)
            page.line(current, who, 16f, Page.INK)
            page.button(getString(R.string.login_continue), primary = true) { openMain(this) }
            page.text(getString(R.string.login_other_account), 14f)
        } else {
            page.text(getString(R.string.login_body))
        }

        val configured = AuthService.isConfigured(this)
        val email = page.field(getString(R.string.auth_email), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val password = page.field(getString(R.string.auth_password), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        error = page.text("", 14f, Page.ERROR)

        submit = page.button(getString(R.string.login_submit), primary = !AuthService.hasSession(this)) {
            val e = email.text.toString().trim()
            val p = password.text.toString()
            AuthInput.loginError(e, p)?.let { showError(getString(it)); return@button }
            busy(true)
            AuthService.signIn(this, e, p) { message ->
                if (isDestroyed) return@signIn
                busy(false)
                if (message == null) openMain(this) else showError(message)
            }
        }
        page.button(getString(R.string.login_to_signup)) {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        if (!configured) {
            submit.isEnabled = false
            val notice = page.card(20)
            page.line(notice, getString(R.string.auth_not_configured), 15f, Page.INK, bold = true, top = 0)
            page.line(notice, getString(R.string.auth_guest_hint))
            page.button(getString(R.string.auth_guest)) {
                AuthService.enterGuest(this)
                openMain(this)
            }
        }
        page.button(getString(R.string.common_back)) { finish() }
    }

    private fun busy(on: Boolean) {
        submit.isEnabled = !on
        submit.setText(if (on) R.string.auth_working else R.string.login_submit)
        if (on) error.text = ""
    }

    private fun showError(message: String) { error.text = message }

    companion object {
        /** Goes to the friend selection and closes the opening and any login popups behind it. */
        fun openMain(activity: Activity) {
            activity.startActivity(Intent(activity, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            activity.finish()
        }

        /** Card-sized popup window: the opening stays visible (dimmed) around it. */
        fun sizeAsPopup(activity: Activity, page: Page) {
            page.root.setBackgroundColor(Color.TRANSPARENT)
            val metrics = activity.resources.displayMetrics
            val width = min((Page.MAX_COLUMN_DP * metrics.density).roundToInt(), metrics.widthPixels - dp(activity, 32))
            // A fixed height (not wrap) lets the form scroll down to the login/sign-up buttons on short landscape screens.
            val height = metrics.heightPixels - dp(activity, 24)
            activity.window.setLayout(width, height)
        }

        private fun dp(context: Context, v: Int) = (v * context.resources.displayMetrics.density).roundToInt()
    }
}
