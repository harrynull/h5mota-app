package tech.harrynull.h5mota

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import tech.harrynull.h5mota.models.TowerRepo
import tech.harrynull.h5mota.ui.theme.H5motaTheme
import tech.harrynull.h5mota.ui.views.HomeScreen
import tech.harrynull.h5mota.ui.views.PlayScreen
import tech.harrynull.h5mota.ui.views.TowerScreen

sealed class NavigationItem(val route: String, val title: String, val icon: ImageVector?) {
    data object Home : NavigationItem("home", "探索", Icons.Rounded.Home)
    data object Recent : NavigationItem("recent", "最近", Icons.Rounded.History)
    data object Favorite : NavigationItem("favorite", "收藏", Icons.Rounded.Favorite)
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = NavigationItem.Home.route
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavigationItem.Home.route) {
            HomeScreen(navController = navController)
        }
        composable("game/{id}") { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("id")!!
            TowerScreen(navController = navController, tower = TowerRepo.getTower(gameId))
        }
        composable("game/{id}/play") { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("id")!!
            PlayScreen(tower = TowerRepo.getTower(gameId))
        }
    }
}

@Composable
fun AppNavigationBar(navController: NavHostController) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Recent,
        NavigationItem.Favorite
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                label = { Text(text = item.title) },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                icon = {
                    Icon(
                        imageVector = item.icon!!,
                        contentDescription = item.title
                    )
                },
                onClick = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            H5motaTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        AppNavigationBar(navController = navController)
                    },
                    contentWindowInsets = WindowInsets.navigationBars
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
