package com.mkz.bingocard.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class BingoCardAiResponseParserTest {

    @Test
    fun parse_column_object_maps_to_row_major_grid() {
        val response = """
            {
              "grid": {
                "B": [1, 2, 3, 4, 5],
                "I": [16, 17, 18, 19, 20],
                "N": [31, 32, null, 34, 35],
                "G": [46, 47, 48, 49, 50],
                "O": [61, 62, 63, 64, 65]
              },
              "color": "#123456"
            }
        """.trimIndent()

        val parsed = BingoCardAiResponseParser.parse(response)

        assertEquals(
            listOf(
                1, 16, 31, 46, 61,
                2, 17, 32, 47, 62,
                3, 18, null, 48, 63,
                4, 19, 34, 49, 64,
                5, 20, 35, 50, 65
            ),
            parsed?.grid?.toList()
        )
        assertEquals(0xFF123456L, parsed?.colorArgb)
    }

    @Test
    fun parse_flat_column_major_array_normalizes_to_row_major_grid() {
        val response = """
            {
              "grid": [1, 2, 3, 4, 5, 16, 17, 18, 19, 20, 31, 32, null, 34, 35, 46, 47, 48, 49, 50, 61, 62, 63, 64, 65],
              "color": "#ABCDEF"
            }
        """.trimIndent()

        val parsed = BingoCardAiResponseParser.parse(response)

        assertEquals(
            listOf(
                1, 16, 31, 46, 61,
                2, 17, 32, 47, 62,
                3, 18, null, 48, 63,
                4, 19, 34, 49, 64,
                5, 20, 35, 50, 65
            ),
            parsed?.grid?.toList()
        )
        assertEquals(0xFFABCDEFL, parsed?.colorArgb)
    }

    @Test
    fun parse_forces_free_space_to_null() {
        val response = """
            {
              "grid": {
                "B": [5, 6, 7, 8, 9],
                "I": [16, 17, 18, 19, 20],
                "N": [31, 32, 33, 34, 35],
                "G": [46, 47, 48, 49, 50],
                "O": [61, 62, 63, 64, 65]
              }
            }
        """.trimIndent()

        val parsed = BingoCardAiResponseParser.parse(response)

        assertNull(parsed?.grid?.get(12))
    }

    @Test
    fun parse_rejects_out_of_range_values() {
        val response = """
            {
              "grid": {
                "B": [1, 2, 3, 4, 5],
                "I": [16, 17, 18, 19, 20],
                "N": [31, 32, null, 34, 35],
                "G": [46, 47, 48, 49, 50],
                "O": [12, 62, 63, 64, 65]
              }
            }
        """.trimIndent()

        try {
            BingoCardAiResponseParser.parse(response)
            fail("Expected invalid scan exception")
        } catch (e: InvalidBingoCardScanException) {
            assertTrue(e.issues.any { it.contains("out of range", ignoreCase = true) })
        }
    }

    @Test
    fun parse_rejects_duplicate_values() {
        val response = """
            {
              "grid": {
                "B": [1, 2, 3, 4, 5],
                "I": [16, 17, 18, 19, 20],
                "N": [31, 32, null, 34, 35],
                "G": [46, 47, 48, 49, 50],
                "O": [61, 62, 63, 64, 16]
              }
            }
        """.trimIndent()

        try {
            BingoCardAiResponseParser.parse(response)
            fail("Expected invalid scan exception")
        } catch (e: InvalidBingoCardScanException) {
            assertTrue(e.issues.any { it.contains("Duplicate value 16", ignoreCase = true) })
        }
    }
}