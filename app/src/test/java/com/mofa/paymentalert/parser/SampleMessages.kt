package com.mofa.paymentalert.parser

/**
 * Corpus of real-world payment message formats.
 *
 * Append every new format found on the receiver's phone here as a new sample.
 * Use `expected = null` for messages that must be ignored (debits, failures,
 * personal SMS, OTPs).
 */
object SampleMessages {

    data class Expected(
        val amount: Double,
        val sender: String?,
    )

    data class NotificationSample(
        val description: String,
        val pkg: String,
        val title: String,
        val text: String,
        val expected: Expected?,
    )

    data class SmsSample(
        val description: String,
        val senderId: String,
        val body: String,
        val expected: Expected?,
    )

    val notifications = listOf(
        NotificationSample(
            "PhonePe: standard received from",
            pkg = "com.phonepe.app",
            title = "Payment Received",
            text = "₹250 received from Rahul Sharma",
            expected = Expected(250.0, "Rahul Sharma"),
        ),
        NotificationSample(
            "PhonePe: short title form",
            pkg = "com.phonepe.app",
            title = "₹1 received",
            text = "from Test User via PhonePe UPI",
            expected = Expected(1.0, "Test User"),
        ),
        NotificationSample(
            "PhonePe: name paid you",
            pkg = "com.phonepe.app",
            title = "Rahul Sharma",
            text = "Rahul Sharma paid you ₹1250.50",
            expected = Expected(1250.5, "Rahul Sharma"),
        ),
        NotificationSample(
            "PhonePe: name in title, paid you in text",
            pkg = "com.phonepe.app",
            title = "Priya Verma",
            text = "paid you ₹75",
            expected = Expected(75.0, "Priya Verma"),
        ),
        NotificationSample(
            "PhonePe: vpa fallback",
            pkg = "com.phonepe.app",
            title = "Payment Received",
            text = "You received ₹50 from (rahul.k@ybl) on PhonePe",
            expected = Expected(50.0, "rahul.k@ybl"),
        ),
        NotificationSample(
            "PhonePe: combined summary with incoming and outgoing",
            pkg = "com.phonepe.app",
            title = "2 new completed transaction updates",
            text = "MOHOMMAD AMAAN has sent ₹1 to your bank account State Bank of India-7899. " +
                "₹1 has been sent to 9123456780@ptsbi",
            expected = Expected(1.0, "MOHOMMAD AMAAN"),
        ),
        NotificationSample(
            "PhonePe: incoming sent-to-your-account wording",
            pkg = "com.phonepe.app",
            title = "Payment received",
            text = "MOHOMMAD AMAAN has sent ₹250 to your bank account State Bank of India-7899",
            expected = Expected(250.0, "MOHOMMAD AMAAN"),
        ),
        NotificationSample(
            "PhonePe: name in title, has sent in text",
            pkg = "com.phonepe.app",
            title = "MOHOMMAD AMAAN",
            text = "has sent ₹1 to your bank account State Bank of India-7899",
            expected = Expected(1.0, "MOHOMMAD AMAAN"),
        ),
        NotificationSample(
            "PhonePe: name in title, plain received in text",
            pkg = "com.phonepe.app",
            title = "RAHUL KUMAR",
            text = "₹1 received",
            expected = Expected(1.0, "RAHUL KUMAR"),
        ),
        NotificationSample(
            "PhonePe UPI credit card: sent bare amount to you",
            pkg = "com.phonepe.app",
            title = "Mota",
            text = "sent 1 to you",
            expected = Expected(1.0, "Mota"),
        ),
        NotificationSample(
            "PhonePe UPI credit card: sent rupee amount to you",
            pkg = "com.phonepe.app",
            title = "Rahul Kumar",
            text = "sent ₹150 to you",
            expected = Expected(150.0, "Rahul Kumar"),
        ),
        NotificationSample(
            "PhonePe: sent amount to someone else ignored",
            pkg = "com.phonepe.app",
            title = "Rahul",
            text = "sent 1 to Priya",
            expected = null,
        ),
        NotificationSample(
            "PhonePe: bare vpa sender without label or parens",
            pkg = "com.phonepe.app",
            title = "Payment Received",
            text = "You received ₹1 from 9876543210@ybl",
            expected = Expected(1.0, "9876543210@ybl"),
        ),
        NotificationSample(
            "PhonePe: outgoing sent-to-vpa ignored",
            pkg = "com.phonepe.app",
            title = "Transaction update",
            text = "₹1 has been sent to 9123456780@ptsbi",
            expected = null,
        ),
        NotificationSample(
            "PhonePe: outgoing payment ignored",
            pkg = "com.phonepe.app",
            title = "Paid to Rahul",
            text = "You paid ₹250 to Rahul Sharma",
            expected = null,
        ),
        NotificationSample(
            "PhonePe: failed payment ignored",
            pkg = "com.phonepe.app",
            title = "Payment Failed",
            text = "Your payment of ₹250 to Rahul Sharma failed",
            expected = null,
        ),
        NotificationSample(
            "Unmonitored package ignored",
            pkg = "com.example.other",
            title = "Payment Received",
            text = "₹250 received from Rahul",
            expected = null,
        ),
    )

