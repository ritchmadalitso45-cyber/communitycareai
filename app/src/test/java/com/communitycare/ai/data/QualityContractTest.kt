package com.communitycare.ai.data

import com.google.gson.Gson
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class QualityContractTest {
    private val profile = Profile("worker", "Worker", "worker", "Abcdef12345")
    private fun snapshot() = QualitySnapshot(profile.orgUnit, "2026-11-23T08:00:00Z", "OWNED_DHIS2",
        "PILOT_INCLUDES_ENGINEERING_TEST_RECORDS", 6, 6, 0,
        mapOf("ROUTINE" to 0, "REFERRAL" to 6, "URGENT_REFERRAL" to 0),
        0, 200, false, 10, "reporting-iforest-v1", "test-fingerprint",
        "EXPERIMENTAL_SMALL_SAMPLE", 6, 5, "CURRENT_SNAPSHOT", emptyList(), "Experimental only")
    @Test fun validSnapshotRetainsActualCounts() {
        val parsed = Gson().fromJson(Gson().toJson(snapshot()), QualitySnapshot::class.java).validate(profile)
        assertEquals(6, parsed.categoryCounts["REFERRAL"])
        assertEquals(6, parsed.trainingSamples)
    }
    @Test(expected = IOException::class) fun anotherFacilityIsRefused() {
        snapshot().copy(orgUnit = "Zbcdef12345").validate(profile)
    }
    @Test(expected = IOException::class) fun inconsistentTotalsAreRefused() {
        snapshot().copy(reportCount = 100).validate(profile)
    }
    @Test(expected = IOException::class) fun unknownModelStateIsRefused() {
        snapshot().copy(modelStatus = "CLINICALLY_APPROVED").validate(profile)
    }
}
