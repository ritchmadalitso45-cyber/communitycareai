package com.communitycare.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.communitycare.ai.ui.theme.*

@Composable
fun SignInScreen(
    onSignIn: (String, String, Boolean) -> Unit,
    onSignUp: () -> Unit, busy: Boolean = false, error: String? = null, message: String? = null,
    onReset: (String) -> Unit, onBrowserSignIn: () -> Unit
) {
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe      by remember { mutableStateOf(false) }
    var resetOpen by remember { mutableStateOf(false) }
    var resetIdentity by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFD4F1F4), Color(0xFFB0E8EC), TealLight))
            )
    ) {
        // Top arc
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(bottomStart = 999.dp, bottomEnd = 999.dp))
                .background(Brush.verticalGradient(listOf(TealPrimary, TealDark)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // ── Header ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LocalHospital, null, tint = White, modifier = Modifier.size(38.dp))
            }

            Spacer(Modifier.height(10.dp))
            Text("Welcome Back", fontWeight = FontWeight.Black, fontSize = 24.sp, color = White)
            Text("Sign in to your account", fontSize = 13.sp, color = White.copy(alpha = 0.85f))

            Spacer(Modifier.height(32.dp))

            // ── Card ──────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    (formError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    message?.let { Text(it, color = TealDark) }
                    AuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email or username",
                        leadingIcon = Icons.Filled.Email,
                        keyboardType = KeyboardType.Text
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, null, tint = TealPrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    null, tint = TealPrimary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = authTextFieldColors()
                    )

                    // Remember me + Forgot password row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                            )
                            Text("Remember me", fontSize = 13.sp, color = TextSecondary)
                        }
                        TextButton(onClick = { resetIdentity = email; resetOpen = true }, enabled = !busy) {
                            Text("Forgot Password?", fontSize = 13.sp, color = TealPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) formError = "Enter your email or username and password."
                            else { formError = null; onSignIn(email, password, rememberMe); password = "" }
                        }, enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text(if (busy) "Signing in…" else "Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = White)
                    }

                    TextButton(onClick = onBrowserSignIn, enabled = !busy) { Text("Browser sign-in & account setup") }
                    // ── Divider ───────────────────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
                        Text("  or continue with  ", fontSize = 12.sp, color = TextSecondary)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
                    }

                    Text("Google & Microsoft · Setup pending", fontSize = 11.sp, color = TextSecondary)
                    // ── Social logins ─────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SocialButton(modifier = Modifier.weight(1f), label = "Google", icon = Icons.Filled.Language)
                        SocialButton(modifier = Modifier.weight(1f), label = "Microsoft", icon = Icons.Filled.Computer)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            TextButton(onClick = onSignUp) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = TextSecondary)) { append("Don't have an account?  ") }
                        withStyle(SpanStyle(color = TealPrimary, fontWeight = FontWeight.Bold)) { append("Sign Up") }
                    },
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
    if (resetOpen) AlertDialog(onDismissRequest = { resetOpen = false }, title = { Text("Reset password") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(resetIdentity, { resetIdentity = it }, label = { Text("Email or username") }, singleLine = true)
            Text("Email recovery requires the programme's email service. If unavailable, contact your administrator.", fontSize = 12.sp)
        }
    }, confirmButton = { TextButton(enabled = resetIdentity.isNotBlank() && !busy, onClick = { onReset(resetIdentity); resetOpen = false }) { Text("Request reset") } },
        dismissButton = { TextButton(onClick = { resetOpen = false }) { Text("Cancel") } })
}

@Composable
private fun SocialButton(modifier: Modifier, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OutlinedButton(
        onClick = {}, enabled = false,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
    ) {
        Icon(icon, contentDescription = label, tint = TealPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp)
    }
}
