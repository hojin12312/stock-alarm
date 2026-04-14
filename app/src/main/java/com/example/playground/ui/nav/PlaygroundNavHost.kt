package com.example.playground.ui.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.playground.di.ServiceLocator
import com.example.playground.ui.dashboard.DashboardScreen
import com.example.playground.ui.dashboard.DashboardViewModel
import com.example.playground.ui.search.SearchScreen
import com.example.playground.ui.search.SearchViewModel
import com.example.playground.ui.watchlist.WatchlistScreen
import com.example.playground.ui.watchlist.WatchlistViewModel

@Composable
fun PlaygroundApp() {
    MaterialTheme {
        Surface(modifier = Modifier, color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            val context = LocalContext.current
            val repo = ServiceLocator.provideRepository(context)

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        val backStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = backStackEntry?.destination?.hierarchy
                            ?.firstOrNull()?.route
                        Destination.values().forEach { dest ->
                            NavigationBarItem(
                                selected = currentRoute == dest.route,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label) },
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Destination.Search.route,
                ) {
                    composable(Destination.Search.route) {
                        val vm: SearchViewModel = viewModel(factory = SearchViewModel.Factory(repo))
                        SearchScreen(viewModel = vm, contentPadding = innerPadding)
                    }
                    composable(Destination.Watchlist.route) {
                        val vm: WatchlistViewModel = viewModel(
                            factory = WatchlistViewModel.Factory(repo),
                        )
                        WatchlistScreen(viewModel = vm, contentPadding = innerPadding)
                    }
                    composable(Destination.Dashboard.route) {
                        val notifier = ServiceLocator.provideNotifier(context)
                        val vm: DashboardViewModel = viewModel(
                            factory = DashboardViewModel.Factory(repo, notifier),
                        )
                        DashboardScreen(viewModel = vm, contentPadding = innerPadding)
                    }
                }
            }
        }
    }
}

@Suppress("unused")
private fun PaddingValues.unusedHelper() = Unit
