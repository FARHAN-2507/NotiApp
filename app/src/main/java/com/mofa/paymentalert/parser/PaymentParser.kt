package com.mofa.paymentalert.parser

import com.mofa.paymentalert.data.model.ParsedPayment
import com.mofa.paymentalert.data.model.PaymentSource

object PaymentParser {

    private val amountRegex = Regex(
        """(?:inr|rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )

    private val toYouAmountRegex = Regex(
        """\b(?:sent|transferred|paid)\s+([\d,]+(?:\.\d{1,2})?)\s+to you\b""",
        RegexOption.IGNORE_CASE,
    )

    private val fromPattern = Regex(
        """\bfrom\s*:?\s+([A-Za-z][A-Za-z .]{1,40}?)(?=\s*(?:[.,;(\-]|\bon\b|\bat\b|\bvia\b|\bfor\b|\bupi\b|\bref\b|$))""",
        RegexOption.IGNORE_CASE,
    )

    private val paidYouPattern = Regex(
        """^([A-Za-z][A-Za-z .]{1,40}?)\s+(?:has\s+)?(?:paid you|sent you)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val hasSentPattern = Regex(
        """^([A-Za-z][A-Za-z .]{1,40}?)\s+has sent\b""",
        RegexOption.IGNORE_CASE,
    )

    private val byPattern = Regex(
        """\bby\s+([A-Za-z][A-Za-z .]{1,40}?)(?=\s*(?:[.,;(\-]|\bon\b|\bat\b|\bvia\b|\bfor\b|\bupi\b|\bref\b|\bfrom\b|$))""",
        RegexOption.IGNORE_CASE,
    )

    private val parenName = Regex("""\(([A-Za-z][A-Za-z .]{1,40})\)""")

    private val vpaInParens = Regex("""\(([\w.\-]+@[A-Za-z][A-Za-z0-9]*)\)""")

    private val vpaLabeled = Regex(
        """\b(?:vpa|upi)\s*[: ]\s*([\w.\-]+@[A-Za-z][A-Za-z0-9]*)""",
        RegexOption.IGNORE_CASE,
    )

    private val genericVpa = Regex("""([\w.\-]+@[A-Za-z][A-Za-z0-9]*)""")

    private val whitespace = Regex("""\s+""")

    private val sentenceBoundary = Regex("""(?<=[.!?])\s+|\n+""")

    fun parseNotification(packageName: String, title: String, text: String, postTime: Long): ParsedPayment? =
        (parseNotificationDetailed(packageName, title, text, postTime) as? ParseResult.Parsed)?.payment

    fun parseNotificationDetailed(
        packageName: String,
        title: String,
        text: String,
        postTime: Long,
    ): ParseResult {
        if (packageName !in ParserRules.monitoredNotificationPackages) {
            return ParseResult.Rejected("package not monitored")
        }
        val titleClean = clean(title)
        val textClean = clean(text)
        return parseCore(
            combined = join(titleClean, textClean),
            title = titleClean,
            text = textClean,
            source = PaymentSource.NOTIFICATION,
            sourceDetail = packageName,
            receivedAt = postTime,
            rawText = listOf(titleClean, textClean).filter { it.isNotEmpty() }.joinToString("\n"),
        )
    }

    fun parseSms(senderId: String, body: String, timestamp: Long): ParsedPayment? =
        (parseSmsDetailed(senderId, body, timestamp) as? ParseResult.Parsed)?.payment

    fun parseSmsDetailed(senderId: String, body: String, timestamp: Long): ParseResult {
        if (!ParserRules.isBankSender(senderId)) {
            return ParseResult.Rejected("sender is not a bank sender id")
        }
        return parseCore(
            combined = clean(body),
            title = "",
            text = clean(body),
            source = PaymentSource.SMS,
            sourceDetail = senderId,
            receivedAt = timestamp,
            rawText = body.trim(),
        )
    }

    private fun parseCore(
        combined: String,
        title: String,
        text: String,
        source: PaymentSource,
        sourceDetail: String,
        receivedAt: Long,
        rawText: String,
    ): ParseResult {
        if (combined.isEmpty()) return ParseResult.Rejected("empty text")

        val segments = splitSegments(combined)
        if (segments.size > 1) {
            val creditSegment = segments.firstOrNull { isCreditSegment(it) }
            if (creditSegment != null) {
                val amount = extractAmount(creditSegment)
                if (amount != null && amount > 0.0) {
                    val sender = extractSender(creditSegment, "", creditSegment)
                        ?: extractSender(combined, title, text)
                    return ParseResult.Parsed(
                        ParsedPayment(amount, sender, source, sourceDetail, receivedAt, rawText),
                    )
                }
            }
        }

        val lower = combined.lowercase()
        ParserRules.ignorePatterns.firstOrNull { it.containsMatchIn(lower) }?.let {
            return ParseResult.Rejected("ignored keyword matched: ${it.pattern}")
        }
        ParserRules.debitPatterns.firstOrNull { it.containsMatchIn(lower) }?.let {
            return ParseResult.Rejected("debit/outgoing pattern matched: ${it.pattern}")
        }
        if (ParserRules.creditPatterns.none { it.containsMatchIn(lower) }) {
            return ParseResult.Rejected("no credit keyword found")
        }
        val amount = extractAmount(combined) ?: return ParseResult.Rejected("no amount found")
        if (amount <= 0.0) return ParseResult.Rejected("amount is not positive")
        return ParseResult.Parsed(
            ParsedPayment(
                amount = amount,
                sender = extractSender(combined, title, text),
                source = source,
                sourceDetail = sourceDetail,
                receivedAt = receivedAt,
                rawText = rawText,
            ),
        )
    }

    private fun splitSegments(text: String): List<String> =
        text.split(sentenceBoundary).map(::clean).filter { it.isNotEmpty() }

    private fun isCreditSegment(segment: String): Boolean {
        val lower = segment.lowercase()
        if (ParserRules.ignorePatterns.any { it.containsMatchIn(lower) }) return false
        if (ParserRules.debitPatterns.any { it.containsMatchIn(lower) }) return false
        return ParserRules.creditPatterns.any { it.containsMatchIn(lower) }
    }

    fun extractAmount(text: String): Double? =
        amountRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            ?: toYouAmountRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

    fun extractSender(combined: String, title: String, text: String): String? {
        fromPattern.find(combined)?.groupValues?.get(1)?.let { candidate ->
            cleanSender(candidate)?.let { return it }
        }
        paidYouPattern.find(text)?.groupValues?.get(1)?.let { candidate ->
            cleanSender(candidate)?.let { return it }
        }
        paidYouPattern.find(title)?.groupValues?.get(1)?.let { candidate ->
            cleanSender(candidate)?.let { return it }
        }
        hasSentPattern.find(text)?.groupValues?.get(1)?.let { candidate ->
            cleanSender(candidate)?.let { return it }
        }
        if (text.startsWith("paid you", ignoreCase = true) ||
            text.startsWith("sent you", ignoreCase = true) ||
            text.startsWith("has sent", ignoreCase = true)
        ) {
            cleanSender(title)?.takeIf { isPlausibleName(it) }?.let { return it }
        }
        byPattern.find(combined)?.groupValues?.get(1)?.let { candidate ->
            cleanSender(candidate)?.let { return it }
        }
        parenName.find(combined)?.groupValues?.get(1)?.let { candidate ->
            cleanSender(candidate)?.let { return it }
        }
        cleanSender(title)?.takeIf { isPlausibleName(it) }?.let { return it }
        vpaInParens.find(combined)?.groupValues?.get(1)?.let { return it }
        vpaLabeled.find(combined)?.groupValues?.get(1)?.let { return it }
        genericVpa.find(combined)?.groupValues?.get(1)?.let { return it }
        return null
    }

    private fun cleanSender(raw: String): String? {
        val cleaned = whitespace.replace(raw.trim().trim('.', ',', '-', ' '), " ").trim()
        if (cleaned.length < 2) return null
        if (!isPlausibleName(cleaned)) return null
        return cleaned
    }

    private fun isPlausibleName(value: String): Boolean {
        if (value.isEmpty() || value.length > 40) return false
        if (value.any { it.isDigit() }) return false
        val words = value.lowercase().split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty() || words.size > 4) return false
        return words.none { it in ParserRules.senderStopWords }
    }

    private fun clean(value: String): String = whitespace.replace(value.trim(), " ")

    private fun join(title: String, text: String): String =
        listOf(title, text).filter { it.isNotEmpty() }.joinToString(" ")
}
