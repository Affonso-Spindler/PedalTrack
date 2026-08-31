package com.affonso.pedaltrack.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.affonso.pedaltrack.repository.CyclingRepository
import com.affonso.pedaltrack.ui.history.HistoryScreen
import com.affonso.pedaltrack.ui.history.HistoryViewModel
import com.affonso.pedaltrack.ui.log.LogSessionScreen
import com.affonso.pedaltrack.ui.log.LogSessionViewModel
import com.affonso.pedaltrack.ui.summary.SummaryScreen
import com.affonso.pedaltrack.ui.summary.SummaryViewModel

private sealed class Destination(val route: String, val label: String) {
    data object Log : Destination("log", "Lançar")
    data object History : Destination("history", "Histórico")
    data object Summary : Destination("summary", "Resumo")
}

private val destinations = listOf(Destination.Log, Destination.History, Destination.Summary)

@Composable
fun PedalTrackNavHost(repository: CyclingRepository) {
    val navController = rememberNavController()
    val factory = remember(repository) { pedalTrackViewModelFactory(repository) }

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            val icon = when (destination) {
                                Destination.Log -> Icons.Filled.DirectionsBike
                                Destination.History -> Icons.Filled.History
                                Destination.Summary -> Icons.Filled.BarChart
                            }
                            Icon(icon, contentDescription = destination.label)
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Log.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Log.route) {
                val viewModel: LogSessionViewModel = viewModel(factory = factory)
                val uiState by viewModel.uiState.collectAsState()
                // Log is the NavHost's start destination, so its NavBackStackEntry (and this
                // ViewModel) survives every bottom-nav tab switch and init{} only runs once.
                // Re-fetch on every visit to this composable so a session deleted in Histórico,
                // or a new workout synced from Health Connect, shows up without a cold restart.
                LaunchedEffect(Unit) { viewModel.loadSessions() }
                LogSessionScreen(
                    uiState = uiState,
                    onSubmit = viewModel::submit,
                    onDismissError = viewModel::dismissError
                )
            }
            composable(Destination.History.route) {
                val viewModel: HistoryViewModel = viewModel(factory = factory)
                val uiState by viewModel.uiState.collectAsState()
                HistoryScreen(sessions = uiState.sessions, onUpdate = viewModel::update, onDelete = viewModel::delete)
            }
            composable(Destination.Summary.route) {
                val viewModel: SummaryViewModel = viewModel(factory = factory)
                val uiState by viewModel.uiState.collectAsState()
                SummaryScreen(uiState = uiState, onPeriodChange = viewModel::setPeriod)
            }
        }
    }
}

private fun pedalTrackViewModelFactory(repository: CyclingRepository) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        LogSessionViewModel::class.java -> LogSessionViewModel(repository) as T
        HistoryViewModel::class.java -> HistoryViewModel(repository) as T
        SummaryViewModel::class.java -> SummaryViewModel(repository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}
