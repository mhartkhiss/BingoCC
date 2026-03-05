package com.mkz.bingocard.data.repo

import com.mkz.bingocard.data.db.dao.CalledNumberDao
import com.mkz.bingocard.data.db.dao.CalledNumberStatsDao
import com.mkz.bingocard.data.db.dao.CardDao
import com.mkz.bingocard.data.db.dao.ActivePatternDao
import com.mkz.bingocard.data.db.dao.PatternDao
import com.mkz.bingocard.data.db.entities.ActivePatternEntity
import com.mkz.bingocard.data.db.entities.CalledNumberEntity
import com.mkz.bingocard.data.db.entities.CalledNumberStatEntity
import com.mkz.bingocard.data.db.entities.CardEntity
import com.mkz.bingocard.data.db.entities.CellEntity
import com.mkz.bingocard.data.db.entities.PatternEntity
import kotlinx.coroutines.flow.Flow

class BingoRepository(
    private val cardDao: CardDao,
    private val patternDao: PatternDao,
    private val calledNumberDao: CalledNumberDao,
    private val calledNumberStatsDao: CalledNumberStatsDao,
    private val activePatternDao: ActivePatternDao
) {
    fun observeCards(): Flow<List<CardEntity>> = cardDao.observeCards()

    suspend fun getCard(cardId: Long): CardEntity? = cardDao.getCard(cardId)

    suspend fun getCells(cardId: Long): List<CellEntity> = cardDao.getCells(cardId)

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

    suspend fun updateCard(card: CardEntity, cells: List<CellEntity>) {
        cardDao.updateCardMeta(card.id, card.name, card.colorArgb, card.updatedAtEpochMs)
        cardDao.replaceCardCells(card.id, cells, card.updatedAtEpochMs)
        val calledValues = calledNumberDao.getCalledValues()
        calledValues.forEach { value ->
            cardDao.setMarkedByValueForCard(card.id, value, true)
        }
    }

    suspend fun setCardActive(cardId: Long, isActive: Boolean) {
        cardDao.setCardActive(cardId, isActive)
    }

    suspend fun setMarkedByValue(value: Int, isMarked: Boolean) {
        cardDao.setMarkedByValue(value, isMarked)
        if (isMarked) {
            val alreadyCalled = calledNumberDao.getCalledValues().contains(value)
            calledNumberDao.upsertCalledNumber(
                CalledNumberEntity(
                    value = value,
                    calledAtEpochMs = System.currentTimeMillis()
                )
            )
            if (!alreadyCalled) {
                calledNumberStatsDao.increment(value, System.currentTimeMillis())
            }
        }
    }

    suspend fun uncallNumber(value: Int) {
        cardDao.setMarkedByValue(value, false)
        calledNumberDao.deleteByValue(value)
    }

    suspend fun setMarkedAt(cardId: Long, row: Int, col: Int, isMarked: Boolean) {
        cardDao.setMarkedAt(cardId, row, col, isMarked)
        cardDao.touchCard(cardId, System.currentTimeMillis())
    }

    @androidx.room.Transaction
    suspend fun resetAllMarks(activeWins: Map<Long, Int>, disabledWins: Map<Long, Int>) {
        activeWins.forEach { (cardId, wins) ->
            if (wins > 0) {
                cardDao.addHistoricalWins(cardId, wins)
            }
        }
        disabledWins.forEach { (cardId, wins) ->
            if (wins > 0) {
                cardDao.addHistoricalWinsDisabled(cardId, wins)
            }
        }
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

    fun observeCalledNumberStats(): Flow<List<CalledNumberStatEntity>> = calledNumberStatsDao.observeStats()

    suspend fun clearCalledNumbers() = calledNumberDao.clearCalledNumbers()

    suspend fun clearCalledNumberStats() = calledNumberStatsDao.clearStats()

    suspend fun countPatterns(): Int = patternDao.countPatterns()
}
