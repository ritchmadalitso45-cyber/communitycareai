package com.communitycare.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.communitycare.ai.ui.theme.*
import com.communitycare.ai.data.*
import com.communitycare.ai.SyncStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

// ── Data models ───────────────────────────────────────────────────────────

data class QuickAction(val label: String, val icon: ImageVector, val color: Color)
data class RecentPatient(
    val name: String,
    val age: String,
    val diagnosis: String,
    val severity: String,   // "critical" | "moderate" | "stable"
    val time: String,
    val initials: String
)
data class HealthTip(val title: String, val body: String, val icon: ImageVector)

// ── Screen ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(profile: Profile, records: List<LocalEncounter>, sync: SyncStatus,
                    error: String?, onRecord: (Triage) -> Boolean, onSync: () -> Unit,
                    onSignIn: () -> Unit, onLogout: () -> Unit, quality: QualityUiState,
                    onRefreshQuality: () -> Unit, clinic: ClinicStore, app: com.communitycare.ai.AppContainer) {
    var selectedTab by remember { mutableStateOf(0) }
    var clinicMode by remember { mutableStateOf("PATIENT") }
    var searchQuery by remember { mutableStateOf("") }
    var newPatient by remember { mutableStateOf(false) }
    var showStatus by remember { mutableStateOf(false) }
    var roleNotice by remember { mutableStateOf(false) }
    var workMode by remember { mutableStateOf(false) }
    var accountBusy by remember { mutableStateOf(false) }
    var accountError by remember { mutableStateOf<String?>(null) }
    val roleScope = rememberCoroutineScope()
    val local by clinic.snapshot.collectAsState()
    val patients = local.patients.filter { it.belongsTo(profile) }
    val entries = local.entries.filter { it.belongsTo(profile) }
    val today = java.time.LocalDate.now()
    val pending = records.count { it.state == DeliveryState.PENDING || it.state == DeliveryState.QUEUED }
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val recentPatients = patients.filter { it.name.contains(searchQuery, ignoreCase = true) }
        .sortedByDescending { it.createdAt }.take(5).map { p ->
            val last = entries.filter { it.patientId == p.id && it.kind == "NOTE" }.maxByOrNull { it.at }
            RecentPatient(p.name, "${p.ageAtRegistration} yrs at registration", last?.text ?: "No clinical note recorded",
                "unassessed", originalTime(p.createdAt), originalInitials(p.name))
        }
    val statCards = listOf(
        StatItem(patients.count { java.time.Instant.parse(it.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == today }.toString(), "Registered today", Icons.Filled.Group, TealPrimary),
        StatItem(records.count { it.triage != Triage.ROUTINE && it.state == DeliveryState.FORWARDED }.toString(), "Referrals reported", Icons.Filled.Send, AccentAmber),
        StatItem(entries.count { it.kind == "NOTE" }.toString(), "Clinical notes", Icons.Filled.CheckCircle, AccentGreen),
        StatItem(records.count { it.triage == Triage.URGENT_REFERRAL }.toString(), "Urgent reports", Icons.Filled.Emergency, AccentRed)
    )
    val quickActions = listOf(
        QuickAction("New Patient", Icons.Filled.PersonAdd, TealPrimary),
        QuickAction("Appointments", Icons.Filled.CalendarMonth, Color(0xFF5390D9)),
        QuickAction("Treatments", Icons.Filled.MedicalServices, AccentGreen),
        QuickAction("Emergency", Icons.Filled.Emergency, AccentRed)
    )
    val healthTips = listOf(
        HealthTip("Delivery check", "$pending reports await DHIS2 delivery confirmation.", Icons.Filled.Sync),
        HealthTip("Human confirmed care", "Record the outcome you have confirmed using your approved clinical guidance.", Icons.Filled.VerifiedUser),
        HealthTip("Reporting quality AI", "Review live facility reporting patterns in the Assess tab. Scores are experimental.", Icons.Filled.Analytics)
    )
    Scaffold(containerColor = Color(0xFFF0FAFA), bottomBar = {
        BottomNavBar(selectedTab) { selectedTab = it; if (it == 1) clinicMode = "PATIENT"; if (it == 2) workMode = true }
    }) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            when (selectedTab) {
                1 -> ClinicRecordsScreen(profile, clinic, clinicMode, { if(profile.canRecordCare()) newPatient = true else roleNotice = true }, { selectedTab = 2 })
                2 -> Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(selected=workMode,onClick={workMode=true},label={Text("My role tasks")})
                        FilterChip(selected=!workMode,onClick={workMode=false},label={Text("Reports & AI")})
                    }
                    Box(Modifier.weight(1f)) {
                        if(workMode) RoleWorkspace(profile,app)
                        else ReportingScreen(profile, records, sync, error, onRecord, onSync, onSignIn, onLogout, quality, onRefreshQuality)
                    }
                }
                3 -> MessagesScreen(profile,app)
                4 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("My profile", style = MaterialTheme.typography.headlineMedium, color = TealDark)
                    Text(profile.name, style = MaterialTheme.typography.titleLarge)
                    Text("Username: ${profile.username}")
                    Text("Role: ${profile.assignedRole()}", color = TealDark, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick={workMode=true;selectedTab=2}) { Text("Open my role tasks") }
                    OutlinedButton(enabled=!accountBusy,onClick={
                        accountBusy=true;accountError=null
                        roleScope.launch {
                            try { app.auth.refreshProfile(app.api) }
                            catch(c: CancellationException){throw c}
                            catch(_: Exception){accountError="Could not refresh your account. Try again."}
                            finally{accountBusy=false}
                        }
                    }) { Text(if(accountBusy) "Refreshing…" else "Refresh role and account") }
                    accountError?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                    Text("Assigned facility: ${profile.orgUnit}")
                    Text("Your facility is assigned by an administrator. Patient records stay encrypted on this phone; aggregate reports sync to DHIS2.")
                    OutlinedButton(onClick = { uri.openUri("https://care-login.95.111.247.118.sslip.io/realms/communitycare/account/") }) { Text("Manage my account") }
                    Text("New users can register in the app. Email recovery is awaiting Gmail setup.", color = TextSecondary)
                    OutlinedButton(onClick = { showStatus = true }) { Text("Reporting and delivery status") }
                    Button(onClick = onLogout) { Text("Sign out") }
                }
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    HeroBanner(searchQuery, { searchQuery = it }, profile.name,
                        { workMode = false; selectedTab = 2 }, { showStatus = true })
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader("Quick Actions", false)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            quickActions.forEachIndexed { index, action -> QuickActionCard(action) {
                                when(index) {
                                    0 -> if(profile.canRecordCare()) newPatient = true else roleNotice = true
                                    1 -> { clinicMode = "APPOINTMENT"; selectedTab = 1 }
                                    2 -> { clinicMode = "NOTE"; selectedTab = 1 }
                                    else -> { workMode = false; selectedTab = 2 }
                                }
                            } }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(statCards) { StatCard(it) }
                    }
                    Spacer(Modifier.height(20.dp))
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Recent Patients", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            TextButton(onClick = { clinicMode = "PATIENT"; selectedTab = 1 }) { Text("View All ›") }
                        }
                        if (recentPatients.isEmpty()) Surface(shape = RoundedCornerShape(16.dp), color = White) {
                            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                                Text(if (searchQuery.isBlank()) "Your patient register is empty." else "No patients match your search.")
                                Text("Saved records will appear here. No sample patients are inserted.", color = TextSecondary, fontSize = 12.sp)
                                TextButton(onClick = { if(profile.canRecordCare()) newPatient = true else roleNotice = true }) { Text("Add a patient") }
                            }
                        }
                        recentPatients.forEach { patient ->
                            PatientCard(patient) { clinicMode = "PATIENT"; selectedTab = 1 }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.padding(horizontal = 16.dp)) { SectionHeader("Care and reporting reminders", false) }
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(healthTips) { HealthTipCard(it) }
                    }
                    Spacer(Modifier.height(20.dp))
                    SyncStatusBanner(pending, sync, onSync)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
    if (roleNotice) AlertDialog(onDismissRequest={roleNotice=false},title={Text("Supervisor workspace")},
        text={Text("Your role coordinates staff and reviews reporting. Open My role tasks for assignments and oversight.")},
        confirmButton={TextButton(onClick={roleNotice=false;workMode=true;selectedTab=2}){Text("Open my tasks")}},
        dismissButton={TextButton(onClick={roleNotice=false}){Text("Close")}})
    if (newPatient) NewPatientDialog(profile, clinic) { newPatient = false }
    if (showStatus) AlertDialog(onDismissRequest = { showStatus = false }, title = { Text("Reporting status") },
        text = { Text(sync.message ?: "$pending reports await delivery confirmation. Open Assess to review receipts and live reporting-quality analysis.") },
        confirmButton = { TextButton(onClick = { showStatus = false; selectedTab = 2 }) { Text("Open reporting") } },
        dismissButton = { TextButton(onClick = { showStatus = false }) { Text("Close") } })
}

