package com.mkz.bingocard.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mkz.bingocard.data.db.entities.CardEntity
import com.mkz.bingocard.data.db.entities.CellEntity
import com.mkz.bingocard.data.db.entities.PatternEntity
import com.mkz.bingocard.data.repo.BingoRepository
import com.mkz.bingocard.domain.BingoRules
import com.mkz.bingocard.domain.PatternEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

data class CardListItemUi(
    val cardId: Long,
    val name: String,
    val colorArgb: Long,
    val cells: List<CellEntity>,
    val winningCellIndexes: Set<Int>,
    val waitingCellIndexes: Set<Int>,
    val isWin: Boolean,
    val isNearWin: Boolean,
    val markedCount: Int = 0,
    val winCount: Int = 0,
    val dynamicWinCount: Int = 0,
    val isActive: Boolean = true
)

data class PatternChipUi(
    val id: Long,
    val name: String,
    val isActive: Boolean
)

data class CardListUiState(
    val cards: List<CardListItemUi> = emptyList(),
    val patterns: List<PatternChipUi> = emptyList(),
    val calledNumbers: List<Int> = emptyList(),
    val onTogglePattern: (patternId: Long, active: Boolean) -> Unit = { _, _ -> }
)

@OptIn(ExperimentalCoroutinesApi::class)
class CardListViewModel(private val repo: BingoRepository) : ViewModel() {

    val state: StateFlow<CardListUiState> = combine(
        repo.observeCards(),
        repo.observePatterns(),
        repo.observeActivePatterns(),
        repo.observeCalledNumbers()
    ) { cards, patterns, active, calledNumbers ->
        Quad(cards, patterns, active.map { it.patternId }.toSet(), calledNumbers.map { it.value })
    }
        .flatMapLatest { (cards, patterns, activeIds, calledNumbers) ->
            if (cards.isEmpty()) {
                MutableStateFlow(
                    CardListUiState(
                        cards = emptyList(),
                        patterns = patterns.toChips(activeIds),
                        calledNumbers = calledNumbers,
                        onTogglePattern = ::togglePattern
                    )
                ).map { it }
            } else {
                val effectivePatterns = patterns.filterActive(activeIds)
                val cellFlows = cards.map { card -> repo.observeCells(card.id) }
                combine(cellFlows) { cellsArray ->
                    val items = cards.mapIndexed { idx, card ->
                        val cells = cellsArray[idx].toList()
                        val progress = PatternEngine.evaluatePatterns(cells, effectivePatterns)
                        val isWin = if (card.isActive) PatternEngine.isAnyWin(progress) else false
                        val isNearWin = if (card.isActive) PatternEngine.isNearWin(progress) else false
                        val highlights = if (card.isActive) PatternEngine.computeHighlights(cells, effectivePatterns) else PatternEngine.PatternHighlights(emptySet(), emptySet(), 0)
                        val marked = if (card.isActive) cells.count { it.isMarked || BingoRules.isFreeCell(it.row, it.col) } else 0
                        
                        CardListItemUi(
                            cardId = card.id,
                            name = card.name,
                            colorArgb = card.colorArgb,
                            cells = cells,
                            winningCellIndexes = highlights.winningCellIndexes,
                            waitingCellIndexes = highlights.waitingCellIndexes,
                            isWin = isWin,
                            isNearWin = isNearWin,
                            markedCount = marked,
                            winCount = card.historicalWins + highlights.winCount,
                            dynamicWinCount = highlights.winCount,
                            isActive = card.isActive
                        )
                    }

                    val sorted = items.sortedWith(
                        compareByDescending<CardListItemUi> { it.isActive }
                            .thenByDescending { it.isWin }
                            .thenByDescending { it.waitingCellIndexes.size }
                            .thenByDescending { it.markedCount }
                    )

                    CardListUiState(
                        cards = sorted,
                        patterns = patterns.toChips(activeIds),
                        calledNumbers = calledNumbers,
                        onTogglePattern = ::togglePattern
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardListUiState())

    private data class Quad<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    private fun List<PatternEntity>.filterActive(activeIds: Set<Long>): List<PatternEntity> {
        val globallyActive = this.filter { it.isActive }
        if (activeIds.isEmpty()) return globallyActive
        return globallyActive.filter { it.id in activeIds }
    }

    private fun List<PatternEntity>.toChips(activeIds: Set<Long>): List<PatternChipUi> {
        val globallyActive = this.filter { it.isActive }
        val allActive = activeIds.isEmpty()
        return globallyActive.map {
            PatternChipUi(
                id = it.id,
                name = it.name,
                isActive = allActive || it.id in activeIds
            )
        }
    }

    private fun togglePattern(patternId: Long, active: Boolean) {
        viewModelScope.launch {
            repo.setPatternActive(patternId, active)
        }
    }

    fun createRandomCard() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val color = 0xFF000000L or (Random.nextInt(0x00FFFFFF).toLong())
            val card = CardEntity(
                name = "Card ${now % 10_000}",
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                colorArgb = color
            )

            val numbers = BingoRules.generateStandardCardNumbers()
            val cells = ArrayList<CellEntity>(BingoRules.GRID_SIZE * BingoRules.GRID_SIZE)
            for (row in 0 until BingoRules.GRID_SIZE) {
                for (col in 0 until BingoRules.GRID_SIZE) {
                    val idx = row * BingoRules.GRID_SIZE + col
                    val isFree = BingoRules.isFreeCell(row, col)
                    cells.add(
                        CellEntity(
                            cardId = 0L,
                            row = row,
                            col = col,
                            value = numbers[idx],
                            isFree = isFree,
                            isMarked = false
                        )
                    )
                }
            }

            repo.createCard(card, cells)
        }
    }

    fun callNumber(value: Int) {
        viewModelScope.launch {
            repo.setMarkedByValue(value, true)
        }
    }

    fun uncallNumber(value: Int) {
        viewModelScope.launch {
            repo.uncallNumber(value)
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            repo.deleteCard(cardId)
        }
    }

    fun setCardActive(cardId: Long, isActive: Boolean) {
        viewModelScope.launch {
            repo.setCardActive(cardId, isActive)
        }
    }

    fun resetAllMarks() {
        val currentCards = state.value.cards
        val winsMap = currentCards.associate { it.cardId to it.dynamicWinCount }
        viewModelScope.launch {
            repo.resetAllMarks(winsMap)
        }
    }
}

class CardListViewModelFactory(private val repo: BingoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CardListViewModel(repo) as T
    }
}
