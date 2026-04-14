package com.example.playground.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Search(route = "search", label = "검색", icon = Icons.Filled.Search),
    Watchlist(route = "watchlist", label = "관심목록", icon = Icons.Filled.List),
    Dashboard(route = "dashboard", label = "대시보드", icon = Icons.Filled.ShowChart),
    Settings(route = "settings", label = "설정", icon = Icons.Filled.Settings),
}
