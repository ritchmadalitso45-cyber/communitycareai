package com.communitycare.ai.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.communitycare.ai.AppContainer
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable fun PendingRoleScreen(app:AppContainer,onLogout:()->Unit) {
    val scope=rememberCoroutineScope();var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("Staff role awaiting approval",style=MaterialTheme.typography.headlineMedium)
        Text("Your account is created. A Supervisor must approve your staff role before operational tasks become available.")
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Button(enabled=!busy,onClick={busy=true;scope.launch{try{app.auth.refreshProfile(app.api);error=null}catch(c:CancellationException){throw c}catch(_:Exception){error="Could not refresh. Check your connection and try again."}finally{busy=false}}}){Text("Check approval")}
        TextButton(onClick=onLogout){Text("Sign out")}
    }
}
