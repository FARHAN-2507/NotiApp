package com.mofa.paymentalert.data

import com.mofa.paymentalert.data.local.PaymentDao
import com.mofa.paymentalert.data.local.PaymentEntity
import com.mofa.paymentalert.data.local.RawEventDao
import com.mofa.paymentalert.data.local.RawEventEntity
import com.mofa.paymentalert.data.model.ParsedPayment
import com.mofa.paymentalert.parser.ParseResult
import com.mofa.paymentalert.util.formatAmount
import kotlinx.coroutines.flow.Flow

class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val rawEventDao: RawEventDao,
) {

    val payments: Flow<List<PaymentEntity>> = paymentDao.observeAll()

    val pendingUploadCount: Flow<Int> = paymentDao.observePendingCount()

    val rawEvents: Flow<List<RawEventEntity>> = rawEventDao.observeRecent()

    suspend fun ingest(parsed: ParsedPayment): Boolean {
        val since = parsed.receivedAt - DedupPolicy.WINDOW_MS - 1
        val recent = paymentDao.recentWithAmount(parsed.amount, since)
        val duplicate = DedupPolicy.findDuplicate(parsed, recent)
        if (duplicate != null) {
            if (duplicate.sender == null && parsed.sender != null) {
                paymentDao.updateSender(duplicate.id, parsed.sender)
                return true
            }
            return false
        }
        return paymentDao.insert(PaymentEntity.from(parsed)) != -1L
    }

    suspend fun pendingUploads(limit: Int): List<PaymentEntity> = paymentDao.pendingUploads(limit)

    suspend fun pendingCount(): Int = paymentDao.pendingCount()

    suspend fun insertRemote(
        rawTextHash: String,
        amount: Double,
        sender: String?,
        source: String,
        sourceDetail: String,
        receivedAt: Long,
    ): Boolean {
        val entity = PaymentEntity(
            amount = amount,
            sender = sender,
            source = source,
            sourceDetail = sourceDetail,
            receivedAt = receivedAt,
            rawText = "",
            rawTextHash = rawTextHash,
            createdAt = System.currentTimeMillis(),
            uploaded = true,
        )
        return paymentDao.insert(entity) != -1L
    }

    suspend fun markUploaded(id: Long) = paymentDao.markUploaded(id)

    suspend fun requeueAllForUpload() = paymentDao.requeueAllForUpload()

    suspend fun clear() = paymentDao.clear()

    suspend fun recordRawEvent(
        source: String,
        title: String,
        text: String,
        postedAt: Long,
        result: ParseResult,
    ) {
        val status = when (result) {
            is ParseResult.Parsed ->
                "parsed: ${formatAmount(result.payment.amount)} from ${result.payment.sender ?: "unknown sender"}"
            is ParseResult.Rejected -> "rejected: ${result.reason}"
        }
        rawEventDao.insert(
            RawEventEntity(
                source = source,
                title = title,
                text = text,
                postedAt = postedAt,
                recordedAt = System.currentTimeMillis(),
                parsed = result is ParseResult.Parsed,
                status = status,
            ),
        )
        rawEventDao.trim()
    }

    suspend fun clearRawEvents() = rawEventDao.clear()
}
