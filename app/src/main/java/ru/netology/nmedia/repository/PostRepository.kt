package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun shareById(id: Long)
    fun getDraft(): String
    fun setDraft(content: String)
    fun getAllAsync(callback: PostCallback<List<Post>>)
    fun removeBiIdAsync(id: Long, callback: PostCallback<Unit>)
    fun saveAsync(post: Post, callback: PostCallback<Post>)
    fun likeByIdAsync(id: Long, callback: PostCallback<Post>)
    fun dislikeByIdAsync(id: Long, callback: PostCallback<Post>)

    interface PostCallback<T> {
        fun onSuccess(result: T)
        fun onError(e: Throwable)
    }
}