package com.mkz.bingocard.data.repo

import com.mkz.bingocard.data.db.dao.CalledNumberDao
import com.mkz.bingocard.data.db.dao.CardDao
import com.mkz.bingocard.data.db.dao.ActivePatternDao
import com.mkz.bingocard.data.db.dao.PatternDao
import com.mkz.bingocard.data.db.entities.ActivePatternEntity
import com.mkz.bingocard.data.db.entities.CalledNumberEntity
import com.mkz.bingocard.data.db.entities.CardEntity
import com.mkz.bingocard.data.db.entities.CellEntity
import com.mkz.bingocard.data.db.entities.PatternEntity
import kotlinx.coroutines.flow.Flow

class BingoRepository(
    private val cardDao: CardDao,
    private val patternDao: PatternDao,
    private val calledNumberDao: CalledNumberDao,
    private val activePatternDao: ActivePatternDao
) {
    fun observeCards(): Flow<List<CardEntity>> = cardDao.observeCards()

    fun observeCard(cardId: Long): Flow<CardEntity?> = cardDao.observeCard(cardId)

    fun observeCells(cardId: Long): Flow<List<CellEntity>> = cardDao.observeCells(cardId)

    suspend fun createCard(card: CardEntity, cells: List<CellEntity>): Long {
        val now = System.currentTimeMillis()
        val cardId = cardDao.insertCard(card.copy(createdAtEpochMs = now, updatedAtEpochMs = now))
        cardDao.upsertCells(cells.map { it.copy(cardId = cardId) })
        return cardId
    }

    suspend fun deleteCard(cardId: Long) {
        cardDao.deleteCard(cardId)
    }

    suspend fun setMarkedByValue(value: Int, isMarked: Boolean) {
        cardDao.setMarkedByValue(value, isMarked)
        if (isMarked) {
            calledNumberDao.upsertCalledNumber(
                CalledNumberEntity(
                    value = value,
                    calledAtEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun setMarkedAt(cardId: Long, row: Int, col: Int, isMarked: Boolean) {
        cardDao.setMarkedAt(cardId, row, col, isMarked)
        cardDao.touchCard(cardId, System.currentTimeMillis())
    }

    suspend fun resetAllMarks() {
        cardDao.resetAllMarks()
        calledNumberDao.clearCalledNumbers()
    }

    fun observePatterns(): Flow<List<PatternEntity>> = patternDao.observePatterns()

    fun observeActivePatterns(): Flow<List<ActivePatternEntity>> = activePatternDao.observeActivePatterns()

    suspend fun setPatternActive(patternId: Long, active: Boolean) {
        if (active) {
            activePatternDao.setActive(ActivePatternEntity(patternId = patternId))
        } else {
            activePatternDao.setInactive(patternId)
        }
    }

    suspend fun clearActivePatterns() = activePatternDao.clear()

    suspend fun insertPattern(pattern: PatternEntity): Long = patternDao.insertPattern(pattern)

    suspend fun updatePattern(pattern: PatternEntity) = patternDao.updatePattern(pattern)

    suspend fun deleteCustomPattern(patternId: Long) = patternDao.deleteCustomPattern(patternId)

    fun observeCalledNumbers(): Flow<List<CalledNumberEntity>> = calledNumberDao.observeCalledNumbers()

    suspend fun clearCalledNumbers() = calledNumberDao.clearCalledNumbers()

    suspend fun countPatterns(): Int = patternDao.countPatterns()
}
