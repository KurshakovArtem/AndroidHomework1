package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post
import java.io.File

interface PostRepository {
    val data: Flow<List<Post>>
    fun shareById(id: Long)
    fun getDraft(): String
    fun setDraft(content: String)
    suspend fun getAllAsync()
    fun getNewerCount(): Flow<Int>
    suspend fun updateNewerToOld()
    suspend fun removeBiIdAsync(id: Long)
    suspend fun saveAsync(post: Post, photo: File?): Post
    suspend fun retrySaveAsync(post: Post): Post
    suspend fun likeByIdAsync(id: Long)

}