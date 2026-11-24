package com.communitycare.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun MessagesScreen(profile: Profile, app: AppContainer) {
    var messages by remember { mutableStateOf<List<TeamMessage>>(emptyList()) }
    var draft by rememberSaveable { mutableStateOf("") }
    var draftId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var sending by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun refresh() {
        try {
            val result = app.api.messages("Bearer ${app.auth.accessToken(profile, app.api)}", profile.orgUnit).checkedBody().validate(profile)
            if (app.auth.profile.value == profile) { messages = result.messages; error = null }
        } catch (c: CancellationException) { throw c }
        catch (_: Exception) { error = "Can't refresh messages. Check your connection." }
        finally { loading = false }
    }
    LaunchedEffect(profile) { while (isActive) { refresh(); delay(10_000) } }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Messages", style = MaterialTheme.typography.headlineMedium, color = TealDark, fontWeight = FontWeight.Bold)
                Text("Facility team", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            IconButton(onClick = { scope.launch { refresh() } }) { Icon(Icons.Default.Refresh, "Refresh messages") }
            IconButton(onClick = { info = true }) { Icon(Icons.Default.Info, "About team chat") }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = true,
            contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages.asReversed(), key = { it.id }) { message ->
                val mine = message.senderId == profile.subject
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Surface(color = if (mine) TealLight else White, shape = MaterialTheme.shapes.large,
                        modifier = Modifier.widthIn(max = 290.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (mine) "You" else message.senderName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = TealDark)
                            Text(message.text)
                            Text(DateTimeFormatter.ofPattern("dd MMM, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(message.sentAt)),
                                style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }
            if (messages.isEmpty() && !loading) item { Text("Start a conversation with your facility team.", color = TextSecondary) }
        }
        Row(Modifier.fillMaxWidth().imePadding().padding(bottom = 12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(draft, { if (!sending) { draft = it.take(2000); draftId = UUID.randomUUID().toString() } },
                placeholder = { Text("Write a message…") }, modifier = Modifier.weight(1f), maxLines = 4, enabled = !sending)
            FilledIconButton(enabled = draft.isNotBlank() && !sending, onClick = {
                sending = true; error = null
                scope.launch {
                    try {
                        val sent = app.api.sendMessage(draftId, "Bearer ${app.auth.accessToken(profile, app.api)}", profile.orgUnit,
                            MessageForm(draft.trim())).checkedBody().validate()
                        if (sent.id != draftId || sent.senderId != profile.subject || sent.text != draft.trim()) throw java.io.IOException("Unexpected message receipt")
                        if (app.auth.profile.value == profile) {
                            messages = (messages.filterNot { it.id == sent.id } + sent).sortedBy { Instant.parse(it.sentAt) }.takeLast(100)
                            draft = ""; draftId = UUID.randomUUID().toString()
                        }
                    } catch (c: CancellationException) { throw c }
                    catch (_: Exception) { error = "Message not confirmed. Your text is kept here; try sending again." }
                    finally { sending = false }
                }
            }) { if (sending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Send, "Send message") }
        }
    }
    if (info) AlertDialog(onDismissRequest = { info = false }, title = { Text("Facility team chat") },
        text = { Text("Messages are shared with signed-in users assigned to this facility and stored on the server. The latest 100 messages refresh every 10 seconds while this screen is open. Internet is required to send. There are no push notifications yet. Use the shared pilot for test conversations only; don't send patient details.") },
        confirmButton = { TextButton(onClick = { info = false }) { Text("Got it") } })
}
