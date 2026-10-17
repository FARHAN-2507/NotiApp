package com.mofa.paymentalert.parser

import com.mofa.paymentalert.data.model.ParsedPayment

sealed interface ParseResult {
    data class Parsed(val payment: ParsedPayment) : ParseResult
    data class Rejected(val reason: String) : ParseResult
}
