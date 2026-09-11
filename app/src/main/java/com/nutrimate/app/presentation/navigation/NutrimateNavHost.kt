package com.nutrimate.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nutrimate.app.R
import com.nutrimate.app.presentation.grocery.GroceryScreen
import com.nutrimate.app.presentation.home.HomeScreen
import com.nutrimate.app.presentation.logmeal.LogMealScreen
import com.nutrimate.app.presentation.onboarding.OnboardingScreen
import com.nutrimate.app.presentation.recipe.RecipeScreen

/** Navigation routes. */
object Destinations {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val LOG_MEAL = "log_meal"
    const val RECIPE = "recipe"
    const val GROCERY = "grocery"
}

/**
 * App shell. When onboarding has not completed we start at the wizard;
 * otherwise we show the 4-tab scaffold (home / log flow / recipe / grocery).
 */
@Composable
fun NutrimateNavHost(
    onboardingDone: Boolean
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    androidx.compose.runtime.LaunchedEffect(onboardingDone) {
        if (onboardingDone) {
            // ensure we never land on onboarding once done
            if (currentDestination?.route == Destinations.ONBOARDING) {
                navController.navigate(Destinations.HOME) {
                    popUpTo(Destinations.ONBOARDING) { inclusive = true }
                }
            }
        }
    }

    if (!onboardingDone) {
        NavHost(
            navController = navController,
            startDestination = Destinations.ONBOARDING
        ) {
            composable(Destinations.ONBOARDING) {
                OnboardingScreen(onDone = {
                    // Once saved, onboardingDone flow flips and this composable
                    // is replaced by the shell via the conditional above.
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.ONBOARDING) { inclusive = true }
                    }
                })
            }
        }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    Destinations.HOME to R.string.tab_home,
                    Destinations.RECIPE to R.string.tab_recipe,
                    Destinations.GROCERY to R.string.tab_grocery
                ).forEach { (route, labelRes) ->
                    val label = stringResource(labelRes)
                    NavigationBarItem(
                        selected = currentDestination?.route == route,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(label.take(1), style = MaterialTheme.typography.titleMedium) },
                        label = { Text(label, maxLines = 1) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destinations.HOME) {
                HomeScreen(
                    onLogMeal = { navController.navigate(Destinations.LOG_MEAL) },
                    onEatWhat = {
                        navController.navigate(Destinations.RECIPE) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Destinations.LOG_MEAL) {
                LogMealScreen(onDone = { navController.popBackStack() })
            }
            composable(Destinations.RECIPE) { RecipeScreen() }
            composable(Destinations.GROCERY) { GroceryScreen() }
        }
    }
}