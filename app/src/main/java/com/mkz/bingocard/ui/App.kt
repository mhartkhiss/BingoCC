package com.mkz.bingocard.ui

import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.remember
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.mkz.bingocard.data.AppGraph
import com.mkz.bingocard.data.repo.BingoRepository
import com.mkz.bingocard.data.repo.SeedRepository
import com.mkz.bingocard.ui.screens.CardDetailScreen
import com.mkz.bingocard.ui.screens.CardListScreen
import com.mkz.bingocard.ui.screens.CropScreen
import com.mkz.bingocard.ui.screens.PatternsScreen
import com.mkz.bingocard.ui.screens.CameraCaptureScreen
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import com.mkz.bingocard.R
import androidx.core.content.FileProvider
import java.io.File

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
    val repo = BingoRepository(
        db.cardDao(),
        db.patternDao(),
        db.calledNumberDao(),
        db.calledNumberStatsDao(),
        db.activePatternDao()
    )

    LaunchedEffect(Unit) {
        SeedRepository.seedPresetsIfNeeded(repo)
        repo.warmupForStartup()
    }

    val navController = rememberNavController()
    val cardsVm: CardListViewModel = viewModel(factory = CardListViewModelFactory(repo))
    val patternsVm: PatternsViewModel = viewModel(factory = PatternsViewModelFactory(repo))
    val scanVm: ScanViewModel = viewModel(factory = ScanViewModelFactory(repo))

    val cameraImageFile = remember {
        File(context.cacheDir, "bingo_capture.jpg")
    }
    val cameraImageUri = remember(cameraImageFile) {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cameraImageFile)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scanVm.setImage(uri)
            navController.navigate(Routes.Crop)
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize()
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
                        label = { Text(stringResource(id = R.string.sidebar_cards)) },
                        selected = currentRoute == Routes.Cards,
                        onClick = {
                            if (currentRoute != Routes.Cards) {
                                navController.navigate(Routes.Cards) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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
                        label = { Text(stringResource(id = R.string.sidebar_win_patterns)) },
                        selected = currentRoute == Routes.Patterns,
                        onClick = {
                            if (currentRoute != Routes.Patterns) {
                                navController.navigate(Routes.Patterns) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        onManualCreate = {
                            scanVm.startCreateDraft()
                            navController.navigate(Routes.ScanReview)
                        },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onCallNumber = { cardsVm.callNumber(it) },
                        onUncallNumber = { cardsVm.uncallNumber(it) },
                        onDeleteCard = { cardsVm.deleteCard(it) },
                        onResetAll = { cardsVm.resetAllMarks() },
                        onResetCallStats = { cardsVm.clearCalledNumberStats() },
                        onToggleActive = { id, isActive -> cardsVm.setCardActive(id, isActive) }
                    )
                }
                composable(Routes.Patterns) {
                    PatternsScreen(
                        stateFlow = patternsVm.state,
                        onSelect = { patternsVm.selectPattern(it) },
                        onToggleCell = { row, col -> patternsVm.toggleCell(row, col) },
                        onNameChanged = { patternsVm.updateName(it) },
                        onCreate = { patternsVm.createNew() },
                        onSave = { patternsVm.save() },
                        onDelete = { patternsVm.deleteSelected() },
                        onToggleActive = { patternsVm.toggleActive(it) },
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
                        onRecapture = {
                            navController.navigate(Routes.ScanCamera) {
                                popUpTo(Routes.Crop) { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.ScanCamera) {
                    CameraCaptureScreen(
                        outputFile = cameraImageFile,
                        outputUri = cameraImageUri,
                        onCaptured = { uri ->
                            scanVm.setImage(uri)
                            navController.navigate(Routes.Crop) {
                                popUpTo(Routes.ScanCamera) { inclusive = true }
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
                        onCameraScan = { navController.navigate(Routes.ScanCamera) },
                        onPickFromGallery = { galleryLauncher.launch("image/*") },
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
}
