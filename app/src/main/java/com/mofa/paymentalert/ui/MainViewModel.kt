package com.mofa.paymentalert.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mofa.paymentalert.PaymentAlertApp
import com.mofa.paymentalert.data.PaymentRepository
import com.mofa.paymentalert.data.local.PaymentEntity
import com.mofa.paymentalert.data.local.RawEventEntity
import com.mofa.paymentalert.sync.DeviceRole
import com.mofa.paymentalert.sync.SyncConfig
import com.mofa.paymentalert.sync.SyncCoordinator
import com.mofa.paymentalert.service.PaymentWatchService
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionsState(
    val notificationAccess: Boolean = false,
    val smsGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val batteryExempt: Boolean = false,
)

class MainViewModel(
    private val repository: PaymentRepository,
    private val appContext: Context,
    private val syncConfig: SyncConfig,
    private val syncCoordinator: SyncCoordinator,
) : ViewModel() {

    val payments: StateFlow<List<PaymentEntity>> = repository.payments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rawEvents: StateFlow<List<RawEventEntity>> = repository.rawEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingUploadCount: StateFlow<Int> = repository.pendingUploadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _permissions = MutableStateFlow(currentPermissions())
    val permissions: StateFlow<PermissionsState> = _permissions.asStateFlow()

    private val _role = MutableStateFlow(syncConfig.role)
    val role: StateFlow<DeviceRole> = _role.asStateFlow()

    private val _shopCode = MutableStateFlow(syncConfig.shopCode)
    val shopCode: StateFlow<String?> = _shopCode.asStateFlow()

    private val _alertsEnabled = MutableStateFlow(syncConfig.alertsEnabled)
    val alertsEnabled: StateFlow<Boolean> = _alertsEnabled.asStateFlow()

    val firebaseAvailable: Boolean
        get() = syncCoordinator.firebase.isAvailable

    fun refreshPermissions() {
        _permissions.value = currentPermissions()
    }

    fun becomeReceiver() {
        val code = generateShopCode()
        syncConfig.role = DeviceRole.RECEIVER
        syncConfig.shopCode = code
        _role.value = DeviceRole.RECEIVER
        _shopCode.value = code
        viewModelScope.launch {
            syncCoordinator.createShop(code)
            syncCoordinator.enqueueUpload()
        }
    }

    fun becomeOwner() {
        syncConfig.role = DeviceRole.OWNER
        _role.value = DeviceRole.OWNER
    }

    fun regenerateShopCode() {
        if (syncConfig.role != DeviceRole.RECEIVER) return
        val code = generateShopCode()
        syncConfig.shopCode = code
        _shopCode.value = code
        viewModelScope.launch { syncCoordinator.createShop(code) }
    }

    fun linkOwner(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return
        syncConfig.shopCode = trimmed
        _shopCode.value = trimmed
        viewModelScope.launch {
            syncCoordinator.registerOwnerToken()
            if (firebaseAvailable && syncConfig.alertsEnabled) {
                val latest = syncCoordinator.latestPaymentReceivedAt() ?: 0L
                PaymentWatchService.setBaseline(appContext, latest)
                PaymentWatchService.start(appContext)
            }
        }
    }

    fun setAlertsEnabled(enabled: Boolean) {
        syncConfig.alertsEnabled = enabled
        _alertsEnabled.value = enabled
        if (syncConfig.role != DeviceRole.OWNER) return
        if (syncConfig.shopCode.isNullOrBlank()) return
        if (!firebaseAvailable) return
        viewModelScope.launch {
            if (enabled) {
                val latest = syncCoordinator.latestPaymentReceivedAt()
                    ?: PaymentWatchService.baseline(appContext)
                PaymentWatchService.setBaseline(appContext, latest)
                PaymentWatchService.start(appContext)
            } else {
                PaymentWatchService.stop(appContext)
            }
        }
    }

    fun ensureOwnerWatch() {
        if (syncConfig.role != DeviceRole.OWNER) return
        if (syncConfig.shopCode.isNullOrBlank()) return
        if (!syncConfig.alertsEnabled) return
        if (!firebaseAvailable) return
        PaymentWatchService.start(appContext)
    }

    fun changeRole() {
        syncConfig.role = DeviceRole.NONE
        syncConfig.shopCode = null
        _role.value = DeviceRole.NONE
        _shopCode.value = null
        PaymentWatchService.stop(appContext)
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clear() }
    }

    fun requestBatteryExemption() {
        try {
            appContext.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:${appContext.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (t: Throwable) {
            try {
                appContext.startActivity(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (ignored: Throwable) {
                // Nothing more we can do; user can open battery settings manually.
            }
        }
    }

    fun clearRawEvents() {
        viewModelScope.launch { repository.clearRawEvents() }
    }

    private fun generateShopCode(): String = (100000 + Random.nextInt(900000)).toString()

    private fun currentPermissions() = PermissionsState(
        notificationAccess = NotificationManagerCompat.getEnabledListenerPackages(appContext)
            .contains(appContext.packageName),
        smsGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED,
        notificationsGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED,
        batteryExempt = (appContext.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(appContext.packageName),
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PaymentAlertApp
                MainViewModel(app.repository, app, app.syncConfig, app.syncCoordinator)
            }
        }
    }
}
