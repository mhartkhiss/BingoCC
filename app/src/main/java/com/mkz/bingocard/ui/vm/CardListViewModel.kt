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
    val isWin: Boolean,
    val isNearWin: Boolean
)

data class PatternChipUi(
    val id: Long,
    val name: String,
    val isActive: Boolean
)

data class CardListUiState(
    val cards: List<CardListItemUi> = emptyList(),
    val patterns: List<PatternChipUi> = emptyList(),
    val onTogglePattern: (patternId: Long, active: Boolean) -> Unit = { _, _ -> }
)

@OptIn(ExperimentalCoroutinesApi::class)
class CardListViewModel(private val repo: BingoRepository) : ViewModel() {

    val state: StateFlow<CardListUiState> = combine(
        repo.observeCards(),
        repo.observePatterns(),
        repo.observeActivePatterns()
    ) { cards, patterns, active ->
        Triple(cards, patterns, active.map { it.patternId }.toSet())
    }
        .flatMapLatest { (cards, patterns, activeIds) ->
            if (cards.isEmpty()) {
                MutableStateFlow(
                    CardListUiState(
                        cards = emptyList(),
                        patterns = patterns.toChips(activeIds),
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
                        val isWin = PatternEngine.isAnyWin(progress)
                        val isNearWin = PatternEngine.isNearWin(progress)
                        CardListItemUi(
                            cardId = card.id,
                            name = card.name,
                            colorArgb = card.colorArgb,
                            isWin = isWin,
                            isNearWin = isNearWin
                        )
                    }

                    val sorted = items.sortedWith(
                        compareByDescending<CardListItemUi> { it.isWin }
                            .thenByDescending { it.isNearWin }
                    )

                    CardListUiState(
                        cards = sorted,
                        patterns = patterns.toChips(activeIds),
                        onTogglePattern = ::togglePattern
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardListUiState())

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

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            repo.deleteCard(cardId)
        }
    }

    fun resetAllMarks() {
        viewModelScope.launch {
            repo.resetAllMarks()
        }
    }
}

class CardListViewModelFactory(private val repo: BingoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CardListViewModel(repo) as T
    }
}
