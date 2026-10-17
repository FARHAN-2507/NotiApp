package com.mofa.paymentalert.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.mofa.paymentalert.PaymentAlertApp
import com.mofa.paymentalert.sync.DeviceRole
import com.mofa.paymentalert.util.NotificationHelper
import com.mofa.paymentalert.util.formatAmount
import com.mofa.paymentalert.util.formatAmountSpeech
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PaymentWatchService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listener: ListenerRegistration? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val text = "Waiting for new payments from the receiver phone"
        ServiceCompat.startForeground(
            this,
            NotificationHelper.watchingNotificationId(),
            NotificationHelper.buildWatchingNotification(this, text),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
                ttsReady = true
                pendingSpeech?.let { queued ->
                    pendingSpeech = null
                    speak(queued)
                }
            } else {
                Log.w(TAG, "Text-to-speech init failed: $status")
            }
        }
        startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        listener?.remove()
        listener = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        scope.cancel()
        super.onDestroy()
    }

    private fun speak(text: String) {
        if (!ttsReady) {
            pendingSpeech = text
            return
        }
        try {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "payment-${System.currentTimeMillis()}")
        } catch (t: Throwable) {
            Log.w(TAG, "Text-to-speech failed: ${t.message}")
        }
    }

    private fun startListening() {
        val app = application as PaymentAlertApp
        val config = app.syncConfig
        val code = config.shopCode
        if (config.role != DeviceRole.OWNER ||
            code.isNullOrBlank() ||
            !config.alertsEnabled ||
            !app.firebaseSync.isAvailable
        ) {
            stopSelf()
            return
        }

        scope.launch {
            while (isActive && !app.firebaseSync.ensureSignedIn()) {
                delay(RETRY_MS)
            }
            if (!isActive) return@launch

            listener?.remove()
            listener = FirebaseFirestore.getInstance()
                .collection("shops")
                .document(code)
                .collection("payments")
                .orderBy("receivedAt", Query.Direction.DESCENDING)
                .limit(MAX_DOCS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        Log.w(TAG, "Payment listener error: ${error?.message}")
                        return@addSnapshotListener
                    }
                    val docs = snapshot.documents
                    if (docs.isEmpty()) return@addSnapshotListener

                    scope.launch {
                        docs.forEach { doc ->
                            try {
                                val receivedAt = doc.numberLong("receivedAt") ?: return@forEach
                                val amount = doc.numberDouble("amount") ?: return@forEach
                                app.repository.insertRemote(
                                    rawTextHash = doc.id,
                                    amount = amount,
                                    sender = doc.getString("sender"),
                                    source = doc.getString("source") ?: "NOTIFICATION",
                                    sourceDetail = doc.getString("sourceDetail").orEmpty(),
                                    receivedAt = receivedAt,
                                )
                            } catch (t: Throwable) {
                                Log.w(TAG, "Cannot mirror payment doc ${doc.id}: ${t.message}")
                            }
                        }
                    }

                    val doc = docs.first()
                    val receivedAt = doc.numberLong("receivedAt") ?: return@addSnapshotListener
                    val baseline = baseline(this@PaymentWatchService)
                    if (receivedAt <= baseline) return@addSnapshotListener

                    setBaseline(this@PaymentWatchService, receivedAt)
                    if (baseline <= 0L) return@addSnapshotListener
                    if (!app.syncConfig.alertsEnabled) return@addSnapshotListener

                    val amount = doc.numberDouble("amount") ?: 0.0
                    val sender = doc.getString("sender").orEmpty()
                    val body = if (sender.isNotBlank()) "from $sender" else "Payment received"
                    NotificationHelper.showPaymentNotification(
                        this@PaymentWatchService,
                        "${formatAmount(amount)} received",
                        body,
                    )
                    speak(
                        if (sender.isNotBlank()) {
                            "${formatAmountSpeech(amount)} received from $sender"
                        } else {
                            "${formatAmountSpeech(amount)} received"
                        },
                    )
                }
        }
    }

    companion object {
        private const val TAG = "PaymentWatchService"
        private const val RETRY_MS = 15_000L
        private const val MAX_DOCS = 200L
        private const val PREFS = "watch_state"
        private const val KEY_BASELINE = "last_notified_received_at"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, PaymentWatchService::class.java),
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Cannot start watch service: ${t.message}")
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PaymentWatchService::class.java))
        }

        fun baseline(context: Context): Long =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_BASELINE, 0L)

        fun setBaseline(context: Context, value: Long) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_BASELINE, value)
                .apply()
        }
    }
}

private fun DocumentSnapshot.numberLong(field: String): Long? = when (val value = get(field)) {
    is Long -> value
    is Double -> value.toLong()
    is Number -> value.toLong()
    else -> null
}

private fun DocumentSnapshot.numberDouble(field: String): Double? = when (val value = get(field)) {
    is Double -> value
    is Long -> value.toDouble()
    is Number -> value.toDouble()
    else -> null
}
