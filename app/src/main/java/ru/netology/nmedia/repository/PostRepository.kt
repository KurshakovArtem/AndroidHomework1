package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getAll(): List<Post>
    fun likeById(id: Long): Post
    fun dislikeById(id: Long): Post
    fun shareById(id: Long)
    fun removeById(id: Long)
    fun save(post: Post)
    fun getDraft(): String
    fun setDraft(content: String)
    fun getAllAsync(callback: GetAllCallback)
    fun removeBiIdAsync(id: Long, callback: EmptyBodyCallback)
    fun saveAsync(post: Post, callback: PostBodyCallback)
    fun likeByIdAsync(id: Long, callback: PostBodyCallback)
    fun dislikeByIdAsync(id: Long, callback: PostBodyCallback)

    interface GetAllCallback {
        fun onSuccess(posts: List<Post>) {}
        fun onError(e: Exception) {}
    }

    interface EmptyBodyCallback {
        fun onSuccess() {}
        fun onError(e: Exception) {}
    }

    interface PostBodyCallback {
        fun onSuccess(post: Post) {}
        fun onError(e: Exception) {}
    }
}