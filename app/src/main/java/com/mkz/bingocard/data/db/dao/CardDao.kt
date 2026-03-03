package com.mkz.bingocard.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mkz.bingocard.data.db.entities.CardEntity
import com.mkz.bingocard.data.db.entities.CellEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY updatedAtEpochMs DESC")
    fun observeCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :cardId")
    fun observeCard(cardId: Long): Flow<CardEntity?>

    @Insert
    suspend fun insertCard(card: CardEntity): Long

    @Update
    suspend fun updateCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteCard(cardId: Long)

    @Query("SELECT * FROM cells WHERE cardId = :cardId ORDER BY row ASC, col ASC")
    fun observeCells(cardId: Long): Flow<List<CellEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCells(cells: List<CellEntity>)

    @Update
    suspend fun updateCell(cell: CellEntity)

    @Query("UPDATE cells SET isMarked = :isMarked WHERE value = :value")
    suspend fun setMarkedByValue(value: Int, isMarked: Boolean)

    @Query("UPDATE cells SET isMarked = :isMarked WHERE cardId = :cardId AND row = :row AND col = :col")
    suspend fun setMarkedAt(cardId: Long, row: Int, col: Int, isMarked: Boolean)

    @Query("UPDATE cards SET updatedAtEpochMs = :updatedAtEpochMs WHERE id = :cardId")
    suspend fun touchCard(cardId: Long, updatedAtEpochMs: Long)

    @Transaction
    suspend fun replaceCardCells(cardId: Long, cells: List<CellEntity>, updatedAtEpochMs: Long) {
        @Suppress("RedundantSuspendModifier")
        suspend fun deleteCells() {
            // Keeping deletes out for now; rely on REPLACE upsert.
        }
        deleteCells()
        upsertCells(cells)
        touchCard(cardId, updatedAtEpochMs)
    }
}
