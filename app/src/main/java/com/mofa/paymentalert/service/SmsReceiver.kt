package com.mofa.paymentalert.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.mofa.paymentalert.PaymentAlertApp
import com.mofa.paymentalert.parser.ParseResult
import com.mofa.paymentalert.parser.ParserRules
import com.mofa.paymentalert.parser.PaymentParser
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val app = context.applicationContext as PaymentAlertApp
        val pendingResult = goAsync()

        app.applicationScope.launch {
            try {
                messages
                    .groupBy { it.originatingAddress.orEmpty() }
                    .forEach { (address, parts) ->
                        val body = parts.joinToString(separator = "") { it.messageBody.orEmpty() }
                        val timestamp = parts.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
                        val result = PaymentParser.parseSmsDetailed(address, body, timestamp)

                        if (ParserRules.isBankSender(address)) {
                            app.repository.recordRawEvent("sms:$address", "", body, timestamp, result)
                        }
                        if (result is ParseResult.Parsed) {
                            val inserted = app.repository.ingest(result.payment)
                            if (inserted) app.syncCoordinator.onPaymentDetected()
                        }
                    }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
