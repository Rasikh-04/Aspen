package app.aspen.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.aspen.design.AspenTheme
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.nav_calm
import app.aspen.ui.generated.resources.nav_home
import app.aspen.ui.generated.resources.nav_reflect
import app.aspen.ui.generated.resources.nav_settings
import app.aspen.ui.home.CalmHomeScreen
import app.aspen.ui.screen.CalmScreen
import app.aspen.ui.screen.ReflectScreen
import app.aspen.ui.screen.SafetyPlaceholderScreen
import app.aspen.ui.screen.SettingsScreen

private data class Tab(val route: String, val label: StringResource)

/**
 * The shared navigation shell (docs/06 §4). A calm bottom presence (Home / Reflect / Calm / Settings)
 * with a soft dot indicator instead of loud icons. Safety lives in the graph but NOT in the bar —
 * it is reached as an affordance from Home (CLAUDE.md #6).
 */
@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    val tabs = listOf(
        Tab(Routes.HOME, Res.string.nav_home),
        Tab(Routes.REFLECT, Res.string.nav_reflect),
        Tab(Routes.CALM, Res.string.nav_calm),
        Tab(Routes.SETTINGS, Res.string.nav_settings),
    )

    Scaffold(
        containerColor = AspenTheme.colors.background,
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar(containerColor = AspenTheme.colors.surface) {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                launchSingleTop = true
                                popUpTo(Routes.HOME)
                            }
                        },
                        icon = {
                            val dot = if (selected) AspenTheme.colors.primary else AspenTheme.colors.border
                            Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
                        },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                CalmHomeScreen(
                    onHardMoment = { navController.navigate(Routes.CALM) },
                    onReachPerson = { navController.navigate(Routes.SAFETY) },
                )
            }
            composable(Routes.REFLECT) { ReflectScreen() }
            composable(Routes.CALM) { CalmScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.SAFETY) {
                SafetyPlaceholderScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
