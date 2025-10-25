package ru.netology.nmedia.dto

data class Post(
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String = "",
    val content: String,
    val published: String,
    val likedByMe: Boolean = false,
    val likes: Int = 0,
    val share: Int = 0,
    val postViews: Int = 0,
    val syncServerState: Boolean = true,
    val attachment: Attachment? = null,
    val isVisible: Boolean = true,
    val ownedByMe: Boolean = false
)
