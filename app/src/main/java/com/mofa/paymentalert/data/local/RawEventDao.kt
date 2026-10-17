package com.mofa.paymentalert.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RawEventDao {

    @Query("SELECT * FROM raw_events ORDER BY id DESC LIMIT 50")
    fun observeRecent(): Flow<List<RawEventEntity>>

    @Insert
    suspend fun insert(event: RawEventEntity)

    @Query(
        "DELETE FROM raw_events WHERE id NOT IN " +
            "(SELECT id FROM raw_events ORDER BY id DESC LIMIT 50)",
    )
    suspend fun trim()

    @Query("DELETE FROM raw_events")
    suspend fun clear()
}
