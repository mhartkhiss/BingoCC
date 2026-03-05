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

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCard(cardId: Long): CardEntity?

    @Insert
    suspend fun insertCard(card: CardEntity): Long

    @Update
    suspend fun updateCard(card: CardEntity)

    @Query("UPDATE cards SET name = :name, colorArgb = :colorArgb, updatedAtEpochMs = :updatedAt WHERE id = :cardId")
    suspend fun updateCardMeta(cardId: Long, name: String, colorArgb: Long, updatedAt: Long)

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteCard(cardId: Long)

    @Query("SELECT * FROM cells WHERE cardId = :cardId ORDER BY row ASC, col ASC")
    fun observeCells(cardId: Long): Flow<List<CellEntity>>

    @Query("SELECT * FROM cells WHERE cardId = :cardId ORDER BY row ASC, col ASC")
    suspend fun getCells(cardId: Long): List<CellEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCells(cells: List<CellEntity>)

    @Update
    suspend fun updateCell(cell: CellEntity)

    @Query("UPDATE cells SET isMarked = :isMarked WHERE value = :value")
    suspend fun setMarkedByValue(value: Int, isMarked: Boolean)

    @Query("UPDATE cells SET isMarked = :isMarked WHERE cardId = :cardId AND value = :value")
    suspend fun setMarkedByValueForCard(cardId: Long, value: Int, isMarked: Boolean)

    @Query("UPDATE cells SET isMarked = :isMarked WHERE cardId = :cardId AND row = :row AND col = :col")
    suspend fun setMarkedAt(cardId: Long, row: Int, col: Int, isMarked: Boolean)

    @Query("UPDATE cells SET isMarked = 0")
    suspend fun resetAllMarks()

    @Query("UPDATE cards SET historicalWins = historicalWins + :addedWins WHERE id = :cardId")
    suspend fun addHistoricalWins(cardId: Long, addedWins: Int)

    @Query("UPDATE cards SET historicalWinsDisabled = historicalWinsDisabled + :addedWins WHERE id = :cardId")
    suspend fun addHistoricalWinsDisabled(cardId: Long, addedWins: Int)

    @Query("UPDATE cards SET isActive = :isActive WHERE id = :cardId")
    suspend fun setCardActive(cardId: Long, isActive: Boolean)

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
