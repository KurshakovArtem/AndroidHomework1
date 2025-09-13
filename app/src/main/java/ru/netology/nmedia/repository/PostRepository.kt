package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.dto.Post

interface PostRepository {
    val data: LiveData<List<Post>>
    fun shareById(id: Long)
    fun getDraft(): String
    fun setDraft(content: String)
    suspend fun getAllAsync()
    suspend fun removeBiIdAsync(id: Long)
    suspend fun saveAsync(post: Post): Post
    suspend fun likeByIdAsync(id: Long)
    fun isEmpty(): LiveData<Boolean>
}