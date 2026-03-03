package com.mkz.bingocard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mkz.bingocard.ui.vm.CardListUiState
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    stateFlow: StateFlow<CardListUiState>,
    onCreateRandom: () -> Unit,
    onCardClick: (Long) -> Unit,
    onCallNumber: (Int) -> Unit
) {
    val state by stateFlow.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Bingo Cards") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCreateRandom) {
                    Text("New Random")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.calledNumberText,
                    onValueChange = state.onCalledNumberTextChanged,
                    label = { Text("Called number (1-75)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button(
                    onClick = {
                        val v = state.calledNumberText.toIntOrNull()
                        if (v != null && v in 1..75) onCallNumber(v)
                    },
                    enabled = state.calledNumberText.toIntOrNull() in 1..75
                ) {
                    Text("Mark")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.cards, key = { it.cardId }) { item ->
                    val base = Color(item.colorArgb.toInt())
                    val container = if (item.isWin) base.copy(alpha = 0.25f) else base.copy(alpha = 0.12f)
                    val border = if (item.isNearWin) base.copy(alpha = 0.9f) else base.copy(alpha = 0.3f)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCardClick(item.cardId) },
                        colors = CardDefaults.cardColors(containerColor = container)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(item.name)
                            Text(
                                text = when {
                                    item.isWin -> "WIN"
                                    item.isNearWin -> "1 away"
                                    else -> ""
                                },
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .background(border.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
