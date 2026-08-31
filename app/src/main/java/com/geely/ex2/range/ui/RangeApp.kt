package com.geely.ex2.range.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geely.ex2.range.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.geely.ex2.range.ui.dashboard.DashboardScreen
import com.geely.ex2.range.ui.help.HelpScreen
import com.geely.ex2.range.ui.settings.SettingsScreen
import com.geely.ex2.range.ui.theme.RangeTheme
import com.geely.ex2.range.ui.theme.resolveDarkTheme

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_HELP = "help"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun RangeApp(viewModel: RangeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val context = LocalContext.current
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

    val configuration = LocalConfiguration.current
    val screenInsets = WindowInsets.systemBars.union(WindowInsets.tappableElement)
    val insetPadding = screenInsets.asPaddingValues()
    val fallbackBottomPadding = if (
        insetPadding.calculateBottomPadding() < 24.dp &&
        configuration.screenWidthDp >= 960
    ) {
        88.dp
    } else {
        0.dp
    }

    RangeTheme(darkTheme = resolveDarkTheme(state.settings.themeMode)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(screenInsets)
                .padding(bottom = fallbackBottomPadding),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TabLabel(
                        title = "Главная",
                        selected = route == ROUTE_DASHBOARD,
                        onClick = { navController.navigate(ROUTE_DASHBOARD) { launchSingleTop = true } },
                    )
                    Spacer(Modifier.width(8.dp))
                    TabLabel(
                        title = "Справка",
                        selected = route == ROUTE_HELP,
                        onClick = { navController.navigate(ROUTE_HELP) { launchSingleTop = true } },
                    )
                    Spacer(Modifier.width(8.dp))
                    TabLabel(
                        title = "Настройки",
                        selected = route == ROUTE_SETTINGS,
                        onClick = { navController.navigate(ROUTE_SETTINGS) { launchSingleTop = true } },
                    )
                }
                Text(
                    stringResource(R.string.app_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center),
                )
                IconButton(
                    onClick = { showAppInfo = true },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(40.dp),
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.app_info_content_description),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (showAppInfo) {
                AppInfoDialog(onDismiss = { showAppInfo = false })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavHost(
                navController = navController,
                startDestination = ROUTE_DASHBOARD,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                composable(ROUTE_DASHBOARD) {
                    DashboardScreen(
                        state = state,
                        onResetPeriod = viewModel::resetPeriod,
                    )
                }
                composable(ROUTE_HELP) {
                    HelpScreen(
                        state = state,
                        onCapacityChange = viewModel::setUserCapacityKwh,
                    )
                }
                composable(ROUTE_SETTINGS) {
                    SettingsScreen(
                        state = state,
                        onOverlayEnabledChange = { enabled -> onOverlayEnabledChange(enabled) },
                        onThemeModeChange = viewModel::setThemeMode,
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun TabLabel(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                fontSize = 14.sp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .height(2.dp)
                    .width(48.dp),
            ) {
                if (selected) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                    ) {}
                }
            }
        }
    }
}
