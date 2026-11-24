package com.communitycare.ai.data
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class RolePoliciesTest {
    private fun profile(role:String?)=Profile("worker", "Grace Phiri", "grace", "Abcdef12345",role)
    private fun case(status:String,assigned:String?="worker")=CareCase("12345678-1234-1234-1234-123456789012","Outreach visit","Follow-up requested by the community team.","ROUTINE","worker","HSA",assigned,status,1,"2026-11-24T09:00:00Z",emptyList())
    @Test fun hsaHasCommunityFollowUpButNoClinicalClosure() {
        val actions=actionsFor(profile("HSA"),case("FOLLOW_UP")).map{it.key}
        assertTrue("COMPLETE_FOLLOW_UP" in actions);assertFalse("CLOSE" in actions)
        assertFalse(actionsFor(profile("HSA"),case("CLINICAL_REVIEW")).any{it.key=="ESCALATE"})
    }
    @Test fun nurseCanEscalateOwnReviewButCannotCloseClinically() {
        val actions=actionsFor(profile("Nurse"),case("NURSE_REVIEW")).map{it.key}
        assertTrue("ESCALATE" in actions && "FOLLOW_UP" in actions);assertFalse("CLOSE" in actions)
        assertTrue(actionsFor(profile("Nurse"),case("NURSE_REVIEW","another-worker")).isEmpty())
    }
    @Test fun clinicianClosesOnlyTheirOwnClinicalReview() {
        assertTrue(actionsFor(profile("Clinical Officer"),case("CLINICAL_REVIEW")).any{it.key=="CLOSE"})
        assertFalse(actionsFor(profile("Clinical Officer"),case("CLINICAL_REVIEW","another-worker")).any{it.key=="CLOSE"})
    }
    @Test fun supervisorCoordinatesWithoutCreatingClinicalRecords() {
        val supervisor=profile("Supervisor")
        assertFalse(supervisor.canRecordCare())
        val actions=actionsFor(supervisor,case("NURSE_REVIEW")).map{it.key}
        assertTrue("ASSIGN" in actions && "RETURN_TO_NURSE" in actions)
        assertFalse("CLOSE" in actions || "COMPLETE_FOLLOW_UP" in actions)
        assertTrue(actionsFor(supervisor,case("CLOSED",null)).single().key=="REOPEN")
    }
    @Test fun missingLegacyRoleFailsClosedUntilServerRefresh() {
        val old=Gson().fromJson("""{"subject":"worker","name":"Grace Phiri","username":"grace","orgUnit":"Abcdef12345"}""",Profile::class.java).validate()
        assertEquals("PENDING",old.assignedRole());assertFalse(old.canRecordCare())
    }
    @Test fun supervisorAndPendingRolesCannotWriteLocalPatientRecords() {
        var writes=0;val store=ClinicStore({null},{writes++})
        listOf("Supervisor","PENDING").forEach{role->assertThrows(IllegalArgumentException::class.java){store.addPatient(profile(role),"Community Care Reviewer",30)}}
        assertEquals(0,writes)
    }
    @Test fun workspaceRejectsAnotherFacilityOrRole() {
        assertThrows(IllegalArgumentException::class.java){CaseInbox("Zbcdef12345","HSA",emptyList(),emptyMap(),false).validate(profile("HSA"))}
        assertThrows(IllegalArgumentException::class.java){CaseInbox("Abcdef12345","Supervisor",emptyList(),emptyMap(),false).validate(profile("HSA"))}
    }
}
