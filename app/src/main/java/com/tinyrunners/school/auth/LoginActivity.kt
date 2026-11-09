package com.tinyrunners.school.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.TextView
import com.tinyrunners.school.MainActivity
import com.tinyrunners.school.R
import com.tinyrunners.school.ui.Page

class LoginActivity : Activity() {
    private lateinit var error: TextView
    private lateinit var submit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AuthService.hasSession(this)) { openMain(); return }

        val page = Page(this).show()
        page.eyebrow(getString(R.string.characters_brand))
        page.title(getString(R.string.login_title))
        page.text(getString(R.string.login_body))

        val configured = AuthService.isConfigured(this)
        val email = page.field(getString(R.string.auth_email), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val password = page.field(getString(R.string.auth_password), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        error = page.text("", 14f, Page.ERROR)

        submit = page.button(getString(R.string.login_submit), primary = true) {
            val e = email.text.toString().trim()
            val p = password.text.toString()
            AuthInput.loginError(e, p)?.let { showError(getString(it)); return@button }
            busy(true)
            AuthService.signIn(this, e, p) { message ->
                if (isDestroyed) return@signIn
                busy(false)
                if (message == null) openMain() else showError(message)
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
                openMain()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Returning from a successful sign-up lands here already signed in.
        if (::submit.isInitialized && AuthService.hasSession(this)) openMain()
    }

    private fun busy(on: Boolean) {
        submit.isEnabled = !on
        submit.setText(if (on) R.string.auth_working else R.string.login_submit)
        if (on) error.text = ""
    }

    private fun showError(message: String) { error.text = message }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
