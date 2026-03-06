package com.mkz.bingocard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mkz.bingocard.domain.BingoRules
import com.mkz.bingocard.ui.vm.ScanUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReviewScreen(
    stateFlow: StateFlow<ScanUiState>,
    onCellChanged: (row: Int, col: Int, value: Int?) -> Unit,
    onColorChanged: (Long) -> Unit,
    onNameChanged: (String) -> Unit,
    onRandomize: () -> Unit,
    onCameraScan: () -> Unit,
    onPickFromGallery: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val state by stateFlow.collectAsState()

    val colors = listOf<Long>(
        0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB,
        0xFF1E88E5, 0xFF039BE5, 0xFF00ACC1, 0xFF00897B, 0xFF43A047,
        0xFF7CB342, 0xFFC0CA33, 0xFFFDD835, 0xFFFFB300, 0xFFFB8C00,
        0xFFF4511E, 0xFF6D4C41, 0xFF757575, 0xFF546E7A
    )

    val selectedColor = state.cardColor?.let { Color(it.toInt()) } ?: MaterialTheme.colorScheme.primary
    val isEditing = state.editingCardId != null
    val hasPopulatedGrid = state.grid.any { it != null }
    var previewDismissed by remember(state.analyzedImageUri, hasPopulatedGrid) { mutableStateOf(false) }
    val showAnalyzedResultLayout = state.analyzedImageUri != null && hasPopulatedGrid && !previewDismissed
    val paletteScroll = rememberScrollState()
    val scrollScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Card" else "Add Card") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card name input
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.cardName,
                onValueChange = onNameChanged,
                label = { Text("Card Name") },
                placeholder = { Text("e.g. Card 9-21") },
                singleLine = true
            )

            // Color palette
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 28.dp)
                        .horizontalScroll(paletteScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { cCode ->
                        val isSelected = state.cardColor == cCode
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 38.dp else 30.dp)
                                .background(color = Color(cCode.toInt()), shape = CircleShape)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                    } else Modifier
                                )
                                .clickable { onColorChanged(cCode) }
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "More colors",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (paletteScroll.value < paletteScroll.maxValue) 0.75f else 0.35f),
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.CenterEnd)
                        .clickable(enabled = paletteScroll.value < paletteScroll.maxValue) {
                            scrollScope.launch {
                                val step = 180
                                val target = (paletteScroll.value + step).coerceAtMost(paletteScroll.maxValue)
                                paletteScroll.animateScrollTo(target)
                            }
                        }
                        .padding(end = 2.dp)
                )
            }

            if (state.isProcessing) {
                val loadingPreviewUri = state.analyzedImageUri
                val loadingPreviewSize = state.analyzedImageSizeBytes
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Analyzing bingo card...")
                    if (loadingPreviewUri != null && loadingPreviewSize != null) {
                        AnalyzedImagePreview(
                            imageUri = loadingPreviewUri,
                            imageSizeBytes = loadingPreviewSize
                        )
                    }
                }
            } else if (state.errorMessage != null) {
                Text("Error: ${state.errorMessage}", color = MaterialTheme.colorScheme.error)
            } else {
                // Card with colored border matching selected color
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 3.dp,
                            color = selectedColor,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = selectedColor.copy(alpha = 0.06f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (showAnalyzedResultLayout) 8.dp else 12.dp),
                        verticalArrangement = Arrangement.spacedBy(if (showAnalyzedResultLayout) 6.dp else 8.dp)
                    ) {
                        // BINGO header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf("B", "I", "N", "G", "O").forEach { letter ->
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = selectedColor
                                )
                            }
                        }

                        for (r in 0 until BingoRules.GRID_SIZE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(if (showAnalyzedResultLayout) 6.dp else 8.dp)
                            ) {
                                for (c in 0 until BingoRules.GRID_SIZE) {
                                    val isFree = BingoRules.isFreeCell(r, c)
                                    val idx = r * BingoRules.GRID_SIZE + c
                                    val text = if (isFree) "FREE" else (state.grid[idx]?.toString() ?: "")
                                    OutlinedTextField(
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (showAnalyzedResultLayout) Modifier.height(54.dp)
                                                else Modifier.height(56.dp)
                                            ),
                                        value = text,
                                        onValueChange = { new ->
                                            if (isFree) return@OutlinedTextField
                                            val parsed = new.toIntOrNull()
                                            onCellChanged(r, c, parsed)
                                        },
                                        enabled = !isFree,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isEditing && !showAnalyzedResultLayout) {
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRandomize,
                        enabled = !state.isProcessing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Randomize",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Randomize Grid")
                    }
                }

                val analyzedPreviewUri = state.analyzedImageUri
                val analyzedPreviewSize = state.analyzedImageSizeBytes
                if (showAnalyzedResultLayout && analyzedPreviewUri != null && analyzedPreviewSize != null) {
                    AnalyzedImagePreview(
                        imageUri = analyzedPreviewUri,
                        imageSizeBytes = analyzedPreviewSize,
                        expanded = true,
                        onClose = { previewDismissed = true }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!isEditing && !showAnalyzedResultLayout) {
                Text(
                    text = "Tip: You can use Camera or Gallery below to scan a card directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onCameraScan,
                        enabled = !state.isProcessing
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera Scan",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Camera")
                    }

                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onPickFromGallery,
                        enabled = !state.isProcessing
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Pick from Gallery",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Gallery")
                    }
                }
            }

            if (showAnalyzedResultLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { previewDismissed = true },
                        enabled = !state.isProcessing
                    ) {
                        Text("Retry")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onSave,
                        enabled = !state.isProcessing
                    ) {
                        Text(if (isEditing) "Update" else "Save")
                    }
                }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSave,
                    enabled = !state.isProcessing
                ) {
                    Text(if (isEditing) "Update" else "Save")
                }
            }
        }
    }
}

@Composable
private fun AnalyzedImagePreview(
    imageUri: android.net.Uri,
    imageSizeBytes: Long,
    expanded: Boolean = false,
    onClose: (() -> Unit)? = null
) {
    val sizeText = if (imageSizeBytes >= 1024 * 1024) {
        String.format("%.2f MB", imageSizeBytes / (1024f * 1024f))
    } else {
        String.format("%.1f KB", imageSizeBytes / 1024f)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = "Image Size · $sizeText",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close preview"
                        )
                    }
                }
            }
            AsyncImage(
                model = imageUri,
                contentDescription = "Analyzed image preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (expanded) 170.dp else 120.dp)
            )
        }
    }
}
