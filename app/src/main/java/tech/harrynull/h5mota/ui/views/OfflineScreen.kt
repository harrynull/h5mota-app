package tech.harrynull.h5mota.ui.views

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import tech.harrynull.h5mota.models.Tower
import tech.harrynull.h5mota.models.TowerRepo
import tech.harrynull.h5mota.utils.DownloadManager

data class OfflineScreenUiState(
    val towers: List<Tower> = listOf()
)

class OfflineScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OfflineScreenUiState())
    val uiState: StateFlow<OfflineScreenUiState> = _uiState.asStateFlow()

    fun load(context: Context) {
        if (uiState.value.towers.isNotEmpty()) {
            return
        }
        val towerRepo = TowerRepo(context)
        val downloadManager = DownloadManager(context)
        viewModelScope.launch {
            val allDownloaded = downloadManager.getAllDownloaded().map {
                towerRepo.loadTower(it)
            }
            _uiState.update { currentState ->
                currentState.copy(towers = allDownloaded)
            }
        }
    }
}

@Composable
fun OfflineScreen(
    navigateToGame: (Tower) -> Unit,
    viewModel: OfflineScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.load(LocalContext.current)

    LazyColumn {
        item {
            Row(modifier = Modifier.padding(top = 50.dp, start = 20.dp)) {
                Text("离线游戏", style = MaterialTheme.typography.headlineMedium)
            }
        }
        items(uiState.towers, key = { tower -> tower.name }) { tower ->
            GameBox(navigateToGame, tower)
        }
    }

}