package ru.netology.nmedia.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post
import java.io.File

interface PostRepository {
    val data: Flow<PagingData<Post>>
    fun shareById(id: Long)
    fun getDraft(): String
    fun setDraft(content: String)
    suspend fun getAllAsync()
    //fun getNewerCount(): Flow<Int>
    suspend fun updateNewerToOld()
    suspend fun removeBiIdAsync(id: Long)
    suspend fun saveAsync(post: Post, photo: File?): Post
    suspend fun retrySaveAsync(post: Post): Post
    suspend fun likeByIdAsync(id: Long)
    suspend fun getPostById(id: Long): Post?

}