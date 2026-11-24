package com.communitycare.ai.data

import java.io.IOException

// Plain classes deliberately avoid generated toString methods containing secrets.
class NativeLogin(val username: String, val password: String)
class NativeRefresh(val refreshToken: String)
class NativeRegistration(val fullName: String, val email: String, val phone: String, val role: String, val password: String)
class NativeReset(val username: String)
class NativeNotice(val message: String)
class RegistrationResult(val status: String, val message: String) {
    fun validate(): RegistrationResult {
        if (status !in setOf("ENGINEERING_PILOT", "AWAITING_ASSIGNMENT")) throw IOException("Invalid registration response")
        return this
    }
}
class NativeTokens(val accessToken: String, val refreshToken: String, val expiresIn: Long) {
    fun validate(): NativeTokens {
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank() || accessToken.length > 65536 ||
            refreshToken.length > 65536 || expiresIn !in 30L..86400L) throw IOException("Invalid sign-in session")
        return this
    }
}
class NativeStoredSession(val accessToken: String, val refreshToken: String, val expiresAt: Long,
    val profile: Profile, val remember: Boolean)

fun registrationError(fullName: String, email: String, phone: String, password: String, confirm: String): String? = when {
    fullName.trim().split(Regex("\\s+")).size < 2 -> "Enter your first and last name."
    !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim()) -> "Enter a valid email address."
    !Regex("^[+0-9 ()-]{7,32}$").matches(phone.trim()) -> "Enter a valid phone number."
    password.length !in 8..256 -> "Use a password with at least eight characters."
    password != confirm -> "Passwords do not match."
    else -> null
}
