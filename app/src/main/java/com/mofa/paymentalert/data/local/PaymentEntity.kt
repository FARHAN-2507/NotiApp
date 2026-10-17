package com.mofa.paymentalert.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mofa.paymentalert.data.model.ParsedPayment
import com.mofa.paymentalert.util.sha256

@Entity(
    tableName = "payments",
    indices = [Index(value = ["rawTextHash"], unique = true)],
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val sender: String?,
    val source: String,
    val sourceDetail: String,
    val receivedAt: Long,
    val rawText: String,
    val rawTextHash: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val uploaded: Boolean = false,
) {
    companion object {
        fun from(
            parsed: ParsedPayment,
            createdAt: Long = System.currentTimeMillis(),
            uploaded: Boolean = false,
        ): PaymentEntity =
            PaymentEntity(
                amount = parsed.amount,
                sender = parsed.sender,
                source = parsed.source.name,
                sourceDetail = parsed.sourceDetail,
                receivedAt = parsed.receivedAt,
                rawText = parsed.rawText,
                rawTextHash = contentHash(parsed.receivedAt, parsed.rawText),
                createdAt = createdAt,
                uploaded = uploaded,
            )

        fun contentHash(receivedAt: Long, rawText: String): String =
            sha256("$receivedAt|$rawText")
    }
}
