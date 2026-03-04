package com.mkz.bingocard.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewModule
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mkz.bingocard.domain.BingoRules
import com.mkz.bingocard.ui.vm.CardListItemUi
import com.mkz.bingocard.ui.vm.CardListUiState
import kotlin.math.roundToInt
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    stateFlow: StateFlow<CardListUiState>,
    onCardClick: (Long) -> Unit,
    onGalleryClick: (android.net.Uri) -> Unit,
    onManualCreate: () -> Unit,
    onOpenDrawer: () -> Unit,
    onCallNumber: (Int) -> Unit,
    onDeleteCard: (Long) -> Unit,
    onResetAll: () -> Unit
) {
    val state by stateFlow.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<Long?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showAlreadyCalledDialog by remember { mutableStateOf(false) }
    var alreadyCalledNumber by remember { mutableStateOf<Int?>(null) }
    var calledNumberText by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    var historyFilterText by remember { mutableStateOf("") }
    var historyShowRemaining by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(CardListViewMode.List) }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    
    var lastCards by remember { mutableStateOf(state.cards) }

    LaunchedEffect(state.cards) {
        val oldWins = lastCards.filter { it.isWin }.map { it.cardId }.toSet()
        val newWins = state.cards.filter { it.isWin }.map { it.cardId }
        val justWon = newWins.firstOrNull { it !in oldWins }
        
        if (justWon != null) {
            val index = state.cards.indexOfFirst { it.cardId == justWon }
            if (index >= 0) {
                if (viewMode == CardListViewMode.List) {
                    listState.animateScrollToItem(index)
                } else {
                    gridState.animateScrollToItem(index)
                }
            }
        }
        lastCards = state.cards
    }

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
                ,
                actions = {
                    IconButton(
                        onClick = {
                            viewMode = when (viewMode) {
                                CardListViewMode.List -> CardListViewMode.Grid
                                CardListViewMode.Grid -> CardListViewMode.List
                            }
                        }
                    ) {
                        val icon = when (viewMode) {
                            CardListViewMode.List -> Icons.Default.ViewModule
                            CardListViewMode.Grid -> Icons.AutoMirrored.Filled.ViewList
                        }
                        Icon(imageVector = icon, contentDescription = "Toggle view")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { showHistorySheet = true }) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "Called Numbers History")
                    }

                    val tryMarkCalledNumber = {
                        val v = calledNumberText.toIntOrNull()
                        if (v != null && v in 1..75) {
                            if (v in state.calledNumbers) {
                                alreadyCalledNumber = v
                                showAlreadyCalledDialog = true
                            } else {
                                onCallNumber(v)
                                calledNumberText = ""
                                keyboardController?.hide()
                            }
                        }
                    }

                    OutlinedTextField(
                        modifier = Modifier.width(150.dp).padding(horizontal = 8.dp),
                        value = calledNumberText,
                        onValueChange = { calledNumberText = it },
                        label = { Text("Call #") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { tryMarkCalledNumber() }
                        ),
                        singleLine = true
                    )
                    
                    val v = calledNumberText.toIntOrNull()
                    Button(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = { 
                            tryMarkCalledNumber()
                        },
                        enabled = v != null && v in 1..75
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Mark")
                    }
                },
                floatingActionButton = {
                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingActionButton(
                            onClick = { showResetConfirm = true },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset All Marks")
                        }

                        FloatingActionButton(
                            onClick = { showAddSheet = true },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Card")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (showAlreadyCalledDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAlreadyCalledDialog = false
                    alreadyCalledNumber = null
                },
                title = { Text("Already called") },
                text = {
                    Text("${alreadyCalledNumber ?: "That number"} is already in the called history.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showAlreadyCalledDialog = false
                            alreadyCalledNumber = null
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (viewMode == CardListViewMode.List) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.cards, key = { it.cardId }) { item ->
                        val base = Color(item.colorArgb.toInt())
                        val container = if (item.isWin) base.copy(alpha = 0.22f) else base.copy(alpha = 0.10f)
                        val badgeColor = when {
                            item.isWin -> Color(0xFF2E7D32)
                            item.isNearWin -> Color(0xFFF57C00)
                            else -> Color.Transparent
                        }
                        val borderColor = base.copy(alpha = 0.90f)

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    cardToDelete = item.cardId
                                }
                                false
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
                            Box {
                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(CardDefaults.elevatedShape)
                                        .background(Color.Transparent)
                                        .clickable { onCardClick(item.cardId) },
                                    colors = CardDefaults.elevatedCardColors(containerColor = container),
                                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Transparent)
                                                .padding(20.dp)
                                        ) {
                                            if (item.isWin) {
                                                SlidingWinnerBanner()
                                            } else {
                                                Text(text = item.name, style = MaterialTheme.typography.titleLarge)
                                            }
                                            val status = when {
                                                item.isWin -> "WIN"
                                                item.isNearWin -> "${item.waitingCellIndexes.size} waiting"
                                                else -> null
                                            }
                                            if (status != null) {
                                                Text(
                                                    text = status,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.White,
                                                    modifier = Modifier
                                                        .padding(top = 12.dp)
                                                        .background(badgeColor, shape = MaterialTheme.shapes.small)
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                        }
                                        Spacer(modifier = Modifier.padding(top = 10.dp))
                                        BingoCardTable(
                                            item = item,
                                            cellSize = 34.dp
                                        )
                                    }
                                }

                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier
                                        .matchParentSize()
                                ) {
                                    drawRoundRect(
                                        color = borderColor,
                                        style = Stroke(width = 5f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f)
                                    )
                                }

                                if (item.isWin) {
                                    WinConfettiOverlay(modifier = Modifier.matchParentSize(), seed = item.cardId)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(minSize = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.cards, key = { it.cardId }) { item ->
                        val base = Color(item.colorArgb.toInt())
                        val container = if (item.isWin) base.copy(alpha = 0.18f) else base.copy(alpha = 0.10f)
                        val borderColor = base.copy(alpha = 0.90f)

                        Box {
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCardClick(item.cardId) },
                                colors = CardDefaults.elevatedCardColors(containerColor = container),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (item.isWin) {
                                        SlidingWinnerBanner()
                                    } else {
                                        Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                                    }
                                    BingoCardTable(item = item, cellSize = 44.dp)
                                }
                            }

                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.matchParentSize()
                            ) {
                                drawRoundRect(
                                    color = borderColor,
                                    style = Stroke(width = 6f),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f)
                                )
                            }

                            if (item.isWin) {
                                WinConfettiOverlay(modifier = Modifier.matchParentSize(), seed = item.cardId)
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
                                onManualCreate()
                            }
                        ) {
                            Text("Manual Entry")
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
            if (showHistorySheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showHistorySheet = false
                        historyFilterText = ""
                    }
                ) {
                    val filter = historyFilterText.trim()
                    val filteredNumbers = remember(state.calledNumbers, filter, historyShowRemaining) {
                        val source = if (historyShowRemaining) {
                            (1..75).filter { it !in state.calledNumbers }
                        } else {
                            state.calledNumbers
                        }

                        if (filter.isBlank()) {
                            source
                        } else {
                            val q = filter.lowercase()
                            val firstChar = q.firstOrNull()
                            val columnLetter = when (firstChar) {
                                'b', 'i', 'n', 'g', 'o' -> firstChar
                                else -> null
                            }
                            val digitsOnly = q.filter { it.isDigit() }
                            val wantedValue = digitsOnly.toIntOrNull()

                            source.filter { number ->
                                val letterMatches = when (columnLetter) {
                                    'b' -> number in 1..15
                                    'i' -> number in 16..30
                                    'n' -> number in 31..45
                                    'g' -> number in 46..60
                                    'o' -> number in 61..75
                                    else -> true
                                }
                                val numberMatches = wantedValue?.let { it == number } ?: number.toString().contains(digitsOnly)
                                letterMatches && (digitsOnly.isBlank() || numberMatches)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Called Numbers",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${filteredNumbers.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilterChip(
                            selected = historyShowRemaining,
                            onClick = { historyShowRemaining = !historyShowRemaining },
                            label = { Text("Remaining") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = historyFilterText,
                            onValueChange = { historyFilterText = it },
                            label = { Text("Search") },
                            placeholder = { Text("e.g. 12, B12, G") },
                            singleLine = true
                        )

                        if (filteredNumbers.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = if (filter.isBlank()) "No numbers called yet." else "No matches.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredNumbers) { number ->
                                    ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = number.toString(),
                                                style = MaterialTheme.typography.titleLarge,
                                                color = if (historyShowRemaining) Color(0xFFF57C00) else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = when (number) {
                                                    in 1..15 -> "B"
                                                    in 16..30 -> "I"
                                                    in 31..45 -> "N"
                                                    in 46..60 -> "G"
                                                    in 61..75 -> "O"
                                                    else -> ""
                                                },
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
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

private enum class CardListViewMode {
    List,
    Grid
}

@Composable
private fun SlidingWinnerBanner() {
    val transition = rememberInfiniteTransition(label = "winnerBanner")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing)
        ),
        label = "winnerBannerPhase"
    )

    var textWidthPx by remember { mutableStateOf(1) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(Color(0xFF2E7D32).copy(alpha = 0.12f))
            .padding(vertical = 6.dp)
    ) {
        val unit = textWidthPx.coerceAtLeast(1)
        val x = -((phase * unit).roundToInt() % unit)

        Row(modifier = Modifier.offset { IntOffset(x, 0) }) {
            repeat(12) {
                Text(
                    text = "PALDOOOOOOO!   ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF2E7D32),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.onSizeChanged { textWidthPx = it.width }
                )
            }
        }
    }
}

@Composable
private fun BingoCardTable(
    item: CardListItemUi,
    cellSize: androidx.compose.ui.unit.Dp
) {
    val base = Color(item.colorArgb.toInt())
    val shape = MaterialTheme.shapes.small
    val winBorder = Color(0xFF2E7D32)
    val waitBorder = Color(0xFFF57C00)
    val winBg = Color(0xFFB7F3C3)

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text("B", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("I", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("N", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("G", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("O", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }

        for (row in 0 until BingoRules.GRID_SIZE) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until BingoRules.GRID_SIZE) {
                    val idx = row * BingoRules.GRID_SIZE + col
                    val isFree = BingoRules.isFreeCell(row, col)
                    val cell = item.cells.firstOrNull { it.row == row && it.col == col }
                    val text = if (isFree) "FREE" else cell?.value?.toString() ?: ""
                    val marked = cell?.isMarked == true || isFree

                    val hasWin = idx in item.winningCellIndexes
                    val hasWait = idx in item.waitingCellIndexes

                    val cellBg = when {
                        isFree -> Color.Gray.copy(alpha = 0.20f)
                        hasWin -> winBg
                        marked -> MaterialTheme.colorScheme.primary
                        else -> base.copy(alpha = 0.14f)
                    }

                    val textColor = when {
                        isFree -> Color(0xFFFF7F50)
                        marked -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    val borderColor = when {
                        hasWin -> winBorder
                        hasWait -> waitBorder
                        isFree -> MaterialTheme.colorScheme.primary
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .size(cellSize)
                            .clip(shape)
                            .background(cellBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor
                        )

                        if (borderColor != Color.Transparent) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                                drawRoundRect(
                                    color = borderColor,
                                    style = Stroke(width = 4f),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WinConfettiOverlay(
    modifier: Modifier,
    seed: Long
) {
    val transition = rememberInfiniteTransition(label = "confetti")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing)
        ),
        label = "confettiPhase"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val colors = listOf(
            Color(0xFF66BB6A),
            Color(0xFFFFCA28),
            Color(0xFF42A5F5),
            Color(0xFFAB47BC),
            Color(0xFFEF5350)
        )

        val s = seed.absoluteValue.toInt().coerceAtLeast(1)
        for (i in 0 until 22) {
            val x = ((s * (i + 7)) % 97) / 97f
            val y0 = ((s * (i + 19)) % 89) / 89f
            val speed = 0.35f + (((s * (i + 11)) % 100) / 100f) * 0.85f
            val drift = (((s * (i + 29)) % 100) / 100f - 0.5f) * 0.06f

            val y = ((y0 + phase * speed) % 1f)
            val cx = ((x + phase * drift).coerceIn(0f, 1f) * w)
            val cy = (y * h)
            val r = 3f + (((s * (i + 3)) % 10).toFloat())
            drawCircle(
                color = colors[i % colors.size].copy(alpha = 0.35f),
                radius = r,
                center = androidx.compose.ui.geometry.Offset(cx, cy)
            )
        }
    }
}
