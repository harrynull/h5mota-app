package tech.harrynull.h5mota.ui.views

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import tech.harrynull.h5mota.api.MotaApi
import tech.harrynull.h5mota.models.Tower


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    var towers by remember { mutableStateOf(listOf<Tower>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var pagesLoaded by remember { mutableStateOf(0) }
    var sortMode by remember { mutableStateOf(MotaApi.SortMode.Hot) }
    val listState = rememberLazyListState()
    val ctx = LocalContext.current

    suspend fun load() {
        try {
            // first load
            Log.i("HomeScreen", "Loading page ${pagesLoaded + 1}")
            if (pagesLoaded == 0) {
                towers =
                    MotaApi().list(ctx = ctx, page = 1, sortMode = sortMode).towers.toMutableList()
            } else {
                towers += MotaApi().list(
                    ctx = ctx,
                    page = pagesLoaded + 1,
                    sortMode = sortMode
                ).towers
            }
            pagesLoaded++
        } catch (e: Exception) {
            Log.e("HomeScreen", "Failed to load towers", e)
            snackbarHostState.showSnackbar("加载失败: ${e.localizedMessage}")
        }
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
                Row(
                    modifier = Modifier.padding(top = 32.dp, start = 16.dp)
                ) {
                    Text(
                        "探索",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    // Sort mode

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.TopStart)
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        TextButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                sortMode.displayName,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            MotaApi.SortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    onClick = {
                                        sortMode = mode
                                        expanded = false
                                        scope.launch {
                                            pagesLoaded = 0
                                            load()
                                        }
                                    }, text = {
                                        Text(mode.displayName)
                                    })
                            }
                        }
                    }
                }
            }
            items(towers, key = { tower -> tower.name }) { tower ->
                GameBox(navController, tower)
            }
        }
    }
}