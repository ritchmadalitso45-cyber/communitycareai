package com.communitycare.ai.data

import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.util.UUID

data class ClinicPatient(val id: String, val owner: String, val orgUnit: String,
    val name: String, val ageAtRegistration: Int, val createdAt: String) {
    fun belongsTo(profile: Profile) = owner == profile.subject && orgUnit == profile.orgUnit
}
data class ClinicEntry(val id: String, val owner: String, val orgUnit: String,
    val patientId: String, val kind: String, val text: String, val at: String) {
    fun belongsTo(profile: Profile) = owner == profile.subject && orgUnit == profile.orgUnit
}
data class ClinicSnapshot(val patients: List<ClinicPatient> = emptyList(), val entries: List<ClinicEntry> = emptyList())

/** Local patient register. Uses the same encrypted storage as the reporting outbox.
 * Names and notes are never sent through the aggregate DHIS2 reporting endpoint. */
class ClinicStore(read: () -> String?, private val write: (String) -> Unit) {
    private val gson = Gson()
    private val state = MutableStateFlow(read()?.let {
        gson.fromJson(it, ClinicSnapshot::class.java).also { snapshot ->
            require(snapshot != null && snapshot.patients != null && snapshot.entries != null)
            require(snapshot.patients.all { p -> p.name.isNotBlank() && p.owner.isNotBlank() && p.ageAtRegistration in 0..130 })
        }
    } ?: ClinicSnapshot())
    val snapshot = state.asStateFlow()

    @Synchronized fun addPatient(profile: Profile, name: String, age: Int): ClinicPatient {
        profile.validate()
        require(profile.canRecordCare()) { "Your role cannot create care records" }
        val clean = name.trim()
        require(clean.isNotBlank() && clean.length <= 120 && age in 0..130)
        require(state.value.patients.size < 5000)
        val patient = ClinicPatient(UUID.randomUUID().toString(), profile.subject, profile.orgUnit,
            clean, age, Instant.now().toString())
        save(state.value.copy(patients = state.value.patients + patient))
        return patient
    }

    @Synchronized fun addEntry(profile: Profile, patientId: String, kind: String, text: String, at: String) {
        profile.validate()
        require(profile.canRecordCare()) { "Your role cannot create care records" }
        require(state.value.patients.any { it.id == patientId && it.belongsTo(profile) })
        require(kind in setOf("APPOINTMENT", "NOTE"))
        require(text.trim().isNotBlank() && text.trim().length <= 2000)
        Instant.parse(at)
        require(state.value.entries.size < 20000)
        val entry = ClinicEntry(UUID.randomUUID().toString(), profile.subject, profile.orgUnit,
            patientId, kind, text.trim(), at)
        save(state.value.copy(entries = state.value.entries + entry))
    }

    private fun save(snapshot: ClinicSnapshot) {
        write(gson.toJson(snapshot))
        state.value = snapshot
    }
}
