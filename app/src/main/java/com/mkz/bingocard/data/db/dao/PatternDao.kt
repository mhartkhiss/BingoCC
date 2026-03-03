package com.mkz.bingocard.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mkz.bingocard.data.db.entities.PatternEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternDao {
    @Query("SELECT * FROM patterns ORDER BY isPreset DESC, name ASC")
    fun observePatterns(): Flow<List<PatternEntity>>

    @Insert
    suspend fun insertPattern(pattern: PatternEntity): Long

    @Update
    suspend fun updatePattern(pattern: PatternEntity)

    @Query("DELETE FROM patterns WHERE id = :patternId AND isPreset = 0")
    suspend fun deleteCustomPattern(patternId: Long)

    @Query("SELECT COUNT(*) FROM patterns")
    suspend fun countPatterns(): Int
}
