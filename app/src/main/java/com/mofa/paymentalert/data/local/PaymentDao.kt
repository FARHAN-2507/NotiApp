package com.mofa.paymentalert.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE amount = :amount AND receivedAt >= :since")
    suspend fun recentWithAmount(amount: Double, since: Long): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(payment: PaymentEntity): Long

    @Query("SELECT COUNT(*) FROM payments WHERE uploaded = 0")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM payments WHERE uploaded = 0")
    suspend fun pendingCount(): Int

    @Query("SELECT * FROM payments WHERE uploaded = 0 ORDER BY receivedAt ASC LIMIT :limit")
    suspend fun pendingUploads(limit: Int): List<PaymentEntity>

    @Query("UPDATE payments SET uploaded = 1 WHERE id = :id")
    suspend fun markUploaded(id: Long)

    @Query("UPDATE payments SET sender = :sender, uploaded = 0 WHERE id = :id")
    suspend fun updateSender(id: Long, sender: String)

    @Query("UPDATE payments SET uploaded = 0 WHERE rawText != ''")
    suspend fun requeueAllForUpload()

    @Query("DELETE FROM payments")
    suspend fun clear()
}
