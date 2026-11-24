package com.communitycare.ai.data

import java.time.Instant
import java.util.UUID

val staffRoles = setOf("HSA", "Nurse", "Clinical Officer", "Supervisor")
val caseStatuses = setOf("AWAITING_NURSE", "NURSE_REVIEW", "AWAITING_CLINICAL_OFFICER", "CLINICAL_REVIEW", "FOLLOW_UP", "CLOSED")
fun Profile.assignedRole() = role?.takeIf { it in staffRoles } ?: "PENDING"
fun Profile.canRecordCare() = assignedRole() in setOf("HSA", "Nurse", "Clinical Officer")
fun statusLabel(value: String) = when(value) {
    "AWAITING_NURSE" -> "Awaiting nurse"
    "NURSE_REVIEW" -> "Nursing review"
    "AWAITING_CLINICAL_OFFICER" -> "Awaiting clinical officer"
    "CLINICAL_REVIEW" -> "Clinical review"
    "FOLLOW_UP" -> "Community follow-up"
    "CLOSED" -> "Closed"
    else -> "Unknown status"
}
data class CaseHistory(val name: String, val note: String, val actorRole: String, val action: String,
    val status: String, val version: Int, val at: String)
data class CareCase(val id: String, val patientReference: String, val summary: String, val priority: String,
    val createdBy: String, val creatorRole: String, val assignedTo: String?, val status: String,
    val version: Int, val updatedAt: String, val history: List<CaseHistory>) {
    fun validate(): CareCase {
        UUID.fromString(id); Instant.parse(updatedAt)
        require(status in caseStatuses && version > 0 && priority in setOf("ROUTINE", "URGENT"))
        require(patientReference.isNotBlank() && patientReference.length<=64 && summary.isNotBlank() && summary.length<=2000)
        require(createdBy.isNotBlank() && creatorRole in staffRoles && history.size<=50)
        history.forEach { require(it.actorRole in staffRoles && it.status in caseStatuses); Instant.parse(it.at) }
        return this
    }
}
data class CaseInbox(val orgUnit: String, val role: String, val items: List<CareCase>, val counts: Map<String,Int>, val limited: Boolean) {
    fun validate(profile: Profile): CaseInbox {
        require(orgUnit==profile.orgUnit && role==profile.assignedRole() && items.size<=100)
        require(counts.all { it.key in caseStatuses && it.value>=0 } && items.map { it.id }.distinct().size==items.size)
        items.forEach { it.validate() };return this
    }
}
data class CaseForm(val patientReference: String, val summary: String, val priority: String)
data class CaseCommand(val commandId: String, val version: Int, val action: String, val note: String, val assigneeId: String? = null)
data class StaffMember(val id: String, val name: String, val username: String, val role: String, val requestedRole: String)
data class StaffDirectory(val orgUnit: String, val members: List<StaffMember>, val limited: Boolean) {
    fun validate(profile: Profile): StaffDirectory {
        require(orgUnit==profile.orgUnit && members.size<=100)
        members.forEach { require(it.id.isNotBlank() && it.name.isNotBlank() && it.role in staffRoles+"PENDING" && it.requestedRole in staffRoles) }
        return this
    }
}
data class StaffRoleChange(val role: String, val reason: String, val requestId: String)
data class WorkAction(val key: String, val label: String, val assignRole: String? = null)
fun actionsFor(profile: Profile, item: CareCase): List<WorkAction> {
    val role=profile.assignedRole(); val mine=item.assignedTo==profile.subject
    val actions=mutableListOf<WorkAction>()
    when(role) {
        "HSA" -> if (item.status=="FOLLOW_UP" && mine) actions+=WorkAction("COMPLETE_FOLLOW_UP","Complete follow-up")
        "Nurse" -> {
            if (item.status=="AWAITING_NURSE" && item.assignedTo==null) actions+=WorkAction("CLAIM","Start nursing review")
            if (item.status=="NURSE_REVIEW" && mine) {
                actions+=WorkAction("ESCALATE","Refer to clinical officer")
                actions+=WorkAction("FOLLOW_UP","Plan community follow-up","HSA")
            }
        }
        "Clinical Officer" -> {
            if (item.status=="AWAITING_CLINICAL_OFFICER" && item.assignedTo==null) actions+=WorkAction("CLAIM","Start clinical review")
            if (item.status=="CLINICAL_REVIEW" && mine) {
                actions+=WorkAction("FOLLOW_UP","Plan community follow-up","HSA")
                actions+=WorkAction("CLOSE","Close reviewed case")
            }
        }
        "Supervisor" -> {
            if (item.status=="CLOSED") actions+=WorkAction("REOPEN","Reopen for nursing review")
            else {
                val required=when(item.status) {
                    "AWAITING_NURSE","NURSE_REVIEW" -> "Nurse"
                    "AWAITING_CLINICAL_OFFICER","CLINICAL_REVIEW" -> "Clinical Officer"
                    "FOLLOW_UP" -> "HSA"
                    else -> null
                }
                if(required!=null) actions+=WorkAction("ASSIGN","Assign staff member",required)
                actions+=WorkAction("RETURN_TO_NURSE","Return to nursing queue")
            }
        }
    }
    if(item.status!="CLOSED" && (role=="Supervisor" || (role=="HSA" && (item.createdBy==profile.subject || mine)) || (role=="Nurse" && item.status=="NURSE_REVIEW" && mine) || (role=="Clinical Officer" && item.status=="CLINICAL_REVIEW" && mine)))
        actions+=WorkAction("ADD_NOTE",if(role=="Supervisor") "Add coordination note" else "Add review note")
    return actions
}
