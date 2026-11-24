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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.communitycare.ai.ui.theme.*
import com.communitycare.ai.data.registrationError

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SignUpScreen(
    onSignUp: (String, String, String, String, String) -> Unit,
    onSignIn: () -> Unit, busy: Boolean = false, error: String? = null, message: String? = null
) {
    var fullName       by remember { mutableStateOf("") }
    var email          by remember { mutableStateOf("") }
    var phone          by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var confirmPass    by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible  by remember { mutableStateOf(false) }
    var role           by remember { mutableStateOf("HSA") }
    var expanded       by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    val roles = listOf("HSA", "Nurse", "Clinical Officer", "Supervisor")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFD4F1F4), Color(0xFFB0E8EC), TealLight))
            )
    ) {
        // Top decorative arc
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(bottomStart = 999.dp, bottomEnd = 999.dp))
                .background(
                    Brush.verticalGradient(listOf(TealPrimary, TealDark))
                )
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

            // ── Header inside the arc ──────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PersonAdd,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("Create Account", fontWeight = FontWeight.Black, fontSize = 24.sp, color = White)


            Spacer(Modifier.height(32.dp))

            // ── Form card ─────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    (formError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    message?.let { Text(it, color = TealDark) }
                    AuthTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = "Full Name",
                        leadingIcon = Icons.Filled.Person
                    )

                    AuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        leadingIcon = Icons.Filled.Email,
                        keyboardType = KeyboardType.Email
                    )

                    AuthTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Phone Number",
                        leadingIcon = Icons.Filled.Phone,
                        keyboardType = KeyboardType.Phone
                    )

                    // Role dropdown
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = role,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Role") },
                            leadingIcon = {
                                Icon(Icons.Filled.Badge, contentDescription = null, tint = TealPrimary)
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(14.dp),
                            colors = authTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            roles.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r) },
                                    onClick = { role = r; expanded = false }
                                )
                            }
                        }
                    }

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = TealPrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null, tint = TealPrimary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = authTextFieldColors()
                    )

                    // Confirm password
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = {
                            Icon(Icons.Filled.LockOpen, contentDescription = null, tint = TealPrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(
                                    if (confirmVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null, tint = TealPrimary
                                )
                            }
                        },
                        visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = confirmPass.isNotEmpty() && password != confirmPass,
                        supportingText = {
                            if (confirmPass.isNotEmpty() && password != confirmPass)
                                Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = authTextFieldColors()
                    )

                    Text("Test-pilot accounts can use their selected staff role immediately. Operational roles require approval. No staff role grants identity-server administrator access.", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            formError = registrationError(fullName, email, phone, password, confirmPass)
                            if (formError == null) { onSignUp(fullName, email, phone, role, password); password = ""; confirmPass = "" }
                        }, enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text(if (busy) "Creating account…" else "Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = White)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            TextButton(onClick = onSignIn) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = TextSecondary)) { append("Already have an account?  ") }
                        withStyle(SpanStyle(color = TealPrimary, fontWeight = FontWeight.Bold)) { append("Sign In") }
                    },
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
