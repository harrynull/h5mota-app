package tech.harrynull.h5mota.ui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import tech.harrynull.h5mota.R
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
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
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