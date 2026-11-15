package com.tinyrunners.school.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.userProfileChangeRequest
import com.tinyrunners.school.R

/**
 * Firebase Authentication (email + password, and guests). A guest plays at once on a Firebase anonymous account, so
 * their coins and records are saved like anyone's; later [linkGuest] attaches an e-mail and password to that same
 * account (same uid), so nothing is lost. When app/google-services.json is missing, Firebase is not initialised:
 * [isConfigured] is false and the login screen offers a device-only guest mode for development.
 */
object AuthService {
    private const val PREFS = "auth"
    private const val KEY_GUEST = "guest"

    fun isConfigured(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    private fun auth(context: Context): FirebaseAuth? = if (isConfigured(context)) FirebaseAuth.getInstance() else null

    fun currentUid(context: Context): String? = auth(context)?.currentUser?.uid

    fun currentEmail(context: Context): String? = auth(context)?.currentUser?.email
    fun currentName(context: Context): String? = auth(context)?.currentUser?.displayName?.takeIf { it.isNotBlank() }

    /** A guest of either kind: a Firebase anonymous account, or the device-only mode of a build without Firebase. */
    fun isGuest(context: Context): Boolean = isCloudGuest(context) || isDeviceGuest(context)

    /** Playing on a Firebase anonymous account (records saved; can become a full account with [linkGuest]). */
    fun isCloudGuest(context: Context): Boolean = auth(context)?.currentUser?.isAnonymous == true

    private fun isDeviceGuest(context: Context): Boolean =
        !isConfigured(context) && context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_GUEST, false)

    /** "가입 없이 시작하기": a Firebase anonymous account, or the device-only mode when Firebase is not set up. */
    fun startGuest(context: Context, done: (String?) -> Unit) {
        val a = auth(context) ?: run { enterGuest(context); return done(null) }
        a.signInAnonymously()
            .addOnSuccessListener { done(null) }
            .addOnFailureListener { done(message(context, it)) }
    }

    /**
     * Turns the guest's anonymous account into a full one: the e-mail and password are linked to the same account, so
     * the uid — and every coin, record and story read under it — stays.
     */
    fun linkGuest(context: Context, name: String, email: String, password: String, done: (String?) -> Unit) {
        val user = auth(context)?.currentUser?.takeIf { it.isAnonymous }
            ?: return done(context.getString(R.string.auth_error_unknown))
        user.linkWithCredential(EmailAuthProvider.getCredential(email, password))
            .addOnSuccessListener { result ->
                result.user?.updateProfile(userProfileChangeRequest { displayName = name })
                    ?.addOnCompleteListener { done(null) } ?: done(null)
            }
            .addOnFailureListener { done(message(context, it)) }
    }

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

    private fun message(context: Context, e: Exception): String = context.getString(when {
        // Anonymous sign-in switched off in the Firebase console.
        e is FirebaseAuthException && e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" -> R.string.auth_error_guest_disabled
        else -> when (e) {
        is FirebaseAuthWeakPasswordException -> R.string.auth_error_weak_password
        is FirebaseAuthUserCollisionException -> R.string.auth_error_email_in_use
        is FirebaseAuthInvalidUserException -> R.string.auth_error_wrong_login
        is FirebaseAuthInvalidCredentialsException -> R.string.auth_error_wrong_login
        is FirebaseNetworkException -> R.string.auth_error_network
        else -> R.string.auth_error_unknown
        }
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
