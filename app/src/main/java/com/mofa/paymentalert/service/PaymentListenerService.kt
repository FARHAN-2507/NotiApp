package com.mofa.paymentalert.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.mofa.paymentalert.PaymentAlertApp
import com.mofa.paymentalert.parser.ParseResult
import com.mofa.paymentalert.parser.ParserRules
import com.mofa.paymentalert.parser.PaymentParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PaymentListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var rescanJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        rescanJob = scope.launch {
            while (isActive) {
                delay(RESCAN_INTERVAL_MS)
                rescanActiveNotifications()
            }
        }
    }

    override fun onDestroy() {
        rescanJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        recordSystemEvent("Notification listener connected")
        if (!markerInitialized(this)) {
            setLastProcessedPostTime(this, System.currentTimeMillis())
            markMarkerInitialized(this)
        }
        rescanActiveNotifications()
    }

    override fun onListenerDisconnected() {
        recordSystemEvent("Notification listener disconnected, rebinding")
        requestRebind(ComponentName(this, PaymentListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in ParserRules.monitoredNotificationPackages) return
        processNotification(sbn)
    }

    private fun rescanActiveNotifications() {
        val active = try {
            activeNotifications ?: return
        } catch (t: Throwable) {
            Log.w(TAG, "Cannot read active notifications: ${t.message}")
            return
        }
        val marker = lastProcessedPostTime(this)
        val missed = active
            .filter { it.packageName in ParserRules.monitoredNotificationPackages }
            .filter { it.postTime > marker }
            .sortedBy { it.postTime }
        if (missed.isEmpty()) return

        missed.forEach { processNotification(it) }
        recordSystemEvent("Recovered ${missed.size} missed notification(s)")
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString(" ")
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                .orEmpty()

        setLastProcessedPostTime(this, sbn.postTime)
        val result = PaymentParser.parseNotificationDetailed(sbn.packageName, title, text, sbn.postTime)

        val app = application as PaymentAlertApp
        app.applicationScope.launch {
            app.repository.recordRawEvent(sbn.packageName, title, text, sbn.postTime, result)
            if (result is ParseResult.Parsed) {
                val inserted = app.repository.ingest(result.payment)
                if (inserted) app.syncCoordinator.onPaymentDetected()
            }
        }
    }

    private fun recordSystemEvent(message: String) {
        val app = application as PaymentAlertApp
        app.applicationScope.launch {
            app.repository.recordRawEvent(
                source = "system",
                title = "Listener",
                text = message,
                postedAt = System.currentTimeMillis(),
                result = ParseResult.Rejected(message),
            )
        }
    }

    companion object {
        private const val TAG = "PaymentListener"
        private const val RESCAN_INTERVAL_MS = 3 * 60 * 1000L
        private const val PREFS = "listener_state"
        private const val KEY_LAST_POST_TIME = "last_processed_post_time"
        private const val KEY_MARKER_INITIALIZED = "marker_initialized"

        private fun markerInitialized(context: Context): Boolean =
            prefs(context).getBoolean(KEY_MARKER_INITIALIZED, false)

        private fun markMarkerInitialized(context: Context) {
            prefs(context).edit().putBoolean(KEY_MARKER_INITIALIZED, true).apply()
        }

        private fun lastProcessedPostTime(context: Context): Long =
            prefs(context).getLong(KEY_LAST_POST_TIME, 0L)

        private fun setLastProcessedPostTime(context: Context, value: Long) {
            val prefs = prefs(context)
            if (value <= prefs.getLong(KEY_LAST_POST_TIME, 0L)) return
            prefs.edit().putLong(KEY_LAST_POST_TIME, value).apply()
        }

        private fun prefs(context: Context) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
}
