package com.appropriate.riskauditor.feature

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.appropriate.riskauditor.core.model.RiskCategory
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.feature.apps.AppListScreen
import com.appropriate.riskauditor.feature.apps.AppListViewModel
import com.appropriate.riskauditor.feature.common.*
import com.appropriate.riskauditor.feature.dashboard.DashboardScreen
import com.appropriate.riskauditor.feature.dashboard.DashboardViewModel
import com.appropriate.riskauditor.feature.detail.AppDetailScreen
import com.appropriate.riskauditor.feature.detail.AppDetailViewModel
import com.appropriate.riskauditor.feature.history.HistoryScreen
import com.appropriate.riskauditor.feature.history.HistoryViewModel
import com.appropriate.riskauditor.feature.settings.SettingsScreen
import com.appropriate.riskauditor.feature.settings.SettingsViewModel

sealed class AppDestination(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : AppDestination("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Apps : AppDestination("apps", "Apps", Icons.Default.Apps)
    data object History : AppDestination("history", "History", Icons.Default.History)
    data object Settings : AppDestination("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun AppNavHost(
    dashboardViewModel: DashboardViewModel,
    appListViewModel: AppListViewModel,
    appDetailViewModel: AppDetailViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarDestinations = listOf(
        AppDestination.Dashboard,
        AppDestination.Apps,
        AppDestination.History,
        AppDestination.Settings
    )

    val showBottomBar = bottomBarDestinations.any { destination ->
        currentRoute == destination.route || currentRoute?.startsWith("${destination.route}?") == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary
                ) {
                    bottomBarDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route ||
                                (dest == AppDestination.Apps && currentRoute?.startsWith("apps") == true)

                        NavigationBarItem(
                            icon = { Icon(dest.icon, contentDescription = dest.title) },
                            label = { Text(dest.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentGreen,
                                selectedTextColor = AccentGreen,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = AccentGreen.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Dashboard
            composable(AppDestination.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToAppDetail = { pkg ->
                        val encodedPkg = Uri.encode(pkg)
                        navController.navigate("detail/$encodedPkg")
                    },
                    onNavigateToAppList = {
                        appListViewModel.clearFilters()
                        navController.navigate(AppDestination.Apps.route)
                    },
                    onNavigateToAppListWithBand = { band ->
                        appListViewModel.clearFilters()
                        appListViewModel.selectOnlyBand(band)
                        navController.navigate("apps?band=${band.name}")
                    },
                    onNavigateToAppListWithCategory = { category ->
                        appListViewModel.clearFilters()
                        appListViewModel.selectCategory(category)
                        navController.navigate("apps?category=${category.name}")
                    },
                    onNavigateToSettings = {
                        navController.navigate(AppDestination.Settings.route)
                    }
                )
            }

            // 2. Apps List
            composable(
                route = "apps?band={band}&category={category}",
                arguments = listOf(
                    navArgument("band") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("category") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val bandArg = backStackEntry.arguments?.getString("band")
                val categoryArg = backStackEntry.arguments?.getString("category")

                LaunchedEffect(bandArg, categoryArg) {
                    if (bandArg != null) {
                        try {
                            val band = RiskLevel.valueOf(bandArg)
                            appListViewModel.selectOnlyBand(band)
                        } catch (e: Exception) { /* ignore invalid arg */ }
                    }
                    if (categoryArg != null) {
                        try {
                            val cat = RiskCategory.valueOf(categoryArg)
                            appListViewModel.selectCategory(cat)
                        } catch (e: Exception) { /* ignore invalid arg */ }
                    }
                }

                AppListScreen(
                    viewModel = appListViewModel,
                    onNavigateToAppDetail = { pkg ->
                        val encodedPkg = Uri.encode(pkg)
                        navController.navigate("detail/$encodedPkg")
                    }
                )
            }

            // Fallback for simple "apps" route
            composable(AppDestination.Apps.route) {
                AppListScreen(
                    viewModel = appListViewModel,
                    onNavigateToAppDetail = { pkg ->
                        val encodedPkg = Uri.encode(pkg)
                        navController.navigate("detail/$encodedPkg")
                    }
                )
            }

            // 3. History
            composable(AppDestination.History.route) {
                HistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateToAppDetail = { pkg ->
                        val encodedPkg = Uri.encode(pkg)
                        navController.navigate("detail/$encodedPkg")
                    }
                )
            }

            // 4. Settings
            composable(AppDestination.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }

            // 5. App Detail
            composable(
                route = "detail/{packageName}",
                arguments = listOf(
                    navArgument("packageName") { type = NavType.StringType }
                )
            ) { entry ->
                val rawPkg = entry.arguments?.getString("packageName") ?: ""
                val packageName = Uri.decode(rawPkg)

                AppDetailScreen(
                    packageName = packageName,
                    viewModel = appDetailViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
