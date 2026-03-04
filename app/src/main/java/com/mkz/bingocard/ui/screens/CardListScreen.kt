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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mkz.bingocard.ui.vm.CardListUiState
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    stateFlow: StateFlow<CardListUiState>,
    onCardClick: (Long) -> Unit,
    onCreateRandom: () -> Unit,
    onGalleryClick: (android.net.Uri) -> Unit,
    onOpenDrawer: () -> Unit,
    onCallNumber: (Int) -> Unit,
    onDeleteCard: (Long) -> Unit,
    onResetAll: () -> Unit
) {
    val state by stateFlow.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<Long?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var calledNumberText by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    val scannerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultData = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            resultData?.pages?.firstOrNull()?.imageUri?.let { uri ->
                showAddSheet = false
                onGalleryClick(uri)
            }
        }
    }

    val launchScanner = {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        val scanner = GmsDocumentScanning.getClient(options)
        activity?.let {
            scanner.getStartScanIntent(it).addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
        }
    }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            showAddSheet = false
            onGalleryClick(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bingo Cards") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    OutlinedTextField(
                        modifier = Modifier.width(150.dp).padding(horizontal = 8.dp),
                        value = calledNumberText,
                        onValueChange = { calledNumberText = it },
                        label = { Text("Call #") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    
                    val v = calledNumberText.toIntOrNull()
                    Button(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = { 
                            if (v != null && v in 1..75) {
                                onCallNumber(v)
                                calledNumberText = ""
                            }
                        },
                        enabled = v != null && v in 1..75
                    ) {
                        Text("Mark")
                    }

                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset All Marks")
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddSheet = true },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Card")
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.cards, key = { it.cardId }) { item ->
                    val base = Color(item.colorArgb.toInt())
                    val container = if (item.isWin) base.copy(alpha = 0.22f) else base.copy(alpha = 0.10f)
                    val badge = if (item.isNearWin) base.copy(alpha = 0.20f) else base.copy(alpha = 0.14f)

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                cardToDelete = item.cardId
                            }
                            false // Don't actually dismiss yet, wait for dialog
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color = MaterialTheme.colorScheme.error
                            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, shape = CardDefaults.elevatedShape)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onError)
                                }
                            }
                        }
                    ) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCardClick(item.cardId) },
                            colors = CardDefaults.elevatedCardColors(containerColor = container),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(text = item.name, style = MaterialTheme.typography.titleLarge)
                                val status = when {
                                    item.isWin -> "WIN"
                                    item.isNearWin -> "1 away"
                                    else -> null
                                }
                                if (status != null) {
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .background(badge, shape = MaterialTheme.shapes.small)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showAddSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAddSheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Add card", style = MaterialTheme.typography.titleMedium)

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showAddSheet = false
                                onCreateRandom()
                            }
                        ) {
                            Text("New Random")
                        }
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { launchScanner() }
                        ) {
                            Text("Camera ")
                        }
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { galleryLauncher.launch("image/*") }
                        ) {
                            Text("Gallery")
                        }

                        Spacer(modifier = Modifier.padding(bottom = 12.dp))
                    }
                }
            }
            if (cardToDelete != null) {
                AlertDialog(
                    onDismissRequest = { cardToDelete = null },
                    title = { Text("Delete Card") },
                    text = { Text("Are you sure you want to delete this card?") },
                    confirmButton = {
                        TextButton(onClick = { 
                            onDeleteCard(cardToDelete!!)
                            cardToDelete = null 
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { cardToDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            if (showResetConfirm) {
                AlertDialog(
                    onDismissRequest = { showResetConfirm = false },
                    title = { Text("Reset All Marks") },
                    text = { Text("Are you sure you want to completely clear the marks on all your Bingo cards?") },
                    confirmButton = {
                        TextButton(onClick = { 
                            onResetAll()
                            showResetConfirm = false 
                        }) {
                            Text("Reset", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
