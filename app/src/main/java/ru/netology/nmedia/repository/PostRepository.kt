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
    fun getAllAsync(callback: PostCallback<List<Post>>)
    fun removeBiIdAsync(id: Long, callback: PostCallback<Unit>)
    fun saveAsync(post: Post, callback: PostCallback<Post>)
    fun likeByIdAsync(id: Long, callback: PostCallback<Post>)
    fun dislikeByIdAsync(id: Long, callback: PostCallback<Post>)

    interface PostCallback<T> {
        fun onSuccess(result: T)
        fun onError(e: Exception)
    }
}