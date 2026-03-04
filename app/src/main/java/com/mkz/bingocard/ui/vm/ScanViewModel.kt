package com.mkz.bingocard.ui.vm

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mkz.bingocard.data.db.entities.CardEntity
import com.mkz.bingocard.data.db.entities.CellEntity
import com.mkz.bingocard.data.repo.BingoRepository
import com.mkz.bingocard.data.repo.SettingsRepository
import com.mkz.bingocard.domain.BingoRules
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONException

data class ScanUiState(
    val imageUri: Uri? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val grid: Array<Int?> = Array(BingoRules.GRID_SIZE * BingoRules.GRID_SIZE) { null },
    val cardColor: Long? = null
)

class ScanViewModel(private val repo: BingoRepository, private val settingsRepo: SettingsRepository) : ViewModel() {
    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state

    fun onImagePicked(uri: Uri) {
        _state.value = _state.value.copy(imageUri = uri, isProcessing = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val bitmap = getBitmapFromUri(uri)
                if (bitmap == null) {
                     _state.value = _state.value.copy(isProcessing = false, errorMessage = "Failed to open image")
                     return@launch
                }

                val apiKeys = settingsRepo.getApiKeys()
                if (apiKeys.isEmpty()) {
                    _state.value = _state.value.copy(isProcessing = false, errorMessage = "No API keys configured. Please add one in Settings.")
                    return@launch
                }

                val prompt = """
                    This is an image of a Bingo ticket/card.
                    1. Extract the 5x5 grid of numbers. Use 'null' exactly in the 13th spot (index 12) for the FREE space.
                    2. Determine the dominant background color of the physical card (e.g., if it's a red card, blue card, etc.).
                    Return ONLY a JSON object with this exact structure:
                    {
                      "grid": [25 integers/nulls],
                      "color": "#RRGGBB"
                    }
                    Do not wrap with ```json or any other text.
                """.trimIndent()

                var lastError: Throwable? = null
                var mappedGrid: Array<Int?>? = null
                var mappedColor: Long? = null

                for (apiKey in apiKeys) {
                    try {
                        val generativeModel = GenerativeModel(
                            modelName = "gemini-flash-lite-latest", // or gemini-2.5-flash
                            apiKey = apiKey
                        )

                        val response = generativeModel.generateContent(
                            content {
                                image(bitmap)
                                text(prompt)
                            }
                        )

                        val respText = response.text?.trim() ?: "{}"
                        val result = parseGeminiResponse(respText)
                        if (result != null) {
                            mappedGrid = result.first
                            mappedColor = result.second
                            break // Success! Exit the retry loop
                        } else {
                            lastError = IllegalStateException("Failed to parse Gemini JSON response")
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        lastError = t
                        // If it fails, we just continue to the next key
                    }
                }

                if (mappedGrid != null) {
                    _state.value = _state.value.copy(
                        isProcessing = false,
                        grid = mappedGrid,
                        cardColor = mappedColor,
                        errorMessage = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isProcessing = false, 
                        errorMessage = lastError?.localizedMessage ?: "All API keys failed"
                    )
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                _state.value = _state.value.copy(isProcessing = false, errorMessage = t.localizedMessage ?: "Unexpected error parsing image")
            }
        }
    }

    private fun parseGeminiResponse(jsonText: String): Pair<Array<Int?>, Long?>? {
        val grid = Array<Int?>(25) { null }
        var parsedColor: Long? = null
        try {
            val cleanJson = jsonText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val jsonObj = org.json.JSONObject(cleanJson)
            val jsonArray = jsonObj.optJSONArray("grid") ?: return null
            for (i in 0 until minOf(25, jsonArray.length())) {
                if (!jsonArray.isNull(i)) {
                    grid[i] = jsonArray.getInt(i)
                }
            }
            grid[12] = null // Ensure free space is always null
            
            val colorStr = jsonObj.optString("color")
            if (colorStr.isNotEmpty() && colorStr.startsWith("#")) {
                try {
                    parsedColor = android.graphics.Color.parseColor(colorStr).toLong() // this will map standard colors
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return Pair(grid, parsedColor)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        val context = appContextProvider()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun updateCell(row: Int, col: Int, value: Int?) {
        val idx = row * BingoRules.GRID_SIZE + col
        val copy = _state.value.grid.copyOf()
        copy[idx] = value
        _state.value = _state.value.copy(grid = copy)
    }

    fun saveAsNewCard() {
        val current = _state.value
        val now = System.currentTimeMillis()
        val color = current.cardColor ?: (0xFF000000L or (Random.nextInt(0x00FFFFFF).toLong()))
        val card = CardEntity(
            name = "Scanned ${now % 10_000}",
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            colorArgb = color
        )

        val cells = ArrayList<CellEntity>(BingoRules.GRID_SIZE * BingoRules.GRID_SIZE)
        for (r in 0 until BingoRules.GRID_SIZE) {
            for (c in 0 until BingoRules.GRID_SIZE) {
                val idx = r * BingoRules.GRID_SIZE + c
                val isFree = BingoRules.isFreeCell(r, c)
                val v = if (isFree) null else current.grid[idx]
                cells.add(
                    CellEntity(
                        cardId = 0L,
                        row = r,
                        col = c,
                        value = v,
                        isFree = isFree,
                        isMarked = false
                    )
                )
            }
        }

        viewModelScope.launch {
            repo.createCard(card, cells)
        }
    }

    private fun appContextProvider() = com.mkz.bingocard.vision.AndroidContextProvider.appContext
}

class ScanViewModelFactory(private val repo: BingoRepository, private val settingsRepo: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ScanViewModel(repo, settingsRepo) as T
    }
}
