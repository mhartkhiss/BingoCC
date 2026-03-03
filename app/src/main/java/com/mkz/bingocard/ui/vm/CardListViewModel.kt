package com.mkz.bingocard.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mkz.bingocard.data.db.entities.CardEntity
import com.mkz.bingocard.data.db.entities.CellEntity
import com.mkz.bingocard.data.repo.BingoRepository
import com.mkz.bingocard.domain.BingoRules
import com.mkz.bingocard.domain.PatternEngine
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

data class CardListUiState(
    val cards: List<CardListItemUi> = emptyList(),
    val calledNumberText: String = "",
    val onCalledNumberTextChanged: (String) -> Unit = {}
)

class CardListViewModel(private val repo: BingoRepository) : ViewModel() {
    private val calledNumberText = MutableStateFlow("")

    val state: StateFlow<CardListUiState> = combine(
        repo.observeCards(),
        repo.observePatterns(),
        calledNumberText
    ) { cards, patterns, calledText ->
        Triple(cards, patterns, calledText)
    }
        .flatMapLatest { (cards, patterns, calledText) ->
            if (cards.isEmpty()) {
                MutableStateFlow(CardListUiState(emptyList(), calledText) {}).map { it }
            } else {
                val cellFlows = cards.map { card -> repo.observeCells(card.id) }
                combine(cellFlows) { cellsArray ->
                    val items = cards.mapIndexed { idx, card ->
                        val cells = cellsArray[idx].toList()
                        val progress = PatternEngine.evaluatePatterns(cells, patterns)
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
                        calledNumberText = calledText,
                        onCalledNumberTextChanged = { calledNumberText.value = it }
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardListUiState())

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
}

class CardListViewModelFactory(private val repo: BingoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CardListViewModel(repo) as T
    }
}
