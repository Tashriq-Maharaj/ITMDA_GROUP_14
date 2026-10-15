package com.halohome.iotresearch.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halohome.iotresearch.data.api.NetworkModule
import com.halohome.iotresearch.data.models.AuditReportDto
import com.halohome.iotresearch.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveHubScreen(
    auditReport: AuditReportDto?,
    onRecommission: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var isCoolingOn by remember { mutableStateOf(false) }
    var isLightOn by remember { mutableStateOf(true) }
    var showBiometricPrompt by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val securityGrade = auditReport?.grade ?: "B"
    val riskTier = auditReport?.riskTier ?: "Balanced Consumer"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Residence #402 Hub", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Active IoT Gateway Control", fontSize = 11.sp, color = TextSecondary)
                    }
                },
                actions = {
                    Surface(
                        color = if (securityGrade == "A") SuccessLight else if (securityGrade == "B") Color(0xFFE0F2FE) else WarningLight,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "SEC: $securityGrade",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (securityGrade == "A") SuccessGreen else BlueAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateBackground)
            )
        },
        containerColor = SlateBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = SuccessGreen) {}
                            Text("Gateway Status: Online", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("12ms latency", fontSize = 11.sp, color = TextSecondary)
                    }
                    Divider(color = BorderLight)
                    DetailLine("Commissioned Posture", riskTier)
                    DetailLine("Security Score", "${auditReport?.securityScore?.toInt() ?: 75}%")
                    DetailLine("Usability Index", "${auditReport?.usabilityScore?.toInt() ?: 80}%")
                }
            }

            // Sensor Fleet: Living Room Climate Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Living Room Climate Sensor", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ambient Temperature", fontSize = 12.sp, color = TextSecondary)
                            Text("24.8°C", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BlueAccent)
                        }
                        Icon(Icons.Default.Thermostat, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(36.dp))
                    }

                    Divider(color = BorderLight)

                    // Cooling Unit Toggle with Action Security Check
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Air Conditioner / Cooling Unit", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                if (isCoolingOn) "Active • Cooling Living Room" else "Standby",
                                fontSize = 11.sp,
                                color = if (isCoolingOn) SuccessGreen else TextSecondary
                            )
                        }
                        Switch(
                            checked = isCoolingOn,
                            onCheckedChange = { targetState ->
                                // If user configured biometric step-up, prompt for biometric check
                                val requiresStepUp = auditReport?.strengths?.any { it.contains("Biometric") } == true
                                if (requiresStepUp) {
                                    pendingAction = { isCoolingOn = targetState }
                                    showBiometricPrompt = true
                                } else {
                                    isCoolingOn = targetState
                                }
                            }
                        )
                    }
                }
            }

            // Living Room Smart Light Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Living Room Ceiling Lamp", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(if (isLightOn) "ON (450 lumens, Warm White)" else "OFF", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isLightOn,
                        onCheckedChange = { isLightOn = it }
                    )
                }
            }

            // Quick Actions Card
            Card(
                colors = CardDefaults.cardColors(containerColor = LightBlueBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Research Analytics & Export", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                    Text(
                        "Download the publication-ready Excel spreadsheet containing this session's telemetry and trade-off metrics.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Button(
                        onClick = {
                            val exportUrl = "${NetworkModule.baseUrl}api/export/excel"
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(exportUrl))
                            context.startActivity(browserIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Live Excel Report (.xlsx)")
                    }
                }
            }

            // Re-commission Button
            OutlinedButton(
                onClick = onRecommission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Adjust Security & Re-commission")
            }
        }
    }

    // Step-up Biometric / PIN Simulation Dialog
    if (showBiometricPrompt) {
        AlertDialog(
            onDismissRequest = {
                showBiometricPrompt = false
                pendingAction = null
            },
            icon = { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(36.dp)) },
            title = { Text("Biometric Action Authorization", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "You configured high action security. Confirm your fingerprint / PIN to toggle home climate controls.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(onClick = {
                    showBiometricPrompt = false
                    pendingAction?.invoke()
                    pendingAction = null
                }) {
                    Text("Authorize Action")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBiometricPrompt = false
                    pendingAction = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
    }
}
