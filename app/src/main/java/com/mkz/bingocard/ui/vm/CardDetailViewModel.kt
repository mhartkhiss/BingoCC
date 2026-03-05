package com.mkz.bingocard.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mkz.bingocard.data.db.entities.CellEntity
import com.mkz.bingocard.data.repo.BingoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CardDetailUiState(
    val title: String = "Card",
    val colorArgb: Long = 0xFFCCCCCC,
    val cells: List<CellEntity> = emptyList(),
    val isActive: Boolean = true
)

class CardDetailViewModel(
    private val repo: BingoRepository,
    private val cardId: Long
) : ViewModel() {

    val state: StateFlow<CardDetailUiState> = combine(
        repo.observeCard(cardId),
        repo.observeCells(cardId)
    ) { card, cells ->
        CardDetailUiState(
            title = card?.name ?: "Card",
            colorArgb = card?.colorArgb ?: 0xFFCCCCCC,
            cells = cells,
            isActive = card?.isActive ?: true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardDetailUiState())

    fun setMarkedByValue(value: Int, isMarked: Boolean) {
        viewModelScope.launch {
            repo.setMarkedByValue(value, isMarked)
        }
    }

    fun deleteCard() {
        viewModelScope.launch {
            repo.deleteCard(cardId)
        }
    }

    fun toggleActive(isActive: Boolean) {
        viewModelScope.launch {
            repo.setCardActive(cardId, isActive)
        }
    }
}

class CardDetailViewModelFactory(
    private val repo: BingoRepository,
    private val cardId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CardDetailViewModel(repo, cardId) as T
    }
}
