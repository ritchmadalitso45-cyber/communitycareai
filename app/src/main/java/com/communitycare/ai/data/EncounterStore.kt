package com.communitycare.ai.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class EncounterStore(private val storage: SecureStorage) : EncounterQueue {
    private val gson = Gson()
    private val type = object : TypeToken<List<LocalEncounter>>() {}.type
    private val mutableRecords = MutableStateFlow<List<LocalEncounter>>(
        storage.read("encounters.bin")?.let { gson.fromJson<List<LocalEncounter>>(it, type) } ?: emptyList()
    )
    val records = mutableRecords.asStateFlow()

    @Synchronized override fun snapshot() = mutableRecords.value.toList()

    @Synchronized fun add(profile: Profile, triage: Triage) {
        require(profile.canRecordCare()) { "Your role cannot create care reports" }
        save(mutableRecords.value + LocalEncounter.create(profile, triage))
    }

    @Synchronized override fun update(item: LocalEncounter, state: DeliveryState) {
        save(mutableRecords.value.map {
            if (it.id == item.id && it.owner == item.owner && it.orgUnit == item.orgUnit) it.copy(state = state) else it
        })
    }

    private fun save(records: List<LocalEncounter>) {
        storage.write("encounters.bin", gson.toJson(records))
        mutableRecords.value = records
    }
}
