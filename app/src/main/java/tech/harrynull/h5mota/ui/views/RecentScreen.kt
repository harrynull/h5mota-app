package tech.harrynull.h5mota.ui.views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import tech.harrynull.h5mota.models.Tower
import tech.harrynull.h5mota.models.TowerRepo


@Composable
fun RecentScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var towers by remember { mutableStateOf(listOf<Tower>()) }
    val towerRepo = TowerRepo(LocalContext.current)
    LaunchedEffect(true) {
        scope.launch { towers = towerRepo.getRecent() }
    }

    LazyColumn {
        item {
            Row(modifier = Modifier.padding(top = 32.dp, start = 16.dp)) {
                Text("最近游玩", style = MaterialTheme.typography.headlineMedium)
            }
        }
        items(towers, key = { tower -> tower.name }) { tower ->
            GameBox(navController, tower)
        }
    }

}