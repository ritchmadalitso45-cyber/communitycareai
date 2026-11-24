package com.communitycare.ai.data

import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BackendContractTest {
    @Test fun encounterRequestMatchesHostedContractAndUsesBearerToken() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val id = "a810227c-273f-486c-90c3-810fe71a2c33"
            server.enqueue(MockResponse().setResponseCode(202).setBody("""{"encounterId":"$id","state":"QUEUED"}"""))
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .client(OkHttpClient()).addConverterFactory(GsonConverterFactory.create()).build().create(BackendApi::class.java)
            val response = api.submit(id, "Bearer test-token", "Abcdef12345", Submission("2026-01-01T00:00:00Z", Triage.URGENT_REFERRAL))
            assertEquals(DeliveryState.QUEUED, response.checkedBody().deliveryState(id))
            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("/api/v1/encounters/$id", request.path)
            assertEquals("Bearer test-token", request.getHeader("Authorization"))
            assertEquals("Abcdef12345", request.getHeader("X-Organisation-Unit"))
            val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals(setOf("occurredAt", "triage"), body.keySet())
            assertEquals("URGENT_REFERRAL", body.get("triage").asString)
        } finally { server.shutdown() }
    }

    @Test fun profileResponseMatchesAuthenticatedAccountContract() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"subject":"worker","name":"Community Worker","username":"worker01","orgUnit":"Abcdef12345"}"""))
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build().create(BackendApi::class.java)
            val profile = api.me("Bearer test-token").checkedBody().validate()
            assertEquals("worker", profile.subject)
            assertEquals("Abcdef12345", profile.orgUnit)
            assertEquals("/api/v1/me", server.takeRequest().path)
        } finally { server.shutdown() }
    }
}
