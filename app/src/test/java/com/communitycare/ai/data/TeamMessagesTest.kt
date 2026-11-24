package com.communitycare.ai.data
import org.junit.Assert.*
import org.junit.Test

class TeamMessagesTest {
    private val profile = Profile("worker", "Grace Phiri", "grace", "LoeZ9m7qk8B")
    private val message = TeamMessage("12345678-1234-1234-1234-123456789012", "worker", "Grace Phiri", "Good morning, team.", "2026-11-23T10:00:00Z")
    @Test fun rejectsMessagesFromWrongFacility() {
        assertThrows(IllegalArgumentException::class.java) { TeamInbox("OtherOrg123", listOf(message)).validate(profile) }
        assertEquals(message, TeamInbox(profile.orgUnit, listOf(message)).validate(profile).messages.single())
    }
    @Test fun rejectsInvalidOrDuplicateMessages() {
        assertThrows(IllegalArgumentException::class.java) { TeamInbox(profile.orgUnit, listOf(message,message)).validate(profile) }
        assertThrows(IllegalArgumentException::class.java) { message.copy(text=" ").validate() }
        assertThrows(java.time.DateTimeException::class.java) { message.copy(sentAt="yesterday").validate() }
    }
}
