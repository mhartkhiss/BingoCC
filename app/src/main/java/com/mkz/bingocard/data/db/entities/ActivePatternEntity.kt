package com.mkz.bingocard.data.db.entities

import androidx.room.Entity

@Entity(
    tableName = "active_patterns",
    primaryKeys = ["patternId"]
)
data class ActivePatternEntity(
    val patternId: Long
)
