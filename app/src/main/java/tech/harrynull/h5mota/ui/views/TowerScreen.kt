package tech.harrynull.h5mota.ui.views

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import nl.jacobras.humanreadable.HumanReadable
import tech.harrynull.h5mota.R
import tech.harrynull.h5mota.api.MotaApi
import tech.harrynull.h5mota.api.pageUrl
import tech.harrynull.h5mota.models.Comment
import tech.harrynull.h5mota.models.Tower
import tech.harrynull.h5mota.models.TowerDetails
import tech.harrynull.h5mota.utils.DownloadManager


@Composable
fun TowerScreen(navController: NavHostController, tower: Tower) {
    val scope = rememberCoroutineScope()
    var details by remember { mutableStateOf<TowerDetails?>(null) }
    LaunchedEffect(true) {
        scope.launch {
            details = try {
                MotaApi().details(tower.name)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("game/${tower.name}/play") },
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

                    Stats(tower = tower, details = details)
                }

                details?.let { details ->
                    items(details.comments) { comment ->
                        CommentCard(comment)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DifficultyBar(description: String, value: String, width: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(description)
        Surface(
            modifier = Modifier
                .width(width.dp)
                .height(16.dp)
                .padding(start = 8.dp, end = 8.dp),
            color = MaterialTheme.colorScheme.primary
        ) { }
        Text(value)
    }
}

@Composable
fun InfoCard(name: String, value: String) {
    Card(
        modifier = Modifier
            .padding(top = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = name)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun CommentCard(comment: Comment) {
    val hasReplies = comment.replies.isNotEmpty()
    Column(Modifier.padding(horizontal = 32.dp)) {
        Row {
            AsyncImage(
                model = comment.authorAvatar,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(25.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(comment.author, style = MaterialTheme.typography.titleMedium)
                Text(HumanReadable.timeAgo(Instant.fromEpochSeconds(comment.timestamp)))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = comment.comment)
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
            Text(text = tower.title, style = MaterialTheme.typography.headlineLarge)
            // author
            Row {
                Text(text = tower.author)
                tower.author2.takeIf { it.isNotBlank() }?.let {
                    Text(text = " / $it")
                }
            }
            // tags
            Row {
                SuggestionChip(
                    onClick = {},
                    label = { Text(text = "${tower.floors} 层") },
                    enabled = false,
                    modifier = Modifier.padding(end = 8.dp),
                )
                tower.tags.forEach { tag ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(text = tag) },
                        enabled = false,
                        modifier = Modifier.padding(end = 8.dp),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        imageVector = Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = {
                    if (downloaded.value) {
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
                    InfoCard(name = "通关人数", value = tower.win.toString())
                    InfoCard(name = "精美", value = tower.thumb_up.toString())
                    InfoCard(name = "难度", value = tower.difficultrate)
                }

                details?.let { details ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        BoxWithConstraints {
                            val availableWidth =
                                this@BoxWithConstraints.maxWidth.value.toInt() * 0.75
                            val resizedWidths = details.rating.map {
                                (it / details.rating.max()
                                    .toFloat() * availableWidth).toInt()
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