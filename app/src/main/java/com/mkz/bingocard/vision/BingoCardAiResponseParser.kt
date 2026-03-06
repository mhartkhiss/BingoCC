package com.mkz.bingocard.vision

import com.mkz.bingocard.domain.BingoRules
import org.json.JSONException
import org.json.JSONArray
import org.json.JSONObject

data class BingoCardAiAnalysis(
    val grid: Array<Int?>,
    val colorArgb: Long?
)

class InvalidBingoCardScanException(
    message: String,
    val issues: List<String> = emptyList(),
    cause: Throwable? = null
) : IllegalArgumentException(message, cause)

object BingoCardAiResponseParser {

    private val columnLetters = listOf("B", "I", "N", "G", "O")

    val prompt: String = """
        This is an image of a Bingo ticket/card.
        1. Extract the Bingo values strictly by columns in this exact order: B, I, N, G, O.
        2. Inside each column, list the values from top to bottom.
        3. Use null exactly for the center FREE space at column N, row 3.
        4. Determine the border/edge color or the BINGO text color of the physical bingo card.
        Return ONLY a JSON object with this exact structure:
        {
          "grid": {
            "B": [5 integers],
            "I": [5 integers],
            "N": [2 integers, null, 2 integers],
            "G": [5 integers],
            "O": [5 integers]
          },
          "color": "#RRGGBB"
        }
        Do not return a flat array.
        Do not wrap with ```json or any other text.
    """.trimIndent()

    fun parse(jsonText: String): BingoCardAiAnalysis {
        val cleanJson = jsonText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        try {
            val jsonObj = JSONObject(cleanJson)
            val grid = parseGrid(jsonObj)
            val issues = validateGrid(grid)
            if (issues.isNotEmpty()) {
                throw InvalidBingoCardScanException(
                    message = "Scan result looked invalid for a Bingo card. Retry scan with the same image or adjust the crop.",
                    issues = issues
                )
            }
            val colorArgb = parseHexColor(jsonObj.optString("color"))
            return BingoCardAiAnalysis(grid = grid, colorArgb = colorArgb)
        } catch (e: JSONException) {
            throw InvalidBingoCardScanException(
                message = "Scan result was incomplete. Retry scan with the same image or adjust the crop.",
                cause = e
            )
        }
    }

    private fun parseGrid(jsonObj: JSONObject): Array<Int?> {
        val rawGrid = when {
            jsonObj.has("grid") -> jsonObj.get("grid")
            jsonObj.has("gridByColumn") -> jsonObj.get("gridByColumn")
            else -> throw InvalidBingoCardScanException(
                "Scan result was missing the Bingo grid. Retry scan with the same image or adjust the crop."
            )
        }
        return when (rawGrid) {
            is JSONObject -> parseColumnObject(rawGrid)
            is JSONArray -> normalizeFlatGrid(parseFlatArray(rawGrid))
            else -> throw InvalidBingoCardScanException(
                "Scan result used an unsupported grid format. Retry scan with the same image or adjust the crop."
            )
        }
    }

    private fun parseColumnObject(gridObject: JSONObject): Array<Int?> {
        val grid = Array<Int?>(BingoRules.GRID_SIZE * BingoRules.GRID_SIZE) { null }
        for ((col, letter) in columnLetters.withIndex()) {
            if (!gridObject.has(letter)) {
                throw InvalidBingoCardScanException(
                    "Scan result was missing the $letter column. Retry scan with the same image or adjust the crop."
                )
            }
            val columnValues = gridObject.getJSONArray(letter)
            if (columnValues.length() != BingoRules.GRID_SIZE) {
                throw InvalidBingoCardScanException(
                    "Scan result did not return exactly ${BingoRules.GRID_SIZE} values for column $letter. Retry scan with the same image or adjust the crop."
                )
            }
            for (row in 0 until BingoRules.GRID_SIZE) {
                val idx = row * BingoRules.GRID_SIZE + col
                grid[idx] = if (BingoRules.isFreeCell(row, col)) {
                    null
                } else {
                    readIntOrNull(columnValues, row)
                }
            }
        }
        grid[freeIndex()] = null
        return grid
    }

