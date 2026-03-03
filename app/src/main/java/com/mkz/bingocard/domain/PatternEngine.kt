package com.mkz.bingocard.domain

import com.mkz.bingocard.data.db.entities.CellEntity
import com.mkz.bingocard.data.db.entities.PatternEntity

object PatternEngine {
    data class PatternProgress(
        val patternId: Long,
        val patternName: String,
        val missingCount: Int,
        val isWin: Boolean
    )

    fun evaluatePatterns(
        cells: List<CellEntity>,
        patterns: List<PatternEntity>
    ): List<PatternProgress> {
        val marked = BooleanArray(BingoRules.GRID_SIZE * BingoRules.GRID_SIZE)
        for (cell in cells) {
            val idx = cell.row * BingoRules.GRID_SIZE + cell.col
            marked[idx] = cell.isMarked || cell.isFree
        }

        return patterns.map { pattern ->
            val expandedMasks = expandToMasks(pattern)
            val bestMissing = expandedMasks.minOfOrNull { missingCount(it, marked) } ?: Int.MAX_VALUE
            PatternProgress(
                patternId = pattern.id,
                patternName = pattern.name,
                missingCount = bestMissing,
                isWin = bestMissing == 0
            )
        }
    }

    fun isNearWin(progress: List<PatternProgress>): Boolean {
        return progress.any { !it.isWin && it.missingCount == 1 }
    }

    fun isAnyWin(progress: List<PatternProgress>): Boolean {
        return progress.any { it.isWin }
    }

    private fun missingCount(mask: Long, marked: BooleanArray): Int {
        var missing = 0
        for (idx in 0 until BingoRules.GRID_SIZE * BingoRules.GRID_SIZE) {
            if (mask and (1L shl idx) != 0L) {
                if (!marked[idx]) missing++
            }
        }
        return missing
    }

    private fun expandToMasks(pattern: PatternEntity): List<Long> {
        return when {
            pattern.isPreset && pattern.mask == 0L && pattern.name.startsWith("Small Square", ignoreCase = true) -> {
                generateAll2x2Masks()
            }
            pattern.isPreset && pattern.mask == 0L && pattern.name.startsWith("Slant", ignoreCase = true) -> {
                generateSlantingMasksMin3()
            }
            else -> listOf(pattern.mask)
        }
    }

    private fun generateAll2x2Masks(): List<Long> {
        val masks = ArrayList<Long>()
        for (r in 0 until BingoRules.GRID_SIZE - 1) {
            for (c in 0 until BingoRules.GRID_SIZE - 1) {
                var m = 0L
                m = PatternMask.setBit(m, r, c)
                m = PatternMask.setBit(m, r, c + 1)
                m = PatternMask.setBit(m, r + 1, c)
                m = PatternMask.setBit(m, r + 1, c + 1)
                masks.add(m)
            }
        }
        return masks
    }

    private fun generateSlantingMasksMin3(): List<Long> {
        val masks = ArrayList<Long>()
        val n = BingoRules.GRID_SIZE
        val minLen = 3
        val maxLen = n

        fun addDiagonalSegments(isRight: Boolean) {
            for (len in minLen..maxLen) {
                for (startRow in 0..(n - len)) {
                    for (startCol in 0..(n - len)) {
                        var m = 0L
                        for (k in 0 until len) {
                            val r = startRow + k
                            val c = if (isRight) startCol + k else (startCol + (len - 1 - k))
                            m = PatternMask.setBit(m, r, c)
                        }
                        masks.add(m)
                    }
                }
            }
        }

        addDiagonalSegments(isRight = true)
        addDiagonalSegments(isRight = false)
        return masks
    }
}
