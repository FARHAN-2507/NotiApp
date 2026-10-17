package com.mofa.paymentalert.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mofa.paymentalert.PaymentAlertApp
import com.mofa.paymentalert.util.NotificationHelper
import kotlinx.coroutines.launch

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = application as PaymentAlertApp
        app.applicationScope.launch { app.syncCoordinator.onNewToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val amount = message.data["amount"]
        val sender = message.data["sender"].orEmpty()

        val title = message.notification?.title
            ?: amount?.let { "₹$it received" }
            ?: "Payment received"
        val body = message.notification?.body
            ?: sender.ifBlank { "Payment received" }

        NotificationHelper.showPaymentNotification(this, title, body)
    }
}
