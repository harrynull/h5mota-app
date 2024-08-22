package tech.harrynull.h5mota

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import tech.harrynull.h5mota.models.Tower
import tech.harrynull.h5mota.models.TowerRepo
import tech.harrynull.h5mota.ui.theme.H5motaTheme
import tech.harrynull.h5mota.ui.views.FavoriteScreen
import tech.harrynull.h5mota.ui.views.HomeScreen
import tech.harrynull.h5mota.ui.views.OfflineScreen
import tech.harrynull.h5mota.ui.views.PlayScreen
import tech.harrynull.h5mota.ui.views.RecentScreen
import tech.harrynull.h5mota.ui.views.TowerScreen

sealed class NavigationItem(val route: String, val title: String, val icon: ImageVector?) {
    data object Home : NavigationItem("home", "探索", Icons.Rounded.Home)
    data object Recent : NavigationItem("recent", "最近", Icons.Rounded.History)
    data object Favorite : NavigationItem("favorite", "收藏", Icons.Rounded.Favorite)
    data object Offline : NavigationItem("offline", "离线", Icons.Rounded.CloudOff)
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    startDestination: String = NavigationItem.Home.route
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val navigateToGame = { tower: Tower ->
        navController.navigate("game/${tower.name}")
    }
    val navigateToPlay = { tower: Tower ->
        navController.navigate("game/${tower.name}/play")
    }
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {
        composable(NavigationItem.Home.route) {
            HomeScreen(navigateToGame = navigateToGame, snackbarHostState = snackbarHostState)
        }
        composable("game/{id}") { backStackEntry ->
            TowerScreen(
                navigateToPlay = navigateToPlay,
                towerId = backStackEntry.arguments?.getString("id")!!
            )
        }
        composable("game/{id}/play") { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("id")!!
            var tower by remember { mutableStateOf<Tower?>(null) }
            LaunchedEffect(true) {
                scope.launch { tower = TowerRepo(ctx).loadTower(gameId) }
            }
            tower?.let { PlayScreen(tower = it) }
        }
        composable("recent") {
            RecentScreen(navigateToGame = navigateToGame)
        }
        composable("favorite") {
            FavoriteScreen(navigateToGame = navigateToGame)
        }
        composable("offline") {
            OfflineScreen(navigateToGame = navigateToGame)
        }
    }
}

@Composable
fun AppNavigationBar(navController: NavHostController) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Recent,
        NavigationItem.Favorite,
        NavigationItem.Offline,
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                label = { Text(text = item.title) },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                icon = { Icon(imageVector = item.icon!!, contentDescription = item.title) },
                onClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route)
                        }
                        launchSingleTop = true
                        restoreState = true
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
            val snackbarHostState = remember { SnackbarHostState() }

            H5motaTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = { AppNavigationBar(navController = navController) },
                    contentWindowInsets = WindowInsets.navigationBars
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
