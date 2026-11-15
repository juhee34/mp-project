package com.tinyrunners.school.auth

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import com.tinyrunners.school.R
import com.tinyrunners.school.ui.Page

/**
 * Sign-up popup over the opening. A new account goes straight to the friend selection. Opened with [EXTRA_KEEP_GUEST]
 * (from 설정 › 계정 while playing as a guest), the same form instead links the e-mail and password to the guest's
 * account — "계정 만들고 기록 지키기" — and closes, keeping every record.
 */
class SignupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val keep = intent.getBooleanExtra(EXTRA_KEEP_GUEST, false) && AuthService.isCloudGuest(this)
        val page = Page(this).show(scenery = false)
        LoginActivity.sizeAsPopup(this, page)
        page.backPill(getString(R.string.common_back_pill)) { finish() }
        page.eyebrow(getString(R.string.characters_brand))
        page.title(getString(if (keep) R.string.signup_keep_title else R.string.signup_title))
        page.text(getString(if (keep) R.string.signup_keep_body else R.string.signup_body, AuthInput.MIN_PASSWORD))

        val name = page.field(getString(R.string.auth_name, AuthInput.MAX_NAME), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME)
        val email = page.field(getString(R.string.auth_email), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val password = page.field(getString(R.string.auth_password), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val confirm = page.field(getString(R.string.auth_password_confirm), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val error = page.text("", 14f, Page.ERROR)

        lateinit var submit: android.widget.Button
        val submitLabel = if (keep) R.string.signup_keep_submit else R.string.signup_submit
        submit = page.button(getString(submitLabel), primary = true) {
            val n = name.text.toString().trim()
            val e = email.text.toString().trim()
            val p = password.text.toString()
            AuthInput.signUpError(n, e, p, confirm.text.toString())?.let { error.text = getString(it); return@button }
            submit.isEnabled = false
            submit.setText(R.string.auth_working)
            error.text = ""
            val finished: (String?) -> Unit = { message ->
                if (!isDestroyed) {
                    if (message == null) {
                        if (keep) { setResult(RESULT_OK); finish() } else LoginActivity.openMain(this)
                    } else {
                        submit.isEnabled = true
                        submit.setText(submitLabel)
                        error.text = message
                    }
                }
            }
            if (keep) AuthService.linkGuest(this, n, e, p, finished) else AuthService.signUp(this, n, e, p, finished)
        }
        if (!AuthService.isConfigured(this)) {
            submit.isEnabled = false
            page.text(getString(R.string.auth_not_configured), 14f, Page.ERROR)
        }
    }

    companion object {
        /** Link an e-mail and password to the current guest account instead of making a new one. */
        const val EXTRA_KEEP_GUEST = "keepGuest"
    }
}
