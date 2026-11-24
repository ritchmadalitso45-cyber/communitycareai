package com.communitycare.ai.data
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class NativeAccountsTest {
    @Test fun originalRegistrationFieldsAreValidated() {
        assertNull(registrationError("Test Nurse", "test@example.invalid", "+265888111222", "Example123!", "Example123!"))
        assertNotNull(registrationError("Test", "test@example.invalid", "+265888111222", "Example123!", "Example123!"))
        assertNotNull(registrationError("Test Nurse", "bad", "+265888111222", "Example123!", "Example123!"))
        assertNotNull(registrationError("Test Nurse", "test@example.invalid", "+265888111222", "Example123!", "different"))
    }
    @Test fun credentialsAreNotExposedByObjectDescriptions() {
        val login = NativeLogin("test", "NotForLogs123")
        val registration = NativeRegistration("Test Nurse", "test@example.invalid", "+265888111222", "Nurse", "NotForLogs123")
        assertFalse(login.toString().contains("NotForLogs123"))
        assertFalse(registration.toString().contains("NotForLogs123"))
        assertEquals("test", Gson().fromJson(Gson().toJson(login), NativeLogin::class.java).username)
    }
    @Test fun invalidSessionsAreNotAccepted() {
        assertThrows(IOException::class.java) { NativeTokens("", "refresh", 300).validate() }
        assertThrows(IOException::class.java) { NativeTokens("access", "refresh", 0).validate() }
        assertThrows(IOException::class.java) { NativeTokens("access", "refresh", 86401).validate() }
    }
    @Test fun unknownRegistrationStatusIsNotSuccess() {
        assertThrows(IOException::class.java) { RegistrationResult("UNEXPECTED", "").validate() }
        assertEquals("AWAITING_ASSIGNMENT", RegistrationResult("AWAITING_ASSIGNMENT", "").validate().status)
    }
}