    val sms = listOf(
        SmsSample(
            "HDFC credit",
            senderId = "AD-HDFCBK",
            body = "Rs.250.00 credited to HDFC Bank A/c XX1234 on 16-10-26 from RAHUL SHARMA. -HDFC Bank",
            expected = Expected(250.0, "RAHUL SHARMA"),
        ),
        SmsSample(
            "SBI credit with comma amount",
            senderId = "VK-SBIINB",
            body = "Dear Customer, Your A/C X1234 is credited with Rs.1,250.00 on 16Oct26 by UPI from PRIYA VERMA. -SBI",
            expected = Expected(1250.0, "PRIYA VERMA"),
        ),
        SmsSample(
            "SBI credit with by-name form",
            senderId = "VK-SBIINB",
            body = "Your A/C X1234 is credited with Rs.1.00 on 16-10-26 by RAHUL KUMAR.",
            expected = Expected(1.0, "RAHUL KUMAR"),
        ),
        SmsSample(
            "credit with vpa and name in parens",
            senderId = "AX-ICICIB",
            body = "Rs.500.00 credited to A/c XX1234 by UPI. VPA: rahul@ybl (RAHUL KUMAR)",
            expected = Expected(500.0, "RAHUL KUMAR"),
        ),
        SmsSample(
            "ICICI credit INR prefix",
            senderId = "AX-ICICIB",
            body = "ICICI Bank: INR 75.50 credited to your account XX1234 on 16-Oct-26 by UPI from AMAN GUPTA (aman@okicici).",
            expected = Expected(75.5, "AMAN GUPTA"),
        ),
        SmsSample(
            "Kotak credit transfer from",
            senderId = "JD-KOTAKB",
            body = "INR 2,000.00 credited to your Kotak A/c XX9999 on 16-10-2026. Transfer from SNEHA PATIL.",
            expected = Expected(2000.0, "SNEHA PATIL"),
        ),
        SmsSample(
            "HDFC debit ignored",
            senderId = "AD-HDFCBK",
            body = "Rs.250.00 debited from HDFC Bank A/c XX1234 on 16-10-26. -HDFC Bank",
            expected = null,
        ),
        SmsSample(
            "HDFC outgoing UPI ignored",
            senderId = "AD-HDFCBK",
            body = "Sent Rs.500.00 from HDFC Bank A/c XX1234 to rahul@okhdfcbank on 16-10-26. Ref 123456789012.",
            expected = null,
        ),
        SmsSample(
            "Personal SMS ignored",
            senderId = "9876543210",
            body = "Hey, I received your parcel today",
            expected = null,
        ),
        SmsSample(
            "OTP ignored",
            senderId = "AD-AMZNIN",
            body = "123456 is your OTP for your transaction. Do not share it with anyone.",
            expected = null,
        ),
        SmsSample(
            "Promo without credit keyword ignored",
            senderId = "AX-PROMOS",
            body = "Get Rs.500 cashback on your next purchase!",
            expected = null,
        ),
    )
}
