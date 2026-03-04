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

    /**
     * Generates edge-to-edge diagonals (min 3 cells).
     * E.g. top-left to bottom-right (full), B row 3 to N row 1, etc.
     * Excludes "cut" diagonals like top-left to center-only.
     */
    private fun generateSlantingMasksMin3(): List<Long> {
        val masks = ArrayList<Long>()
        val n = BingoRules.GRID_SIZE
        val minLen = 3

        fun addEdgeToEdgeDiagonal(startRow: Int, startCol: Int, dRow: Int, dCol: Int) {
            var len = 0
            var r = startRow
            var c = startCol
            while (r in 0 until n && c in 0 until n) {
                len++
                r += dRow
                c += dCol
            }
            if (len >= minLen) {
                var m = 0L
                r = startRow
                c = startCol
                for (k in 0 until len) {
                    m = PatternMask.setBit(m, r, c)
                    r += dRow
                    c += dCol
                }
                masks.add(m)
            }
        }

        // Down-right (↘): from top edge and left edge
        for (c in 0..(n - minLen)) addEdgeToEdgeDiagonal(0, c, 1, 1)
        for (r in 1..(n - minLen)) addEdgeToEdgeDiagonal(r, 0, 1, 1)

        // Up-right (↗): from bottom edge and left edge
        for (c in 0..(n - minLen)) addEdgeToEdgeDiagonal(n - 1, c, -1, 1)
        for (r in (n - 2) downTo minLen - 1) addEdgeToEdgeDiagonal(r, 0, -1, 1)

        return masks
    }
}