private fun originalInitials(name: String) = name.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
private fun originalTime(value: String) = java.time.format.DateTimeFormatter.ofPattern("dd MMM, HH:mm").withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.parse(value))


// ── Hero Banner ───────────────────────────────────────────────────────────

@Composable
private fun HeroBanner(searchQuery: String, onSearchChange: (String) -> Unit, name: String, onConsultation: () -> Unit, onStatus: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(TealPrimary, TealDark))
            )
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-20).dp)
                .clip(CircleShape)
                .background(White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-10).dp, y = 30.dp)
                .clip(CircleShape)
                .background(White.copy(alpha = 0.08f))
        )

        Column(modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 52.dp, bottom = 24.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f).padding(end=8.dp)) {
                    Text("Welcome back", fontSize = 13.sp, color = White.copy(alpha = 0.80f))
                    Text(name, fontWeight = FontWeight.Black, fontSize = 20.sp, color = White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onStatus) {
                        Icon(Icons.Filled.Notifications, null, tint = White, modifier = Modifier.size(24.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(TealLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(originalInitials(name), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TealDark)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Hero sub-headline
            Text(
                "Caring for Your\nCommunity with AI",
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = White,
                lineHeight = 28.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Offline reporting · DHIS2 · reporting-quality AI",
                fontSize = 12.sp,
                color = White.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onConsultation,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = White),
                elevation = ButtonDefaults.buttonElevation(4.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Filled.PersonAdd, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start New Consultation", fontWeight = FontWeight.Bold, color = TealPrimary, fontSize = 14.sp)
            }
        }
    }

    // Search bar below the banner
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-20).dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search saved patients…", fontSize = 13.sp, color = Color(0xFF9DB5BC)) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = TealPrimary) },
            trailingIcon = { Icon(Icons.Filled.Tune, null, tint = TealPrimary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = TealPrimary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor   = White,
                unfocusedContainerColor = White
            )
        )
    }
}