    private fun parseFlatArray(gridArray: JSONArray): Array<Int?> {
        if (gridArray.length() != BingoRules.GRID_SIZE * BingoRules.GRID_SIZE) {
            throw InvalidBingoCardScanException(
                "Scan result did not return a full ${BingoRules.GRID_SIZE}x${BingoRules.GRID_SIZE} grid. Retry scan with the same image or adjust the crop."
            )
        }
        return Array(BingoRules.GRID_SIZE * BingoRules.GRID_SIZE) { idx ->
            if (idx == freeIndex()) null else readIntOrNull(gridArray, idx)
        }
    }

    private fun normalizeFlatGrid(flatGrid: Array<Int?>): Array<Int?> {
        val rowMajor = flatGrid.copyOf().apply { this[freeIndex()] = null }
        val columnMajor = Array<Int?>(BingoRules.GRID_SIZE * BingoRules.GRID_SIZE) { null }
        for (col in 0 until BingoRules.GRID_SIZE) {
            for (row in 0 until BingoRules.GRID_SIZE) {
                val sourceIdx = col * BingoRules.GRID_SIZE + row
                val targetIdx = row * BingoRules.GRID_SIZE + col
                columnMajor[targetIdx] = if (targetIdx == freeIndex()) null else flatGrid[sourceIdx]
            }
        }
        columnMajor[freeIndex()] = null
        return if (scoreCandidate(rowMajor) >= scoreCandidate(columnMajor)) rowMajor else columnMajor
    }

    private fun scoreCandidate(grid: Array<Int?>): Int {
        var score = 0
        val seen = HashSet<Int>()
        for (row in 0 until BingoRules.GRID_SIZE) {
            for (col in 0 until BingoRules.GRID_SIZE) {
                val idx = row * BingoRules.GRID_SIZE + col
                val value = grid[idx]
                if (BingoRules.isFreeCell(row, col)) {
                    score += if (value == null) 6 else -6
                    continue
                }
                if (value == null) {
                    score -= 2
                    continue
                }
                score += if (value in BingoRules.columnRange(col)) 3 else -5
                if (!seen.add(value)) {
                    score -= 3
                }
            }
        }
        return score
    }

    private fun validateGrid(grid: Array<Int?>): List<String> {
        val issues = mutableListOf<String>()
        val seen = HashMap<Int, Pair<Int, Int>>()

        for (row in 0 until BingoRules.GRID_SIZE) {
            for (col in 0 until BingoRules.GRID_SIZE) {
                val idx = row * BingoRules.GRID_SIZE + col
                val value = grid[idx]
                if (BingoRules.isFreeCell(row, col)) {
                    if (value != null) {
                        issues += "Center free space was not null"
                    }
                    continue
                }
                if (value == null) {
                    issues += "Missing value at ${cellLabel(row, col)}"
                    continue
                }
                if (value !in BingoRules.columnRange(col)) {
                    issues += "Value $value is out of range for column ${columnLetters[col]}"
                }
                val previous = seen.putIfAbsent(value, row to col)
                if (previous != null) {
                    issues += "Duplicate value $value at ${cellLabel(row, col)} and ${cellLabel(previous.first, previous.second)}"
                }
            }
        }

        return issues.distinct()
    }

    private fun readIntOrNull(array: JSONArray, index: Int): Int? {
        if (array.isNull(index)) return null
        return when (val value = array.opt(index)) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun parseHexColor(value: String?): Long? {
        val normalized = value?.trim().orEmpty()
        if (!normalized.startsWith("#")) return null
        val hex = normalized.drop(1)
        return when (hex.length) {
            6 -> hex.toLongOrNull(16)?.let { 0xFF000000L or it }
            8 -> hex.toLongOrNull(16)
            else -> null
        }
    }

    private fun freeIndex(): Int = BingoRules.FREE_ROW * BingoRules.GRID_SIZE + BingoRules.FREE_COL

    private fun cellLabel(row: Int, col: Int): String = "${columnLetters[col]}${row + 1}"
}