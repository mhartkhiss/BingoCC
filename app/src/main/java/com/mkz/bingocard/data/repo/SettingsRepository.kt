package com.mkz.bingocard.data.repo

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bingo_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEYS = "gemini_api_keys"
        private val defaultKeys = listOf(
            "AIzaSyDIIL8LA9NzgWGhHGlIYy-vw92woWzgby4",
            "AIzaSyCnwxDCI21mKDz8MEQHVKrh34gUQ61Nju4"
        )
    }

    fun getApiKeys(): List<String> {
        val keysString = prefs.getString(KEY_API_KEYS, null)
        if (keysString == null) {
            // First time, save defaults
            saveApiKeys(defaultKeys)
            return defaultKeys
        }
        return keysString.split(",").filter { it.isNotBlank() }
    }

    private fun saveApiKeys(keys: List<String>) {
        prefs.edit().putString(KEY_API_KEYS, keys.joinToString(",")).apply()
    }

    fun addApiKey(key: String) {
        val current = getApiKeys().toMutableList()
        if (!current.contains(key) && key.isNotBlank()) {
            current.add(key)
            saveApiKeys(current)
        }
    }

    fun removeApiKey(key: String) {
        val current = getApiKeys().toMutableList()
        if (current.remove(key)) {
            // Ensure we don't end up completely empty if possible, but allow it if user wants to delete all
            saveApiKeys(current)
        }
    }
}
