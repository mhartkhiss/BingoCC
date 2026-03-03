package com.mkz.bingocard.data.db.entities

import androidx.room.Entity

@Entity(
    tableName = "cells",
    primaryKeys = ["cardId", "row", "col"]
)
data class CellEntity(
    val cardId: Long,
    val row: Int,
    val col: Int,
    val value: Int?,
    val isFree: Boolean,
    val isMarked: Boolean
)
