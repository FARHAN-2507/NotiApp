package com.mofa.paymentalert.ui

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mofa.paymentalert.sync.DeviceRole
import com.mofa.paymentalert.ui.screens.AboutScreen
import com.mofa.paymentalert.ui.screens.AlertsScreen
import com.mofa.paymentalert.ui.screens.LogsScreen
import com.mofa.paymentalert.ui.screens.PaymentsScreen
import com.mofa.paymentalert.ui.screens.SetupScreen
import kotlinx.coroutines.launch

private val ScreenContentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val rawEvents by viewModel.rawEvents.collectAsStateWithLifecycle()
    val pendingUploads by viewModel.pendingUploadCount.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val role by viewModel.role.collectAsStateWithLifecycle()
    val shopCode by viewModel.shopCode.collectAsStateWithLifecycle()
    val alertsEnabled by viewModel.alertsEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val setupComplete = permissions.notificationAccess && permissions.smsGranted
    var currentScreen by remember {
        mutableStateOf(if (setupComplete) AppScreen.PAYMENTS else AppScreen.SETUP)
    }
    var showClearDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(setupComplete) {
        if (setupComplete && currentScreen == AppScreen.SETUP) {
            currentScreen = AppScreen.PAYMENTS
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshPermissions() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshPermissions() }

    val visibleScreens = AppScreen.entries

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Payment Alert",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "UPI payment alerts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                visibleScreens.forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen.title) },
                        icon = { Icon(screen.icon, contentDescription = null) },
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(currentScreen.title, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (currentScreen == AppScreen.PAYMENTS && payments.isNotEmpty()) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Clear history")
                            }
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (role == DeviceRole.OWNER) {
                    AlertsToggleBar(
                        enabled = alertsEnabled,
                        onToggle = viewModel::setAlertsEnabled,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        AppScreen.SETUP -> SetupScreen(
                            notificationAccess = permissions.notificationAccess,
                            smsGranted = permissions.smsGranted,
                            batteryExempt = permissions.batteryExempt,
                            contentPadding = ScreenContentPadding,
                            onOpenNotificationSettings = {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                                )
                            },
                            onRequestSms = {
                                smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                            },
                            onRequestBatteryExemption = viewModel::requestBatteryExemption,
                        )

                        AppScreen.ALERTS -> AlertsScreen(
                            firebaseAvailable = viewModel.firebaseAvailable,
                            role = role,
                            shopCode = shopCode,
                            pendingUploads = pendingUploads,
                            notificationsGranted = permissions.notificationsGranted,
                            contentPadding = ScreenContentPadding,
                            onBecomeReceiver = viewModel::becomeReceiver,
                            onBecomeOwner = viewModel::becomeOwner,
                            onRegenerateCode = viewModel::regenerateShopCode,
                            onLinkOwner = viewModel::linkOwner,
                            onChangeRole = viewModel::changeRole,
                            onRequestNotifications = {
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS,
                                )
                            },
                        )

                        AppScreen.PAYMENTS -> PaymentsScreen(
                            payments = payments,
                            contentPadding = ScreenContentPadding,
                        )

                        AppScreen.LOGS -> LogsScreen(
                            rawEvents = rawEvents,
                            contentPadding = ScreenContentPadding,
                            onClear = viewModel::clearRawEvents,
                        )

                        AppScreen.ABOUT -> AboutScreen(
                            contentPadding = ScreenContentPadding,
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear history?") },
            text = { Text("This removes all detected payments from this phone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    },
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun AlertsToggleBar(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Alerts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (enabled) {
                        "On - voice alert on every payment"
                    } else {
                        "Off - alerts paused"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}
