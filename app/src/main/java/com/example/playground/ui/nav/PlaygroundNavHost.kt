package com.example.playground.ui.nav

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.playground.data.model.AlgorithmType
import com.example.playground.data.model.MarketIndex
import com.example.playground.di.ServiceLocator
import com.example.playground.ui.chart.ChartScreen
import com.example.playground.ui.chart.ChartViewModel
import com.example.playground.ui.dashboard.DashboardScreen
import com.example.playground.ui.dashboard.DashboardViewModel
import com.example.playground.ui.market.MarketScreen
import com.example.playground.ui.market.MarketViewModel
import com.example.playground.ui.notification.NotificationSheetContent
import com.example.playground.ui.notification.NotificationViewModel
import com.example.playground.ui.search.SearchScreen
import com.example.playground.ui.search.SearchViewModel
import com.example.playground.ui.settings.SettingsScreen
import com.example.playground.ui.settings.SettingsViewModel
import com.example.playground.ui.theme.AppTheme
import com.example.playground.ui.watchlist.WatchlistScreen
import com.example.playground.ui.watchlist.WatchlistViewModel
import kotlinx.coroutines.launch

object NavRoutes {
    const val CHART_PREFIX = "chart"
    const val CHART_ARG_SYMBOL = "symbol"
    const val CHART_ARG_ALGOS = "algos"
    const val CHART_ARG_NAME = "name"

    fun chartFor(symbol: String, algos: Set<AlgorithmType> = setOf(AlgorithmType.MA_CROSS)): String =
        "$CHART_PREFIX/$symbol?$CHART_ARG_ALGOS=${algos.joinToString(",") { it.name }}"

    fun chartForIndex(index: MarketIndex): String {
        val encodedSymbol = Uri.encode(index.symbol)
        val encodedName = Uri.encode(index.displayName)
        return "$CHART_PREFIX/$encodedSymbol?$CHART_ARG_ALGOS=${AlgorithmType.MA_CROSS.name}&$CHART_ARG_NAME=$encodedName"
    }

    const val CHART_PATTERN = "$CHART_PREFIX/{symbol}?$CHART_ARG_ALGOS={algos}&$CHART_ARG_NAME={name}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaygroundApp() {
    AppTheme {
        Surface(modifier = Modifier, color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            val context = LocalContext.current
            val repo = ServiceLocator.provideRepository(context)
            val notificationDao = ServiceLocator.provideNotificationDao(context)

            val notificationVm: NotificationViewModel = viewModel(
                factory = NotificationViewModel.Factory(notificationDao),
            )
            val notificationItems by notificationVm.items.collectAsStateWithLifecycle()
            val unreadCount by notificationVm.unreadCount.collectAsStateWithLifecycle()

            var showSheet by remember { mutableStateOf(false) }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val scope = rememberCoroutineScope()

            val tabRoutes = Destination.values().map { it.route }.toSet()

            Scaffold(
                topBar = {
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.hierarchy
                        ?.firstOrNull()?.route
                    if (currentRoute in tabRoutes) {
                        TopAppBar(
                            title = {},
                            actions = {
                                IconButton(onClick = {
                                    showSheet = true
                                    notificationVm.markAllRead()
                                }) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadCount > 0) {
                                                Badge(
                                                    modifier = Modifier.size(8.dp),
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Notifications,
                                            contentDescription = "알림",
                                        )
                                    }
                                }
                            },
                        )
                    }
                },
                bottomBar = {
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.hierarchy
                        ?.firstOrNull()?.route
                    if (currentRoute in tabRoutes) {
                        NavigationBar {
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
                    composable(Destination.Market.route) {
                        val vm: MarketViewModel = viewModel(factory = MarketViewModel.Factory(repo))
                        MarketScreen(
                            viewModel = vm,
                            contentPadding = innerPadding,
                            onIndexClick = { index ->
                                navController.navigate(NavRoutes.chartForIndex(index))
                            },
                        )
                    }
                    composable(Destination.Watchlist.route) {
                        val vm: WatchlistViewModel = viewModel(
                            factory = WatchlistViewModel.Factory(repo),
                        )
                        WatchlistScreen(
                            viewModel = vm,
                            contentPadding = innerPadding,
                            onStockClick = { symbol ->
                                navController.navigate(NavRoutes.chartFor(symbol))
                            },
                        )
                    }
                    composable(Destination.Dashboard.route) {
                        val notifier = ServiceLocator.provideNotifier(context)
                        val settings = ServiceLocator.provideAppSettings(context)
                        val vm: DashboardViewModel = viewModel(
                            factory = DashboardViewModel.Factory(repo, notifier, settings),
                        )
                        DashboardScreen(
                            viewModel = vm,
                            contentPadding = innerPadding,
                            onStockClick = { symbol, algos ->
                                navController.navigate(NavRoutes.chartFor(symbol, algos))
                            },
                        )
                    }
                    composable(Destination.Settings.route) {
                        val vm: SettingsViewModel = viewModel(
                            factory = SettingsViewModel.Factory(context),
                        )
                        SettingsScreen(viewModel = vm, contentPadding = innerPadding)
                    }
                    composable(
                        route = NavRoutes.CHART_PATTERN,
                        arguments = listOf(
                            navArgument(NavRoutes.CHART_ARG_SYMBOL) { type = NavType.StringType },
                            navArgument(NavRoutes.CHART_ARG_ALGOS) {
                                type = NavType.StringType
                                defaultValue = AlgorithmType.MA_CROSS.name
                            },
                            navArgument(NavRoutes.CHART_ARG_NAME) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                    ) { backStackEntry ->
                        val symbol = backStackEntry.arguments?.getString(NavRoutes.CHART_ARG_SYMBOL).orEmpty()
                        val algosStr = backStackEntry.arguments?.getString(NavRoutes.CHART_ARG_ALGOS) ?: ""
                        val displayName = backStackEntry.arguments?.getString(NavRoutes.CHART_ARG_NAME)
                        val algos = algosStr.split(",")
                            .mapNotNull { runCatching { AlgorithmType.valueOf(it.trim()) }.getOrNull() }
                            .toSet()
                            .ifEmpty { setOf(AlgorithmType.MA_CROSS) }
                        val settings = ServiceLocator.provideAppSettings(context)
                        val vm: ChartViewModel = viewModel(
                            factory = ChartViewModel.Factory(repo, settings, symbol, displayName),
                        )
                        ChartScreen(
                            viewModel = vm,
                            initialAlgorithms = algos,
                            contentPadding = innerPadding,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }

            if (showSheet) {
                val notificationFilter by notificationVm.filter.collectAsStateWithLifecycle()
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = sheetState,
                ) {
                    NotificationSheetContent(
                        items = notificationItems,
                        filter = notificationFilter,
                        onTypeFilter = notificationVm::setTypeFilter,
                        onStatusFilter = notificationVm::setStatusFilter,
                        onMarketFilter = notificationVm::setMarketFilter,
                        onDelete = { id -> notificationVm.delete(id) },
                        onDeleteAll = {
                            notificationVm.deleteAll()
                            scope.launch {
                                sheetState.hide()
                                showSheet = false
                            }
                        },
                    )
                }
            }
        }
    }
}
