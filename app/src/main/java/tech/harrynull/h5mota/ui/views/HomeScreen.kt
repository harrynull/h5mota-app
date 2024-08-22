package tech.harrynull.h5mota.ui.views

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.harrynull.h5mota.api.MotaApi
import tech.harrynull.h5mota.models.Tower

data class HomeScreenUiState(
    val towers: List<Tower> = listOf(),
    val sortMode: MotaApi.SortMode = MotaApi.SortMode.Hot,
    val pagesLoaded: Int = 0,
    val isRefreshing: Boolean = false,
)

class HomeScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeScreenUiState())
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    fun load(context: Context, onFailure: suspend (String) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { currentState ->
                    currentState.copy(isRefreshing = true)
                }
                val newTowers = MotaApi().list(
                    ctx = context,
                    page = uiState.value.pagesLoaded + 1,
                    sortMode = uiState.value.sortMode
                ).towers
                _uiState.update { currentState ->
                    currentState.copy(
                        towers = currentState.towers + newTowers,
                        pagesLoaded = currentState.pagesLoaded + 1,
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Failed to load towers", e)
                onFailure("加载失败: ${e.localizedMessage}")
            }
        }
    }

    fun clearLoaded() {
        _uiState.update { currentState ->
            currentState.copy(towers = listOf(), pagesLoaded = 0)
        }
    }

    fun setSortMode(mode: MotaApi.SortMode) {
        _uiState.update { currentState ->
            currentState.copy(sortMode = mode)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToGame: (Tower) -> Unit,
    viewModel: HomeScreenViewModel = viewModel(),
    snackbarHostState: SnackbarHostState,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val ctx = LocalContext.current

    fun HomeScreenViewModel.load() {
        viewModel.load(ctx) { message ->
            snackbarHostState.showSnackbar(message)
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
        if (uiState.towers.isNotEmpty() && reachedBottom) viewModel.load()
    }

    LaunchedEffect(true) {
        if (uiState.towers.isEmpty() && !uiState.isRefreshing) viewModel.load()
    }

    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = {
            viewModel.clearLoaded()
            viewModel.load()
        },
        modifier = Modifier.fillMaxWidth(),
        state = state,
        indicator = {
            Indicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp),
                isRefreshing = uiState.isRefreshing,
                state = state,
            )
        }
    ) {
        LazyColumn(state = listState) {
            item {
                Row(
                    modifier = Modifier.padding(top = 50.dp, start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("探索", style = MaterialTheme.typography.headlineMedium)
                    // Sort mode
                    Box(
                        modifier = Modifier
                            .wrapContentSize(Alignment.TopStart)
                            .padding(start = 8.dp)
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(
                                uiState.sortMode.displayName,
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
                                        viewModel.setSortMode(mode)
                                        expanded = false
                                        viewModel.clearLoaded()
                                        viewModel.load()
                                    }, text = {
                                        Text(mode.displayName)
                                    })
                            }
                        }
                    }
                }
            }
            items(uiState.towers, key = { tower -> tower.name }) { tower ->
                GameBox(navigateToGame, tower)
            }
        }
    }
}