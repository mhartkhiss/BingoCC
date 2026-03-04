package com.mkz.bingocard.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mkz.bingocard.data.db.entities.PatternEntity
import com.mkz.bingocard.data.repo.BingoRepository
import com.mkz.bingocard.domain.BingoRules
import com.mkz.bingocard.domain.PatternMask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PatternsUiState(
    val patterns: List<PatternEntity> = emptyList(),
    val selectedId: Long? = null,
    val name: String = "",
    val mask: Long = 0L,
    val isPreset: Boolean = false
)

class PatternsViewModel(private val repo: BingoRepository) : ViewModel() {
    private val selectedId = MutableStateFlow<Long?>(null)
    private val draftName = MutableStateFlow("")
    private val draftMask = MutableStateFlow(0L)
    private val draftIsPreset = MutableStateFlow(false)

    val state: StateFlow<PatternsUiState> = combine(
        repo.observePatterns(),
        selectedId,
        draftName,
        draftMask,
        draftIsPreset
    ) { patterns, selected, name, mask, preset ->
        PatternsUiState(
            patterns = patterns,
            selectedId = selected,
            name = name,
            mask = mask,
            isPreset = preset
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PatternsUiState())

    fun selectPattern(patternId: Long) {
        val p = state.value.patterns.firstOrNull { it.id == patternId } ?: return
        selectedId.value = p.id
        draftName.value = p.name
        draftMask.value = p.mask
        draftIsPreset.value = p.isPreset
    }

    fun createNew() {
        selectedId.value = null
        draftName.value = "Custom"
        draftMask.value = 0L
        draftIsPreset.value = false
    }

    fun updateName(name: String) {
        draftName.value = name
    }

    fun toggleCell(row: Int, col: Int) {
        if (draftIsPreset.value) return
        val idx = row * BingoRules.GRID_SIZE + col
        val bit = 1L shl idx
        val current = draftMask.value
        draftMask.value = if (current and bit != 0L) current and bit.inv() else current or bit
    }

    fun save() {
        if (draftIsPreset.value) return
        val name = draftName.value.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            val existingId = selectedId.value
            if (existingId == null) {
                val entity = PatternEntity(
                    name = name,
                    isPreset = false,
                    mask = normalizeMask(draftMask.value),
                    isActive = true
                )
                val id = repo.insertPattern(entity)
                selectPattern(id)
            } else {
                val entity = PatternEntity(
                    id = existingId,
                    name = name,
                    isPreset = false,
                    mask = normalizeMask(draftMask.value),
                    isActive = true
                )
                repo.updatePattern(entity)
            }
        }
    }

    fun deleteSelected() {
        val id = selectedId.value ?: return
        if (draftIsPreset.value) return
        viewModelScope.launch {
            repo.deleteCustomPattern(id)
            createNew()
        }
    }

    fun toggleActive(pattern: PatternEntity) {
        viewModelScope.launch {
            repo.updatePattern(pattern.copy(isActive = !pattern.isActive))
            // Re-select if it's the currently selected one to keep state in sync
            if (selectedId.value == pattern.id) {
                // state flow will emit automatically since DB changes, but UI selects by ID
            }
        }
    }

    private fun normalizeMask(mask: Long): Long {
        // Keep as-is; FREE handling happens in PatternEngine (FREE is always satisfied).
        return mask
    }
}

class PatternsViewModelFactory(private val repo: BingoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PatternsViewModel(repo) as T
    }
}
