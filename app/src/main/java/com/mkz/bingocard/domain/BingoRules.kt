package com.mkz.bingocard.domain

import kotlin.random.Random

object BingoRules {
    const val GRID_SIZE = 5
    const val FREE_ROW = 2
    const val FREE_COL = 2

    fun isFreeCell(row: Int, col: Int): Boolean = row == FREE_ROW && col == FREE_COL

    fun columnRange(col: Int): IntRange {
        return when (col) {
            0 -> 1..15
            1 -> 16..30
            2 -> 31..45
            3 -> 46..60
            4 -> 61..75
            else -> error("Invalid col: $col")
        }
    }

    fun generateStandardCardNumbers(random: Random = Random.Default): Array<Int?> {
        val result = Array(GRID_SIZE * GRID_SIZE) { null as Int? }
        for (col in 0 until GRID_SIZE) {
            val range = columnRange(col).toMutableList()
            range.shuffle(random)
            for (row in 0 until GRID_SIZE) {
                val idx = row * GRID_SIZE + col
                if (isFreeCell(row, col)) {
                    result[idx] = null
                } else {
                    result[idx] = range.removeAt(0)
                }
            }
        }
        return result
    }
}
