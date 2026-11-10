package com.tinyrunners.school.auth

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import com.tinyrunners.school.R
import com.tinyrunners.school.ui.Page

/** Creates a Firebase account. On success it closes; the login screen sees the session and opens the app. */
class SignupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = Page(this).show()
        page.eyebrow(getString(R.string.characters_brand))
        page.title(getString(R.string.signup_title))
        page.text(getString(R.string.signup_body, AuthInput.MIN_PASSWORD))

        val name = page.field(getString(R.string.auth_name, AuthInput.MAX_NAME), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME)
        val email = page.field(getString(R.string.auth_email), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val password = page.field(getString(R.string.auth_password), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val confirm = page.field(getString(R.string.auth_password_confirm), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val error = page.text("", 14f, Page.ERROR)

        lateinit var submit: android.widget.Button
        submit = page.button(getString(R.string.signup_submit), primary = true) {
            val n = name.text.toString().trim()
            val e = email.text.toString().trim()
            val p = password.text.toString()
            AuthInput.signUpError(n, e, p, confirm.text.toString())?.let { error.text = getString(it); return@button }
            submit.isEnabled = false
            submit.setText(R.string.auth_working)
            error.text = ""
            AuthService.signUp(this, n, e, p) { message ->
                if (isDestroyed) return@signUp
                if (message == null) { finish(); return@signUp }
                submit.isEnabled = true
                submit.setText(R.string.signup_submit)
                error.text = message
            }
        }
        if (!AuthService.isConfigured(this)) {
            submit.isEnabled = false
            page.text(getString(R.string.auth_not_configured), 14f, Page.ERROR)
        }
        page.button(getString(R.string.common_back)) { finish() }
    }
}
