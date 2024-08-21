package tech.harrynull.h5mota.api

import android.content.Context
import android.text.Html
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import tech.harrynull.h5mota.models.Comment
import tech.harrynull.h5mota.models.Tower
import tech.harrynull.h5mota.models.TowerDetails
import tech.harrynull.h5mota.models.TowerRepo
import tech.harrynull.h5mota.models.TowerResponse

val StyleRegex = Regex("<style[^>]*>[^<]*</style>")
fun noStyle(html: String): String = StyleRegex.replace(html, "")

const val Domain = "https://h5mota.com"
const val ListApi =
    "$Domain/backend/towers/list.php?colormode=%23&searchstr=&tags=[]"
const val DetailApi = "$Domain/backend/tower/mock_tower.php"

class MotaApi {
    enum class SortMode(val key: String, val displayName: String) {
        Hot("hot", "热度"),
        New("new", "最新"),
        Play("play", "最多人玩"),
        Comment("comment", "最多评论"),
        LastComment("lastcomment", "最新评论"),
        Pass("pass", "最多通关"),
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(contentType = ContentType.Any, json = json)
        }
    }

    private fun normalizeTower(tower: Tower): Tower =
        tower.copy(
            image = normalizeUrl(tower.image),
            link = normalizeUrl(tower.link),
            text = Html.fromHtml(noStyle(tower.text), Html.FROM_HTML_MODE_LEGACY)
                .toString(),
        )

    suspend fun list(ctx: Context, sortMode: SortMode, page: Int): TowerResponse {
        val response =
            client.get("$ListApi&sortmode=${sortMode.key}&page=$page")
        val towerResponse = response.body() as TowerResponse
        val ret = towerResponse.copy(towers = towerResponse.towers.map { normalizeTower(it) })
        TowerRepo(ctx).persistTowers(ret.towers)
        return ret
    }

    suspend fun details(ctx: Context, name: String): TowerDetails {
        val response = client.get("$DetailApi?name=$name")
        val json = response.bodyAsText()
        val root = this.json.parseToJsonElement(json).jsonObject
        val ret = TowerDetails(
            rating = root["rating"]!!
                .jsonObject["difficultyrating"]!!
                .jsonArray.toList()
                .drop(1)
                .map { it.jsonPrimitive.int }
                .reversed(),
            comments = root["comments"]!!.jsonArray.toList()
                .map {
                    fun parse(obj: JsonElement): Comment {
                        val comment = obj.jsonObject
                        val author = comment["author"]!!.jsonObject["name"]!!.jsonPrimitive.content
                        return Comment(
                            comment = comment["comment"]!!.jsonPrimitive.content,
                            author = author,
                            authorAvatar =
                            normalizeUrl(comment["author"]!!.jsonObject["avatar"]!!.jsonPrimitive.content.takeIf { it.isNotBlank() }
                                ?: "https://ui-avatars.com/api/?name=${author}"),
                            timestamp = comment["timestamp"]!!.jsonPrimitive.long,
                            like = comment["loveNum"]!!.jsonPrimitive.int,
                            replies = if (comment.containsKey("replies"))
                                comment["replies"]!!.jsonArray.map { reply -> parse(reply) }
                            else emptyList()
                        )
                    }
                    return@map parse(it)
                }
        )
        TowerRepo(ctx).persistTowerDetails(name, ret)
        return ret
    }

    companion object {
        fun normalizeUrl(url: String): String =
            if (url.startsWith("http")) url else "$Domain/$url"
    }
}


fun Tower.downloadUrl(): String {
    return "$Domain/games/$name/$name.zip"
}

fun Tower.pageUrl(): String {
    return "$Domain/tower?name=$name"
}
