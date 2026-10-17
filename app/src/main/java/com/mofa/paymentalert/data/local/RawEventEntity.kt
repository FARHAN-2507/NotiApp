package com.mofa.paymentalert.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "raw_events")
data class RawEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val recordedAt: Long,
    val parsed: Boolean,
    val status: String,
)
