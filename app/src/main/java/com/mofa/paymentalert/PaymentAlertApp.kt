package com.mofa.paymentalert

import android.app.Application
import com.mofa.paymentalert.data.PaymentRepository
import com.mofa.paymentalert.data.local.AppDatabase
import com.mofa.paymentalert.sync.DeviceRole
import com.mofa.paymentalert.sync.FirebaseSync
import com.mofa.paymentalert.sync.SyncConfig
import com.mofa.paymentalert.sync.SyncCoordinator
import com.mofa.paymentalert.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaymentAlertApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val repository: PaymentRepository by lazy {
        PaymentRepository(database.paymentDao(), database.rawEventDao())
    }

    val syncConfig: SyncConfig by lazy { SyncConfig(this) }

    val firebaseSync: FirebaseSync by lazy { FirebaseSync(this) }

    val syncCoordinator: SyncCoordinator by lazy {
        SyncCoordinator(this, repository, syncConfig, firebaseSync)
    }

    override fun onCreate() {
        super.onCreate()
        firebaseSync.initialize()
        NotificationHelper.ensureChannel(this)

        applicationScope.launch {
            if (!firebaseSync.isAvailable) return@launch
            firebaseSync.ensureSignedIn()
            when (syncConfig.role) {
                DeviceRole.RECEIVER -> {
                    if (!syncConfig.historyBackfillDone) {
                        repository.requeueAllForUpload()
                        syncConfig.historyBackfillDone = true
                    }
                    syncCoordinator.enqueueUpload()
                }
                DeviceRole.OWNER -> syncCoordinator.registerOwnerToken()
                DeviceRole.NONE -> Unit
            }
        }
    }
}
