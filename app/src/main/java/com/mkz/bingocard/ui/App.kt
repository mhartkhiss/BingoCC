package com.mkz.bingocard.ui

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mkz.bingocard.data.AppGraph
import com.mkz.bingocard.data.repo.BingoRepository
import com.mkz.bingocard.data.repo.SeedRepository
import com.mkz.bingocard.ui.screens.CardDetailScreen
import com.mkz.bingocard.ui.screens.CardListScreen
import com.mkz.bingocard.ui.screens.CropScreen
import com.mkz.bingocard.ui.screens.PatternsScreen
import com.mkz.bingocard.ui.screens.ScanReviewScreen
import com.mkz.bingocard.ui.screens.SplashScreen
import com.mkz.bingocard.ui.vm.CardDetailViewModel
import com.mkz.bingocard.ui.vm.CardDetailViewModelFactory
import com.mkz.bingocard.ui.vm.CardListViewModel
import com.mkz.bingocard.ui.vm.CardListViewModelFactory
import com.mkz.bingocard.ui.vm.PatternsViewModel
import com.mkz.bingocard.ui.vm.PatternsViewModelFactory
import com.mkz.bingocard.ui.vm.ScanViewModel
import com.mkz.bingocard.ui.vm.ScanViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.mkz.bingocard.R

object Routes {
    const val Splash = "splash"
    const val Cards = "cards"
    const val CardDetail = "card/{cardId}"
    const val ScanCamera = "scan/camera"
    const val Crop = "scan/crop"
    const val ScanReview = "scan/review"
    const val Patterns = "patterns"

    fun cardDetail(cardId: Long) = "card/$cardId"
}

@Composable
fun BingoApp() {
    val context = LocalContext.current
    val db = AppGraph.database(context)
    val repo = BingoRepository(db.cardDao(), db.patternDao(), db.calledNumberDao(), db.activePatternDao())

    LaunchedEffect(Unit) {
        SeedRepository.seedPresetsIfNeeded(repo)
    }

    val navController = rememberNavController()
    val cardsVm: CardListViewModel = viewModel(factory = CardListViewModelFactory(repo))

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // ── Header ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.sidebar_image),
                            contentDescription = "Sidebar Header",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Navigation Items ──
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null
                            )
                        },
                        label = { Text("Cards") },
                        selected = currentRoute == Routes.Cards,
                        onClick = {
                            navController.navigate(Routes.Cards) {
                                popUpTo(Routes.Cards) { inclusive = true }
                                launchSingleTop = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null
                            )
                        },
                        label = { Text("Win Patterns") },
                        selected = currentRoute == Routes.Patterns,
                        onClick = {
                            navController.navigate(Routes.Patterns) {
                                launchSingleTop = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // ── Footer ──
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "v1.0  ·  Copyright © 2026 Makizz",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }
            }
        }
    ) {
        val scanVm: ScanViewModel = viewModel(factory = ScanViewModelFactory(repo))

        NavHost(
            navController = navController, 
            startDestination = Routes.Splash,
            enterTransition = { androidx.compose.animation.EnterTransition.None },
            exitTransition = { androidx.compose.animation.ExitTransition.None },
            popEnterTransition = { androidx.compose.animation.EnterTransition.None },
            popExitTransition = { androidx.compose.animation.ExitTransition.None }
        ) {
            composable(Routes.Splash) {
                SplashScreen(
                    onFinished = {
                        navController.navigate(Routes.Cards) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.Cards) {
                CardListScreen(
                    stateFlow = cardsVm.state,
                    onCardClick = { navController.navigate(Routes.cardDetail(it)) },
                    onEditCard = { cardId ->
                        scanVm.loadExistingCard(cardId)
                        navController.navigate(Routes.ScanReview)
                    },
                    onGalleryClick = { uri ->
                        scanVm.setImage(uri)
                        navController.navigate(Routes.Crop)
                    },
                    onManualCreate = {
                        scanVm.onManualCreate()
                        navController.navigate(Routes.ScanReview)
                    },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onCallNumber = { cardsVm.callNumber(it) },
                    onUncallNumber = { cardsVm.uncallNumber(it) },
                    onDeleteCard = { cardsVm.deleteCard(it) },
                    onResetAll = { cardsVm.resetAllMarks() },
                    onToggleActive = { id, isActive -> cardsVm.setCardActive(id, isActive) }
                )
            }
            composable(Routes.Patterns) {
                val vm: PatternsViewModel = viewModel(factory = PatternsViewModelFactory(repo))
                PatternsScreen(
                    stateFlow = vm.state,
                    onSelect = { vm.selectPattern(it) },
                    onToggleCell = { row, col -> vm.toggleCell(row, col) },
                    onNameChanged = { vm.updateName(it) },
                    onCreate = { vm.createNew() },
                    onSave = { vm.save() },
                    onDelete = { vm.deleteSelected() },
                    onToggleActive = { vm.toggleActive(it) },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            composable(Routes.Crop) {
                CropScreen(
                    imageUri = scanVm.state.value.imageUri,
                    onConfirm = { left, top, right, bottom ->
                        scanVm.analyzeCroppedImage(left, top, right, bottom)
                        navController.navigate(Routes.ScanReview) {
                            popUpTo(Routes.Crop) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.ScanReview) {
                ScanReviewScreen(
                    stateFlow = scanVm.state,
                    onCellChanged = { row, col, value -> scanVm.updateCell(row, col, value) },
                    onColorChanged = { scanVm.updateColor(it) },
                    onNameChanged = { scanVm.updateName(it) },
                    onRandomize = { scanVm.randomizeGrid() },
                    onSave = {
                        scanVm.saveCard()
                        navController.popBackStack(Routes.Cards, inclusive = false)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.CardDetail) { backStackEntry ->
                val cardId = backStackEntry.arguments?.getString("cardId")?.toLongOrNull() ?: return@composable
                val vm: CardDetailViewModel = viewModel(factory = CardDetailViewModelFactory(repo, cardId))
                CardDetailScreen(
                    stateFlow = vm.state,
                    onToggleNumber = { value, isMarked -> vm.setMarkedByValue(value, isMarked) },
                    onDelete = { vm.deleteCard() },
                    onToggleActive = { vm.toggleActive(it) },
                    onEditCard = {
                        scanVm.loadExistingCard(cardId)
                        navController.navigate(Routes.ScanReview)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

        }
    }
}
