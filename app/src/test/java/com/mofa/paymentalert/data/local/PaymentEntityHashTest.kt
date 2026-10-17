package com.mofa.paymentalert.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PaymentEntityHashTest {

    @Test
    fun `same text at same time hashes equally`() {
        assertEquals(
            PaymentEntity.contentHash(1_700_000_000_000L, "sent 1 to you"),
            PaymentEntity.contentHash(1_700_000_000_000L, "sent 1 to you"),
        )
    }

    @Test
    fun `same text at different times hashes differently`() {
        assertNotEquals(
            PaymentEntity.contentHash(1_700_000_000_000L, "sent 1 to you"),
            PaymentEntity.contentHash(1_700_000_060_000L, "sent 1 to you"),
        )
    }

    @Test
    fun `different text at same time hashes differently`() {
        assertNotEquals(
            PaymentEntity.contentHash(1_700_000_000_000L, "sent 1 to you"),
            PaymentEntity.contentHash(1_700_000_000_000L, "sent 2 to you"),
        )
    }
}
