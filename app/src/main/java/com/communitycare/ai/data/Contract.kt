package com.communitycare.ai.data

import java.io.IOException
import java.time.Instant
import java.util.UUID
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path

enum class Triage(val label: String) {
    ROUTINE("Routine"), REFERRAL("Referral"), URGENT_REFERRAL("Urgent referral")
}

enum class DeliveryState(val label: String) {
    PENDING("Saved on this device"), QUEUED("Accepted by server"),
    FORWARDED("Delivered to DHIS2"), REJECTED("Rejected — needs review"),
    CONFLICT("Record conflict — needs review")
}

data class Profile(val subject: String, val name: String, val username: String, val orgUnit: String, val role: String? = "HSA") {
    fun validate(): Profile {
        if (subject.isNullOrBlank() || name.isNullOrBlank() || username.isNullOrBlank() ||
            !Regex("[A-Za-z][A-Za-z0-9]{10}").matches(orgUnit.orEmpty()) ||
            (role != null && role !in staffRoles + "PENDING")) {
            throw IOException("Invalid account response")
        }
        return this
    }
}

data class Submission(val occurredAt: String, val triage: Triage)
data class Receipt(val encounterId: String, val state: String) {
    fun deliveryState(expectedId: String): DeliveryState {
        if (encounterId != expectedId) throw IOException("Mismatched receipt")
        return when (state) {
            "QUEUED" -> DeliveryState.QUEUED
            "FORWARDED" -> DeliveryState.FORWARDED
            "REJECTED" -> DeliveryState.REJECTED
            else -> throw IOException("Unknown delivery state")
        }
    }
}

data class LocalEncounter(
    val id: String, val owner: String, val orgUnit: String, val occurredAt: String,
    val triage: Triage, val state: DeliveryState = DeliveryState.PENDING
) {
    val submission: Submission get() = Submission(occurredAt, triage)
    fun belongsTo(profile: Profile) = owner == profile.subject && orgUnit == profile.orgUnit
    companion object {
        fun create(profile: Profile, triage: Triage) = LocalEncounter(
            UUID.randomUUID().toString(), profile.subject, profile.orgUnit, Instant.now().toString(), triage
        )
    }
}

interface BackendApi {
    @GET("api/v1/workflows") suspend fun caseInbox(@Header("Authorization") bearer: String, @Header("X-Organisation-Unit") org: String): Response<CaseInbox>
    @GET("api/v1/workflows/{id}") suspend fun caseDetail(@Path("id") id: String, @Header("Authorization") bearer: String, @Header("X-Organisation-Unit") org: String): Response<CareCase>
    @PUT("api/v1/workflows/{id}") suspend fun createCase(@Path("id") id: String, @Header("Authorization") bearer: String, @Header("X-Organisation-Unit") org: String, @Body form: CaseForm): Response<CareCase>
    @POST("api/v1/workflows/{id}/actions") suspend fun caseAction(@Path("id") id: String, @Header("Authorization") bearer: String, @Header("X-Organisation-Unit") org: String, @Body form: CaseCommand): Response<CareCase>
    @GET("api/v1/staff") suspend fun staff(@Header("Authorization") bearer: String, @Header("X-Organisation-Unit") org: String): Response<StaffDirectory>
    @PUT("api/v1/staff/{id}/role") suspend fun setStaffRole(@Path("id") id: String, @Header("Authorization") bearer: String, @Header("X-Organisation-Unit") org: String, @Body form: StaffRoleChange): Response<StaffMember>

    @GET("api/v1/messages") suspend fun messages(@Header("Authorization") bearer: String, @Header("X-Organisation-Unit") org: String): Response<TeamInbox>
    @PUT("api/v1/messages/{id}") suspend fun sendMessage(@Path("id") id: String, @Header("Authorization") bearer: String, @Header("X-Organisation-Unit") org: String, @Body form: MessageForm): Response<TeamMessage>

    @POST("api/v1/auth/login") suspend fun nativeLogin(@Body form: NativeLogin): Response<NativeTokens>
    @POST("api/v1/auth/refresh") suspend fun nativeRefresh(@Body form: NativeRefresh): Response<NativeTokens>
    @POST("api/v1/auth/register") suspend fun nativeRegister(@Body form: NativeRegistration): Response<RegistrationResult>
    @POST("api/v1/auth/password-reset") suspend fun nativeReset(@Body form: NativeReset): Response<NativeNotice>

    @GET("api/v1/reporting/quality")
    suspend fun quality(@Header("Authorization") bearer: String, @Header("X-Organisation-Unit") orgUnit: String): Response<QualitySnapshot>
    @GET("api/v1/me")
    suspend fun me(@Header("Authorization") bearer: String): Response<Profile>
    @PUT("api/v1/encounters/{id}")
    suspend fun submit(@Path("id") id: String, @Header("Authorization") bearer: String,
                       @Header("X-Organisation-Unit") orgUnit: String, @Body body: Submission): Response<Receipt>
    @GET("api/v1/encounters/{id}")
    suspend fun receipt(@Path("id") id: String, @Header("Authorization") bearer: String,
                        @Header("X-Organisation-Unit") orgUnit: String): Response<Receipt>
}

class HttpFailure(val status: Int) : IOException("Service returned HTTP $status")
fun <T> Response<T>.checkedBody(): T {
    if (!isSuccessful) throw HttpFailure(code())
    return body() ?: throw IOException("Empty response")
}

interface EncounterQueue {
    fun snapshot(): List<LocalEncounter>
    fun update(item: LocalEncounter, state: DeliveryState)
}

interface EncounterRemote {
    suspend fun submit(item: LocalEncounter): Receipt
    suspend fun receipt(item: LocalEncounter): Receipt
}

data class SyncOutcome(val retry: Boolean, val message: String? = null)

class SyncEngine {
    suspend fun sync(profile: Profile, queue: EncounterQueue, remote: EncounterRemote): SyncOutcome {
        val pending = queue.snapshot().filter { it.belongsTo(profile) &&
            it.state in setOf(DeliveryState.PENDING, DeliveryState.QUEUED) }.take(50)
        for (item in pending) {
            try {
                val receipt = if (item.state == DeliveryState.PENDING) remote.submit(item) else {
                    try { remote.receipt(item) }
                    catch (error: HttpFailure) {
                        if (error.status == 404) remote.submit(item) else throw error
                    }
                }
                queue.update(item, receipt.deliveryState(item.id))
            } catch (error: HttpFailure) {
                when (error.status) {
                    409 -> queue.update(item, DeliveryState.CONFLICT)
                    400, 422 -> queue.update(item, DeliveryState.REJECTED)
                    401, 403 -> return SyncOutcome(false, "Sign in again to continue syncing.")
                    else -> return SyncOutcome(true, "Service unavailable. Records remain saved.")
                }
            } catch (error: IOException) {
                return SyncOutcome(true, "Connection interrupted. Records remain saved.")
            }
        }
        val remaining = queue.snapshot().any { it.belongsTo(profile) &&
            it.state in setOf(DeliveryState.PENDING, DeliveryState.QUEUED) }
        return SyncOutcome(remaining)
    }
}
