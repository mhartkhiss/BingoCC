package com.mkz.bingocard.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val colorArgb: Long
)
