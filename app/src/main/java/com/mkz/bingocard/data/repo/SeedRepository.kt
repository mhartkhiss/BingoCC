package com.mkz.bingocard.data.repo

import com.mkz.bingocard.data.PresetPatternSeeder
import com.mkz.bingocard.data.db.entities.PatternEntity

object SeedRepository {
    suspend fun seedPresetsIfNeeded(repo: BingoRepository) {
        if (repo.countPatterns() == 0) {
            for (preset in PresetPatternSeeder.presets()) {
                repo.insertPattern(
                    PatternEntity(
                        name = preset.name,
                        isPreset = true,
                        mask = preset.mask
                    )
                )
            }
        }

        normalizePresetLabels(repo)
    }

    private suspend fun normalizePresetLabels(repo: BingoRepository) {
        repo.renamePresetByName("Diagonal \\u2198", "Diagonal Down-Right")
        repo.renamePresetByName("Diagonal \\u2199", "Diagonal Down-Left")
    }
}
