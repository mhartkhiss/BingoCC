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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
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
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val state by stateFlow.collectAsState()

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
                onClick = onSave,
                enabled = !state.isProcessing
            ) {
                Text("Save")
            }
        }
    }
}
