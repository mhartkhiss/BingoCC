package com.mkz.bingocard.data.repo

import com.mkz.bingocard.data.PresetPatternSeeder
import com.mkz.bingocard.data.db.entities.PatternEntity

object SeedRepository {
    suspend fun seedPresetsIfNeeded(repo: BingoRepository) {
        if (repo.countPatterns() > 0) return
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
}
