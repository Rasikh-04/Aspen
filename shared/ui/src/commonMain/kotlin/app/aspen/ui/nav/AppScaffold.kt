package app.aspen.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.aspen.core.i18n.SupportedLanguage
import app.aspen.design.AspenTheme
import app.aspen.design.LocalReducedMotion
import app.aspen.design.components.AspenAmbientBackground
import app.aspen.domain.safety.model.LocaleKey
import app.aspen.ui.AspenDeps
import app.aspen.ui.generated.resources.Res
import app.aspen.ui.generated.resources.nav_calm
import app.aspen.ui.generated.resources.nav_home
import app.aspen.ui.generated.resources.nav_reflect
import app.aspen.ui.generated.resources.nav_settings
import app.aspen.domain.companion.CompanionEvent
import app.aspen.ui.companion.CompanionController
import app.aspen.ui.companion.InAppCompanionLayer
import app.aspen.ui.debug.CompanionPreviewScreen
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
 * Every route sits on the shared ambient background and routes cross-fade with the motion tokens
 * (fades are the reduced-motion-safe transition, docs/06 §2).
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
    languageOverride: SupportedLanguage? = null,
    onLanguageChange: ((SupportedLanguage?) -> Unit)? = null,
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
    val motion = AspenTheme.motion

    // Phase 5 (docs/05): the in-app companion floats over the shell. The hard-moment flow turns it
    // into gentle presence; everywhere else it is ambient. It is NEVER composed on the safety
    // route — the crisis surface stays absolutely clear (CLAUDE.md #6).
    val companion = remember { CompanionController(deps.companionPrefsStore) }
    val hardMomentRoutes = setOf(Routes.CALM, Routes.BREATHE, Routes.GROUND_54321, Routes.RIDE_URGE)
    LaunchedEffect(currentRoute) {
        companion.on(
            if (currentRoute in hardMomentRoutes) CompanionEvent.HardMomentOpened else CompanionEvent.HardMomentClosed,
        )
    }

    AspenAmbientBackground {
        Box {
            Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (!showBar) return@Scaffold
                AspenBottomBar(
                    tabs = tabs,
                    currentRoute = currentRoute,
                    onSelect = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                            popUpTo(Routes.HOME)
                        }
                    },
                )
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(tween(motion.mediumMs)) },
                exitTransition = { fadeOut(tween(motion.mediumMs)) },
                popEnterTransition = { fadeIn(tween(motion.mediumMs)) },
                popExitTransition = { fadeOut(tween(motion.mediumMs)) },
            ) {
                composable(Routes.HOME) {
                    CalmHomeScreen(
                        onHardMoment = { navController.navigate(Routes.CALM) },
                        onReachPerson = { navController.navigate(Routes.SAFETY) },
                    )
                }
                composable(Routes.REFLECT) {
                    ReflectScreen(
                        loggingService = deps.loggingService,
                        reflectionCompanion = deps.reflectionCompanion,
                        onReachSomeone = { navController.navigate(Routes.SAFETY) },
                    )
                }
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
                    val canPreview = deps.isDebugBuild && deps.companionVoice != null && deps.appConfigProvider != null
                    SettingsScreen(
                        onRevisitQuestions = onRevisitQuestions,
                        languageOverride = languageOverride,
                        onLanguageChange = onLanguageChange,
                        loggingService = deps.loggingService,
                        consentManager = deps.consentManager,
                        reflectionCompanion = deps.reflectionCompanion,
                        companion = if (deps.companionPrefsStore != null) companion else null,
                        overlayControl = deps.overlayControl,
                        notificationsControl = deps.notificationsControl,
                        accountManager = deps.accountManager,
                        backupManager = deps.backupManager,
                        onOpenDebugCompanion = if (canPreview) {
                            { navController.navigate(Routes.DEBUG_COMPANION) }
                        } else {
                            null
                        },
                    )
                }
                composable(Routes.DEBUG_COMPANION) {
                    val voice = deps.companionVoice
                    val configProvider = deps.appConfigProvider
                    if (deps.isDebugBuild && voice != null && configProvider != null) {
                        CompanionPreviewScreen(
                            voice = voice,
                            appConfigProvider = configProvider,
                            safetyEngine = deps.safetyEngine,
                            crisisSignals = deps.crisisSignals,
                            onBack = { navController.popBackStack() },
                        )
                    }
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
            if (currentRoute != Routes.SAFETY) {
                InAppCompanionLayer(
                    controller = companion,
                    voice = deps.companionVoice,
                    appConfigProvider = deps.appConfigProvider,
                )
            }
        }
    }
}

/**
 * The quiet tab bar: a soft raised surface (no hard top divider) whose items are a presence dot +
 * label. Selection settles in with the motion tokens; under reduced motion it snaps.
 */
@Composable
private fun AspenBottomBar(
    tabs: List<Tab>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
) {
    val motion = AspenTheme.motion
    val reducedMotion = LocalReducedMotion.current
    Surface(color = AspenTheme.colors.surface, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .selectableGroup(),
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                val dotColor by animateColorAsState(
                    targetValue = if (selected) AspenTheme.colors.primary else AspenTheme.colors.border,
                    animationSpec = tween(motion.mediumMs),
                )
                val dotSize by animateDpAsState(
                    targetValue = if (selected) 8.dp else 6.dp,
                    animationSpec = if (reducedMotion) snap() else tween(motion.mediumMs),
                )
                val labelColor by animateColorAsState(
                    targetValue = if (selected) AspenTheme.colors.primaryDark else AspenTheme.colors.textMuted,
                    animationSpec = tween(motion.mediumMs),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab.route) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                    Spacer(Modifier.height(AspenTheme.spacing.s))
                    Text(stringResource(tab.label), style = AspenTheme.typography.caption, color = labelColor)
                }
            }
        }
    }
}
