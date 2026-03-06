package com.mkz.bingocard.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.mkz.bingocard.R
import com.mkz.bingocard.ui.components.AppActionDialog
import com.mkz.bingocard.ui.components.AppDialogType
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
    onEditCard: (Long) -> Unit,
    onManualCreate: () -> Unit,
    onOpenDrawer: () -> Unit,
    onCallNumber: (Int) -> Unit,
    onUncallNumber: (Int) -> Unit,
    onDeleteCard: (Long) -> Unit,
    onResetAll: () -> Unit,
    onResetCallStats: () -> Unit,
    onToggleActive: (Long, Boolean) -> Unit
) {
    val state by stateFlow.collectAsState()
    var cardToDelete by remember { mutableStateOf<Long?>(null) }
    var cardOptionsId by remember { mutableStateOf<Long?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showAlreadyCalledDialog by remember { mutableStateOf(false) }
    var alreadyCalledNumber by remember { mutableStateOf<Int?>(null) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var historyFilterText by remember { mutableStateOf("") }
    var historyTab by remember { mutableStateOf(HistoryTab.CALLED) }
    var showResetStatsConfirm by remember { mutableStateOf(false) }
    var showUndoConfirm by remember { mutableStateOf(false) }

    // --- Animation state for newest called-number chip ---
    val newestChipScale = remember { Animatable(1f) }
    val newestChipAlpha = remember { Animatable(1f) }
    var newestChipBorderVisible by remember { mutableStateOf(false) }
    var hasInitializedCalledNumbers by remember { mutableStateOf(false) }
    var prevCalledCount by remember { mutableIntStateOf(0) }
    val animScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(state.isLoading, state.calledNumbers.size) {
        if (!hasInitializedCalledNumbers) {
            if (!state.isLoading) {
                // Treat restored numbers as baseline so relaunch does not animate.
                prevCalledCount = state.calledNumbers.size
                hasInitializedCalledNumbers = true
            }
            return@LaunchedEffect
        }

        if (state.calledNumbers.size > prevCalledCount) {
            newestChipBorderVisible = true
            // New number was just called.
            animScope.launch {
                val cycles = 4
                repeat(cycles) {
                    newestChipScale.animateTo(1.08f, tween(180))
                    newestChipScale.animateTo(1f, tween(180))
                }
            }
            animScope.launch {
                val blinks = 4
                repeat(blinks) {
                    newestChipAlpha.animateTo(0.85f, tween(180))
                    newestChipAlpha.animateTo(1f, tween(180))
                }
            }
            animScope.launch {
                delay(1500)
                newestChipBorderVisible = false
            }
        }
        prevCalledCount = state.calledNumbers.size
    }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val gridState = rememberLazyGridState()
    
    var lastCards by remember { mutableStateOf(state.cards) }
    var hasInitializedCards by remember { mutableStateOf(false) }
    var highlightedNewCardId by remember { mutableStateOf<Long?>(null) }
    var showNewCardHighlight by remember { mutableStateOf(false) }
    var celebratingWinCardIds by remember { mutableStateOf(setOf<Long>()) }
    val newCardBlinkTransition = rememberInfiniteTransition(label = "newCardBorder")
    val newCardBlinkAlpha by newCardBlinkTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 260, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "newCardBorderAlpha"
    )

    LaunchedEffect(state.isLoading, state.cards) {
        if (!hasInitializedCards) {
            if (!state.isLoading) {
                // Treat restored cards as baseline so launch does not trigger "new card" effects.
                lastCards = state.cards
                hasInitializedCards = true
            }
            return@LaunchedEffect
        }

        val oldIds = lastCards.map { it.cardId }.toSet()
        val newCard = state.cards.firstOrNull { it.cardId !in oldIds }
        if (newCard != null) {
            val index = state.cards.indexOfFirst { it.cardId == newCard.cardId }
            if (index >= 0) {
                gridState.animateScrollToItem(index)
                highlightedNewCardId = newCard.cardId
                showNewCardHighlight = true
                delay(3_000)
                showNewCardHighlight = false
                highlightedNewCardId = null
            }
            lastCards = state.cards
            return@LaunchedEffect
        }

        val oldWinIds = lastCards.filter { it.isActive && it.isWin }.map { it.cardId }.toSet()
        val newWins = state.cards.filter { it.isActive && it.isWin }.map { it.cardId }
        val newlyWonIds = newWins.filter { it !in oldWinIds }

        val oldWaitIds = lastCards.filter { it.isActive && it.isNearWin }.map { it.cardId }.toSet()
        val newWaits = state.cards.filter { it.isActive && it.isNearWin }.map { it.cardId }
        val newWaiting = newWaits.any { it !in oldWaitIds }

        if (newlyWonIds.isNotEmpty()) {
            newlyWonIds.forEach { cardId ->
                celebratingWinCardIds = celebratingWinCardIds + cardId
                launch {
                    delay(3_000)
                    celebratingWinCardIds = celebratingWinCardIds - cardId
                }
            }
        }

        if (newlyWonIds.isNotEmpty() || newWaiting) {
            // Scroll to top since sorted cards place wins/waits first
            gridState.animateScrollToItem(0)
        }
        lastCards = state.cards
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state.calledNumbers.isEmpty()) {
                        Text("Bingo Cards")
                    } else {
                        val topBarHistoryScroll = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                                )
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .horizontalScroll(topBarHistoryScroll),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Keep animated edge chips away from menu/add buttons.
                                Spacer(modifier = Modifier.width(8.dp))
                                // Show most recent first (calledNumbers is already DESC from DB)
                                state.calledNumbers.take(15).forEachIndexed { index, number ->
                                    val letter = when (number) {
                                        in 1..15 -> "B"
                                        in 16..30 -> "I"
                                        in 31..45 -> "N"
                                        in 46..60 -> "G"
                                        in 61..75 -> "O"
                                        else -> ""
                                    }
                                    val isNewest = index == 0
                                    val chipModifier = if (isNewest) {
                                        Modifier
                                            .graphicsLayer {
                                                scaleX = newestChipScale.value
                                                scaleY = newestChipScale.value
                                                alpha = newestChipAlpha.value
                                            }
                                    } else Modifier
                                    FilterChip(
                                        selected = false,
                                        onClick = { showHistorySheet = true },
                                        modifier = chipModifier,
                                        border = if (isNewest && newestChipBorderVisible) {
                                            BorderStroke(1.5.dp, Color(0xFFFFD54F))
                                        } else {
                                            null
                                        },
                                        label = {
                                            Text(
                                                text = "$letter$number",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = if (isNewest)
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                                            else
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    if (state.calledNumbers.isEmpty()) {
                        IconButton(onClick = { showHistorySheet = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_history_doc_sample),
                                contentDescription = stringResource(id = R.string.sidebar_history)
                            )
                        }
                    }
                    IconButton(
                        onClick = onManualCreate,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Card",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            CardListBottomBar(
                state = state,
                onCallNumber = onCallNumber,
                onRequestReset = { showResetConfirm = true },
                onRequestUndo = { showUndoConfirm = true },
                onRepeatedNumber = { number ->
                    alreadyCalledNumber = number
                    showAlreadyCalledDialog = true
                }
            )
        }
    ) { innerPadding ->
        if (showAlreadyCalledDialog) {
            AppActionDialog(
                title = "Already called",
                message = "${alreadyCalledNumber ?: "That number"} is already in the called history.",
                confirmLabel = "OK",
                dismissLabel = null,
                type = AppDialogType.WARNING,
                onConfirm = {
                    showAlreadyCalledDialog = false
                    alreadyCalledNumber = null
                },
                onDismiss = {
                    showAlreadyCalledDialog = false
                    alreadyCalledNumber = null
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

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (state.cards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🎱",
                            fontSize = androidx.compose.ui.unit.TextUnit(64f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                        Text(
                            text = "No bingo cards yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Tap the  ＋  button at the top right\nto add your first card!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = androidx.compose.ui.unit.TextUnit(22f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
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
                                    .clickable(enabled = item.isActive) {
                                        cardOptionsId = item.cardId
                                    }
                                    .alpha(if (item.isActive) 1f else 0.4f),
                                colors = CardDefaults.elevatedCardColors(containerColor = container),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val nameChipContainer = base.copy(alpha = 0.18f)
                                            val nameChipTextColor = if (nameChipContainer.luminance() > 0.55f) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                Color.White
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(nameChipContainer)
                                                    .border(
                                                        width = 1.dp,
                                                        color = base.copy(alpha = 0.65f),
                                                        shape = RoundedCornerShape(999.dp)
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Text(
                                                    text = item.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = nameChipTextColor,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            if (item.isWin) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFF2E7D32), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "BINGO",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else if (item.waitingCellIndexes.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFF57C00), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${item.waitingCellIndexes.size} waiting",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        if (item.activeWinCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.error, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "WIN: ${item.activeWinCount}",
                                                    color = MaterialTheme.colorScheme.onError,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        if (item.disabledWinCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF9E9E9E), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "WIN(D): ${item.disabledWinCount}",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Switch(
                                            checked = item.isActive,
                                            onCheckedChange = { onToggleActive(item.cardId, it) },
                                            modifier = Modifier.scale(0.7f)
                                        )
                                    }
                                    if (item.isActive && item.isWin) {
                                        SlidingWinnerBanner()
                                    }
                                    BingoCardTable(item = item, cellSize = 44.dp)
                                }
                            }

                            androidx.compose.foundation.Canvas(
                                modifier = Modifier
                                    .matchParentSize()
                                    .alpha(if (item.isActive) 1f else 0.4f)
                            ) {
                                drawRoundRect(
                                    color = borderColor,
                                    style = Stroke(width = 6f),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f)
                                )
                            }

                            if (showNewCardHighlight && highlightedNewCardId == item.cardId) {
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier.matchParentSize()
                                ) {
                                    drawRoundRect(
                                        color = Color(0xFFFFEB3B).copy(alpha = newCardBlinkAlpha),
                                        style = Stroke(width = 10f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f)
                                    )
                                }
                            }

                            if (item.cardId in celebratingWinCardIds) {
                                WinConfettiOverlay(modifier = Modifier.matchParentSize(), seed = item.cardId)
                            }
                        }
                    }
                }
            }

            if (showHistorySheet) {
                Dialog(
                    onDismissRequest = {
                        showHistorySheet = false
                        historyFilterText = ""
                        historyTab = HistoryTab.CALLED
                    },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val filter = historyFilterText.trim()
                        val sourceNumbers = remember(state.calledNumbers, historyTab) {
                            when (historyTab) {
                                HistoryTab.CALLED -> state.calledNumbers
                                HistoryTab.REMAINING -> (1..75).filter { it !in state.calledNumbers }
                                HistoryTab.STATS -> emptyList()
                            }
                        }
                        val filteredNumbers = remember(sourceNumbers, filter) {
                            if (filter.isBlank()) {
                                sourceNumbers
                            } else {
                                sourceNumbers.filter { matchesNumberQuery(it, filter) }
                            }
                        }
                        val filteredStats = remember(state.calledNumberStats, filter) {
                            val base = state.calledNumberStats.entries
                                .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
                            if (filter.isBlank()) {
                                base
                            } else {
                                base.filter { matchesNumberQuery(it.key, filter) }
                            }
                        }

                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Column {
                                            Text("Called Numbers", style = MaterialTheme.typography.titleLarge)
                                            Text(
                                                text = "${state.calledNumbers.size} called · ${75 - state.calledNumbers.size} remaining",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = {
                                            showHistorySheet = false
                                            historyFilterText = ""
                                        }) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                                        }
                                    }
                                )
                            }
                        ) { historyPadding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(historyPadding)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TabRow(
                                    selectedTabIndex = historyTab.ordinal
                                ) {
                                    Tab(
                                        selected = historyTab == HistoryTab.CALLED,
                                        onClick = { historyTab = HistoryTab.CALLED },
                                        text = { Text("Called (${state.calledNumbers.size})") }
                                    )
                                    Tab(
                                        selected = historyTab == HistoryTab.REMAINING,
                                        onClick = { historyTab = HistoryTab.REMAINING },
                                        text = { Text("Remaining (${75 - state.calledNumbers.size})") }
                                    )
                                    Tab(
                                        selected = historyTab == HistoryTab.STATS,
                                        onClick = { historyTab = HistoryTab.STATS },
                                        text = { Text("Stats") }
                                    )
                                }

                                OutlinedTextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = historyFilterText,
                                    onValueChange = { historyFilterText = it },
                                    label = { Text("Search") },
                                    placeholder = { Text(if (historyTab == HistoryTab.STATS) "e.g. 12, B12" else "e.g. 12, B12, G") },
                                    singleLine = true
                                )

                                if (historyTab == HistoryTab.STATS) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { showResetStatsConfirm = true }) {
                                            Text("Reset Stats", color = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    if (filteredStats.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (filter.isBlank()) "No stats yet." else "No matches.",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "${filteredStats.size} shown",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(filteredStats) { stat ->
                                                val number = stat.key
                                                val count = stat.value
                                                val letter = bingoLetter(number)
                                                val letterColor = bingoLetterColor(letter)

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
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(MaterialTheme.shapes.small)
                                                                .background(letterColor.copy(alpha = 0.15f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = letter,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = letterColor
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        Text(
                                                            text = number.toString(),
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontWeight = FontWeight.Medium,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Text(
                                                            text = "x$count",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (filteredNumbers.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (filter.isBlank()) "No numbers called yet." else "No matches.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "${filteredNumbers.size} shown",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(filteredNumbers) { number ->
                                            val letter = bingoLetter(number)
                                            val letterColor = bingoLetterColor(letter)

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
                                                    // Letter badge
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(MaterialTheme.shapes.small)
                                                            .background(letterColor.copy(alpha = 0.15f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = letter,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = letterColor
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Text(
                                                        text = number.toString(),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (historyTab == HistoryTab.REMAINING) Color(0xFFF57C00) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (cardOptionsId != null) {
                val selectedCard = state.cards.firstOrNull { it.cardId == cardOptionsId }
                Dialog(
                    onDismissRequest = { cardOptionsId = null },
                    properties = DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                        usePlatformDefaultWidth = true
                    )
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(selectedCard?.name ?: "Card", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Choose what to do with this card",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            IconButton(onClick = { cardOptionsId = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            onClick = {
                                val id = cardOptionsId
                                cardOptionsId = null
                                if (id != null) onCardClick(id)
                            }
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open", modifier = Modifier.padding(end = 8.dp))
                            Text("Open")
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                val id = cardOptionsId
                                cardOptionsId = null
                                if (id != null) onEditCard(id)
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.padding(end = 8.dp))
                            Text("Edit Card")
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            onClick = {
                                cardToDelete = cardOptionsId
                                cardOptionsId = null
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.padding(end = 8.dp))
                            Text("Delete Card")
                        }
                    }
                    }
                }
            }

            if (cardToDelete != null) {
                AppActionDialog(
                    title = "Delete Card",
                    message = "Are you sure you want to delete this card?",
                    confirmLabel = "Delete",
                    type = AppDialogType.DESTRUCTIVE,
                    onConfirm = {
                        onDeleteCard(cardToDelete!!)
                        cardToDelete = null
                    },
                    onDismiss = { cardToDelete = null }
                )
            }
            if (showResetConfirm) {
                AppActionDialog(
                    title = "Reset Round",
                    message = "Are you sure you want to reset this round and clear marks on all your Bingo cards?",
                    confirmLabel = "Reset Round",
                    type = AppDialogType.WARNING,
                    onConfirm = {
                        onResetAll()
                        showResetConfirm = false
                    },
                    onDismiss = { showResetConfirm = false }
                )
            }
            if (showResetStatsConfirm) {
                AppActionDialog(
                    title = "Reset Number Stats",
                    message = "Clear all called-number stats counts?",
                    confirmLabel = "Reset Stats",
                    type = AppDialogType.WARNING,
                    onConfirm = {
                        onResetCallStats()
                        showResetStatsConfirm = false
                    },
                    onDismiss = { showResetStatsConfirm = false }
                )
            }
            if (showUndoConfirm && state.calledNumbers.isNotEmpty()) {
                val lastNumber = state.calledNumbers.first() // already DESC order
                val lastLetter = when (lastNumber) {
                    in 1..15 -> "B"
                    in 16..30 -> "I"
                    in 31..45 -> "N"
                    in 46..60 -> "G"
                    in 61..75 -> "O"
                    else -> ""
                }
                AppActionDialog(
                    title = "Undo Last Call",
                    message = "Remove $lastLetter$lastNumber from called numbers and unmark it on all cards?",
                    confirmLabel = "Undo",
                    type = AppDialogType.WARNING,
                    onConfirm = {
                        onUncallNumber(lastNumber)
                        showUndoConfirm = false
                    },
                    onDismiss = { showUndoConfirm = false }
                )
            }
        }
    }
}

private enum class HistoryTab {
    CALLED,
    REMAINING,
    STATS
}

private fun bingoLetter(number: Int): String = when (number) {
    in 1..15 -> "B"
    in 16..30 -> "I"
    in 31..45 -> "N"
    in 46..60 -> "G"
    in 61..75 -> "O"
    else -> ""
}

private fun bingoLetterColor(letter: String): Color = when (letter) {
    "B" -> Color(0xFF1E88E5)
    "I" -> Color(0xFFE53935)
    "N" -> Color(0xFF43A047)
    "G" -> Color(0xFFFDD835)
    "O" -> Color(0xFF8E24AA)
    else -> Color.Unspecified
}

private fun matchesNumberQuery(number: Int, filter: String): Boolean {
    val q = filter.trim().lowercase()
    if (q.isBlank()) return true

    val firstChar = q.firstOrNull()
    val columnLetter = when (firstChar) {
        'b', 'i', 'n', 'g', 'o' -> firstChar
        else -> null
    }
    val digitsOnly = q.filter { it.isDigit() }
    val wantedValue = digitsOnly.toIntOrNull()

    val letterMatches = when (columnLetter) {
        'b' -> number in 1..15
        'i' -> number in 16..30
        'n' -> number in 31..45
        'g' -> number in 46..60
        'o' -> number in 61..75
        else -> true
    }
    val numberMatches = wantedValue?.let { it == number } ?: number.toString().contains(digitsOnly)
    return letterMatches && (digitsOnly.isBlank() || numberMatches)
}

@Composable
private fun CardListBottomBar(
    state: CardListUiState,
    onCallNumber: (Int) -> Unit,
    onRequestReset: () -> Unit,
    onRequestUndo: () -> Unit,
    onRepeatedNumber: (Int) -> Unit
) {
    var calledNumberText by remember { mutableStateOf("") }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    BottomAppBar(
        actions = {
            val typedValue = calledNumberText.toIntOrNull()

            val winningGreen = Color(0xFF4CAF50)
            val defaultGray = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            val markedCellColor = MaterialTheme.colorScheme.primary
            val isRepeatedNumber = typedValue != null && typedValue in state.calledNumbers

            val isWinningNumber = typedValue != null && !isRepeatedNumber && state.cards.any { card ->
                card.isActive && card.waitingCellIndexes.any { idx ->
                    card.cells.getOrNull(idx)?.value == typedValue
                }
            }

            val isMatchingNumber = !isWinningNumber && typedValue != null && !isRepeatedNumber && state.cards.any { card ->
                card.isActive && card.cells.any { it.value == typedValue && !it.isMarked && !it.isFree }
            }

            val borderColor = when {
                isWinningNumber -> winningGreen
                isMatchingNumber -> markedCellColor
                else -> defaultGray
            }

            val contentColor = when {
                isWinningNumber -> winningGreen
                isMatchingNumber -> markedCellColor
                isRepeatedNumber -> defaultGray
                typedValue != null -> markedCellColor
                else -> defaultGray
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onRequestReset,
                    enabled = state.calledNumbers.isNotEmpty(),
                    modifier = Modifier.background(Color(0xFFFF9800).copy(alpha = 0.15f), shape = androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Round",
                        tint = if (state.calledNumbers.isNotEmpty()) {
                            Color(0xFFFF9800)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }

                IconButton(
                    onClick = onRequestUndo,
                    enabled = state.calledNumbers.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo Last Call",
                        tint = if (state.calledNumbers.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                val tryMarkCalledNumber = {
                    if (typedValue != null && typedValue in 1..75) {
                        if (typedValue in state.calledNumbers) {
                            onRepeatedNumber(typedValue)
                        } else {
                            onCallNumber(typedValue)
                            calledNumberText = ""
                            keyboardController?.hide()
                        }
                    }
                }

                val glowModifier = if (isWinningNumber || isMatchingNumber) {
                    Modifier.background(Color.Transparent, androidx.compose.foundation.shape.RoundedCornerShape(26.dp))
                        .shadow(
                            elevation = 16.dp,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                            spotColor = borderColor,
                            ambientColor = borderColor
                        )
                } else Modifier

                val emoji = when {
                    typedValue == null || typedValue !in 1..75 || typedValue in state.calledNumbers -> ""
                    isWinningNumber -> "🎉"
                    isMatchingNumber -> "🤍"
                    else -> "☹️"
                }

                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .padding(end = 12.dp)
                        .then(glowModifier),
                    value = calledNumberText,
                    onValueChange = { newText ->
                        val filtered = newText.filter { it.isDigit() }.take(2)
                        val num = filtered.toIntOrNull()
                        if (filtered.isEmpty() || (num != null && num in 1..75)) {
                            calledNumberText = filtered
                        }
                    },
                    textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        color = contentColor
                    ),
                    placeholder = {
                        Text(
                            text = "Unsay number?...",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium,
                            color = defaultGray
                        )
                    },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = borderColor,
                        unfocusedBorderColor = borderColor,
                        cursorColor = contentColor
                    ),
                    leadingIcon = if (typedValue != null) {
                        {
                            val letter = when (typedValue) {
                                in 1..15 -> "B"
                                in 16..30 -> "I"
                                in 31..45 -> "N"
                                in 46..60 -> "G"
                                in 61..75 -> "O"
                                else -> ""
                            }
                            if (letter.isNotEmpty()) {
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { tryMarkCalledNumber() }
                    ),
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                    trailingIcon = if (calledNumberText.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { tryMarkCalledNumber() },
                                enabled = typedValue != null && typedValue in 1..75,
                                modifier = Modifier.size(32.dp)
                            ) {
                                if (emoji.isNotEmpty()) {
                                    Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                                } else {
                                    val iconTint = if (typedValue != null && typedValue in 1..75) {
                                        contentColor
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Mark",
                                        tint = iconTint
                                    )
                                }
                            }
                        }
                    } else null
                )
            }
        }
    )
}



@Composable
private fun SlidingWinnerBanner() {
    val transition = rememberInfiniteTransition(label = "winnerBanner")

    // Scrolling phase
    val scrollPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        ),
        label = "winnerBannerScroll"
    )

    // Color cycling phase
    val colorPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing)
        ),
        label = "winnerBannerColor"
    )

    val rainbowColors = listOf(
        Color(0xFFE53935), Color(0xFFFF9800), Color(0xFFFDD835),
        Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA)
    )

    var textWidthPx by remember { mutableStateOf(1) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(Color(0xFF2E7D32).copy(alpha = 0.10f))
            .padding(vertical = 4.dp)
    ) {
        val unit = textWidthPx.coerceAtLeast(1)
        val x = -((scrollPhase * unit).roundToInt() % unit)

        Row(modifier = Modifier.offset { IntOffset(x, 0) }) {
            repeat(10) { repeatIdx ->
                val bannerText = "✦ PALDOOO! ✦  "
                Row(modifier = Modifier.onSizeChanged { textWidthPx = it.width }) {
                    bannerText.forEachIndexed { charIdx, c ->
                        val cIdx = ((colorPhase * rainbowColors.size + charIdx * 0.3f + repeatIdx).toInt()) % rainbowColors.size
                        Text(
                            text = c.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = rainbowColors[cIdx],
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
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
    val cellsByPosition = remember(item.cells) { item.cells.associateBy { it.row to it.col } }
    val shape = MaterialTheme.shapes.small
    val winBorder = Color(0xFF2E7D32)
    val waitBorder = Color(0xFFF57C00)
    val winBg = Color(0xFF4CAF50)

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
                    val cell = cellsByPosition[row to col]
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
                        isFree -> MaterialTheme.colorScheme.primary
                        hasWin -> Color.White
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
    var time by remember { mutableStateOf(0f) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        var start = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { frameTime ->
                if (start == 0L) start = frameTime
                time = (frameTime - start) / 1_000_000_000f
            }
        }
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val colors = listOf(
            Color(0xFFE53935), Color(0xFFFF9800), Color(0xFFFDD835),
            Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA),
            Color(0xFFFF5252), Color(0xFF69F0AE)
        )

        val s = seed.absoluteValue.toInt().coerceAtLeast(1)
        val particleCount = 40

        for (i in 0 until particleCount) {
            val x = ((s * (i + 7)) % 97) / 97f
            val y0 = ((s * (i + 19)) % 89) / 89f
            val speed = 0.3f + (((s * (i + 11)) % 100) / 100f) * 0.7f

            val fallDistance = y0 + time * speed
            val y = (fallDistance % 1.2f) - 0.1f
            
            val cx = (x + kotlin.math.sin(time * 2f + i.toFloat()) * 0.05f) * w
            val cy = y * h
            val r = 3f + (((s * (i + 3)) % 8).toFloat())
            val alpha = 0.3f + (kotlin.math.sin(time * 3f + i * 0.7f).absoluteValue) * 0.45f

            val color = colors[i % colors.size].copy(alpha = alpha.coerceIn(0f, 1f))

            if (i % 3 == 0) {
                // Rectangle confetti
                val rectW = r * 2.2f
                val rectH = r * 1.2f
                drawRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(cx - rectW / 2, cy - rectH / 2),
                    size = androidx.compose.ui.geometry.Size(rectW, rectH)
                )
            } else {
                // Circle confetti
                drawCircle(
                    color = color,
                    radius = r,
                    center = androidx.compose.ui.geometry.Offset(cx, cy)
                )
            }
        }
    }
}

