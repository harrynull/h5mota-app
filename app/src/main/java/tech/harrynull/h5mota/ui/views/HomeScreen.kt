package tech.harrynull.h5mota.ui.views

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import tech.harrynull.h5mota.R
import tech.harrynull.h5mota.api.MotaApi
import tech.harrynull.h5mota.models.Tower

@Composable
fun GameBox(navController: NavHostController, tower: Tower) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        onClick = {
            navController.navigate("game/${tower.name}")
        }
    ) {
        Column {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(tower.image)
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder)
                        .build(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.FillWidth,
                    contentDescription = null,
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.75F),
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(4.dp)
                            .padding(end = 1.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocalFireDepartment, null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(tower.hot.toString())
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = tower.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    Text(
                        text = tower.author,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${tower.people} 玩过 ${tower.win} 通关",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    text = tower.text,
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var towers by remember { mutableStateOf(listOf<Tower>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var pagesLoaded by remember { mutableStateOf(0) }
    var sortMode by remember { mutableStateOf(MotaApi.SortMode.Hot) }
    val listState = rememberLazyListState()

    suspend fun load() {
        // first load
        Log.i("HomeScreen", "loading page ${pagesLoaded + 1}")
        if (pagesLoaded == 0) {
            towers = MotaApi().list(page = 1, sortMode = sortMode).towers.toMutableList()
        } else {
            towers += MotaApi().list(page = pagesLoaded + 1, sortMode = sortMode).towers
        }
        pagesLoaded++
    }
    // observe list scrolling
    val reachedBottom: Boolean by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index != 0 && lastVisibleItem?.index == listState.layoutInfo.totalItemsCount - 1
        }
    }

    // load more if scrolled to bottom
    LaunchedEffect(reachedBottom) {
        if (reachedBottom) load()
    }

    LaunchedEffect(true) {
        scope.launch {
            pagesLoaded = 0
            load()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                load()
                isRefreshing = false
            }
        },
    ) {
        LazyColumn(state = listState) {
            item {
                Text(
                    "探索",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 32.dp, start = 16.dp)
                )
            }
            items(towers, key = { tower -> tower.name }) { tower ->
                GameBox(navController, tower)
            }
        }
    }
}