package com.communitycare.ai.data
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class AppointmentTest {
    @Test fun convertsLocalAppointmentToUtc() {
        assertEquals("2026-11-25T07:00:00Z", appointmentInstant("2026-11-25 09:00", ZoneId.of("Africa/Blantyre")).toString())
    }
    @Test fun rejectsInvalidCalendarDateInsteadOfChangingIt() {
        listOf("2026-02-29 09:00", "2026-11-31 09:00", "2026-11-25 24:00").forEach {
            assertThrows(java.time.DateTimeException::class.java) { appointmentInstant(it, ZoneId.of("UTC")) }
        }
    }
    @Test fun acceptsLeapDayAndRejectsNonexistentDaylightSavingTime() {
        assertEquals("2028-02-29T09:00:00Z", appointmentInstant("2028-02-29 09:00", ZoneId.of("UTC")).toString())
        assertThrows(IllegalArgumentException::class.java) { appointmentInstant("2026-03-29 02:30", ZoneId.of("Europe/Berlin")) }
    }
}
