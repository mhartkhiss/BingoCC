package com.mkz.bingocard.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "called_numbers")
data class CalledNumberEntity(
    @PrimaryKey
    val value: Int,
    val calledAtEpochMs: Long
)
