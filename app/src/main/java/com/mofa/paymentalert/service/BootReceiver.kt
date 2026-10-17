package com.mofa.paymentalert.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mofa.paymentalert.PaymentAlertApp
import com.mofa.paymentalert.sync.DeviceRole

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as PaymentAlertApp
        val config = app.syncConfig
        if (config.role == DeviceRole.OWNER &&
            config.alertsEnabled &&
            app.firebaseSync.isAvailable &&
            !config.shopCode.isNullOrBlank()
        ) {
            PaymentWatchService.start(context)
        }
    }
}
