package com.communitycare.ai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.communitycare.ai.data.*
import com.communitycare.ai.ui.theme.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ClinicRecordsScreen(profile: Profile, clinic: ClinicStore, mode: String,
    onAddPatient: () -> Unit, onConsultation: () -> Unit) {
    val snapshot by clinic.snapshot.collectAsState()
    val patients = snapshot.patients.filter { it.belongsTo(profile) }
    val entries = snapshot.entries.filter { it.belongsTo(profile) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<ClinicPatient?>(null) }
    var entryKind by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).safeDrawingPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(when(mode) { "APPOINTMENT" -> "Appointments"; "NOTE" -> "Treatments & notes"; else -> "Patients" },
            style = MaterialTheme.typography.headlineMedium, color = TealDark, fontWeight = FontWeight.Bold)
        Text("Private register · Saved on this phone", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        OutlinedTextField(query, { query = it }, label = { Text("Search patients") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(enabled=profile.canRecordCare(),onClick = onAddPatient) { Text("New patient") }
        if (patients.isEmpty()) Text("No patients saved yet. Add a patient to start your register.")
        if (mode != "PATIENT") {
            Text("Select a patient below to add ${if (mode == "APPOINTMENT") "an appointment" else "a worker-entered note"}.")
            val shown = entries.filter { it.kind == mode }.sortedByDescending { it.at }
            if (shown.isEmpty()) Text(if (mode == "APPOINTMENT") "No appointments recorded." else "No treatment or consultation notes recorded.")
            shown.forEach { entry ->
                val patient = patients.firstOrNull { it.id == entry.patientId }
                if (patient != null) Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(patient.name, fontWeight = FontWeight.Bold)
                        Text(entry.text)
                        Text(clinicTime(entry.at), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        patients.filter { it.name.contains(query, ignoreCase = true) }.sortedByDescending { it.createdAt }.forEach { patient ->
            Card(Modifier.fillMaxWidth().clickable { selected = patient }, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(patient.name, fontWeight = FontWeight.Bold)
                    Text("Age at registration: ${patient.ageAtRegistration} years", color = TextSecondary)
                    Text("Registered ${clinicTime(patient.createdAt)}", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { selected = patient }) { Text("Open patient") }
                }
            }
        }
    }
    selected?.let { patient ->
        if (entryKind == null) AlertDialog(onDismissRequest = { selected = null }, title = { Text(patient.name) },
            text = { Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Age at registration: ${patient.ageAtRegistration}")
                Text("Patient history", fontWeight = FontWeight.Bold)
                val history = entries.filter { it.patientId == patient.id }.sortedByDescending { it.at }
                if (history.isEmpty()) Text("No notes or appointments recorded for this patient.")
                history.forEach { entry ->
                    HorizontalDivider()
                    Text(if (entry.kind == "APPOINTMENT") "Appointment" else "Worker-entered note", fontWeight = FontWeight.Bold)
                    Text(clinicTime(entry.at), style = MaterialTheme.typography.bodySmall)
                    Text(entry.text)
                }
                TextButton(enabled=profile.canRecordCare(),onClick = { entryKind = "NOTE" }) { Text("Add consultation or treatment note") }
                TextButton(enabled=profile.canRecordCare(),onClick = { entryKind = "APPOINTMENT" }) { Text("Schedule appointment") }
                TextButton(onClick = { selected = null; onConsultation() }) { Text("Record confirmed reporting outcome") }
            } }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } })
        else EntryDialog(patient, entryKind!!, onDismiss = { entryKind = null }, onSave = { text, at ->
            clinic.addEntry(profile, patient.id, entryKind!!, text, at)
            entryKind = null; selected = null
        })
    }
}

@Composable fun NewPatientDialog(profile: Profile, clinic: ClinicStore, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("New patient") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Shared pilot: use test patient details only.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(name, { name = it.take(120) }, label = { Text("Patient name") }, singleLine = true)
            OutlinedTextField(age, { age = it.filter(Char::isDigit).take(3) }, label = { Text("Age in years at registration") }, singleLine = true)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { Button(onClick = {
        val number = age.toIntOrNull()
        if (name.isBlank() || number == null || number !in 0..130) error = "Enter a name and an age from 0 to 130."
        else try { clinic.addPatient(profile, name, number); onDismiss() }
        catch (_: Exception) { error = "Could not save this patient. Your existing records have been kept." }
    }) { Text("Save patient") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun EntryDialog(patient: ClinicPatient, kind: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (kind == "APPOINTMENT") "Schedule appointment" else "Care note") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(patient.name)
            OutlinedTextField(text, { text = it.take(2000) }, label = { Text(if (kind == "APPOINTMENT") "Appointment purpose" else "Care note") }, minLines = 3)
            if (kind == "APPOINTMENT") OutlinedTextField(date, { date = it.take(16) }, label = { Text("Local date and time") }, placeholder = { Text("2026-11-25 09:00") }, singleLine = true)
            Text("Saved on this phone. No reminder is sent.", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { Button(onClick = {
        try {
            require(text.isNotBlank())
            val instant = if (kind == "APPOINTMENT") appointmentInstant(date, ZoneId.systemDefault()) else Instant.now()
            onSave(text, instant.toString())
        } catch (_: Exception) { error = "Could not save. Check the text and date, then try again." }
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun clinicTime(value: String) = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(value))
