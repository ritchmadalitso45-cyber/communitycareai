package com.communitycare.ai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.communitycare.ai.ui.theme.*

@Composable
fun GetStartedScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit
) {
    // Pulsing animation for the central icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue  = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFD4F1F4),
                        Color(0xFFB0E8EC),
                        Color(0xFF7DD4DB),
                        TealPrimary
                    )
                )
            )
    ) {
        // Decorative blurred circles (background bubbles)
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .clip(CircleShape)
                .background(White.copy(alpha = 0.18f))
                .blur(30.dp)
        )
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 60.dp)
                .clip(CircleShape)
                .background(TealLight.copy(alpha = 0.25f))
                .blur(25.dp)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 30.dp)
                .clip(CircleShape)
                .background(White.copy(alpha = 0.12f))
                .blur(35.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(24.dp))

            // ── App logo / icon cluster ────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(scale)
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(CircleShape)
                        .background(White.copy(alpha = 0.20f))
                )
                // Mid ring
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .background(White.copy(alpha = 0.30f))
                )
                // Inner circle
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(White, TealLight)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalHospital,
                        contentDescription = "CommunityCare AI",
                        tint = TealPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── App name ──────────────────────────────────────────────
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Black)) {
                        append("Community")
                    }
                    withStyle(SpanStyle(color = White, fontWeight = FontWeight.Black)) {
                        append("Care")
                    }
                    withStyle(SpanStyle(color = TealDark, fontWeight = FontWeight.Bold)) {
                        append(" AI")
                    }
                },
                fontSize = 30.sp,
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Smart Care for Every Community",
                color = White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )

            Spacer(Modifier.height(24.dp))

            // ── Feature highlight card ────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = White.copy(alpha = 0.22f),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureRow(
                        icon = Icons.Filled.MonitorHeart,
                        title = "Offline assessment reports",
                        subtitle = "Save confirmed triage outcomes without internet"
                    )
                    HorizontalDivider(color = White.copy(alpha = 0.30f))
                    FeatureRow(
                        icon = Icons.Filled.Favorite,
                        title = "Secure staff sign-in",
                        subtitle = "Access your assigned CommunityCare facility"
                    )
                    HorizontalDivider(color = White.copy(alpha = 0.30f))
                    FeatureRow(
                        icon = Icons.Filled.LocalHospital,
                        title = "Confirmed delivery",
                        subtitle = "Track your reports through to DHIS2"
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── CTA button ────────────────────────────────────────────
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealDark
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    text = "Get Started  →",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }

            Spacer(Modifier.height(18.dp))

            // ── Sign in link ──────────────────────────────────────────
            TextButton(onClick = onSignIn) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = White.copy(alpha = 0.85f))) {
                            append("Already have an account?  ")
                        }
                        withStyle(SpanStyle(color = TealDark, fontWeight = FontWeight.Bold)) {
                            append("Sign In")
                        }
                    },
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Dot indicators ────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { idx ->
                    Box(
                        modifier = Modifier
                            .size(if (idx == 0) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (idx == 0) TealDark else White.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(White.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = TealDark, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title,   fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextPrimary.copy(alpha = 0.70f))
        }
    }
}
