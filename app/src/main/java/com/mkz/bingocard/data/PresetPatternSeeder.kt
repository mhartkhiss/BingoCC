package com.mkz.bingocard.data

import com.mkz.bingocard.data.db.entities.PatternEntity
import com.mkz.bingocard.domain.BingoRules
import com.mkz.bingocard.domain.PatternMask

object PresetPatternSeeder {
    fun presets(): List<PatternEntity> {
        val nowMask = 0L
        val presets = ArrayList<PatternEntity>()

        // Regular lines
        for (r in 0 until BingoRules.GRID_SIZE) {
            var m = 0L
            for (c in 0 until BingoRules.GRID_SIZE) m = PatternMask.setBit(m, r, c)
            presets.add(PatternEntity(name = "Row ${r + 1}", isPreset = true, mask = m))
        }
        for (c in 0 until BingoRules.GRID_SIZE) {
            var m = 0L
            for (r in 0 until BingoRules.GRID_SIZE) m = PatternMask.setBit(m, r, c)
            presets.add(PatternEntity(name = "Column ${c + 1}", isPreset = true, mask = m))
        }
        run {
            var m = 0L
            for (i in 0 until BingoRules.GRID_SIZE) m = PatternMask.setBit(m, i, i)
            presets.add(PatternEntity(name = "Diagonal Down-Right", isPreset = true, mask = m))
        }
        run {
            var m = 0L
            for (i in 0 until BingoRules.GRID_SIZE) m = PatternMask.setBit(m, i, BingoRules.GRID_SIZE - 1 - i)
            presets.add(PatternEntity(name = "Diagonal Down-Left", isPreset = true, mask = m))
        }

        // Blackout (all except FREE is naturally satisfied if included)
        run {
            var m = 0L
            for (r in 0 until BingoRules.GRID_SIZE) {
                for (c in 0 until BingoRules.GRID_SIZE) {
                    m = PatternMask.setBit(m, r, c)
                }
            }
            presets.add(PatternEntity(name = "Blackout", isPreset = true, mask = m))
        }

        // 4 corners
        run {
            var m = 0L
            m = PatternMask.setBit(m, 0, 0)
            m = PatternMask.setBit(m, 0, BingoRules.GRID_SIZE - 1)
            m = PatternMask.setBit(m, BingoRules.GRID_SIZE - 1, 0)
            m = PatternMask.setBit(m, BingoRules.GRID_SIZE - 1, BingoRules.GRID_SIZE - 1)
            presets.add(PatternEntity(name = "4 Corners", isPreset = true, mask = m))
        }

        // Victory: top-left, top-right, bottom-center
        run {
            var m = 0L
            m = PatternMask.setBit(m, 0, 0)
            m = PatternMask.setBit(m, 0, BingoRules.GRID_SIZE - 1)
            m = PatternMask.setBit(m, BingoRules.GRID_SIZE - 1, 2)
            presets.add(PatternEntity(name = "Victory", isPreset = true, mask = m))
        }

        // Special presets stored as mask=0 and expanded by PatternEngine
        presets.add(PatternEntity(name = "Small Square (Any 2x2)", isPreset = true, mask = nowMask))
        presets.add(PatternEntity(name = "Slant (Min 3)", isPreset = true, mask = nowMask))

        return presets
    }
}
