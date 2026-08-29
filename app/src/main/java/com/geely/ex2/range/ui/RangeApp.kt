package com.geely.ex2.range.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_HELP = "help"

@Composable
fun RangeApp(viewModel: RangeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                TextButton(onClick = { navController.navigate(ROUTE_DASHBOARD) { launchSingleTop = true } }) {
                    Text(
                        "Главная",
                        fontWeight = if (route != ROUTE_HELP) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                TextButton(onClick = { navController.navigate(ROUTE_HELP) { launchSingleTop = true } }) {
                    Text(
                        "Справка",
                        fontWeight = if (route == ROUTE_HELP) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
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
            }
        }
    }
}
