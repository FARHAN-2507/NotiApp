package com.mofa.paymentalert.parser

import com.mofa.paymentalert.data.model.ParsedPayment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentParserTest {

    private val postTime = 1_700_000_000_000L

    @Test
    fun `notification samples parse as expected`() {
        SampleMessages.notifications.forEach { sample ->
            val parsed = PaymentParser.parseNotification(sample.pkg, sample.title, sample.text, postTime)
            assertSample(sample.description, sample.expected, parsed)
        }
    }

    @Test
    fun `sms samples parse as expected`() {
        SampleMessages.sms.forEach { sample ->
            val parsed = PaymentParser.parseSms(sample.senderId, sample.body, postTime)
            assertSample(sample.description, sample.expected, parsed)
        }
    }

    @Test
    fun `detailed api returns rejection reason for debit`() {
        val result = PaymentParser.parseSmsDetailed(
            "AD-HDFCBK",
            "Rs.250.00 debited from HDFC Bank A/c XX1234 on 16-10-26.",
            0L,
        )
        assertTrue(result is ParseResult.Rejected)
    }

    @Test
    fun `detailed api parses monitored notification`() {
        val result = PaymentParser.parseNotificationDetailed(
            "com.phonepe.app",
            "Payment Received",
            "₹250 received from Rahul Sharma",
            0L,
        )
        assertTrue(result is ParseResult.Parsed)
    }

    @Test
    fun `detailed api rejects unmonitored package`() {
        val result = PaymentParser.parseNotificationDetailed(
            "com.example.other",
            "Payment Received",
            "₹250 received from Rahul",
            0L,
        )
        assertTrue(result is ParseResult.Rejected)
    }

    private fun assertSample(
        description: String,
        expected: SampleMessages.Expected?,
        parsed: ParsedPayment?,
    ) {
        if (expected == null) {
            assertNull("$description: expected no payment, got $parsed", parsed)
        } else {
            requireNotNull(parsed) { "$description: expected a parsed payment, got null" }
            assertEquals("$description: amount", expected.amount, parsed.amount, 0.001)
            assertEquals("$description: sender", expected.sender, parsed.sender)
        }
    }
}
