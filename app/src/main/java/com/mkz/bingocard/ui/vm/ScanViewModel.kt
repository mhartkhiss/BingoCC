package com.mkz.bingocard.ui.vm

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mkz.bingocard.BuildConfig
import com.mkz.bingocard.data.db.entities.CardEntity
import com.mkz.bingocard.data.db.entities.CellEntity
import com.mkz.bingocard.data.repo.BingoRepository
import com.mkz.bingocard.domain.BingoRules
import com.mkz.bingocard.vision.GitHubModelsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.json.JSONException
import org.json.JSONObject
import kotlin.random.Random

data class ScanUiState(
    val imageUri: Uri? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val grid: Array<Int?> = Array(BingoRules.GRID_SIZE * BingoRules.GRID_SIZE) { null },
    val cardColor: Long? = null,
    val cardName: String = "",
    val editingCardId: Long? = null
)

class ScanViewModel(private val repo: BingoRepository) : ViewModel() {

    companion object {
        private const val MAX_DIMENSION = 1024
        private val GITHUB_TOKENS: List<String> =
            BuildConfig.GITHUB_TOKENS.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state

    private var createDraftState: ScanUiState = newCreateState()

    private fun newCreateState(): ScanUiState = ScanUiState(
        imageUri = null,
        isProcessing = false,
        errorMessage = null,
        grid = Array(25) { null },
        cardColor = 0xFF42A5F5,
        cardName = "",
        editingCardId = null
    )

    private fun updateCreateDraftIfNeeded(updated: ScanUiState) {
        if (updated.editingCardId == null) {
            createDraftState = updated.copy(
                imageUri = null,
                isProcessing = false,
                errorMessage = null,
                editingCardId = null
            )
        }
    }

    fun startCreateDraft() {
        _state.value = createDraftState.copy(
            imageUri = null,
            isProcessing = false,
            errorMessage = null,
            editingCardId = null
        )
    }

    /** Stores the image URI for the crop screen — no processing yet. */
    fun setImage(uri: Uri) {
        val current = _state.value
        val next = if (current.editingCardId == null) {
            current.copy(
                imageUri = uri,
                isProcessing = false,
                errorMessage = null,
                editingCardId = null
            )
        } else {
            ScanUiState(imageUri = uri)
        }
        _state.value = next
        updateCreateDraftIfNeeded(next)
    }

    /**
     * Called after the user crops the image. Loads the bitmap, applies the
     * crop, downscales to ≤1 MB JPEG, and sends it to GitHub Models for analysis.
     * @param left, top, right, bottom — normalized crop coordinates (0.0–1.0)
     */
    fun analyzeCroppedImage(left: Float, top: Float, right: Float, bottom: Float) {
        val uri = _state.value.imageUri ?: return
        _state.value = _state.value.copy(isProcessing = true, errorMessage = null)

        viewModelScope.launch {
            try {
                var bitmap = getBitmapFromUri(uri)
                if (bitmap == null) {
                    _state.value = _state.value.copy(isProcessing = false, errorMessage = "Failed to open image")
                    return@launch
                }

                // Apply crop
                val cropX = (left * bitmap.width).toInt().coerceIn(0, bitmap.width)
                val cropY = (top * bitmap.height).toInt().coerceIn(0, bitmap.height)
                val cropW = ((right - left) * bitmap.width).toInt().coerceIn(1, bitmap.width - cropX)
                val cropH = ((bottom - top) * bitmap.height).toInt().coerceIn(1, bitmap.height - cropY)
                bitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)

                // Downscale to reduce upload time and token usage
                bitmap = downscaleBitmap(bitmap)

                val prompt = """
                    This is an image of a Bingo ticket/card.
                    1. Extract the 5x5 grid of numbers. Use 'null' exactly in the 13th spot (index 12) for the FREE space.
                    2. Determine the border/edge color or the BINGO text color of the physical bingo card (the color of the card's outer border or frame, not the background or numbers).
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

                for (token in GITHUB_TOKENS) {
                    try {
                        val respText = GitHubModelsClient.analyzeImage(
                            token = token,
                            bitmap = bitmap,
                            prompt = prompt
                        ).trim()

                        val result = parseResponse(respText)
                        if (result != null) {
                            mappedGrid = result.first
                            mappedColor = result.second
                            break
                        } else {
                            lastError = IllegalStateException("Failed to parse AI response")
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        lastError = t
                    }
                }

                if (mappedGrid != null) {
                    val updated = _state.value.copy(
                        isProcessing = false,
                        grid = mappedGrid,
                        cardColor = mappedColor,
                        errorMessage = null
                    )
                    _state.value = updated
                    updateCreateDraftIfNeeded(updated)
                } else {
                    val errorMsg = when (lastError) {
                        is kotlinx.coroutines.TimeoutCancellationException ->
                            "Request timed out. Please try again."
                        else ->
                            lastError?.localizedMessage ?: "All API keys failed"
                    }
                    _state.value = _state.value.copy(isProcessing = false, errorMessage = errorMsg)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                _state.value = _state.value.copy(
                    isProcessing = false,
                    errorMessage = t.localizedMessage ?: "Unexpected error"
                )
            }
        }
    }

    /**
     * Downscales the bitmap so the longest side is at most [MAX_DIMENSION].
     */
    private fun downscaleBitmap(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longest = maxOf(w, h)
        if (longest <= MAX_DIMENSION) return bitmap

        val scale = MAX_DIMENSION.toFloat() / longest
        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun parseResponse(jsonText: String): Pair<Array<Int?>, Long?>? {
        val grid = Array<Int?>(25) { null }
        var parsedColor: Long? = null
        try {
            val cleanJson = jsonText
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val jsonObj = JSONObject(cleanJson)
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
                    parsedColor = android.graphics.Color.parseColor(colorStr).toLong()
                } catch (_: Exception) { }
            }
            return Pair(grid, parsedColor)
        } catch (_: JSONException) { }
        return null
    }

    fun onManualCreate() {
        createDraftState = newCreateState()
        _state.value = createDraftState
    }

    /**
     * Loads an existing card into the review state for editing.
     */
    fun loadExistingCard(cardId: Long) {
        _state.value = ScanUiState(
            isProcessing = true,
            editingCardId = cardId
        )
        viewModelScope.launch {
            val card = withContext(Dispatchers.IO) { repo.getCard(cardId) } ?: return@launch
            val cells = withContext(Dispatchers.IO) { repo.getCells(cardId) }
            val grid = Array<Int?>(25) { null }
            cells.forEach { cell ->
                val idx = cell.row * BingoRules.GRID_SIZE + cell.col
                if (idx in grid.indices) {
                    grid[idx] = if (cell.isFree) null else cell.value
                }
            }
            _state.value = ScanUiState(
                imageUri = null,
                isProcessing = false,
                errorMessage = null,
                grid = grid,
                cardColor = card.colorArgb,
                cardName = card.name,
                editingCardId = card.id
            )
        }
    }

    fun updateName(name: String) {
        val updated = _state.value.copy(cardName = name)
        _state.value = updated
        updateCreateDraftIfNeeded(updated)
    }

    fun randomizeGrid() {
        val numbers = BingoRules.generateStandardCardNumbers()
        val copy = Array<Int?>(BingoRules.GRID_SIZE * BingoRules.GRID_SIZE) { idx ->
            if (idx == 12) null else numbers[idx]
        }
        val updated = _state.value.copy(grid = copy)
        _state.value = updated
        updateCreateDraftIfNeeded(updated)
    }

    fun updateColor(colorArgb: Long) {
        val updated = _state.value.copy(cardColor = colorArgb)
        _state.value = updated
        updateCreateDraftIfNeeded(updated)
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        val context = appContextProvider()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
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
        val updated = _state.value.copy(grid = copy)
        _state.value = updated
        updateCreateDraftIfNeeded(updated)
    }

    /**
     * Saves — creates new card or updates existing based on editingCardId.
     */
    fun saveCard() {
        val current = _state.value
        val now = System.currentTimeMillis()
        val color = current.cardColor ?: (0xFF000000L or (Random.nextInt(0x00FFFFFF).toLong()))
        val name = current.cardName.ifBlank { "Card ${Random.nextInt(1000, 10_000)}" }

        val cells = ArrayList<CellEntity>(BingoRules.GRID_SIZE * BingoRules.GRID_SIZE)
        val editId = current.editingCardId ?: 0L
        for (r in 0 until BingoRules.GRID_SIZE) {
            for (c in 0 until BingoRules.GRID_SIZE) {
                val idx = r * BingoRules.GRID_SIZE + c
                val isFree = BingoRules.isFreeCell(r, c)
                val v = if (isFree) null else current.grid[idx]
                cells.add(
                    CellEntity(
                        cardId = editId,
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
            if (current.editingCardId != null) {
                val card = CardEntity(
                    id = current.editingCardId,
                    name = name,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                    colorArgb = color
                )
                repo.updateCard(card, cells)
            } else {
                val card = CardEntity(
                    name = name,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                    colorArgb = color
                )
                repo.createCard(card, cells)
                createDraftState = newCreateState()
            }
        }
    }

    private fun appContextProvider() = com.mkz.bingocard.vision.AndroidContextProvider.appContext
}

class ScanViewModelFactory(private val repo: BingoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ScanViewModel(repo) as T
    }
}
