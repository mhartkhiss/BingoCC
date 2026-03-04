package com.mkz.bingocard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mkz.bingocard.domain.BingoRules
import com.mkz.bingocard.ui.vm.ScanUiState
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReviewScreen(
    stateFlow: StateFlow<ScanUiState>,
    onCellChanged: (row: Int, col: Int, value: Int?) -> Unit,
    onColorChanged: (Long) -> Unit,
    onRandomize: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review") },
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
            Text("Review and edit", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colors.forEach { cCode ->
                    val isSelected = state.cardColor == cCode
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(color = Color(cCode), shape = CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = CircleShape
                            )
                            .clickable { onColorChanged(cCode) }
                    )
                }
            }

            if (state.isProcessing) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Analyzing bingo card...")
                }
            } else if (state.errorMessage != null) {
                Text("Error: ${state.errorMessage}", color = MaterialTheme.colorScheme.error)
            } else {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (r in 0 until BingoRules.GRID_SIZE) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (c in 0 until BingoRules.GRID_SIZE) {
                                val isFree = BingoRules.isFreeCell(r, c)
                                val idx = r * BingoRules.GRID_SIZE + c
                                val text = if (isFree) "FREE" else (state.grid[idx]?.toString() ?: "")
                                OutlinedTextField(
                                    modifier = Modifier.weight(1f),
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
            } // closes else block

            Spacer(modifier = Modifier.weight(1f))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRandomize,
                enabled = !state.isProcessing
            ) {
                Text("Randomize")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSave,
                enabled = !state.isProcessing
            ) {
                Text("Save")
            }
        }
    }
}
