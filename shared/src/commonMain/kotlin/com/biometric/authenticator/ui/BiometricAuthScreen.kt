package com.biometric.authenticator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biometric.authenticator.biometric.rememberBiometricAuthenticator
import com.biometric.authenticator.model.BiometricStatus
import com.biometric.authenticator.model.BiometricType
import com.biometric.authenticator.viewmodel.AuthLogEntry
import com.biometric.authenticator.viewmodel.AuthState
import com.biometric.authenticator.viewmodel.BiometricUiState
import com.biometric.authenticator.viewmodel.BiometricViewModel

@Composable
fun BiometricAuthScreen(
    viewModel: BiometricViewModel = viewModel { BiometricViewModel() }
) {
    val authenticator = rememberBiometricAuthenticator()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(authenticator) {
        viewModel.checkBiometricAvailability(authenticator)
    }

    MaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    HeaderCard()
                }

                // Device Capability Status
                item {
                    DeviceStatusCard(
                        status = uiState.biometricStatus,
                        type = uiState.biometricType,
                        onRefresh = { viewModel.checkBiometricAvailability(authenticator) }
                    )
                }

                // Authentication Action Card
                item {
                    AuthActionCard(
                        uiState = uiState,
                        onAuthenticate = { viewModel.authenticate(authenticator) },
                        onLogout = { viewModel.logout() }
                    )
                }

                // Secret Vault Card (when authenticated)
                item {
                    AnimatedVisibility(visible = uiState.isAuthenticated) {
                        SecretVaultCard(
                            secretData = uiState.secretVaultData ?: "No data"
                        )
                    }
                }

                // Audit Log Section
                item {
                    LogSectionHeader(
                        logCount = uiState.authLogs.size,
                        onClearLogs = { viewModel.clearLogs() }
                    )
                }

                if (uiState.authLogs.isEmpty()) {
                    item {
                        EmptyLogCard()
                    }
                } else {
                    items(
                        items = uiState.authLogs,
                        key = { it.id + it.timeString }
                    ) { logEntry ->
                        LogEntryRow(logEntry)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                FingerprintCanvasIcon(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Biometric Authenticator",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "MVVM • Android & iOS Support",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DeviceStatusCard(
    status: BiometricStatus,
    type: BiometricType,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Device Biometric Support",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Check", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val (statusText, statusBg, statusContentColor) = when (status) {
                is BiometricStatus.Available -> Triple(
                    "Ready (${status.type.displayName})",
                    Color(0xFFE8F5E9),
                    Color(0xFF2E7D32)
                )
                is BiometricStatus.NotEnrolled -> Triple(
                    "Not Enrolled (No Fingerprint/Face set)",
                    Color(0xFFFFF8E1),
                    Color(0xFFF57F17)
                )
                is BiometricStatus.HardwareUnavailable -> Triple(
                    "Hardware Unavailable",
                    Color(0xFFFFEBEE),
                    Color(0xFFC62828)
                )
                is BiometricStatus.NotAvailable -> Triple(
                    status.reason,
                    Color(0xFFFFEBEE),
                    Color(0xFFC62828)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Type: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = statusContentColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthActionCard(
    uiState: BiometricUiState,
    onAuthenticate: () -> Unit,
    onLogout: () -> Unit
) {
    val isAvailable = uiState.biometricStatus is BiometricStatus.Available
    val isAuthenticating = uiState.authState is AuthState.Authenticating

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (uiState.isAuthenticated) "Authenticated Session" else "Biometric Verification",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Fingerprint scanner button
            val scannerBg by animateColorAsState(
                targetValue = when {
                    uiState.isAuthenticated -> Color(0xFF4CAF50)
                    uiState.authState is AuthState.Error -> Color(0xFFE53935)
                    isAuthenticating -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(scannerBg)
                    .clickable(enabled = isAvailable && !isAuthenticating && !uiState.isAuthenticated) {
                        onAuthenticate()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    FingerprintCanvasIcon(
                        modifier = Modifier.size(56.dp),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auth status description text
            when (val state = uiState.authState) {
                is AuthState.Authenticating -> {
                    Text(
                        text = "Touch fingerprint sensor or scan face...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                is AuthState.Success -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
                is AuthState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
                is AuthState.Idle -> {
                    Text(
                        text = if (uiState.isAuthenticated)
                            "Access granted to secure features."
                        else if (isAvailable)
                            "Tap icon or button below to scan fingerprint"
                        else
                            "Biometrics unavailable on this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!uiState.isAuthenticated) {
                Button(
                    onClick = onAuthenticate,
                    enabled = isAvailable && !isAuthenticating,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isAuthenticating) "Authenticating..." else "Authenticate with Fingerprint",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onAuthenticate,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify Again")
                    }
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Lock Vault")
                    }
                }
            }
        }
    }
}

@Composable
private fun SecretVaultCard(secretData: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔒 Unlocked Vault Content",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Protected Data Payload:",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = secretData,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFF1B5E20),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun LogSectionHeader(
    logCount: Int,
    onClearLogs: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Authentication Audit Logs ($logCount)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (logCount > 0) {
            OutlinedButton(
                onClick = onClearLogs,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Clear", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptyLogCard() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No authentication attempts recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LogEntryRow(entry: AuthLogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isSuccess) Color(0xFFF1F8E9) else Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.status,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun FingerprintCanvasIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val strokeWidth = w * 0.08f

        // Draw concentric fingerprint arches
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.15f),
            size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.8f),
            style = Stroke(width = strokeWidth)
        )

        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.3f),
            size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.5f),
            style = Stroke(width = strokeWidth)
        )

        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.45f),
            size = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.24f),
            style = Stroke(width = strokeWidth)
        )

        // Center ridge line
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.57f),
            end = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.85f),
            strokeWidth = strokeWidth
        )

        // Outer side ridges
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.55f),
            end = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.85f),
            strokeWidth = strokeWidth
        )

        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.55f),
            end = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.85f),
            strokeWidth = strokeWidth
        )
    }
}
