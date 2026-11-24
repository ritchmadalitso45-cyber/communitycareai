package com.communitycare.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.communitycare.ai.AppContainer
import com.communitycare.ai.data.*
import com.communitycare.ai.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable fun RoleWorkspace(profile: Profile, app: AppContainer) {
    var inbox by remember { mutableStateOf<CaseInbox?>(null) }
    var staff by remember { mutableStateOf<List<StaffMember>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<CareCase?>(null) }
    var action by remember { mutableStateOf<WorkAction?>(null) }
    var creating by remember { mutableStateOf(false) }
    var changeStaff by remember { mutableStateOf<StaffMember?>(null) }
    var showTeam by remember { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    suspend fun bearer()="Bearer ${app.auth.accessToken(profile,app.api)}"
    suspend fun reload(includeStaff: Boolean=true) {
        val data=app.api.caseInbox(bearer(),profile.orgUnit).checkedBody().validate(profile)
        if(app.auth.profile.value!=profile)return
        inbox=data
        if(includeStaff)staff=app.api.staff(bearer(),profile.orgUnit).checkedBody().validate(profile).members
    }
    fun failure(e: Exception): String = if(e is HttpFailure) when(e.status) {
        403 -> "Your role or assignment changed. Refresh your account from Profile."
        404 -> "This record or staff member is not available to your account."
        409 -> "This task changed. Refresh and review its latest status."
        422 -> "Check the note, reference and assigned staff role."
        else -> "Could not reach the task service. Please try again."
    } else "Could not connect. Your existing records are unchanged."
    fun runTask(work: suspend ()->Unit) {
        if(busy)return
        busy=true;error=null
        scope.launch {
            try { work() } catch(c: CancellationException){throw c}
            catch(e: Exception){error=failure(e)} finally{busy=false}
        }
    }
    LaunchedEffect(profile) {
        while(isActive) {
            try { reload(includeStaff=staff.isEmpty()) } catch(c: CancellationException){throw c}
            catch(e:Exception){error=failure(e)}
            delay(15_000)
        }
    }
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Text("${profile.assignedRole()} workspace",style=MaterialTheme.typography.headlineSmall,color=TealDark,fontWeight=FontWeight.Bold)
            Text(when(profile.assignedRole()) {
                "HSA" -> "Community referrals and follow-up visits"
                "Nurse" -> "Nursing reviews and clinical escalation"
                "Clinical Officer" -> "Clinical reviews and recorded outcomes"
                else -> "Team assignments, role approvals and oversight"
            },color=TextSecondary,style=MaterialTheme.typography.bodySmall)
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                if(profile.canRecordCare())Button(enabled=!busy,onClick={creating=true}) { Text(if(profile.assignedRole()=="HSA")"New referral" else "New review") }
                OutlinedButton(enabled=!busy,onClick={runTask{reload()}}){Text("Refresh")}
                if(profile.assignedRole()=="Supervisor")TextButton(onClick={showTeam=!showTeam}){Text(if(showTeam)"Cases" else "Staff roles")}
            }
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        }
        if(showTeam && profile.assignedRole()=="Supervisor") {
            if(staff.isEmpty())item{Text("No staff have loaded. Refresh to try again.")}
            items(staff,key={it.id}) { member ->
                Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=White)) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text(member.name,fontWeight=FontWeight.Bold)
                        Text(if(member.role=="PENDING")"Awaiting approval · ${member.requestedRole}" else member.role,color=TealDark)
                        if(member.id!=profile.subject)TextButton(enabled=!busy,onClick={changeStaff=member}){Text(if(member.role=="PENDING")"Approve role" else "Change role")}
                    }
                }
            }
        } else {
            inbox?.let { data ->
                item {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        WorkspaceMetric("Open",data.counts.filterKeys{it!="CLOSED"}.values.sum(),Modifier.weight(1f))
                        WorkspaceMetric("Closed",data.counts["CLOSED"]?:0,Modifier.weight(1f))
                    }
                    if(data.limited)Text("Showing the latest 100 cases.",style=MaterialTheme.typography.bodySmall)
                }
                if(data.items.isEmpty())item{Text("No cases in your workspace yet.",color=TextSecondary)}
                items(data.items,key={it.id}) { case ->
                    Card(onClick={runTask{selected=app.api.caseDetail(case.id,bearer(),profile.orgUnit).checkedBody().validate()}},colors=CardDefaults.cardColors(containerColor=White)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text(case.patientReference,fontWeight=FontWeight.Bold)
                            Text(statusLabel(case.status),color=TealDark,style=MaterialTheme.typography.labelLarge)
                            if(case.priority=="URGENT")Text("Urgent · Confirmed by worker",color=AccentRed,style=MaterialTheme.typography.labelSmall)
                            Text(case.summary,maxLines=2,overflow=TextOverflow.Ellipsis)
                            Text(if(case.assignedTo==profile.subject)"Assigned to you" else staff.firstOrNull{it.id==case.assignedTo}?.let{"Assigned to ${it.name}"}?:"Unassigned",style=MaterialTheme.typography.bodySmall,color=TextSecondary)
                        }
                    }
                }
            } ?: item { Text("Loading your tasks…",color=TextSecondary) }
        }
        item{Text("Shared engineering pilot · Use test details only",style=MaterialTheme.typography.labelSmall,color=TextSecondary)}
    }
    if(creating)NewCaseDialog(profile,busy,error,onDismiss={if(!busy)creating=false}){id,form->runTask{
        app.api.createCase(id,bearer(),profile.orgUnit,form).checkedBody().validate()
        creating=false;reload(false)
    }}
    selected?.let { case ->
        if(action==null)AlertDialog(onDismissRequest={selected=null},title={Text(case.patientReference)},text={
            Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(statusLabel(case.status),fontWeight=FontWeight.Bold,color=TealDark)
                Text(case.summary)
                case.history.forEach { event ->
                    HorizontalDivider();Text("${event.name} · ${event.actorRole}",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge)
                    Text(event.note)
                    Text("${statusLabel(event.status)} · ${workflowTime(event.at)}",style=MaterialTheme.typography.bodySmall,color=TextSecondary)
                }
                actionsFor(profile,case).forEach { choice ->TextButton(enabled=!busy,onClick={action=choice}){Text(choice.label)} }
            }
        },confirmButton={TextButton(onClick={selected=null}){Text("Close")}})
        else CaseActionDialog(case,action!!,staff,busy,error,onDismiss={if(!busy)action=null}) { command ->runTask {
            app.api.caseAction(case.id,bearer(),profile.orgUnit,command).checkedBody().validate()
            action=null;selected=null;reload(false)
        }}
    }
    changeStaff?.let { member ->StaffRoleDialog(member,busy,error,onDismiss={if(!busy)changeStaff=null}) { form ->runTask {
        app.api.setStaffRole(member.id,bearer(),profile.orgUnit,form).checkedBody()
        changeStaff=null;reload()
    }} }
}

