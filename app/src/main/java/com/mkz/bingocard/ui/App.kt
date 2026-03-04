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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mkz.bingocard.data.AppGraph
import com.mkz.bingocard.data.repo.BingoRepository
import com.mkz.bingocard.data.repo.SeedRepository
import com.mkz.bingocard.data.repo.SettingsRepository
import com.mkz.bingocard.ui.screens.CardDetailScreen
import com.mkz.bingocard.ui.screens.CardListScreen
import com.mkz.bingocard.ui.screens.PatternsScreen
import com.mkz.bingocard.ui.screens.ScanReviewScreen
import com.mkz.bingocard.ui.screens.SettingsScreen
import com.mkz.bingocard.ui.vm.CardDetailViewModel
import com.mkz.bingocard.ui.vm.CardDetailViewModelFactory
import com.mkz.bingocard.ui.vm.CardListViewModel
import com.mkz.bingocard.ui.vm.CardListViewModelFactory
import com.mkz.bingocard.ui.vm.PatternsViewModel
import com.mkz.bingocard.ui.vm.PatternsViewModelFactory
import com.mkz.bingocard.ui.vm.ScanViewModel
import com.mkz.bingocard.ui.vm.ScanViewModelFactory
import com.mkz.bingocard.ui.vm.SettingsViewModel
import com.mkz.bingocard.ui.vm.SettingsViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object Routes {
    const val Cards = "cards"
    const val CardDetail = "card/{cardId}"
    const val ScanCamera = "scan/camera"
    const val ScanReview = "scan/review"
    const val Patterns = "patterns"
    const val Settings = "settings"

    fun cardDetail(cardId: Long) = "card/$cardId"
}

@Composable
fun BingoApp() {
    val context = LocalContext.current
    val db = AppGraph.database(context)
    val repo = BingoRepository(db.cardDao(), db.patternDao(), db.calledNumberDao(), db.activePatternDao())
    val settingsRepo = remember { SettingsRepository(context) }

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
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                NavigationDrawerItem(
                    label = { androidx.compose.material3.Text("Cards") },
                    selected = currentRoute == Routes.Cards,
                    onClick = {
                        navController.navigate(Routes.Cards) {
                            popUpTo(Routes.Cards) { inclusive = true }
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { androidx.compose.material3.Text("Win Patterns") },
                    selected = currentRoute == Routes.Patterns,
                    onClick = {
                        navController.navigate(Routes.Patterns) {
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { androidx.compose.material3.Text("AI Settings") },
                    selected = currentRoute == Routes.Settings,
                    onClick = {
                        navController.navigate(Routes.Settings) {
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                }
            }
        }
    ) {
        val scanVm: ScanViewModel = viewModel(factory = ScanViewModelFactory(repo, settingsRepo))

        NavHost(
            navController = navController, 
            startDestination = Routes.Cards,
            enterTransition = { androidx.compose.animation.EnterTransition.None },
            exitTransition = { androidx.compose.animation.ExitTransition.None },
            popEnterTransition = { androidx.compose.animation.EnterTransition.None },
            popExitTransition = { androidx.compose.animation.ExitTransition.None }
        ) {
            composable(Routes.Cards) {
                CardListScreen(
                    stateFlow = cardsVm.state,
                    onCardClick = { navController.navigate(Routes.cardDetail(it)) },
                    onGalleryClick = { uri ->
                        scanVm.onImagePicked(uri)
                        navController.navigate(Routes.ScanReview)
                    },
                    onManualCreate = {
                        scanVm.onManualCreate()
                        navController.navigate(Routes.ScanReview)
                    },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onCallNumber = { cardsVm.callNumber(it) },
                    onDeleteCard = { cardsVm.deleteCard(it) },
                    onResetAll = { cardsVm.resetAllMarks() }
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
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ScanReview) {
                ScanReviewScreen(
                    stateFlow = scanVm.state,
                    onCellChanged = { row, col, value -> scanVm.updateCell(row, col, value) },
                    onColorChanged = { scanVm.updateColor(it) },
                    onRandomize = { scanVm.randomizeGrid() },
                    onSave = {
                        scanVm.saveAsNewCard()
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
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.Settings) {
                val vm: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(settingsRepo))
                SettingsScreen(
                    stateFlow = vm.state,
                    onAddKey = { vm.addKey(it) },
                    onRemoveKey = { vm.removeKey(it) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
