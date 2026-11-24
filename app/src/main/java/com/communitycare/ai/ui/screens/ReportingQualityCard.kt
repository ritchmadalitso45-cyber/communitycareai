package com.communitycare.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.communitycare.ai.data.*
import com.communitycare.ai.ui.theme.*
import java.util.Locale

@Composable
fun ReportingQualityCard(state: QualityUiState, onRefresh: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reporting-quality AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Live DHIS2 · Experimental", style = MaterialTheme.typography.labelMedium, color = TealDark)
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.snapshot?.let { data ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QualityNumber("Reports", data.reportCount.toString(), Modifier.weight(1f))
                    QualityNumber("Late >24h", data.delayedOver24Hours.toString(), Modifier.weight(1f))
                    QualityNumber("Review", if (data.modelStatus == "INSUFFICIENT_DATA") "—" else data.candidates.count { it.reviewSuggested }.toString(), Modifier.weight(1f))
                }
                listOf(Triage.ROUTINE, Triage.REFERRAL, Triage.URGENT_REFERRAL).forEach { category ->
                    val count = data.categoryCounts[category.name] ?: 0
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category.label, style = MaterialTheme.typography.bodySmall)
                        Text(count.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(progress = { if (data.validCount == 0) 0f else count.toFloat() / data.validCount },
                        modifier = Modifier.fillMaxWidth().height(8.dp), color = when(category) {
                            Triage.ROUTINE -> TealPrimary
                            Triage.REFERRAL -> AccentAmber
                            Triage.URGENT_REFERRAL -> AccentRed
                        }, trackColor = TealLight)
                }
                Text("Pilot data · Includes test reports", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                if (data.modelStatus == "INSUFFICIENT_DATA") Text("More reports needed for AI review.", style = MaterialTheme.typography.bodySmall)
                if (expanded) {
                    HorizontalDivider()
                    Text("Routine ${data.categoryCounts[Triage.ROUTINE.name]} · Referral ${data.categoryCounts[Triage.REFERRAL.name]} · Urgent ${data.categoryCounts[Triage.URGENT_REFERRAL.name]}")
                    Text("${data.validCount} valid · ${data.invalidCount} invalid. Delayed means more than 24 hours after assessment.")
                    Text("The model uses ${data.trainingSamples} reports. These experimental scores are not clinical decisions or probabilities.")
                    if (data.modelStatus == "EXPERIMENTAL_SMALL_SAMPLE") Text("Small sample: patterns are particularly unreliable.")
                    data.candidates.forEach { item ->
                        Text("${item.eventId}: score ${String.format(Locale.ROOT, "%.3f", item.anomalyScore)} · ${String.format(Locale.ROOT, "%.1f", item.delayHours)} hours late", style = MaterialTheme.typography.bodySmall)
                    }
                    if (data.truncated) Text("Latest ${data.snapshotLimit} reports only.")
                    Text("Updated ${data.generatedAt}\nModel ${data.modelVersion}\nData ${data.datasetFingerprint}", style = MaterialTheme.typography.bodySmall)
                    Text(data.notice, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh, enabled = !state.loading) { Text("Refresh") }
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide details" else "Details") }
            }
        }
    }
}

@Composable private fun QualityNumber(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(TealLight, MaterialTheme.shapes.medium).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TealDark)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TealDark)
    }
}
