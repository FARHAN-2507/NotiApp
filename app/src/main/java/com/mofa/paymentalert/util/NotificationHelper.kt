package com.mofa.paymentalert.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mofa.paymentalert.MainActivity
import com.mofa.paymentalert.R

object NotificationHelper {

    const val CHANNEL_PAYMENTS = "payments"
    const val CHANNEL_WATCHING = "watching"

    private const val PAYMENT_NOTIFICATION_ID = 1
    private const val WATCHING_NOTIFICATION_ID = 2

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_PAYMENTS) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PAYMENTS,
                    "Payment alerts",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Alerts when a payment is received"
                },
            )
        }
        if (manager.getNotificationChannel(CHANNEL_WATCHING) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_WATCHING,
                    "Payment watcher",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Ongoing notification while the owner phone watches for payments"
                },
            )
        }
    }

    fun showPaymentNotification(context: Context, title: String, body: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_PAYMENTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(PAYMENT_NOTIFICATION_ID, notification)
    }

    fun buildWatchingNotification(context: Context, text: String): android.app.Notification {
        ensureChannel(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_WATCHING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Watching for payments")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .build()
    }

    fun watchingNotificationId(): Int = WATCHING_NOTIFICATION_ID
}
