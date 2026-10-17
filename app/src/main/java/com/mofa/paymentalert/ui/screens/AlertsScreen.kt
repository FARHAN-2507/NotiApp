package com.mofa.paymentalert.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mofa.paymentalert.sync.DeviceRole
import com.mofa.paymentalert.ui.StatusPill

@Composable
fun AlertsScreen(
    firebaseAvailable: Boolean,
    role: DeviceRole,
    shopCode: String?,
    pendingUploads: Int,
    notificationsGranted: Boolean,
    contentPadding: PaddingValues,
    onBecomeReceiver: () -> Unit,
    onBecomeOwner: () -> Unit,
    onRegenerateCode: () -> Unit,
    onLinkOwner: (String) -> Unit,
    onChangeRole: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Owner alerts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Send payment alerts from this phone to the owner",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        SyncStatusPill(
                            role = role,
                            shopCode = shopCode,
                            firebaseAvailable = firebaseAvailable,
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    if (!firebaseAvailable) {
                        Text(
                            text = "Not configured yet. Add app/google-services.json from your " +
                                "Firebase project and rebuild the app (see README).",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        return@Column
                    }

                    when (role) {
                        DeviceRole.NONE -> {
                            Text(
                                text = "Choose how this phone is used:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = onBecomeReceiver,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("This phone receives payments") }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onBecomeOwner,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("I want alerts on this phone") }
                        }

                        DeviceRole.RECEIVER -> {
                            Text(
                                text = "Enter this code on the owner's phone to link it:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = shopCode ?: "------",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 8.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (pendingUploads > 0) {
                                    "$pendingUploads payment(s) waiting to upload"
                                } else {
                                    "All payments uploaded"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row {
                                TextButton(onClick = onRegenerateCode) { Text("New code") }
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = onChangeRole) { Text("Change role") }
                            }
                        }

                        DeviceRole.OWNER -> {
                            if (shopCode.isNullOrBlank()) {
                                Text(
                                    text = "Enter the 6-digit code shown on the receiver's phone:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(8.dp))
                                var codeInput by remember { mutableStateOf("") }
                                OutlinedTextField(
                                    value = codeInput,
                                    onValueChange = {
                                        codeInput = it.filter(Char::isDigit).take(6)
                                    },
                                    label = { Text("Shop code") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = { onLinkOwner(codeInput) },
                                        enabled = codeInput.length == 6,
                                    ) { Text("Link") }
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(onClick = onChangeRole) { Text("Change role") }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Connected to shop $shopCode",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Payment alerts arrive as notifications on this " +
                                        "phone, even when the app is closed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (!notificationsGranted) {
                                    Spacer(Modifier.height(10.dp))
                                    Button(
                                        onClick = onRequestNotifications,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text("Allow notifications") }
                                }
                                Spacer(Modifier.height(4.dp))
                                Row {
                                    TextButton(onClick = { openBatterySettings(context) }) {
                                        Text("Battery settings")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(onClick = onChangeRole) { Text("Change role") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusPill(role: DeviceRole, shopCode: String?, firebaseAvailable: Boolean) {
    val (text, container, content) = when {
        !firebaseAvailable -> Triple(
            "Off",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        role == DeviceRole.NONE -> Triple(
            "Not set up",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        role == DeviceRole.RECEIVER -> Triple(
            "Receiver",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        shopCode.isNullOrBlank() -> Triple(
            "Waiting for code",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> Triple(
            "Connected",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
    StatusPill(text = text, container = container, content = content)
}

private fun openBatterySettings(context: Context) {
    try {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"),
            ),
        )
    } catch (t: Throwable) {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}
