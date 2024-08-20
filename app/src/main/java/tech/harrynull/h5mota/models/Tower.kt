package tech.harrynull.h5mota.models

import kotlinx.serialization.Serializable


@Serializable
data class TowerResponse(
    val page: Int,
    val total: Int,
    val towers: List<Tower>
)

@Serializable
data class Tower(
    val id: Int, // numerical id
    val name: String, // string id
    val title: String, // title
    val author: String, // first author name
    val author2: String, // second author name
    val link: String, // game link
    val floors: Int, // number of floors
    val image: String, // cover image link
    val text: String, // description
    val tags: List<String>, // tags
    val people: Int, // number of people played
    val win: Int, // number of people won
    val difficultrate: String,
    val comment: String, // number of comments
    val hot: Int, // hot rating
    val thumb_up: Int, // thumb up rating

)

@Serializable
data class Comment(
    val comment: String,
    val author: String,
    val authorAvatar: String,
    val timestamp: Long,
    val like: Int, // loveNum
    val replies: List<Comment>,
)

@Serializable
data class DetailedResponse(
    val rating: List<Int>,
    val comments: List<Comment>
)
