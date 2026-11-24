package com.communitycare.ai

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.communitycare.ai.data.*
import com.communitycare.ai.navigation.AppNavGraph
import com.communitycare.ai.sync.EncounterSyncWorker
import com.communitycare.ai.ui.theme.CommunityCareTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var busy by mutableStateOf(false)
    private var error by mutableStateOf<String?>(null)
    private var accountMessage by mutableStateOf<String?>(null)
    private val container get() = (application as CommunityCareApplication).container
    private val authorization = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        lifecycleScope.launch {
            busy = true
            try {
                container?.let {
                    it.auth.completeLogin(result.data, it.api)
                    it.syncStatus.value = SyncStatus()
                }
                EncounterSyncWorker.schedule(this@MainActivity)
                error = null
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = signInError(failure) }
            finally { busy = false }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setOnExitAnimationListener { screen ->
            screen.view.animate().alpha(0f).setDuration(220L)
                .withEndAction { screen.remove() }.start()
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            CommunityCareTheme {
                Surface(Modifier.fillMaxSize()) {
                    val app = container
                    if (app == null) {
                        Text((application as CommunityCareApplication).startupError ?: "Unable to start the application.",
                            modifier = Modifier.safeDrawingPadding().padding(24.dp))
                    } else {
                        AppNavGraph(rememberNavController(), app, busy, error, message = accountMessage,
                            onSignIn = { username, password, remember -> accountAction("login") { service ->
                                service.auth.loginNative(username, password, remember, service.api)
                                service.syncStatus.value = SyncStatus()
                                EncounterSyncWorker.schedule(this@MainActivity)
                                null
                            } },
                            onRegister = { name, email, phone, role, password -> accountAction("register") { service ->
                                val result = service.api.nativeRegister(NativeRegistration(name.trim(), email.trim(), phone.trim(), role, password)).checkedBody().validate()
                                if (result.status == "ENGINEERING_PILOT") {
                                    service.auth.loginNative(email.trim(), password, true, service.api)
                                    EncounterSyncWorker.schedule(this@MainActivity)
                                    null
                                } else "Account created. Your administrator must assign your facility before you sign in."
                            } },
                            onReset = { username -> accountAction("reset") { service ->
                                service.api.nativeReset(NativeReset(username.trim())).checkedBody()
                                "If your account has an email address, reset instructions have been requested."
                            } },
                            onBrowserSignIn = {
                            if (!busy) lifecycleScope.launch {
                                busy = true; error = null
                                try { authorization.launch(app.auth.loginIntent()) }
                                catch (cancelled: CancellationException) { throw cancelled }
                                catch (failure: Exception) { error = signInError(failure) }
                                finally { busy = false }
                            }
                        }, onRecord = { triage ->
                            try {
                                val account = app.auth.profile.value ?: throw IllegalStateException("Sign-in required")
                                app.encounters.add(account, triage)
                                error = null
                                EncounterSyncWorker.schedule(this@MainActivity)
                                true
                            } catch (failure: Exception) {
                                error = "Could not save this report. Please try again before leaving this screen."
                                false
                            }
                        }, onSync = { EncounterSyncWorker.schedule(this@MainActivity) }, onLogout = {
                            lifecycleScope.launch {
                                EncounterSyncWorker.cancelImmediate(this@MainActivity)
                                app.auth.logout()
                                app.syncStatus.value = SyncStatus()
                                app.storage.write("sync-status.bin", null)
                                error = null
                            }
                        })
                    }
                }
            }
        }
    }
    private fun accountAction(action: String, work: suspend (AppContainer) -> String?) {
        if (busy) return
        lifecycleScope.launch {
            busy = true; error = null; accountMessage = null
            try { accountMessage = work(container ?: throw IllegalStateException("App unavailable")) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                error = if (failure is HttpFailure) when(failure.status) {
                    401 -> "Sign-in failed. Check your email or username and password, or complete account setup with your administrator."
                    403 -> "Your account needs an assigned facility. Contact your administrator."
                    409 -> "Account could not be created. If already registered, use Sign In."
                    422 -> "Check your account details and password policy."
                    429 -> "Too many attempts. Please try again later."
                    503 -> if (action == "reset") "Password-reset email is unavailable. Contact your programme administrator." else "Account service is temporarily unavailable. Please try again."
                    else -> "Could not complete this request. Please try again."
                } else "Could not connect. Check your internet connection and try again."
            } finally { busy = false }
        }
    }

    private fun signInError(error: Exception) = if (error is HttpFailure && error.status == 403)
        "Your account needs a facility assignment. Contact your administrator."
    else "Sign-in could not complete. Check your connection and try again."
}
