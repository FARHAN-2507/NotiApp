package com.mofa.paymentalert.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mofa.paymentalert.PaymentAlertApp

class UploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PaymentAlertApp
        val uploaded = app.syncCoordinator.uploadPending()
        val remaining = app.repository.pendingCount()

        if (remaining == 0) return Result.success()
        if (!app.syncCoordinator.shouldKeepRetrying()) return Result.success()
        if (uploaded == 0 && runAttemptCount >= MAX_ATTEMPTS) return Result.failure()
        return Result.retry()
    }

    companion object {
        private const val MAX_ATTEMPTS = 8
    }
}
