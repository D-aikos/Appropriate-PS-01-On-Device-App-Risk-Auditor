package com.appropriate.riskauditor.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object AppList : Screen("app_list", "Apps", Icons.Default.Apps)
    object History : Screen("history", "History", Icons.Default.History)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object AppDetail : Screen("app_detail/{packageName}", "Detail", Icons.Default.Apps) {
        fun createRoute(packageName: String) = "app_detail/$packageName"
    }
}
