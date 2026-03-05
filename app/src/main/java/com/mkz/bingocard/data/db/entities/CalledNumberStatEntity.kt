package com.mkz.bingocard.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "called_number_stats")
data class CalledNumberStatEntity(
    @PrimaryKey
    val value: Int,
    val callCount: Int,
    val updatedAtEpochMs: Long
)