@Composable private fun WorkspaceMetric(label:String,value:Int,modifier:Modifier) {
    Card(modifier,colors=CardDefaults.cardColors(containerColor=TealLight)) { Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(value.toString(),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=TealDark);Text(label,color=TealDark)
    } }
}
@Composable private fun NewCaseDialog(profile:Profile,busy:Boolean,error:String?,onDismiss:()->Unit,onSave:(String,CaseForm)->Unit) {
    var reference by remember{mutableStateOf("")};var summary by remember{mutableStateOf("")};var urgent by remember{mutableStateOf(false)}
    val identity=remember{UUID.randomUUID().toString()}
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(profile.assignedRole()=="HSA")"New community referral" else "New review")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Use a visit reference rather than a patient name.",style=MaterialTheme.typography.bodySmall)
            OutlinedTextField(reference,{reference=it.take(64)},label={Text("Visit reference")},singleLine=true,enabled=!busy)
            OutlinedTextField(summary,{summary=it.take(2000)},label={Text("Worker-confirmed reason for review")},minLines=3,enabled=!busy)
            Row{Checkbox(urgent,{urgent=it},enabled=!busy);Text("Urgent review")}
            error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        }
    },confirmButton={Button(enabled=!busy && reference.isNotBlank() && summary.trim().length>=3,onClick={onSave(identity,CaseForm(reference.trim(),summary.trim(),if(urgent)"URGENT" else "ROUTINE"))}){Text("Save")}},dismissButton={TextButton(enabled=!busy,onClick=onDismiss){Text("Cancel")}})
}
@Composable private fun CaseActionDialog(case:CareCase,action:WorkAction,staff:List<StaffMember>,busy:Boolean,error:String?,onDismiss:()->Unit,onSave:(CaseCommand)->Unit) {
    var note by remember{mutableStateOf("")};var target by remember{mutableStateOf<String?>(null)}
    var commandId by remember{mutableStateOf(UUID.randomUUID().toString())}
    val choices=staff.filter{it.role==action.assignRole}
    AlertDialog(onDismissRequest=onDismiss,title={Text(action.label)},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(case.patientReference)
        if(action.assignRole!=null) {
            Text("Select ${action.assignRole}",fontWeight=FontWeight.Bold)
            choices.forEach{member->Row{RadioButton(target==member.id,{target=member.id;commandId=UUID.randomUUID().toString()},enabled=!busy);TextButton(onClick={target=member.id;commandId=UUID.randomUUID().toString()},enabled=!busy){Text(member.name)}}}
            if(choices.isEmpty())Text("No eligible staff loaded. Close and refresh the team directory.")
        }
        OutlinedTextField(note,{note=it.take(2000);commandId=UUID.randomUUID().toString()},label={Text(if(action.key=="ADD_NOTE")"Confirmed observation or review note" else "Reason and confirmed next step")},minLines=3,enabled=!busy)
        Text("This records your decision. It does not generate clinical advice or contact emergency services.",style=MaterialTheme.typography.bodySmall)
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={Button(enabled=!busy && note.trim().length>=3 && (action.assignRole==null || target!=null),onClick={onSave(CaseCommand(commandId,case.version,action.key,note.trim(),target))}){Text("Confirm")}},dismissButton={TextButton(enabled=!busy,onClick=onDismiss){Text("Cancel")}})
}
@Composable private fun StaffRoleDialog(member:StaffMember,busy:Boolean,error:String?,onDismiss:()->Unit,onSave:(StaffRoleChange)->Unit) {
    var role by remember{mutableStateOf(member.requestedRole)};var reason by remember{mutableStateOf("")}
    var identity by remember{mutableStateOf(UUID.randomUUID().toString())}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Confirm staff role")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(member.name,fontWeight=FontWeight.Bold)
        staffRoles.forEach{r->Row{RadioButton(role==r,{role=r;identity=UUID.randomUUID().toString()},enabled=!busy);TextButton(onClick={role=r;identity=UUID.randomUUID().toString()},enabled=!busy){Text(r)}}}
        OutlinedTextField(reason,{reason=it.take(500);identity=UUID.randomUUID().toString()},label={Text("Approval or change reason")},enabled=!busy)
        Text("Confirm the worker's duties before approval. They must refresh their account to use the new role.",style=MaterialTheme.typography.bodySmall)
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={Button(enabled=!busy && reason.trim().length>=3,onClick={onSave(StaffRoleChange(role,reason.trim(),identity))}){Text("Save role")}},dismissButton={TextButton(enabled=!busy,onClick=onDismiss){Text("Cancel")}})
}
private fun workflowTime(value:String)=DateTimeFormatter.ofPattern("dd MMM, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(value))
