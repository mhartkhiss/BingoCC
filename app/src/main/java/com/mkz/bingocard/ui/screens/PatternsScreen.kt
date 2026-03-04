package com.mkz.bingocard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mkz.bingocard.domain.BingoRules
import com.mkz.bingocard.domain.PatternEngine
import com.mkz.bingocard.domain.PatternMask
import com.mkz.bingocard.data.db.entities.PatternEntity
import com.mkz.bingocard.ui.vm.PatternsUiState
import kotlinx.coroutines.flow.StateFlow

// Color palette for different pattern instances
private val patternColors = listOf(
    Color(0xFF4CAF50), // Green
    Color(0xFF2196F3), // Blue
    Color(0xFFFF9800), // Orange
    Color(0xFF9C27B0), // Purple
    Color(0xFFF44336), // Red
    Color(0xFF00BCD4), // Cyan
    Color(0xFFFFEB3B), // Yellow
    Color(0xFF795548), // Brown
    Color(0xFF607D8B), // Blue Grey
    Color(0xFFE91E63), // Pink
)



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
    onBack: () -> Unit
) {
    val state by stateFlow.collectAsState()
    var showEditorSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patterns") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { showEditorSheet = true }
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Editor Actions")
            }
        }
    ) { innerPadding ->
        val config = LocalConfiguration.current
        val isNarrow = config.screenWidthDp < 600
        val cellSizeDp: Dp = if (isNarrow) {
            val total = config.screenWidthDp - 48 - 24
            (total / BingoRules.GRID_SIZE).coerceAtLeast(32).dp
        } else 44.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Grid", style = MaterialTheme.typography.titleSmall)
                    
                    val specialPattern = state.patterns.find { it.id == state.selectedId && it.isPreset && it.mask == 0L }
                    val specialMasks = specialPattern?.let {
                        when {
                            it.name.startsWith("Small Square", ignoreCase = true) -> generateAll2x2Masks()
                            it.name.startsWith("Slant", ignoreCase = true) -> generateSlantingMasksMin3()
                            else -> emptyList()
                        }
                    } ?: emptyList()
                    
                    for (r in 0 until BingoRules.GRID_SIZE) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (c in 0 until BingoRules.GRID_SIZE) {
                                val idx = r * BingoRules.GRID_SIZE + c
                                val selected = state.mask and (1L shl idx) != 0L
                                val isFree = BingoRules.isFreeCell(r, c)
                                
                                val specialIndex = specialMasks.indexOfFirst { (it and (1L shl idx)) != 0L }
                                
                                val bg = when {
                                    isFree -> Color.Gray.copy(alpha = 0.25f)
                                    specialIndex != -1 -> patternColors[specialIndex % patternColors.size].copy(alpha = 0.45f)
                                    selected -> Color(0xFF4CAF50).copy(alpha = 0.45f)
                                    else -> Color.Black.copy(alpha = 0.08f)
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .size(cellSizeDp)
                                        .background(bg, RoundedCornerShape(8.dp))
                                        .clickable(enabled = !state.isPreset && !isFree) { onToggleCell(r, c) },
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(if (isFree) "FREE" else "", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val regularPatterns = state.patterns
                if (regularPatterns.isNotEmpty()) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Saved", style = MaterialTheme.typography.titleSmall)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                for (p in regularPatterns) {
                                    val isSelected = state.selectedId == p.id
                                    val bg = if (isSelected) Color.Black.copy(alpha = 0.06f) else Color.Transparent
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(bg, RoundedCornerShape(10.dp))
                                            .clickable { onSelect(p.id) }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = p.name, 
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (p.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = if (p.isPreset) "Preset" else "Custom",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (p.isActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                        Switch(
                                            checked = p.isActive,
                                            onCheckedChange = { onToggleActive(p) }
                                        )
                                    }
                                    Spacer(modifier = Modifier.padding(bottom = 6.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (showEditorSheet) {
                ModalBottomSheet(onDismissRequest = { showEditorSheet = false }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Pattern Actions", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.name,
                            onValueChange = onNameChanged,
                            enabled = !state.isPreset,
                            label = { Text("Pattern Name") },
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { onCreate(); showEditorSheet = false }
                            ) { Text("New") }
                            
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { onSave(); showEditorSheet = false },
                                enabled = !state.isPreset
                            ) { Text("Save") }
                            
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { showDeleteDialog = true },
                                enabled = !state.isPreset && state.selectedId != null,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) { Text("Delete") }
                        }
                    }
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Pattern") },
                    text = { Text("Are you sure you want to delete this custom pattern?") },
                    confirmButton = {
                        TextButton(onClick = { onDelete(); showDeleteDialog = false; showEditorSheet = false }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
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
