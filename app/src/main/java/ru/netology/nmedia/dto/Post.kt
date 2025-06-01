package ru.netology.nmedia.dto

data class Post(
    val id: Int,
    val author: String,
    val content: String,
    val published: String,
    var likes: Int = 1_099_999,
    var likedByMe: Boolean = false,
    var share: Int = 9_999,
    var postViews: Int = 5
)
