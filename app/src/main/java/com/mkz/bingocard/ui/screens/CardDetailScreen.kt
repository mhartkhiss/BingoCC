package com.mkz.bingocard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mkz.bingocard.domain.BingoRules
import com.mkz.bingocard.ui.vm.CardDetailUiState
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    stateFlow: StateFlow<CardDetailUiState>,
    onToggleCell: (row: Int, col: Int, isMarked: Boolean) -> Unit,
    onBack: () -> Unit
) {
    val state by stateFlow.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<")
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
            val bg = Color(state.colorArgb.toInt()).copy(alpha = 0.08f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in 0 until BingoRules.GRID_SIZE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0 until BingoRules.GRID_SIZE) {
                            val cell = state.cells.firstOrNull { it.row == row && it.col == col }
                            val isFree = BingoRules.isFreeCell(row, col)
                            val text = if (isFree) "FREE" else cell?.value?.toString() ?: ""
                            val marked = cell?.isMarked == true || isFree
                            val base = Color(state.colorArgb.toInt())
                            val cellBg = if (marked) base.copy(alpha = 0.35f) else base.copy(alpha = 0.12f)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .size(56.dp)
                                    .background(cellBg)
                                    .clickable(enabled = !isFree) {
                                        onToggleCell(row, col, !marked)
                                    },
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = text, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
