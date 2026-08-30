package com.geely.ex2.range.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.geely.ex2.range.ui.dashboard.DashboardScreen
import com.geely.ex2.range.ui.help.HelpScreen
import com.geely.ex2.range.ui.settings.SettingsScreen

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_HELP = "help"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun RangeApp(viewModel: RangeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "EX2 Расход",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.Center) {
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
                Spacer(Modifier.weight(1f))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavHost(
                navController = navController,
                startDestination = ROUTE_DASHBOARD,
                modifier = Modifier.fillMaxSize(),
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
                    SettingsScreen(state = state)
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
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
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
