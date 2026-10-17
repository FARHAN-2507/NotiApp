package com.mofa.paymentalert.data

import com.mofa.paymentalert.data.local.PaymentEntity
import com.mofa.paymentalert.data.model.ParsedPayment
import com.mofa.paymentalert.data.model.PaymentSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DedupPolicyTest {

    private val base = 1_700_000_000_000L

    private fun entity(
        amount: Double,
        sender: String?,
        at: Long,
        source: String = "NOTIFICATION",
        detail: String = "com.phonepe.app",
    ) = PaymentEntity(
        id = 0,
        amount = amount,
        sender = sender,
        source = source,
        sourceDetail = detail,
        receivedAt = at,
        rawText = "raw-$at-$amount",
        rawTextHash = "hash-$at-$amount-$detail",
        createdAt = at,
    )

    private fun candidate(
        amount: Double,
        sender: String?,
        at: Long,
        source: PaymentSource = PaymentSource.SMS,
        detail: String = "AD-HDFCBK",
    ) = ParsedPayment(amount, sender, source, detail, at, "raw-candidate")

    @Test
    fun `same amount within window with matching sender is duplicate`() {
        val existing = listOf(entity(250.0, "Rahul Sharma", base))
        assertTrue(DedupPolicy.isDuplicate(candidate(250.0, "RAHUL SHARMA", base + 60_000), existing))
    }

    @Test
    fun `same amount within window with different senders is not duplicate`() {
        val existing = listOf(entity(10.0, "Rahul", base))
        assertFalse(DedupPolicy.isDuplicate(candidate(10.0, "Priya", base + 30_000), existing))
    }

    @Test
    fun `same amount outside window is not duplicate`() {
        val existing = listOf(entity(250.0, "Rahul", base))
        assertFalse(
            DedupPolicy.isDuplicate(candidate(250.0, "Rahul", base + DedupPolicy.WINDOW_MS + 1), existing),
        )
    }

    @Test
    fun `different amount within window is not duplicate`() {
        val existing = listOf(entity(251.0, "Rahul", base))
        assertFalse(DedupPolicy.isDuplicate(candidate(250.0, "Rahul", base + 1_000), existing))
    }

    @Test
    fun `unknown sender matches within window`() {
        val existing = listOf(entity(250.0, null, base))
        assertTrue(DedupPolicy.isDuplicate(candidate(250.0, "Rahul", base + 1_000), existing))
    }

    @Test
    fun `same channel payments are not deduped`() {
        val existing = listOf(entity(1.0, "Test User", base, source = "SMS", detail = "AD-HDFCBK"))
        assertFalse(DedupPolicy.isDuplicate(candidate(1.0, "Test User", base + 5_000), existing))
    }

    @Test
    fun `find duplicate returns matching row with missing sender`() {
        val existing = listOf(entity(250.0, null, base))
        val match = DedupPolicy.findDuplicate(candidate(250.0, "Rahul", base + 1_000), existing)
        assertNotNull(match)
        assertNull(match?.sender)
    }

    @Test
    fun `find duplicate returns null for different sender`() {
        val existing = listOf(entity(10.0, "Rahul", base))
        assertNull(DedupPolicy.findDuplicate(candidate(10.0, "Priya", base + 30_000), existing))
    }
}
