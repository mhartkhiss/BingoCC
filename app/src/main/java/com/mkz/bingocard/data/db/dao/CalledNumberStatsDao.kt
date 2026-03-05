package com.mkz.bingocard.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.mkz.bingocard.data.db.entities.CalledNumberStatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalledNumberStatsDao {
    @Query("SELECT * FROM called_number_stats ORDER BY callCount DESC, value ASC")
    fun observeStats(): Flow<List<CalledNumberStatEntity>>

    @Query("""
        INSERT INTO called_number_stats(value, callCount, updatedAtEpochMs)
        VALUES(:value, 1, :now)
        ON CONFLICT(value) DO UPDATE SET
            callCount = callCount + 1,
            updatedAtEpochMs = :now
    """)
    suspend fun increment(value: Int, now: Long)

    @Query("DELETE FROM called_number_stats")
    suspend fun clearStats()
}
