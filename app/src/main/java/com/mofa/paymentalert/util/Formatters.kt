package com.mofa.paymentalert.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.getDefault())

fun formatTimestamp(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)

fun formatAmount(amount: Double): String {
    val number = if (amount % 1.0 == 0.0) {
        "%,.0f".format(Locale.US, amount)
    } else {
        "%,.2f".format(Locale.US, amount)
    }
    return "₹$number"
}

fun formatAmountSpeech(amount: Double): String {
    val number = if (amount % 1.0 == 0.0) {
        "%,.0f".format(Locale.US, amount)
    } else {
        "%,.2f".format(Locale.US, amount)
    }
    return "$number rupees"
}
