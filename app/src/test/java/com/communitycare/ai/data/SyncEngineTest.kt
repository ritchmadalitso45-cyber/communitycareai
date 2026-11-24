package com.communitycare.ai.data

import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SyncEngineTest {
    private val profile = Profile("worker-a", "Worker", "worker", "Abcdef12345")
    private fun item(state: DeliveryState = DeliveryState.PENDING) = LocalEncounter.create(profile, Triage.REFERRAL).copy(state = state)
    private class Queue(items: List<LocalEncounter>) : EncounterQueue {
        val items = items.toMutableList()
        override fun snapshot() = items.toList()
        override fun update(item: LocalEncounter, state: DeliveryState) {
            val index = items.indexOfFirst { it.id == item.id }
            items[index] = items[index].copy(state = state)
        }
    }
    private class Remote(var state: String = "QUEUED", var failure: IOException? = null) : EncounterRemote {
        val submitted = mutableListOf<LocalEncounter>()
        var polls = 0
        override suspend fun submit(item: LocalEncounter): Receipt {
            submitted.add(item)
            failure?.let { throw it }
            return Receipt(item.id, state)
        }
        override suspend fun receipt(item: LocalEncounter): Receipt {
            polls++
            failure?.let { throw it }
            return Receipt(item.id, state)
        }
    }

    @Test fun acceptedIsNotDelivered() = runTest {
        val queue = Queue(listOf(item()))
        val outcome = SyncEngine().sync(profile, queue, Remote())
        assertEquals(DeliveryState.QUEUED, queue.items.single().state)
        assertTrue(outcome.retry)
    }

    @Test fun queuedRecordIsPolledAndOnlyConfirmedReceiptIsDelivered() = runTest {
        val queue = Queue(listOf(item(DeliveryState.QUEUED)))
        val remote = Remote("FORWARDED")
        assertFalse(SyncEngine().sync(profile, queue, remote).retry)
        assertEquals(1, remote.polls)
        assertTrue(remote.submitted.isEmpty())
        assertEquals(DeliveryState.FORWARDED, queue.items.single().state)
    }

    @Test fun networkRetryPreservesUuidAndPayload() = runTest {
        val record = item()
        val queue = Queue(listOf(record))
        val remote = Remote(failure = IOException("offline"))
        assertTrue(SyncEngine().sync(profile, queue, remote).retry)
        assertEquals(record, queue.items.single())
        remote.failure = null
        SyncEngine().sync(profile, queue, remote)
        assertEquals(remote.submitted[0].id, remote.submitted[1].id)
        assertEquals(remote.submitted[0].submission, remote.submitted[1].submission)
    }

    @Test fun anotherUsersOrOrganisationsReportsAreNeverSent() = runTest {
        val queue = Queue(listOf(item().copy(owner = "another-user"), item().copy(orgUnit = "Zbcdef12345")))
        val remote = Remote()
        SyncEngine().sync(profile, queue, remote)
        assertTrue(remote.submitted.isEmpty())
        assertTrue(queue.items.all { it.state == DeliveryState.PENDING })
    }

    @Test fun permanentErrorsRequireReviewAndStopRetryingThatRecord() = runTest {
        for ((code, expected) in listOf(409 to DeliveryState.CONFLICT, 422 to DeliveryState.REJECTED)) {
            val queue = Queue(listOf(item()))
            assertFalse(SyncEngine().sync(profile, queue, Remote(failure = HttpFailure(code))).retry)
            assertEquals(expected, queue.items.single().state)
        }
    }

    @Test fun authenticationFailureKeepsReportAndRequestsSignIn() = runTest {
        val record = item()
        val queue = Queue(listOf(record))
        val outcome = SyncEngine().sync(profile, queue, Remote(failure = HttpFailure(403)))
        assertEquals(record, queue.items.single())
        assertFalse(outcome.retry)
        assertTrue(outcome.message!!.contains("Sign in"))
    }

    @Test fun missingServerReceiptIsResubmittedWithTheSameUuid() = runTest {
        val record = item(DeliveryState.QUEUED)
        val queue = Queue(listOf(record))
        val remote = object : EncounterRemote {
            override suspend fun receipt(item: LocalEncounter): Receipt = throw HttpFailure(404)
            override suspend fun submit(item: LocalEncounter): Receipt {
                assertEquals(record.id, item.id)
                return Receipt(item.id, "QUEUED")
            }
        }
        assertTrue(SyncEngine().sync(profile, queue, remote).retry)
    }

    @Test fun mismatchedOrUnknownReceiptCannotConfirmDelivery() {
        for (receipt in listOf(Receipt("wrong-id", "FORWARDED"), Receipt("expected", "UNKNOWN"))) {
            try { receipt.deliveryState("expected"); fail("Invalid receipt accepted") }
            catch (expected: IOException) { /* Expected: preserve the report for retry. */ }
        }
    }
}
