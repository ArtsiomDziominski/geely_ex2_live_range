package com.geely.ex2.range.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.SoundEffectConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.geely.ex2.range.R
import com.geely.ex2.range.ui.dashboard.DashboardScreen
import com.geely.ex2.range.ui.help.HelpScreen
import com.geely.ex2.range.ui.layout.LocalRangeLayout
import com.geely.ex2.range.ui.layout.ProvideRangeLayout
import com.geely.ex2.range.ui.settings.SettingsScreen
import com.geely.ex2.range.ui.theme.RangeTheme
import com.geely.ex2.range.ui.theme.Spacing
import com.geely.ex2.range.ui.theme.resolveDarkTheme
import com.geely.ex2.range.ui.trips.TripsScreen

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_TRIPS = "trips"
private const val ROUTE_HELP = "help"
private const val ROUTE_SETTINGS = "settings"

@Immutable
private data class Destination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

private val Destinations = listOf(
    Destination(ROUTE_DASHBOARD, "Главная", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    Destination(ROUTE_TRIPS, "Поездки", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    Destination(ROUTE_HELP, "Справка", Icons.AutoMirrored.Filled.Help, Icons.AutoMirrored.Outlined.HelpOutline),
    Destination(ROUTE_SETTINGS, "Настройки", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeApp(viewModel: RangeViewModel = viewModel(), navigateHomeSignal: Int = 0) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val context = LocalContext.current
    val view = LocalView.current
    var showAppInfo by remember { mutableStateOf(false) }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.onOverlayPermissionResult()
    }

    fun requestOverlayPermission() {
        overlayPermissionLauncher.launch(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ),
        )
    }

    fun onOverlayEnabledChange(enabled: Boolean) {
        if (enabled) {
            if (!viewModel.setOverlayEnabled(true)) {
                requestOverlayPermission()
            }
        } else {
            viewModel.setOverlayEnabled(false)
        }
    }

    fun navigateTab(destination: String) {
        if (route == destination) return
        view.playSoundEffect(SoundEffectConstants.CLICK)
        navController.navigate(destination) {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
        }
    }

    // Виджет поверх экрана открывает уже запущенное приложение через onNewIntent —
    // сигнал растёт при каждом таком запросе, и мы переключаемся на главную вкладку.
    LaunchedEffect(navigateHomeSignal) {
        if (navigateHomeSignal > 0) {
            navigateTab(ROUTE_DASHBOARD)
        }
    }

    val darkTheme = resolveDarkTheme(settings.themeMode)
    // Иконки системных панелей должны следовать теме приложения, а не системы.
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    RangeTheme(darkTheme = darkTheme) {
        ProvideRangeLayout {
            val layout = LocalRangeLayout.current
            val screenInsets = WindowInsets.systemBars.union(WindowInsets.tappableElement)
            val insetPadding = screenInsets.asPaddingValues()
            // Часть прошивок ГУ отдаёт нулевой нижний инсет, но рисует док ~40–80 px.
            val fallbackBottom = if (
                layout.useNavigationRail &&
                layout.isLandscape &&
                insetPadding.calculateBottomPadding() < 8.dp
            ) {
                40.dp
            } else {
                0.dp
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.app_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { showAppInfo = true },
                                modifier = Modifier.size(Spacing.touchTarget),
                            ) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = stringResource(
                                        R.string.app_info_content_description,
                                    ),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                    )
                },
                bottomBar = {
                    if (!layout.useNavigationRail) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            Destinations.forEach { destination ->
                                val selected = route == destination.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTab(destination.route) },
                                    icon = {
                                        Icon(
                                            if (selected) destination.selectedIcon else destination.icon,
                                            contentDescription = destination.label,
                                        )
                                    },
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(bottom = fallbackBottom),
                ) {
                    if (layout.useNavigationRail) {
                        NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                            Destinations.forEach { destination ->
                                val selected = route == destination.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTab(destination.route) },
                                    icon = {
                                        Icon(
                                            if (selected) destination.selectedIcon else destination.icon,
                                            contentDescription = destination.label,
                                        )
                                    },
                                )
                            }
                        }
                    }
                    RangeNavHost(
                        navController = navController,
                        viewModel = viewModel,
                        onOverlayEnabledChange = { enabled -> onOverlayEnabledChange(enabled) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (showAppInfo) {
                AppInfoDialog(onDismiss = { showAppInfo = false })
            }
        }
    }
}

@Composable
private fun RangeNavHost(
    navController: NavHostController,
    viewModel: RangeViewModel,
    onOverlayEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    NavHost(
        navController = navController,
        startDestination = ROUTE_DASHBOARD,
        modifier = modifier,
    ) {
        composable(ROUTE_DASHBOARD) {
            val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
            DashboardScreen(
                engine = dashboard.engine,
                connectError = dashboard.connectError,
                onResetPeriod = viewModel::resetPeriod,
                onRetry = viewModel::retry,
            )
        }
        composable(ROUTE_TRIPS) {
            val trips by viewModel.trips.collectAsStateWithLifecycle()
            TripsScreen(
                state = trips,
                onDeleteTrip = viewModel::deleteTrip,
                onClearTrips = viewModel::clearTrips,
            )
        }
        composable(ROUTE_HELP) {
            val help by viewModel.help.collectAsStateWithLifecycle()
            HelpScreen(
                usableCapacityKwh = help.usableCapacityKwh,
                engine = help.engine,
                raw = help.raw,
                onCapacityChange = viewModel::setUserCapacityKwh,
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                settings = settings,
                onOverlayEnabledChange = onOverlayEnabledChange,
                onThemeModeChange = viewModel::setThemeMode,
            )
        }
    }
}
