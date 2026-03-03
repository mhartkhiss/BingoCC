package com.mkz.bingocard.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mkz.bingocard.data.AppGraph
import com.mkz.bingocard.data.repo.BingoRepository
import com.mkz.bingocard.data.repo.SeedRepository
import com.mkz.bingocard.ui.screens.CardDetailScreen
import com.mkz.bingocard.ui.screens.CardListScreen
import com.mkz.bingocard.ui.vm.CardDetailViewModel
import com.mkz.bingocard.ui.vm.CardDetailViewModelFactory
import com.mkz.bingocard.ui.vm.CardListViewModel
import com.mkz.bingocard.ui.vm.CardListViewModelFactory

object Routes {
    const val Cards = "cards"
    const val CardDetail = "card/{cardId}"

    fun cardDetail(cardId: Long) = "card/$cardId"
}

@Composable
fun BingoApp() {
    val context = LocalContext.current
    val db = AppGraph.database(context)
    val repo = BingoRepository(db.cardDao(), db.patternDao(), db.calledNumberDao())

    LaunchedEffect(Unit) {
        SeedRepository.seedPresetsIfNeeded(repo)
    }

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.Cards) {
        composable(Routes.Cards) {
            val vm: CardListViewModel = viewModel(factory = CardListViewModelFactory(repo))
            CardListScreen(
                stateFlow = vm.state,
                onCreateRandom = { vm.createRandomCard() },
                onCardClick = { navController.navigate(Routes.cardDetail(it)) },
                onCallNumber = { vm.callNumber(it) }
            )
        }
        composable(Routes.CardDetail) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId")?.toLongOrNull() ?: return@composable
            val vm: CardDetailViewModel = viewModel(factory = CardDetailViewModelFactory(repo, cardId))
            CardDetailScreen(
                stateFlow = vm.state,
                onToggleCell = { row, col, isMarked -> vm.setMarkedAt(row, col, isMarked) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
