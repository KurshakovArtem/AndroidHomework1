package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Post
import kotlin.Long

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val author: String,
    val content: String,
    val published: String,
    val likes: Int = 0,
    val likedByMe: Boolean = false,
    val share: Int = 0,
    val postViews: Int = 0,
    //val videoUrl: String? = null
) {
    fun toDto() = Post(
        id = id,
        author = author,
        content = content,
        published = published,
        likes = likes,
        likedByMe = likedByMe,
        share = share,
        postViews = postViews,
        //videoUrl = videoUrl
    )

    companion object {
        fun fromDto(dto: Post) = PostEntity(
            id = dto.id,
            author = dto.author,
            content = dto.content,
            published = dto.published,
            likes = dto.likes,
            likedByMe = dto.likedByMe,
            share = dto.share,
            postViews = dto.postViews,
            //videoUrl = dto.videoUrl
        )
    }
}