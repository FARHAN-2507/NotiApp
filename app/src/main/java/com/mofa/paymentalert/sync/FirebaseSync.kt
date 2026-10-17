package com.mofa.paymentalert.sync

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.mofa.paymentalert.data.local.PaymentEntity
import kotlinx.coroutines.tasks.await

class FirebaseSync(private val appContext: Context) {

    var isAvailable: Boolean = false
        private set

    fun initialize() {
        isAvailable = try {
            FirebaseApp.initializeApp(appContext) ?: FirebaseApp.getApps(appContext).firstOrNull()
            FirebaseApp.getApps(appContext).isNotEmpty()
        } catch (t: Throwable) {
            Log.w(TAG, "Firebase is not configured: ${t.message}")
            false
        }
    }

    suspend fun ensureSignedIn(): Boolean {
        if (!isAvailable) return false
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { return true }
        return try {
            auth.signInAnonymously().await()
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Anonymous sign-in failed: ${t.message}")
            false
        }
    }

    suspend fun ensureShop(code: String): Boolean {
        if (!ensureSignedIn()) return false
        return try {
            FirebaseFirestore.getInstance()
                .collection("shops")
                .document(code)
                .set(mapOf("createdAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                .await()
            true
        } catch (t: Throwable) {
            Log.w(TAG, "ensureShop failed: ${t.message}")
            false
        }
    }

    suspend fun uploadPayment(code: String, payment: PaymentEntity): Boolean {
        if (!ensureSignedIn()) return false
        return try {
            FirebaseFirestore.getInstance()
                .collection("shops")
                .document(code)
                .collection("payments")
                .document(payment.rawTextHash)
                .set(
                    mapOf(
                        "amount" to payment.amount,
                        "sender" to payment.sender,
                        "source" to payment.source,
                        "sourceDetail" to payment.sourceDetail,
                        "receivedAt" to payment.receivedAt,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
                .await()
            true
        } catch (t: Throwable) {
            Log.w(TAG, "uploadPayment failed: ${t.message}")
            false
        }
    }

    suspend fun registerOwnerToken(code: String, token: String): Boolean {
        if (!ensureSignedIn()) return false
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return try {
            FirebaseFirestore.getInstance()
                .collection("shops")
                .document(code)
                .collection("tokens")
                .document(uid)
                .set(
                    mapOf(
                        "token" to token,
                        "platform" to "android",
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                .await()
            true
        } catch (t: Throwable) {
            Log.w(TAG, "registerOwnerToken failed: ${t.message}")
            false
        }
    }

    suspend fun latestPaymentReceivedAt(code: String): Long? {
        if (!ensureSignedIn()) return null
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("shops")
                .document(code)
                .collection("payments")
                .orderBy("receivedAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.getLong("receivedAt")
        } catch (t: Throwable) {
            Log.w(TAG, "latestPaymentReceivedAt failed: ${t.message}")
            null
        }
    }

    suspend fun currentMessagingToken(): String? {
        if (!isAvailable) return null
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (t: Throwable) {
            Log.w(TAG, "FCM token unavailable: ${t.message}")
            null
        }
    }

    companion object {
        private const val TAG = "FirebaseSync"
    }
}
