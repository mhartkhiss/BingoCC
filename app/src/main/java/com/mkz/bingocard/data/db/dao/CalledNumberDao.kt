package com.mkz.bingocard.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mkz.bingocard.data.db.entities.CalledNumberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalledNumberDao {
    @Query("SELECT * FROM called_numbers ORDER BY calledAtEpochMs DESC")
    fun observeCalledNumbers(): Flow<List<CalledNumberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalledNumber(called: CalledNumberEntity)

    @Query("DELETE FROM called_numbers")
    suspend fun clearCalledNumbers()
}
