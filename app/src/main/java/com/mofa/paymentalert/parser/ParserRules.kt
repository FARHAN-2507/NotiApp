package com.mofa.paymentalert.parser

object ParserRules {

    val monitoredNotificationPackages: Set<String> = setOf(
        "com.phonepe.app",
        // Add more as they come into use; the parser handles any package in this set.
        // "com.google.android.apps.nbu.paisa.user", // Google Pay
        // "net.one97.paytm",                         // Paytm
        // "in.org.npci.upiapp",                      // BHIM
    )

    val creditPatterns: List<Regex> = listOf(
        Regex("""\bcredited\b"""),
        Regex("""\breceived\b"""),
        Regex("""\bpaid you\b"""),
        Regex("""\bsent you\b"""),
        Regex("""\bdeposited\b"""),
        Regex("""\bhas sent\b"""),
        Regex("""\bto your (?:bank )?account\b"""),
        Regex("""\b(?:sent|transferred|paid)\b[^.!?]{0,20}\bto you\b"""),
    )

    val debitPatterns: List<Regex> = listOf(
        Regex("""\bdebited\b"""),
        Regex("""\bwithdrawn?\b"""),
        Regex("""\byou paid\b"""),
        Regex("""\bpaid to (?!you)\b"""),
        Regex("""\bsent to (?!you)\b"""),
        Regex("""\bsent\s+(?:rs\.?|inr|₹)\s*[\d,.]*\s*from\b"""),
        Regex("""\btransferred to (?!you)\b"""),
        Regex("""\bspent\b"""),
    )

    val ignorePatterns: List<Regex> = listOf(
        Regex("""\bfailed\b"""),
        Regex("""\bdeclined\b"""),
        Regex("""\bcancell?ed\b"""),
        Regex("""\bexpired\b"""),
        Regex("""\breversed\b"""),
        Regex("""\bpending\b"""),
        Regex("""\brequests?\b"""),
    )

    val bankTokens: List<String> = listOf(
        "HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "PNB", "BOB", "BARODA", "BOI",
        "CANARA", "UNION", "YES", "IDFC", "FEDERAL", "INDUS", "RBL", "IDBI",
        "UCO", "IOB", "CBI", "PAYTM", "PHONEPE", "AIRTEL", "JIO",
    )

    val senderStopWords: Set<String> = setOf(
        "payment", "payments", "received", "receive", "alert", "transaction",
        "bank", "money", "customer", "account", "acct", "upi", "success",
        "successful", "credited", "debited", "details", "info", "phonepe",
        "google", "pay", "wallet", "credit", "imps", "neft", "rtgs", "ach",
        "nach", "transfer",
    )

    private val genericSenderId = Regex("""^[A-Z]{2}-[A-Z0-9]{3,11}$""")
    private val mobileNumber = Regex("""^\+?[0-9]{10,14}$""")
    private val shortCode = Regex("""^[0-9]{5,6}$""")

    fun isBankSender(senderId: String): Boolean {
        val id = senderId.uppercase().trim()
        if (id.isEmpty()) return false
        if (mobileNumber.matches(id)) return false
        val token = id.substringAfterLast('-')
        if (bankTokens.any { token.contains(it) }) return true
        return genericSenderId.matches(id) || shortCode.matches(id)
    }
}
