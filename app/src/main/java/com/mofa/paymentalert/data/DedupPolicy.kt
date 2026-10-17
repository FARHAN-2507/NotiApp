package com.mofa.paymentalert.data

import com.mofa.paymentalert.data.local.PaymentEntity
import com.mofa.paymentalert.data.model.ParsedPayment
import kotlin.math.abs

object DedupPolicy {

    const val WINDOW_MS: Long = 3 * 60 * 1000

    fun isDuplicate(candidate: ParsedPayment, existing: List<PaymentEntity>): Boolean =
        findDuplicate(candidate, existing) != null

    fun findDuplicate(candidate: ParsedPayment, existing: List<PaymentEntity>): PaymentEntity? =
        existing.firstOrNull { payment ->
            payment.amount == candidate.amount &&
                abs(payment.receivedAt - candidate.receivedAt) <= WINDOW_MS &&
                payment.source != candidate.source.name &&
                sendersCompatible(payment.sender, candidate.sender)
        }

    fun sendersCompatible(a: String?, b: String?): Boolean {
        val tokenA = firstToken(a)
        val tokenB = firstToken(b)
        if (tokenA == null || tokenB == null) return true
        return tokenA == tokenB
    }

    private fun firstToken(name: String?): String? =
        name?.trim()?.lowercase()?.split(Regex("\\s+"))?.firstOrNull()?.takeIf { it.isNotEmpty() }
}
