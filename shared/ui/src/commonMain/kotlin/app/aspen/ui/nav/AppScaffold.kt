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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import app.aspen.domain.safety.model.LocaleKey
import app.aspen.ui.AspenDeps
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.nav_calm
import app.aspen.ui.generated.resources.nav_home
import app.aspen.ui.generated.resources.nav_reflect
import app.aspen.ui.generated.resources.nav_settings
import app.aspen.ui.grounding.BreatheScreen
import app.aspen.ui.grounding.Ground54321Screen
import app.aspen.ui.grounding.GroundingChooser
import app.aspen.ui.grounding.RideTheUrgeScreen
import app.aspen.ui.home.CalmHomeScreen
import app.aspen.ui.reflect.ReflectScreen
import app.aspen.ui.screen.SafetyPlaceholderScreen
import app.aspen.ui.screen.SafetyScreen
import app.aspen.ui.settings.SettingsScreen

private data class Tab(val route: String, val label: StringResource)

/**
 * The shared navigation shell (docs/06 §4). A calm bottom presence (Home / Reflect / Calm / Settings)
 * with a soft dot indicator. Safety lives in the graph but NOT in the bar — it is reached as an
 * affordance from Home (CLAUDE.md #6). Grounding tools are full-screen routes with the bar hidden.
 *
 * [startAtSafety] deep-links once to Flow C (used when onboarding's closing screen routes to help).
 * [onRevisitQuestions] re-opens the questionnaire (Settings → Flow 0.4).
 */
@Composable
fun AppScaffold(
    deps: AspenDeps = AspenDeps(),
    startAtSafety: Boolean = false,
    onConsumedStart: () -> Unit = {},
    onRevisitQuestions: () -> Unit = {},
) {
    val navController = rememberNavController()
    val tabs = listOf(
        Tab(Routes.HOME, Res.string.nav_home),
        Tab(Routes.REFLECT, Res.string.nav_reflect),
        Tab(Routes.CALM, Res.string.nav_calm),
        Tab(Routes.SETTINGS, Res.string.nav_settings),
    )

    LaunchedEffect(startAtSafety) {
        if (startAtSafety) {
            navController.navigate(Routes.SAFETY)
            onConsumedStart()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBar = currentRoute == null || currentRoute in Routes.tabRoutes

    Scaffold(
        containerColor = AspenTheme.colors.background,
        bottomBar = {
            if (!showBar) return@Scaffold
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
            composable(Routes.REFLECT) { ReflectScreen(loggingService = deps.loggingService) }
            composable(Routes.CALM) {
                GroundingChooser(
                    onBreathe = { navController.navigate(Routes.BREATHE) },
                    onGround54321 = { navController.navigate(Routes.GROUND_54321) },
                    onRideUrge = { navController.navigate(Routes.RIDE_URGE) },
                    onWriteItDown = { navController.navigate(Routes.REFLECT) },
                    onReachSomeone = { navController.navigate(Routes.SAFETY) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onRevisitQuestions = onRevisitQuestions, loggingService = deps.loggingService)
            }
            composable(Routes.BREATHE) { BreatheScreen(onExit = { navController.popBackStack() }) }
            composable(Routes.GROUND_54321) { Ground54321Screen(onExit = { navController.popBackStack() }) }
            composable(Routes.RIDE_URGE) { RideTheUrgeScreen(onExit = { navController.popBackStack() }) }
            composable(Routes.SAFETY) {
                val onBack = { navController.popBackStack(); Unit }
                val resolver = deps.crisisResolver
                if (resolver == null) {
                    SafetyPlaceholderScreen(onBack = onBack)
                } else {
                    // Region is an explicit user choice, independent of UI language (CLAUDE.md #11).
                    var region by remember { mutableStateOf(LocaleKey.INTL) }
                    SafetyScreen(
                        resources = resolver.resolve(region),
                        selectedRegion = region,
                        onRegionChange = { region = it },
                        onContact = { /* Phase 2: contacts are TODO-VERIFY and non-actionable. */ },
                        onReachTrustedPerson = { /* Phase 2: trusted-contact capture lands with consent UI. */ },
                        onBack = onBack,
                    )
                }
            }
        }
    }
}
