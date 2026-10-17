package com.mofa.paymentalert.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mofa.paymentalert.data.PaymentRepository
import com.mofa.paymentalert.service.UploadWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withTimeoutOrNull

class SyncCoordinator(
    private val context: Context,
    private val repository: PaymentRepository,
    private val config: SyncConfig,
    val firebase: FirebaseSync,
) {

    fun onPaymentDetected() {
        if (config.role != DeviceRole.RECEIVER) return
        if (config.shopCode.isNullOrBlank()) return
        if (!firebase.isAvailable) return
        enqueueUpload()
    }

    fun enqueueUpload() {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    suspend fun uploadPending(): Int {
        if (!firebase.isAvailable) return 0
        if (config.role != DeviceRole.RECEIVER) return 0
        val code = config.shopCode ?: return 0
        if (!firebase.ensureSignedIn()) return 0

        var uploaded = 0
        while (uploaded < MAX_TOTAL) {
            val batch = repository.pendingUploads(MAX_BATCH)
            if (batch.isEmpty()) break
            var progressed = false
            for (payment in batch) {
                val ok = withTimeoutOrNull(UPLOAD_TIMEOUT_MS) {
                    firebase.uploadPayment(code, payment)
                } ?: false
                if (!ok) break
                repository.markUploaded(payment.id)
                uploaded++
                progressed = true
            }
            if (!progressed) break
        }
        return uploaded
    }

    suspend fun createShop(code: String): Boolean = firebase.ensureShop(code)

    suspend fun registerOwnerToken() {
        if (!firebase.isAvailable) return
        if (config.role != DeviceRole.OWNER) return
        val code = config.shopCode ?: return
        val token = firebase.currentMessagingToken() ?: return
        firebase.registerOwnerToken(code, token)
    }

    suspend fun latestPaymentReceivedAt(): Long? {
        if (!firebase.isAvailable) return null
        if (config.role != DeviceRole.OWNER) return null
        val code = config.shopCode ?: return null
        return firebase.latestPaymentReceivedAt(code)
    }

    suspend fun onNewToken(token: String) {
        if (!firebase.isAvailable) return
        if (config.role != DeviceRole.OWNER) return
        val code = config.shopCode ?: return
        if (!firebase.ensureSignedIn()) return
        firebase.registerOwnerToken(code, token)
    }

    fun shouldKeepRetrying(): Boolean =
        firebase.isAvailable &&
            config.role == DeviceRole.RECEIVER &&
            !config.shopCode.isNullOrBlank()

    companion object {
        const val WORK_NAME = "upload-payments"
        private const val MAX_BATCH = 50
        private const val MAX_TOTAL = 500
        private const val UPLOAD_TIMEOUT_MS = 20_000L
    }
}
