package com.appropriate.riskauditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.appropriate.riskauditor.app.Screen
import com.appropriate.riskauditor.app.theme.*
import com.appropriate.riskauditor.feature.apps.AppDetailScreen
import com.appropriate.riskauditor.feature.apps.AppDetailViewModel
import com.appropriate.riskauditor.feature.apps.AppListScreen
import com.appropriate.riskauditor.feature.apps.AppListViewModel
import com.appropriate.riskauditor.feature.dashboard.DashboardScreen
import com.appropriate.riskauditor.feature.dashboard.DashboardViewModel
import com.appropriate.riskauditor.feature.history.HistoryScreen
import com.appropriate.riskauditor.feature.history.HistoryViewModel
import com.appropriate.riskauditor.feature.settings.SettingsScreen
import com.appropriate.riskauditor.feature.settings.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val dashboardViewModel by viewModels<DashboardViewModel>()
    private val appListViewModel by viewModels<AppListViewModel>()
    private val appDetailViewModel by viewModels<AppDetailViewModel>()
    private val historyViewModel by viewModels<HistoryViewModel>()
    private val settingsViewModel by viewModels<SettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RiskAuditorTheme {
                MainAppNavigation(
                    dashboardViewModel = dashboardViewModel,
                    appListViewModel = appListViewModel,
                    appDetailViewModel = appDetailViewModel,
                    historyViewModel = historyViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppNavigation(
    dashboardViewModel: DashboardViewModel,
    appListViewModel: AppListViewModel,
    appDetailViewModel: AppDetailViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavScreens = listOf(
        Screen.Dashboard,
        Screen.AppList,
        Screen.History,
        Screen.Settings
    )

    val showBottomBar = currentRoute in bottomNavScreens.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary
                ) {
                    bottomNavScreens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
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
                                indicatorColor = DarkSurfaceVariant,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        },
        containerColor = DarkBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToAppDetail = { packageName ->
                        navController.navigate(Screen.AppDetail.createRoute(packageName))
                    },
                    onNavigateToAppList = {
                        navController.navigate(Screen.AppList.route)
                    }
                )
            }

            composable(Screen.AppList.route) {
                AppListScreen(
                    viewModel = appListViewModel,
                    onNavigateToAppDetail = { packageName ->
                        navController.navigate(Screen.AppDetail.createRoute(packageName))
                    }
                )
            }

            composable(
                route = Screen.AppDetail.route,
                arguments = listOf(navArgument("packageName") { type = NavType.StringType })
            ) { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                AppDetailScreen(
                    packageName = packageName,
                    viewModel = appDetailViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(viewModel = historyViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
