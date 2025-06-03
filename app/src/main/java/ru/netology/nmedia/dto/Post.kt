package ru.netology.nmedia.dto

data class Post(
    val id: Long,
    val author: String,
    val content: String,
    val published: String,
    val likes: Int = 5,
    val likedByMe: Boolean = false,
    val share: Int = 10_999,
    val postViews: Int = 5
)