// ── Quick Action Card ─────────────────────────────────────────────────────

@Composable
private fun QuickActionCard(action: QuickAction, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(action.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(action.icon, contentDescription = action.label, tint = action.color, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(action.label, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ── Stats ─────────────────────────────────────────────────────────────────

data class StatItem(val value: String, val label: String, val icon: ImageVector, val color: Color)

@Composable
private fun StatCard(stat: StatItem) {
    Surface(
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(stat.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(stat.icon, null, tint = stat.color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(stat.value, fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextPrimary)
                Text(stat.label, fontSize = 11.sp, color = TextSecondary, lineHeight = 14.sp)
            }
        }
    }
}

// ── Patient Card ──────────────────────────────────────────────────────────

@Composable
private fun PatientCard(patient: RecentPatient, onClick: () -> Unit) {
    val severityColor = when (patient.severity) {
        "critical" -> AccentRed
        "moderate" -> AccentAmber
        else       -> TextSecondary
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(TealLight.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(patient.initials, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TealDark)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(patient.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                Text("${patient.age}  ·  ${patient.diagnosis}",
                    fontSize = 12.sp, color = TextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccessTime, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(patient.time, fontSize = 11.sp, color = TextSecondary)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(severityColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(patient.severity.replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp, color = severityColor, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
                Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Health Tip Card ───────────────────────────────────────────────────────

@Composable
private fun HealthTipCard(tip: HealthTip) {
    Surface(
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TealPrimary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tip.icon, null, tint = TealPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(tip.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(tip.body, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
        }
    }
}

// ── Sync Status Banner ────────────────────────────────────────────────────

@Composable
private fun SyncStatusBanner(pending: Int, sync: SyncStatus, onSync: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = TealPrimary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TealPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Sync, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (sync.busy) "Checking DHIS2 delivery" else "Reporting status", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TealDark)
                Text(sync.message ?: "$pending reports await confirmation. Last checked: ${sync.checkedAt?.let { originalTime(it) } ?: "not yet"}",
                    fontSize = 11.sp, color = TextSecondary)
            }
            TextButton(onClick = onSync, enabled = !sync.busy) {
                Text("Sync", color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, showAll: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        if (showAll) {
            TextButton(onClick = {}) {
                Text("View All  ›", fontSize = 13.sp, color = TealPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Bottom Navigation Bar ─────────────────────────────────────────────────

@Composable
private fun BottomNavBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Triple("Home",       Icons.Filled.Home,            Icons.Outlined.Home),
        Triple("Patients",   Icons.Filled.Group,           Icons.Outlined.Group),
        Triple("Assess",   Icons.Filled.MonitorHeart,    Icons.Outlined.MonitorHeart),
        Triple("Messages",   Icons.Filled.Message,         Icons.Outlined.Message),
        Triple("Profile",    Icons.Filled.Person,          Icons.Outlined.Person)
    )

    NavigationBar(
        containerColor = White,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { idx, (label, filledIcon, outlinedIcon) ->
            NavigationBarItem(
                selected = selected == idx,
                onClick  = { onSelect(idx) },
                icon = {
                    if (idx == 2) {
                        // Prominent centre FAB-style button
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected == idx) TealPrimary
                                    else TealPrimary.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                filledIcon, null,
                                tint = if (selected == idx) White else TealPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    } else {
                        Icon(
                            if (selected == idx) filledIcon else outlinedIcon,
                            contentDescription = label,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = TealPrimary,
                    selectedTextColor   = TealPrimary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor      = TealPrimary.copy(alpha = 0.08f)
                )
            )
        }
    }
}
