package com.mofa.paymentalert.data.model

enum class PaymentSource { NOTIFICATION, SMS }

data class ParsedPayment(
    val amount: Double,
    val sender: String?,
    val source: PaymentSource,
    val sourceDetail: String,
    val receivedAt: Long,
    val rawText: String,
)
