package com.mkz.bingocard.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mkz.bingocard.data.db.entities.ActivePatternEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivePatternDao {
    @Query("SELECT * FROM active_patterns")
    fun observeActivePatterns(): Flow<List<ActivePatternEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setActive(entity: ActivePatternEntity)

    @Query("DELETE FROM active_patterns WHERE patternId = :patternId")
    suspend fun setInactive(patternId: Long)

    @Query("DELETE FROM active_patterns")
    suspend fun clear()
}
