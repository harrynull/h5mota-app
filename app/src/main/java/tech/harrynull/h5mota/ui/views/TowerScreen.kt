package tech.harrynull.h5mota.ui.views

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import nl.jacobras.humanreadable.HumanReadable
import tech.harrynull.h5mota.R
import tech.harrynull.h5mota.api.MotaApi
import tech.harrynull.h5mota.api.pageUrl
import tech.harrynull.h5mota.models.Comment
import tech.harrynull.h5mota.models.Tower
import tech.harrynull.h5mota.models.TowerDetails
import tech.harrynull.h5mota.models.TowerRepo
import tech.harrynull.h5mota.utils.DownloadManager

data class TowerScreenUiState(
    val tower: Tower? = null,
    val details: TowerDetails? = null,
)

class TowerScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TowerScreenUiState())
    val uiState: StateFlow<TowerScreenUiState> = _uiState.asStateFlow()

    fun load(context: Context, towerId: String) {
        if (uiState.value.tower != null) return
        viewModelScope.launch {
            val tower = TowerRepo(context).loadTower(towerId)
            _uiState.update { currentState ->
                currentState.copy(tower = tower)
            }
        }
    }

    fun loadDetails(context: Context, towerId: String) {
        if (uiState.value.details != null) return
        viewModelScope.launch {
            val details = try {
                MotaApi().details(context, towerId)
            } catch (e: Exception) {
                e.printStackTrace()
                // try fallback to local storage
                TowerRepo(context).loadTowerDetails(towerId)
            }
            _uiState.update { currentState ->
                currentState.copy(details = details)
            }
        }
    }
}

@Composable
fun TowerScreen(
    navigateToPlay: (Tower) -> Unit,
    towerId: String,
    viewModel: TowerScreenViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    LaunchedEffect(true) {
        viewModel.load(ctx, towerId)
        viewModel.loadDetails(ctx, towerId)
    }

    uiState.tower?.let { tower ->
        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { navigateToPlay(tower) },
                    icon = { Icon(Icons.Filled.PlayArrow, null) },
                    text = { Text(text = "启动") },
                )
            },
            contentWindowInsets = WindowInsets.navigationBars
        ) { _ ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                LazyColumn {
                    item {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(tower.image)
                                .crossfade(true)
                                .placeholder(R.drawable.placeholder)
                                .build(),
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth,
                            contentDescription = null,
                        )

                        // on background
                        Information(tower = tower)

                        Stats(tower = tower, details = uiState.details)
                    }

                    uiState.details?.let { details ->
                        items(details.comments) { comment ->
                            Box(modifier = Modifier.padding(end = 32.dp)) {
                                CommentCard(comment)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DifficultyBar(description: String, value: String, width: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(description, style = MaterialTheme.typography.labelMedium)
        Surface(
            modifier = Modifier
                .width(width.dp + 16.dp)
                .height(16.dp)
                .padding(start = 8.dp, end = 8.dp),
            color = MaterialTheme.colorScheme.primary
        ) { }
        Text(value, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun StatsInfoCard(name: String, value: String) {
    Card(
        modifier = Modifier.padding(top = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(horizontal = 5.dp)
        ) {
            Text(text = name)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun CommentCard(comment: Comment) {
    val hasReplies = comment.replies.isNotEmpty()
    Column(Modifier.padding(start = 32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = comment.authorAvatar,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(comment.author, style = MaterialTheme.typography.titleSmall)
                Text(
                    HumanReadable.timeAgo(Instant.fromEpochSeconds(comment.timestamp)),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = comment.comment, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (hasReplies) {
            Spacer(modifier = Modifier.height(16.dp))
            comment.replies.forEach {
                CommentCard(comment = it)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Information(tower: Tower) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceDim,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .padding(bottom = 16.dp)
        ) {
            // title
            Text(text = tower.title, style = MaterialTheme.typography.headlineMedium)
            // author
            Row {
                Text(text = tower.author)
                tower.author2.takeIf { it.isNotBlank() }?.let {
                    Text(text = " / $it")
                }
            }
            // tags
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(text = "${tower.floors} 层") },
                    enabled = false,
                )
                tower.tags.forEach { tag ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(text = tag) },
                        enabled = false,
                    )
                }
            }
            // actions
            val ctx = LocalContext.current
            val downloadManager = DownloadManager(ctx)
            val downloaded = remember {
                mutableStateOf(downloadManager.downloaded(tower))
            }
            val downloadProgress = remember { mutableStateOf<Int?>(null) }
            val scope = rememberCoroutineScope()
            val towerRepo = TowerRepo(ctx)
            var favorite by remember { mutableStateOf(false) }
            LaunchedEffect(true) { scope.launch { favorite = towerRepo.isStarred(tower) } }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // open externally
                IconButton(onClick = {
                    val browserIntent =
                        Intent(Intent.ACTION_VIEW, Uri.parse(tower.pageUrl()))
                    startActivity(ctx, browserIntent, null)
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                // favorite
                IconButton(onClick = {
                    scope.launch {
                        favorite = !favorite
                        towerRepo.setStarred(tower, favorite)
                    }
                }) {
                    Icon(
                        imageVector = if (favorite) Icons.Filled.Favorite else
                            Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                // download
                IconButton(onClick = {
                    // don't download if already downloaded or downloading
                    if (downloaded.value || downloadProgress.value != null) {
                        return@IconButton
                    }
                    downloadManager.download(tower,
                        onProgress = { progress -> downloadProgress.value = progress },
                        onCompleted = {
                            downloaded.value = downloadManager.downloaded(tower)
                            downloadProgress.value = null
                        }
                    )
                }) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = if (downloaded.value)
                            Color(0xFF43A047)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
                downloadProgress.value?.let { progress ->
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = { progress / 100f },
                    )
                }
            }
        }
    }
}

@Composable
fun Stats(tower: Tower, details: TowerDetails?) {
    Box(modifier = Modifier.offset(y = (-32).dp)) {
        Card(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.background)
        ) {
            Column(Modifier.padding(top = 32.dp, start = 32.dp, end = 32.dp)) {
                Text(text = tower.text)

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    StatsInfoCard(name = "通关人数", value = tower.win.toString())
                    StatsInfoCard(name = "精美", value = tower.thumb_up.toString())
                    StatsInfoCard(name = "难度", value = tower.difficultrate)
                }

                details?.let { details ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        BoxWithConstraints {
                            val availableWidth =
                                this@BoxWithConstraints.maxWidth.value.toInt() * 0.6
                            val resizedWidths = details.rating.map {
                                (it / details.rating.max()
                                    .toFloat() * availableWidth).toInt().coerceAtLeast(8)
                            }
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                listOf(
                                    "极难",
                                    "较难",
                                    "一般",
                                    "较易",
                                    "极易"
                                ).mapIndexed { index, s ->
                                    DifficultyBar(
                                        s,
                                        details.rating[index].toString(),
                                        resizedWidths[index]
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    "评论 (${tower.comment})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 32.dp)
                )
            }
        }
    }
}