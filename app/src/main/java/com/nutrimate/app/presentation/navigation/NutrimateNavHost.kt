package com.nutrimate.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nutrimate.app.R
import com.nutrimate.app.presentation.grocery.GroceryScreen
import com.nutrimate.app.presentation.home.HomeScreen
import com.nutrimate.app.presentation.recipe.RecipeScreen

/** Bottom-level destinations for the 4-tab shell. */
object Destinations {
    const val HOME = "home"
    const val LOG_MEAL = "log_meal"
    const val RECIPE = "recipe"
    const val GROCERY = "grocery"
}

@Composable
fun NutrimateNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    Destinations.HOME to R.string.tab_home,
                    Destinations.LOG_MEAL to R.string.tab_log_meal,
                    Destinations.RECIPE to R.string.tab_recipe,
                    Destinations.GROCERY to R.string.tab_grocery
                ).forEach { (route, labelRes) ->
                    val label = stringResource(labelRes)
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
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
            composable(Destinations.HOME) { HomeScreen() }
            composable(Destinations.LOG_MEAL) { HomeScreen(mode = "log_meal_placeholder") }
            composable(Destinations.RECIPE) { RecipeScreen() }
            composable(Destinations.GROCERY) { GroceryScreen() }
        }
    }
}