package com.communitycare.ai.data

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class QualityCandidate(val eventId: String, val occurredAt: String, val category: String,
    val delayHours: Double, val anomalyScore: Double, val reviewSuggested: Boolean)

data class QualitySnapshot(val orgUnit: String, val generatedAt: String, val source: String,
    val dataContext: String, val reportCount: Int, val validCount: Int, val invalidCount: Int,
    val categoryCounts: Map<String, Int>, val delayedOver24Hours: Int,
    val snapshotLimit: Int, val truncated: Boolean, val cacheSeconds: Int,
    val modelVersion: String, val datasetFingerprint: String, val modelStatus: String,
    val trainingSamples: Int, val minimumSamples: Int, val fittedOn: String,
    val candidates: List<QualityCandidate>, val notice: String) {
    fun validate(expected: Profile): QualitySnapshot {
        if (orgUnit != expected.orgUnit || source != "OWNED_DHIS2" ||
            modelVersion.isNullOrBlank() || generatedAt.isNullOrBlank() ||
            reportCount < 0 || validCount < 0 || invalidCount < 0 ||
            reportCount != validCount + invalidCount || trainingSamples != validCount ||
            categoryCounts == null || categoryCounts.values.any { it < 0 } ||
            categoryCounts.values.sum() != validCount ||
            !categoryCounts.keys.containsAll(Triage.entries.map { it.name }) ||
            candidates == null || candidates.size > 5 || notice.isNullOrBlank() ||
            modelStatus !in setOf("INSUFFICIENT_DATA", "EXPERIMENTAL_SMALL_SAMPLE", "EXPERIMENTAL_UNVALIDATED") ||
            candidates.any { !it.anomalyScore.isFinite() || it.anomalyScore !in 0.0..1.0 ||
                !it.delayHours.isFinite() || it.delayHours < 0 || it.eventId.isNullOrBlank() ||
                it.category !in Triage.entries.map { category -> category.name } } ||
            (modelStatus == "INSUFFICIENT_DATA" && candidates.isNotEmpty())) {
            throw IOException("Invalid reporting-quality response")
        }
        return this
    }
}

data class QualityUiState(val owner: String? = null, val orgUnit: String? = null,
    val snapshot: QualitySnapshot? = null, val loading: Boolean = false, val error: String? = null)

class QualityRepository(private val auth: AuthManager, private val api: BackendApi) {
    private val mutableState = MutableStateFlow(QualityUiState())
    val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    fun clear() { mutableState.value = QualityUiState() }
    suspend fun refresh(expected: Profile) = mutex.withLock {
        if (auth.profile.value != expected) return@withLock
        mutableState.value = QualityUiState(expected.subject, expected.orgUnit, loading = true)
        try {
            val result = api.quality("Bearer ${auth.accessToken(expected, api)}", expected.orgUnit)
                .checkedBody().validate(expected)
            if (auth.profile.value == expected) mutableState.value =
                QualityUiState(expected.subject, expected.orgUnit, snapshot = result)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            if (auth.profile.value == expected) mutableState.value = QualityUiState(
                expected.subject, expected.orgUnit, error = if (failure is HttpFailure && failure.status in setOf(401,403))
                    "Sign in again to refresh reporting analysis." else
                    "Live DHIS2 analysis is unavailable. Saved device reports are unaffected.")
        } finally {
            if (mutableState.value.owner == expected.subject && mutableState.value.orgUnit == expected.orgUnit)
                mutableState.value = mutableState.value.copy(loading = false)
        }
    }
}
