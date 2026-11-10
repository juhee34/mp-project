package com.tinyrunners.school.auth

import com.tinyrunners.school.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInputTest {
    @Test fun loginChecks() {
        assertEquals(R.string.auth_error_email_format, AuthInput.loginError("not-an-email", "pw"))
        assertEquals(R.string.auth_error_password_empty, AuthInput.loginError("a@b.co", ""))
        assertNull(AuthInput.loginError(" a@b.co ", "secret"))
    }

    @Test fun signUpChecks() {
        assertEquals(R.string.auth_error_name_empty, AuthInput.signUpError(" ", "a@b.co", "secret1", "secret1"))
        assertEquals(R.string.auth_error_name_long, AuthInput.signUpError("가".repeat(13), "a@b.co", "secret1", "secret1"))
        assertEquals(R.string.auth_error_email_format, AuthInput.signUpError("치즈", "a@b", "secret1", "secret1"))
        assertEquals(R.string.auth_error_weak_password, AuthInput.signUpError("치즈", "a@b.co", "12345", "12345"))
        assertEquals(R.string.auth_error_password_mismatch, AuthInput.signUpError("치즈", "a@b.co", "secret1", "secret2"))
        assertNull(AuthInput.signUpError("치즈", "a@b.co", "secret1", "secret1"))
    }
}
