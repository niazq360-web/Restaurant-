package com.example.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun AnnualRolloverDialog(
    currentYear: Int,
    onComplete: () -> Unit
) {
    // 2 minutes = 120 seconds countdown
    var secondsLeft by remember { mutableStateOf(120) }
    var isDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
        isDone = true
    }

    val progress = (120 - secondsLeft) / 120f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    val currentStatusText = when {
        isDone -> "Update Complete! Reopening POS..."
        secondsLeft > 90 -> "Creating 12-Month Offline Master Archive..."
        secondsLeft > 60 -> "Compacting & Securing SQLite Records..."
        secondsLeft > 30 -> "Setting Up Year ${currentYear + 1} Financial Ledger..."
        else -> "Finalizing 12-Month Rollover & Resetting Cycle..."
    }

    Dialog(
        onDismissRequest = { /* Non-cancellable during update */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isDone) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(36.dp))
                    } else {
                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                }

                Text(
                    text = if (isDone) "Year ${currentYear + 1} Activated!" else "12-Month Update in Progress",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Past 12-month records are being archived permanently. Your products, workers, customers, and settings remain 100% safe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Progress Bar & Timer
                if (!isDone) {
                    val minutes = secondsLeft / 60
                    val secs = secondsLeft % 60
                    val timerDisplay = String.format("%02d:%02d", minutes, secs)

                    Text(
                        text = timerDisplay,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )

                    Text(
                        text = currentStatusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Convenient button for testing or instant finish
                    OutlinedButton(
                        onClick = {
                            secondsLeft = 0
                            isDone = true
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fast-Forward / Finish Now", fontSize = 12.sp)
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✓ 12-Month Ledger Renewed", fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                            Text("Your next 12-month cycle is active. This cycle continues indefinitely year after year!", fontSize = 11.sp, textAlign = TextAlign.Center, color = Color(0xFF047857))
                        }
                    }

                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Open POS App (Year ${currentYear + 1})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
