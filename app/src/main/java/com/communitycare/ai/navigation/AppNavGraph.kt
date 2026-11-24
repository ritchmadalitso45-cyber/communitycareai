package com.communitycare.ai.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.communitycare.ai.AppContainer
import com.communitycare.ai.data.Triage
import com.communitycare.ai.data.assignedRole
import com.communitycare.ai.data.QualityUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.communitycare.ai.ui.screens.*

@Composable
fun AppNavGraph(navController: NavHostController, container: AppContainer, busy: Boolean,
                error: String?, onSignIn: (String, String, Boolean) -> Unit,
                onRegister: (String, String, String, String, String) -> Unit,
                onReset: (String) -> Unit, onBrowserSignIn: () -> Unit, message: String?, onRecord: (Triage) -> Boolean,
                onSync: () -> Unit, onLogout: () -> Unit) {
    val profile by container.auth.profile.collectAsState()
    val allRecords by container.encounters.records.collectAsState()
    val sync by container.syncStatus.collectAsState()
    val quality by container.quality.state.collectAsState()
    val scope = rememberCoroutineScope()
    val account = profile
    LaunchedEffect(account) {
        if (account == null) container.quality.clear()
        else while (isActive) {
            if(account.assignedRole()=="PENDING") {
                try { container.auth.refreshProfile(container.api) }
                catch(c: kotlinx.coroutines.CancellationException){throw c}
                catch(_: Exception) { }
            } else container.quality.refresh(account)
            delay(30_000)
        }
    }
    LaunchedEffect(sync.checkedAt) {
        if (account != null && sync.checkedAt != null) container.quality.refresh(account)
    }
    if (account != null) {
        key(account.subject, account.orgUnit, account.assignedRole()) {
        if(account.assignedRole()=="PENDING") {
            PendingRoleScreen(container,onLogout)
        } else {
        DashboardScreen(account, allRecords.filter { it.belongsTo(account) }, sync,
            error, onRecord, onSync, onBrowserSignIn, onLogout,
            if (quality.owner == account.subject && quality.orgUnit == account.orgUnit) quality else QualityUiState(),
            { scope.launch { container.quality.refresh(account) } }, container.clinic, container)
        }
        }
        return
    }
    NavHost(navController = navController, startDestination = NavRoutes.GET_STARTED) {
        composable(NavRoutes.GET_STARTED) {
            GetStartedScreen(onGetStarted = { navController.navigate(NavRoutes.SIGN_UP) },
                onSignIn = { navController.navigate(NavRoutes.SIGN_IN) })
        }
        composable(NavRoutes.SIGN_UP) {
            SignUpScreen(onSignUp = onRegister, onSignIn = { navController.navigate(NavRoutes.SIGN_IN) }, busy = busy, error = error, message = message)
        }
        composable(NavRoutes.SIGN_IN) {
            SignInScreen(onSignIn, { navController.navigate(NavRoutes.SIGN_UP) }, busy, error, message, onReset, onBrowserSignIn)
        }
    }
}
