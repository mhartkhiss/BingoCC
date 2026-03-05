package com.mkz.bingocard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mkz.bingocard.domain.BingoRules
import com.mkz.bingocard.domain.PatternMask
import com.mkz.bingocard.data.db.entities.PatternEntity
import com.mkz.bingocard.ui.components.AppActionDialog
import com.mkz.bingocard.ui.components.AppDialogType
import com.mkz.bingocard.ui.vm.PatternsUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay

private enum class PatternFilterTab {
    PRESETS,
    CUSTOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternsScreen(
    stateFlow: StateFlow<PatternsUiState>,
    onSelect: (Long) -> Unit,
    onToggleCell: (row: Int, col: Int) -> Unit,
    onNameChanged: (String) -> Unit,
    onCreate: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (PatternEntity) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val state by stateFlow.collectAsState()
    var showPatternEditorModal by remember { mutableStateOf(false) }
    var isEditingMode by remember { mutableStateOf(false) }
    var showCustomActionsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var customActionsPatternId by remember { mutableStateOf<Long?>(null) }
    var listFilterTab by remember { mutableStateOf(PatternFilterTab.PRESETS) }
    var previousSelectionBeforeCreate by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patterns") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
    ) { innerPadding ->
        val config = LocalConfiguration.current
        val isNarrow = config.screenWidthDp < 600
        val screenPadding = if (isNarrow) 12.dp else 16.dp
        val cardPadding = if (isNarrow) 12.dp else 16.dp
        val sectionSpacing = if (isNarrow) 8.dp else 12.dp
        val rowSpacing = if (isNarrow) 4.dp else 6.dp
        val patternRowVerticalPadding = if (isNarrow) 8.dp else 10.dp
        val previewCellSizeDp: Dp = if (isNarrow) {
            val total = config.screenWidthDp - 48 - 24
            (total / BingoRules.GRID_SIZE).coerceAtLeast(24).dp
        } else 44.dp
        val previewCellSizeCompact: Dp = if (isNarrow) 24.dp else 30.dp
        val selectedPattern = state.patterns.find { it.id == state.selectedId }

        fun cancelCreateModal() {
            showPatternEditorModal = false
            val previousId = previousSelectionBeforeCreate
            if (previousId != null) {
                onSelect(previousId)
            }
            previousSelectionBeforeCreate = null
            isEditingMode = false
        }

        val filteredPatterns = remember(state.patterns, listFilterTab) {
            when (listFilterTab) {
                PatternFilterTab.PRESETS -> state.patterns.filter { it.isPreset }
                PatternFilterTab.CUSTOM -> state.patterns.filter { !it.isPreset }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(screenPadding),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(cardPadding),
                    verticalArrangement = Arrangement.spacedBy(if (isNarrow) 8.dp else 10.dp)
                ) {
                    Text("Grid Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = selectedPattern?.name ?: "Select a pattern from the list",
                        style = if (isNarrow) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "Pattern",
                                style = if (isNarrow) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Gray.copy(alpha = 0.20f)
                        ) {
                            Text(
                                text = "FREE",
                                style = if (isNarrow) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    val specialPattern = state.patterns.find { it.id == state.selectedId && it.isPreset && it.mask == 0L }
                    val specialMasks = specialPattern?.let {
                        when {
                            it.name.startsWith("Small Square", ignoreCase = true) -> generateAll2x2Masks()
                            it.name.startsWith("Slant", ignoreCase = true) -> generateSlantingMasksMin3()
                            else -> emptyList()
                        }
                    } ?: emptyList()

                    var specialMaskIndex by remember(specialPattern?.id, specialMasks.size) { mutableIntStateOf(0) }
                    LaunchedEffect(specialPattern?.id, specialMasks.size) {
                        specialMaskIndex = 0
                        if (specialMasks.isNotEmpty()) {
                            while (true) {
                                delay(650)
                                specialMaskIndex = (specialMaskIndex + 1) % specialMasks.size
                            }
                        }
                    }
                    val animatedSpecialMask = specialMasks.getOrNull(specialMaskIndex) ?: 0L
                    
                    for (r in 0 until BingoRules.GRID_SIZE) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rowSpacing)) {
                            for (c in 0 until BingoRules.GRID_SIZE) {
                                val idx = r * BingoRules.GRID_SIZE + c
                                val selected = state.mask and (1L shl idx) != 0L
                                val isFree = BingoRules.isFreeCell(r, c)
                                val isAnimatedSpecialCell = (animatedSpecialMask and (1L shl idx)) != 0L
                                
                                val presetGreen = Color(0xFF4CAF50)
                                val bg = when {
                                    isFree -> Color.Gray.copy(alpha = 0.25f)
                                    isAnimatedSpecialCell -> presetGreen.copy(alpha = 0.45f)
                                    selected -> presetGreen.copy(alpha = 0.45f)
                                    else -> Color.Black.copy(alpha = 0.08f)
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .size(previewCellSizeDp.coerceAtMost(previewCellSizeCompact))
                                        .background(bg, RoundedCornerShape(8.dp))
                                        .clickable(enabled = false) { },
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(if (isFree) "F" else "", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            if (filteredPatterns.isNotEmpty()) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(cardPadding),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Pattern List",
                                style = if (isNarrow) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = {
                                    previousSelectionBeforeCreate = state.selectedId
                                    onCreate()
                                    isEditingMode = false
                                    showPatternEditorModal = true
                                }
                            ) {
                                Text("New Pattern")
                            }
                        }
                        TabRow(selectedTabIndex = listFilterTab.ordinal) {
                            Tab(
                                selected = listFilterTab == PatternFilterTab.PRESETS,
                                onClick = { listFilterTab = PatternFilterTab.PRESETS },
                                text = { Text("Presets") }
                            )
                            Tab(
                                selected = listFilterTab == PatternFilterTab.CUSTOM,
                                onClick = { listFilterTab = PatternFilterTab.CUSTOM },
                                text = { Text("Custom") }
                            )
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(if (isNarrow) 6.dp else 8.dp)
                        ) {
                            items(filteredPatterns, key = { it.id }) { p ->
                                val isSelected = state.selectedId == p.id
                                val bg = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bg, RoundedCornerShape(12.dp))
                                        .clickable {
                                            onSelect(p.id)
                                            if (!p.isPreset) {
                                                customActionsPatternId = p.id
                                                showCustomActionsDialog = true
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = patternRowVerticalPadding),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = p.name,
                                            style = if (isNarrow) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (p.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (p.isPreset)
                                                MaterialTheme.colorScheme.secondaryContainer
                                            else
                                                MaterialTheme.colorScheme.tertiaryContainer
                                        ) {
                                            Text(
                                                text = if (p.isPreset) "Preset" else "Custom",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (p.isPreset)
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = p.isActive,
                                        onCheckedChange = { onToggleActive(p) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(cardPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (listFilterTab == PatternFilterTab.PRESETS) "No preset patterns." else "No custom patterns yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showPatternEditorModal) {
                Dialog(
                    onDismissRequest = {
                        if (isEditingMode) {
                            showPatternEditorModal = false
                        } else {
                            cancelCreateModal()
                        }
                    },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(cardPadding),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isEditingMode) "Edit Pattern" else "New Pattern",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = state.name,
                                onValueChange = onNameChanged,
                                label = { Text("Pattern Name") },
                                singleLine = true
                            )

                            val modalCellSizeDp: Dp = if (isNarrow) {
                                val total = config.screenWidthDp - 64 - 24
                                (total / BingoRules.GRID_SIZE).coerceAtLeast(36).dp
                            } else {
                                48.dp
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(rowSpacing)) {
                                for (r in 0 until BingoRules.GRID_SIZE) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(rowSpacing)
                                    ) {
                                        for (c in 0 until BingoRules.GRID_SIZE) {
                                            val idx = r * BingoRules.GRID_SIZE + c
                                            val selected = state.mask and (1L shl idx) != 0L
                                            val isFree = BingoRules.isFreeCell(r, c)
                                            val bg = when {
                                                isFree -> Color.Gray.copy(alpha = 0.25f)
                                                selected -> Color(0xFF4CAF50).copy(alpha = 0.45f)
                                                else -> Color.Black.copy(alpha = 0.08f)
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .size(modalCellSizeDp)
                                                    .background(bg, RoundedCornerShape(8.dp))
                                                    .clickable(enabled = !isFree) { onToggleCell(r, c) },
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = if (isFree) "FREE" else "",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (isEditingMode) {
                                            showPatternEditorModal = false
                                        } else {
                                            cancelCreateModal()
                                        }
                                    }
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        onSave()
                                        showPatternEditorModal = false
                                        previousSelectionBeforeCreate = null
                                        isEditingMode = false
                                    }
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }

            val customActionPattern = state.patterns.firstOrNull { it.id == customActionsPatternId && !it.isPreset }
            if (showCustomActionsDialog && customActionPattern != null) {
                Dialog(
                    onDismissRequest = { showCustomActionsDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = true)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(customActionPattern.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Choose what to do with this pattern",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                onClick = {
                                    showCustomActionsDialog = false
                                    isEditingMode = true
                                    showPatternEditorModal = true
                                }
                            ) {
                                Text("Edit Pattern")
                            }

                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                onClick = {
                                    showCustomActionsDialog = false
                                    showDeleteConfirmDialog = true
                                }
                            ) {
                                Text("Delete Pattern")
                            }
                        }
                    }
                }
            }

            if (showDeleteConfirmDialog && customActionPattern != null) {
                AppActionDialog(
                    title = "Delete Pattern",
                    message = "Are you sure you want to delete this custom pattern?",
                    confirmLabel = "Delete",
                    type = AppDialogType.DESTRUCTIVE,
                    onConfirm = {
                        onDelete()
                        showDeleteConfirmDialog = false
                        customActionsPatternId = null
                    },
                    onDismiss = { showDeleteConfirmDialog = false }
                )
            }
        }
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
