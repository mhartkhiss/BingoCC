package com.mkz.bingocard.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mkz.bingocard.data.repo.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SettingsUiState(
    val apiKeys: List<String> = emptyList()
)

class SettingsViewModel(private val settingsRepo: SettingsRepository) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    init {
        loadKeys()
    }

    private fun loadKeys() {
        _state.value = SettingsUiState(apiKeys = settingsRepo.getApiKeys())
    }

    fun addKey(key: String) {
        settingsRepo.addApiKey(key.trim())
        loadKeys()
    }

    fun removeKey(key: String) {
        settingsRepo.removeApiKey(key)
        loadKeys()
    }
}

class SettingsViewModelFactory(private val settingsRepo: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(settingsRepo) as T
    }
}
