package tech.harrynull.h5mota.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import nl.jacobras.humanreadable.HumanReadable
import tech.harrynull.h5mota.R
import tech.harrynull.h5mota.api.MotaApi
import tech.harrynull.h5mota.models.Comment
import tech.harrynull.h5mota.models.DetailedResponse
import tech.harrynull.h5mota.models.Tower

@Composable
fun TowerScreen(navController: NavHostController, tower: Tower) {
    val scope = rememberCoroutineScope()
    var details by remember { mutableStateOf<DetailedResponse?>(null) }
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
                text = { Text(text = "Play") },
            )
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .wrapContentHeight(),
            color = MaterialTheme.colorScheme.surfaceDim
        ) {
            Column {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(tower.image)
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder)
                        .build(),
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                    contentDescription = null,
                )

                // on background
                Column(modifier = Modifier.padding(32.dp)) {
                    Text(text = tower.title, style = MaterialTheme.typography.headlineLarge)
                    Row {
                        Text(text = tower.author)
                        tower.author2.takeIf { it.isNotBlank() }?.let {
                            Text(text = " / $it")
                        }
                    }
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
                }
                Card(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.background)
                ) {
                    Column(Modifier.padding(32.dp)) {
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
                            "Comments (${tower.comment})",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 32.dp)
                        )

//                        LazyColumn {
//                            details?.let { details ->
//                                items(details.comments) { comment ->
//                                    CommentCard(comment)
//                                }
//                            }
//                        }
                        details?.let { details ->
                            details.comments.forEach { comment ->
                                CommentCard(comment)
                            }
                        }
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
        modifier = Modifier.padding(top = 16.dp),
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
    Column(Modifier.padding(vertical = 16.dp)) {
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
                Text(comment.author)
                Text(HumanReadable.timeAgo(Instant.fromEpochSeconds(comment.timestamp)))
            }
        }
        Card(modifier = Modifier.padding(top = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = comment.comment)
            }
        }
    }
}