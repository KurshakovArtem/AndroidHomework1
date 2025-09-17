package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.toAttachmentType

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val author: String,
    val authorAvatar: String,
    val content: String,
    val published: String,
    val likes: Int = 0,
    val likedByMe: Boolean = false,
    val share: Int = 0,
    val postViews: Int = 0,
    val syncServerState: Boolean = true,
    val attachment: Boolean = false,
    val attachmentUrl: String = "",
    val attachmentDescription: String = "",
    val attachmentType: String = "EMPTY",
) {
    fun toDto() = Post(
        id = id,
        author = author,
        authorAvatar = authorAvatar,
        content = content,
        published = published,
        likes = likes,
        likedByMe = likedByMe,
        share = share,
        postViews = postViews,
        syncServerState = syncServerState,
        attachment = if (!attachment) null else Attachment(
            url = attachmentUrl,
            description = attachmentDescription,
            type = attachmentType.toAttachmentType()
        )
    )

    companion object {
        fun fromDto(dto: Post) = PostEntity(
            id = dto.id,
            author = dto.author,
            authorAvatar = dto.authorAvatar,
            content = dto.content,
            published = dto.published,
            likes = dto.likes,
            likedByMe = dto.likedByMe,
            share = dto.share,
            postViews = dto.postViews,
            attachment = dto.attachment != null,
            syncServerState = dto.syncServerState,
            attachmentUrl = if (dto.attachment != null) dto.attachment.url else "",
            attachmentDescription = if (dto.attachment != null) dto.attachment.description else "",
            attachmentType = if (dto.attachment != null) dto.attachment.type.toString() else "EMPTY",
        )
    }
}

fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto)
fun List<Post>.toEntity(): List<PostEntity> = map(PostEntity::fromDto)