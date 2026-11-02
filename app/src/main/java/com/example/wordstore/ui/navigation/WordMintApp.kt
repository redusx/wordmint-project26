package com.example.wordstore.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.wordstore.AppContainer
import com.example.wordstore.R
import com.example.wordstore.domain.repository.PackEvent
import com.example.wordstore.domain.repository.PackFailure
import com.example.wordstore.ui.components.CanvasBackground
import com.example.wordstore.ui.components.GlassNavigationBar
import com.example.wordstore.ui.components.NavigationItem
import com.example.wordstore.ui.format.languageName
import com.example.wordstore.ui.screens.detail.WordDetailScreen
import com.example.wordstore.ui.screens.dictionary.DictionaryScreen
import com.example.wordstore.ui.screens.library.LibraryScreen
import com.example.wordstore.ui.screens.onboarding.OnboardingScreen
import com.example.wordstore.ui.screens.progress.ProgressScreen
import com.example.wordstore.ui.screens.settings.SettingsScreen
import com.example.wordstore.ui.screens.study.StudyScreen
import com.example.wordstore.ui.screens.today.TodayScreen
import com.example.wordstore.ui.theme.AppIcons
import com.example.wordstore.ui.theme.Motion
import com.example.wordstore.ui.theme.Radii
import com.example.wordstore.ui.theme.Spacing
import com.example.wordstore.ui.theme.WordMintTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

object Routes {
    const val ONBOARDING = "onboarding"
    const val TODAY = "today"
    const val DICTIONARY = "dictionary"
    const val LIBRARY = "library"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val STUDY = "study"
    const val WORD = "word/{wordId}"

    fun word(id: Long) = "word/$id"
}

private val TopLevelRoutes = setOf(Routes.TODAY, Routes.DICTIONARY, Routes.LIBRARY, Routes.PROGRESS)

@Composable
fun WordMintApp(
    container: AppContainer,
    onboardingDone: Boolean,
    openStudyRequest: Boolean,
    onStudyRequestHandled: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val hazeState = remember { HazeState() }
    val snackbars = remember { SnackbarHostState() }
    val context = LocalContext.current

    val startDestination = remember { if (onboardingDone) Routes.TODAY else Routes.ONBOARDING }

    LaunchedEffect(openStudyRequest, onboardingDone, currentRoute) {
        if (openStudyRequest && onboardingDone && currentRoute != null) {
            if (currentRoute != Routes.STUDY) navController.navigate(Routes.STUDY) { launchSingleTop = true }
            onStudyRequestHandled()
        }
    }

    LaunchedEffect(Unit) {
        container.languagePacks.events.collect { event ->
            val message = when (event) {
                is PackEvent.Downloaded -> context.getString(R.string.pack_ready, languageName(event.code))
                is PackEvent.Failed -> context.getString(
                    when (event.reason) {
                        PackFailure.NETWORK -> R.string.pack_failed_network
                        PackFailure.WIFI_REQUIRED -> R.string.pack_failed_wifi
                        PackFailure.STORAGE -> R.string.pack_failed_storage
                        PackFailure.BUSY -> R.string.pack_failed_busy
                        PackFailure.TIMEOUT -> R.string.pack_failed_timeout
                        PackFailure.UNKNOWN -> R.string.pack_failed_unknown
                    },
                    languageName(event.code)
                )
            }
            snackbars.showSnackbar(message)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().haze(hazeState)) {
            CanvasBackground {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    // The old screen leaves first, so two screens of text never overlap.
                    enterTransition = { fadeIn(tween(Motion.STANDARD, delayMillis = Motion.QUICK)) },
                    exitTransition = { fadeOut(tween(Motion.QUICK)) },
                    popEnterTransition = { fadeIn(tween(Motion.STANDARD, delayMillis = Motion.QUICK)) },
                    popExitTransition = { fadeOut(tween(Motion.QUICK)) }
                ) {
                    composable(Routes.ONBOARDING) {
                        OnboardingScreen(
                            container = container,
                            onFinished = {
                                navController.navigate(Routes.TODAY) {
                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Routes.TODAY) {
                        TodayScreen(
                            container = container,
                            onStudy = { navController.navigate(Routes.STUDY) },
                            onLookUp = { navController.navigateTopLevel(Routes.DICTIONARY) },
                            onSettings = { navController.navigate(Routes.SETTINGS) },
                            onOpenWord = { navController.navigate(Routes.word(it)) }
                        )
                    }
                    composable(Routes.DICTIONARY) {
                        DictionaryScreen(
                            container = container,
                            onOpenWord = { navController.navigate(Routes.word(it)) }
                        )
                    }
                    composable(Routes.LIBRARY) {
                        LibraryScreen(
                            container = container,
                            onOpenWord = { navController.navigate(Routes.word(it)) },
                            onLookUp = { navController.navigateTopLevel(Routes.DICTIONARY) }
                        )
                    }
                    composable(Routes.PROGRESS) {
                        ProgressScreen(
                            container = container,
                            onOpenWord = { navController.navigate(Routes.word(it)) }
                        )
                    }
                    composable(
                        Routes.SETTINGS,
                        enterTransition = { pushIn() },
                        popExitTransition = { pushOut() }
                    ) {
                        SettingsScreen(container = container, onBack = { navController.popBackStack() })
                    }
                    composable(
                        Routes.STUDY,
                        enterTransition = { pushIn() },
                        popExitTransition = { pushOut() }
                    ) {
                        LaunchedEffect(Unit) { container.reminderNotifier.cancel() }
                        StudyScreen(
                            container = container,
                            onExit = { navController.popBackStack() },
                            onLookUp = {
                                navController.popBackStack()
                                navController.navigateTopLevel(Routes.DICTIONARY)
                            }
                        )
                    }
                    composable(
                        Routes.WORD,
                        arguments = listOf(navArgument("wordId") { type = NavType.LongType }),
                        enterTransition = { pushIn() },
                        popExitTransition = { pushOut() }
                    ) { entry ->
                        WordDetailScreen(
                            container = container,
                            wordId = entry.arguments?.getLong("wordId") ?: 0L,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

        if (currentRoute in TopLevelRoutes) {
            GlassNavigationBar(
                items = listOf(
                    NavigationItem(Routes.TODAY, stringResource(R.string.nav_today), AppIcons.Today),
                    NavigationItem(Routes.DICTIONARY, stringResource(R.string.nav_dictionary), AppIcons.Dictionary),
                    NavigationItem(Routes.LIBRARY, stringResource(R.string.nav_words), AppIcons.Words),
                    NavigationItem(Routes.PROGRESS, stringResource(R.string.nav_progress), AppIcons.Progress)
                ),
                selectedRoute = currentRoute,
                hazeState = hazeState,
                onSelect = { navController.navigateTopLevel(it.route) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        SnackbarHost(
            hostState = snackbars,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (currentRoute in TopLevelRoutes) Spacing.navigationClearance else Spacing.l)
        ) { data ->
            val colors = WordMintTheme.colors
            Snackbar(
                snackbarData = data,
                shape = Radii.m,
                containerColor = colors.ink,
                contentColor = colors.canvas
            )
        }
    }
}

private fun androidx.navigation.NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.TODAY) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun pushIn(): EnterTransition =
    slideInHorizontally(tween(Motion.STANDARD, easing = Motion.Emphasized)) { it / 5 } + fadeIn(tween(Motion.STANDARD))

private fun pushOut(): ExitTransition =
    slideOutHorizontally(tween(Motion.QUICK)) { it / 5 } + fadeOut(tween(Motion.QUICK))
