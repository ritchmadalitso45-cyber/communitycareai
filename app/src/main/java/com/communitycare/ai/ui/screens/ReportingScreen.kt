package com.communitycare.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.communitycare.ai.SyncStatus
import com.communitycare.ai.data.*
import com.communitycare.ai.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportingScreen(profile: Profile, records: List<LocalEncounter>, sync: SyncStatus,
                    error: String?, onRecord: (Triage) -> Boolean, onSync: () -> Unit,
                    onSignIn: () -> Unit, onLogout: () -> Unit, quality: QualityUiState,
                    onRefreshQuality: () -> Unit) {
    var recordDialog by remember { mutableStateOf(false) }
    var logoutDialog by remember { mutableStateOf(false) }
    var triage by remember { mutableStateOf<Triage?>(null) }
    var saveError by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val countToday = records.count { Instant.parse(it.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate() == today }
    val pending = records.count { it.state == DeliveryState.PENDING || it.state == DeliveryState.QUEUED }
    val delivered = records.count { it.state == DeliveryState.FORWARDED }
    val review = records.count { it.state == DeliveryState.REJECTED || it.state == DeliveryState.CONFLICT }
    Scaffold(containerColor = TealSurface, topBar = {
        TopAppBar(title = { Text("Reporting") }, actions = {
            TextButton(onClick = { logoutDialog = true }) { Text("Sign out") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Column(Modifier.fillMaxWidth().background(TealDark).padding(20.dp)) {
                    Text("Assessment reports", color = Color.White,
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(if(profile.canRecordCare()) "Record the outcome you confirmed." else "Supervisor reporting oversight", color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Button(enabled = profile.canRecordCare(), onClick = { triage = null; saveError = false; recordDialog = true }) { Text("New report") }
                }
            }
            item { ReportingQualityCard(quality, onRefreshQuality) }
            item { Text("On this phone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric("Saved today", countToday, Modifier.weight(1f))
                    Metric("Awaiting delivery", pending, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric("Delivered", delivered, Modifier.weight(1f))
                    Metric("Needs review", review, Modifier.weight(1f))
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (sync.busy) "Checking delivery…" else "Delivery to DHIS2", fontWeight = FontWeight.Bold)
                        Text(sync.message ?: if (pending > 0) "$pending reports awaiting confirmation. They remain saved on this device."
                            else "All saved reports delivered.")
                        sync.checkedAt?.let { Text("Last checked: ${displayTime(it)}", style = MaterialTheme.typography.bodySmall) }
                        Button(onClick = onSync, enabled = !sync.busy) { Text("Sync now") }
                        if (sync.message?.contains("Sign in") == true) {
                            TextButton(onClick = onSignIn) { Text("Sign in again") }
                        }
                    }
                }
            }
            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            item { Text("Recent reports", style = MaterialTheme.typography.titleLarge) }
            if (records.isEmpty()) item { Text("No reports saved yet. Record an assessment outcome to begin.") }
            items(records.sortedByDescending { it.occurredAt }.take(50), key = { it.id }) { record ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(record.triage.label, fontWeight = FontWeight.Bold,
                            color = if (record.triage == Triage.URGENT_REFERRAL) AccentRed else TealDark)
                        Text(displayTime(record.occurredAt))
                        Text(record.state.label, color = if (record.state in setOf(DeliveryState.REJECTED, DeliveryState.CONFLICT))
                            MaterialTheme.colorScheme.error else TextSecondary)
                        Text("Reference ${record.id.take(8)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    if (recordDialog) AlertDialog(onDismissRequest = { recordDialog = false },
        title = { Text("Record confirmed triage") }, text = {
            Column {
                Text("Select the outcome you have confirmed using your approved clinical guidance.")
                Triage.entries.forEach { option ->
                    Row {
                        RadioButton(selected = triage == option, onClick = { triage = option })
                        TextButton(onClick = { triage = option }) { Text(option.label) }
                    }
                }
                Text("This report includes the assessment time and triage category.", style = MaterialTheme.typography.bodySmall)
                if (saveError) Text("This report was not saved. Keep this screen open and try again.", color = MaterialTheme.colorScheme.error)
            }
        }, confirmButton = { Button(enabled = triage != null, onClick = {
            val selected = triage
            if (selected != null) {
                val saved = onRecord(selected)
                saveError = !saved
                if (saved) recordDialog = false
            }
        }) { Text("Save report") } },
        dismissButton = { TextButton(onClick = { recordDialog = false }) { Text("Cancel") } })
    if (logoutDialog) AlertDialog(onDismissRequest = { logoutDialog = false }, title = { Text("Sign out?") },
        text = { Text("Your reports stay encrypted on this device. Sign in with the same account to view or sync them again.") },
        confirmButton = { TextButton(onClick = { logoutDialog = false; onLogout() }) { Text("Sign out") } },
        dismissButton = { TextButton(onClick = { logoutDialog = false }) { Text("Cancel") } })
}

@Composable private fun Metric(label: String, count: Int, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium, color = TealDark)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
private fun displayTime(value: String) = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    .withZone(ZoneId.systemDefault()).format(Instant.parse(value))
