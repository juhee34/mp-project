package com.tinyrunners.school.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.userProfileChangeRequest
import com.tinyrunners.school.R

/**
 * Firebase Authentication (email + password). When app/google-services.json is missing, Firebase is not
 * initialised: [isConfigured] is false and the login screen offers a device-only guest mode for development.
 */
object AuthService {
    private const val PREFS = "auth"
    private const val KEY_GUEST = "guest"

    fun isConfigured(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    private fun auth(context: Context): FirebaseAuth? = if (isConfigured(context)) FirebaseAuth.getInstance() else null

    fun currentUid(context: Context): String? = auth(context)?.currentUser?.uid

    fun currentEmail(context: Context): String? = auth(context)?.currentUser?.email
    fun currentName(context: Context): String? = auth(context)?.currentUser?.displayName?.takeIf { it.isNotBlank() }

    fun isGuest(context: Context): Boolean =
        !isConfigured(context) && context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_GUEST, false)

    /** True when the app may show the main screens. */
    fun hasSession(context: Context): Boolean = currentUid(context) != null || isGuest(context)

    fun enterGuest(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_GUEST, true).apply()
    }

    fun signOut(context: Context) {
        auth(context)?.signOut()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_GUEST).apply()
    }

    fun signIn(context: Context, email: String, password: String, done: (String?) -> Unit) {
        val a = auth(context) ?: return done(context.getString(R.string.auth_not_configured))
        a.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { done(null) }
            .addOnFailureListener { done(message(context, it)) }
    }

    fun signUp(context: Context, name: String, email: String, password: String, done: (String?) -> Unit) {
        val a = auth(context) ?: return done(context.getString(R.string.auth_not_configured))
        a.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                // The nickname is a nicety; a failure here should not undo a created account.
                result.user?.updateProfile(userProfileChangeRequest { displayName = name })
                    ?.addOnCompleteListener { done(null) } ?: done(null)
            }
            .addOnFailureListener { done(message(context, it)) }
    }

    private fun message(context: Context, e: Exception): String = context.getString(when (e) {
        is FirebaseAuthWeakPasswordException -> R.string.auth_error_weak_password
        is FirebaseAuthUserCollisionException -> R.string.auth_error_email_in_use
        is FirebaseAuthInvalidUserException -> R.string.auth_error_wrong_login
        is FirebaseAuthInvalidCredentialsException -> R.string.auth_error_wrong_login
        is FirebaseNetworkException -> R.string.auth_error_network
        else -> R.string.auth_error_unknown
    })
}

/** Input checks shared by the login and sign-up screens; returns a string resource id or null. */
object AuthInput {
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    const val MIN_PASSWORD = 6
    const val MAX_NAME = 12

    fun loginError(email: String, password: String): Int? = when {
        !EMAIL.matches(email.trim()) -> R.string.auth_error_email_format
        password.isEmpty() -> R.string.auth_error_password_empty
        else -> null
    }

    fun signUpError(name: String, email: String, password: String, confirm: String): Int? = when {
        name.isBlank() -> R.string.auth_error_name_empty
        name.trim().length > MAX_NAME -> R.string.auth_error_name_long
        !EMAIL.matches(email.trim()) -> R.string.auth_error_email_format
        password.length < MIN_PASSWORD -> R.string.auth_error_weak_password
        password != confirm -> R.string.auth_error_password_mismatch
        else -> null
    }
}
