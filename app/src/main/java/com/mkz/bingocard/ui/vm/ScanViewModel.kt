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
import com.mkz.bingocard.vision.BingoCardAiResponseParser
import com.mkz.bingocard.vision.GitHubModelsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

data class ScanUiState(
    val imageUri: Uri? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val grid: Array<Int?> = Array(BingoRules.GRID_SIZE * BingoRules.GRID_SIZE) { null },
    val cardColor: Long? = null,
    val cardName: String = "",
    val editingCardId: Long? = null,
    val analyzedImageUri: Uri? = null,
    val analyzedImageSizeBytes: Long? = null,
    val lastCropBounds: CropBounds? = null
)

data class CropBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

class ScanViewModel(private val repo: BingoRepository) : ViewModel() {

    companion object {
        private const val MAX_DIMENSION = 480
        private val GITHUB_TOKENS: List<String> =
            BuildConfig.GITHUB_TOKENS.split(",").map { it.trim() }.filter { it.isNotBlank() }
        private val tokenCursor = AtomicInteger(0)
    }

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state

    private var createDraftState: ScanUiState = newCreateState()
    private var editBaselineState: ScanUiState? = null
    private var isNameUserEdited: Boolean = false

    private fun newCreateState(): ScanUiState = ScanUiState(
        imageUri = null,
        isProcessing = false,
        errorMessage = null,
        grid = Array(25) { null },
        cardColor = 0xFF42A5F5,
        cardName = "",
        editingCardId = null,
        analyzedImageUri = null,
        analyzedImageSizeBytes = null,
        lastCropBounds = null
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

    /**
     * Returns all configured tokens in a round-robin order so every new AI
     * request starts with a different key while still falling back through all keys.
     */
    private fun tokensForCurrentRequest(): List<String> {
        if (GITHUB_TOKENS.isEmpty()) return emptyList()
        val start = Math.floorMod(tokenCursor.getAndIncrement(), GITHUB_TOKENS.size)
        return List(GITHUB_TOKENS.size) { idx ->
            GITHUB_TOKENS[(start + idx) % GITHUB_TOKENS.size]
        }
    }

    fun startCreateDraft() {
        editBaselineState = null
        isNameUserEdited = createDraftState.cardName.isNotBlank()
        _state.value = createDraftState.copy(
            imageUri = null,
            isProcessing = false,
            errorMessage = null,
            editingCardId = null
        )
        autoPopulateNameFromGridIfEligible(_state.value.grid)
    }

    /** Stores the image URI for the crop screen — no processing yet. */
    fun setImage(uri: Uri) {
        val current = _state.value
        val next = if (current.editingCardId == null) {
            current.copy(
                imageUri = uri,
                isProcessing = false,
                errorMessage = null,
                editingCardId = null,
                analyzedImageUri = null,
                analyzedImageSizeBytes = null
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
        val cropBounds = CropBounds(left, top, right, bottom)
        _state.value = _state.value.copy(
            isProcessing = true,
            errorMessage = null,
            lastCropBounds = cropBounds
        )

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

                val analyzedImage = persistAnalyzedPreview(bitmap)
                _state.value = _state.value.copy(
                    analyzedImageUri = analyzedImage.first,
                    analyzedImageSizeBytes = analyzedImage.second,
                    isProcessing = true,
                    errorMessage = null
                )

                val prompt = BingoCardAiResponseParser.prompt

                var lastError: Throwable? = null
                var mappedGrid: Array<Int?>? = null
                var mappedColor: Long? = null

                for (token in tokensForCurrentRequest()) {
                    try {
                        val respText = GitHubModelsClient.analyzeImage(
                            token = token,
                            bitmap = bitmap,
                            prompt = prompt
                        ).trim()

                        val result = BingoCardAiResponseParser.parse(respText)
                        mappedGrid = result.grid
                        mappedColor = result.colorArgb
                        break
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
                    autoPopulateNameFromGridIfEligible(mappedGrid)
                    updateCreateDraftIfNeeded(_state.value)
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

    private fun persistAnalyzedPreview(bitmap: Bitmap): Pair<Uri, Long> {
        val context = appContextProvider()
        val file = File(context.cacheDir, "analyzed_preview.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            out.flush()
        }
        return Pair(Uri.fromFile(file), file.length())
    }

    fun onManualCreate() {
        editBaselineState = null
        isNameUserEdited = false
        createDraftState = newCreateState()
        _state.value = createDraftState
    }

    fun resetInputs() {
        val current = _state.value
        if (current.isProcessing) return

        if (current.editingCardId != null) {
            val baseline = editBaselineState ?: return
            _state.value = baseline.copy(
                isProcessing = false,
                errorMessage = null,
                analyzedImageUri = null,
                analyzedImageSizeBytes = null,
                imageUri = current.imageUri,
                lastCropBounds = current.lastCropBounds
            )
            isNameUserEdited = true
            return
        }

        val resetState = current.copy(
            isProcessing = false,
            errorMessage = null,
            grid = Array(BingoRules.GRID_SIZE * BingoRules.GRID_SIZE) { null },
            cardColor = 0xFF42A5F5,
            cardName = "",
            analyzedImageUri = null,
            analyzedImageSizeBytes = null
        )
        isNameUserEdited = false
        _state.value = resetState
        updateCreateDraftIfNeeded(resetState)
    }

    fun retryLastScan() {
        val current = _state.value
        val cropBounds = current.lastCropBounds ?: return
        if (current.imageUri == null || current.isProcessing) return
        analyzeCroppedImage(
            left = cropBounds.left,
            top = cropBounds.top,
            right = cropBounds.right,
            bottom = cropBounds.bottom
        )
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
            editBaselineState = _state.value.copy()
            isNameUserEdited = true
        }
    }

    fun updateName(name: String) {
        if (_state.value.editingCardId != null) {
            val updated = _state.value.copy(cardName = name)
            _state.value = updated
            updateCreateDraftIfNeeded(updated)
            return
        }

        if (name.isBlank()) {
            isNameUserEdited = false
            autoPopulateNameFromGridIfEligible(_state.value.grid)
            return
        }

        isNameUserEdited = true
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
        autoPopulateNameFromGridIfEligible(copy)
        updateCreateDraftIfNeeded(_state.value)
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
        autoPopulateNameFromGridIfEligible(copy)
        updateCreateDraftIfNeeded(_state.value)
    }

    private fun autoPopulateNameFromGridIfEligible(grid: Array<Int?>) {
        val current = _state.value
        if (current.editingCardId != null) return
        if (isNameUserEdited) return

        val suggested = buildAutoCardName(grid) ?: return
        if (current.cardName == suggested) return

        val updated = current.copy(cardName = suggested)
        _state.value = updated
        updateCreateDraftIfNeeded(updated)
    }

    private fun buildAutoCardName(grid: Array<Int?>): String? {
        val bFirst = firstColumnValue(grid, 0)
        val iFirst = firstColumnValue(grid, 1)
        if (bFirst == null || iFirst == null) return null
        return "Card $bFirst-$iFirst"
    }

    private fun firstColumnValue(grid: Array<Int?>, col: Int): Int? {
        for (row in 0 until BingoRules.GRID_SIZE) {
            val idx = row * BingoRules.GRID_SIZE + col
            val value = grid.getOrNull(idx)
            if (value != null) return value
        }
        return null
    }

    /**
     * Saves — creates new card or updates existing based on editingCardId.
     */
    fun saveCard() {
        val current = _state.value
        val now = System.currentTimeMillis()
        val color = current.cardColor ?: (0xFF000000L or (Random.nextInt(0x00FFFFFF).toLong()))
        val name = current.cardName.ifBlank {
            buildAutoCardName(current.grid) ?: "Card ${Random.nextInt(1000, 10_000)}"
        }

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
