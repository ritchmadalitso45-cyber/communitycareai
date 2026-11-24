package com.communitycare.ai.data

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class ClinicStoreTest {
    private val nurse = Profile("nurse-a", "Nurse A", "nurse.a", "LoeZ9m7qk8B")
    @Test fun recordsPersistWithoutSeededPatients() {
        var disk: String? = null
        val store = ClinicStore({ disk }, { disk = it })
        assertTrue(store.snapshot.value.patients.isEmpty())
        val patient = store.addPatient(nurse, "Test Patient", 27)
        store.addEntry(nurse, patient.id, "NOTE", "Worker entered observation", Instant.now().toString())
        val reopened = ClinicStore({ disk }, { disk = it })
        assertEquals(patient, reopened.snapshot.value.patients.single())
        assertEquals(1, reopened.snapshot.value.entries.size)
    }
    @Test fun anotherWorkerOrFacilityCannotAddToPatient() {
        val store = ClinicStore({ null }, {})
        val p = store.addPatient(nurse, "Test Patient", 27)
        listOf(nurse.copy(subject="other-worker"), nurse.copy(orgUnit="Abcdef12345")).forEach { other ->
            assertFalse(p.belongsTo(other))
            assertThrows(IllegalArgumentException::class.java) {
                store.addEntry(other, p.id, "NOTE", "not authorised", Instant.now().toString())
            }
        }
        assertTrue(store.snapshot.value.entries.isEmpty())
    }
    @Test fun failedPersistenceDoesNotPublishUnsavedData() {
        val store = ClinicStore({ null }, { throw java.io.IOException("disk full") })
        assertThrows(java.io.IOException::class.java) { store.addPatient(nurse, "Test Patient", 27) }
        assertTrue(store.snapshot.value.patients.isEmpty())
    }
    @Test fun invalidInputIsRejectedBeforeWrite() {
        var writes = 0
        val store = ClinicStore({ null }, { writes++ })
        assertThrows(IllegalArgumentException::class.java) { store.addPatient(nurse, " ", 27) }
        assertThrows(IllegalArgumentException::class.java) { store.addPatient(nurse, "Test", -1) }
        assertEquals(0, writes)
    }
}
