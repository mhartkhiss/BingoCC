package com.mkz.bingocard.domain

object PatternMask {
    fun bitIndex(row: Int, col: Int): Int = row * BingoRules.GRID_SIZE + col

    fun hasBit(mask: Long, row: Int, col: Int): Boolean {
        val idx = bitIndex(row, col)
        return mask and (1L shl idx) != 0L
    }

    fun setBit(mask: Long, row: Int, col: Int): Long {
        val idx = bitIndex(row, col)
        return mask or (1L shl idx)
    }

    fun fromCells(cells: Collection<Pair<Int, Int>>): Long {
        var mask = 0L
        for ((r, c) in cells) {
            mask = setBit(mask, r, c)
        }
        return mask
    }
}
