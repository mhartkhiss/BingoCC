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
    val colorArgb: Long,
    @androidx.room.ColumnInfo(defaultValue = "0")
    val historicalWins: Int = 0,
    @androidx.room.ColumnInfo(defaultValue = "0")
    val historicalWinsDisabled: Int = 0,
    @androidx.room.ColumnInfo(defaultValue = "1")
    val isActive: Boolean = true
)
