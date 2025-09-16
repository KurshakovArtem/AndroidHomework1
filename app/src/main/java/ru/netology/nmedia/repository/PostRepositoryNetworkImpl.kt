package ru.netology.nmedia.repository


import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity

class PostRepositoryNetworkImpl(private val dao: PostDao) : PostRepository {
    private var draftContent = ""

    override val data: LiveData<List<Post>> = dao.getAll().map(List<PostEntity>::toDto)


    override suspend fun getAllAsync() {
        withContext(Dispatchers.IO + SupervisorJob()) {
            try {
                val syncServerTry = data.value.orEmpty().map { post ->
                    async {
                        try {
                            if (!post.syncServerState) {
                                retrySaveAsync(post)
                            }
                        } catch (_: RuntimeException) {
                            println("+1 необновлённый пост")
                        }
                    }
                }
                syncServerTry.awaitAll()

                val posts = try {
                    PostApi.service.getAll().map { post ->
                        // Не обновляем с сервера посты, которые отредактированны, но не синхронезированны с сервером
                        val postId = post.id
                        if (data.value.orEmpty().find { it.id == postId }?.syncServerState
                                ?: false
                        ) {
                            post.copy(syncServerState = true)
                        } else {
                            data.value.orEmpty().find { it.id == postId } ?: post.copy(
                                syncServerState = true
                            )
                        }
                    }
                } catch (_: Exception) {
                    println("Ошибка обновления постов")
                    throw RuntimeException()
                }
                dao.insert(posts.toEntity())
            } catch (_: RuntimeException) {
                throw RuntimeException("Server error")
            }
        }
    }

    override suspend fun saveAsync(post: Post): Post {
        val minimalId = if ((data.value?.minOfOrNull { it.id } ?: 0) >= 0) -1
        else (data.value?.minOfOrNull { it.id } ?: 0) - 1
        // Если новый пост, то id отрицательный
        // Если редактируем пост, то id оригинального поста
        val notSyncId = if (post.id != 0L) post.id else minimalId
        //val post1 = post.copy(id = notSyncId, syncServerState = false)
        dao.insert(PostEntity.fromDto(post.copy(id = notSyncId, syncServerState = false)))

        try {
            val postFromServer = PostApi.service.save(post).copy(syncServerState = true)
            dao.removeById(notSyncId)
            dao.insert(PostEntity.fromDto(postFromServer))
            return postFromServer
        } catch (_: Exception) {
            return dao.getPostById(notSyncId)?.toDto()
                ?: throw java.lang.RuntimeException("DB error")
        }

    }

    override suspend fun retrySaveAsync(post: Post): Post {
        try {
            val postFromServer = if (post.id < 0) {
                PostApi.service.save(post.copy(id = 0))
            } else {
                PostApi.service.save(post)
            }
            dao.removeById(post.id)
            dao.insert(PostEntity.fromDto(postFromServer.copy(syncServerState = true)))
            return postFromServer.copy(syncServerState = true)
        } catch (_: Exception) {
            throw RuntimeException("Server error")
        }
    }

    override suspend fun removeBiIdAsync(id: Long) {
        val oldPost = dao.getPostById(id)?.toDto() ?: throw RuntimeException("DB error")
        if (!oldPost.syncServerState && oldPost.id < 0) {
            dao.removeById(id)
            return
        }
        try {
            dao.removeById(id)
            PostApi.service.removeById(id)
        } catch (_: Exception) {
            dao.insert(PostEntity.fromDto(oldPost))
            throw RuntimeException("Server error")
        }
    }

    override suspend fun likeByIdAsync(id: Long) {
        val isLiked =
            dao.getPostById(id)?.toDto()?.likedByMe ?: throw RuntimeException("DB error")
        try {
            dao.likeById(id)
            val postFromServer =
                if (!isLiked) PostApi.service.likeById(id) else PostApi.service.dislikeById(id)
            dao.insert(PostEntity.fromDto(postFromServer.copy(syncServerState = true)))
        } catch (_: Exception) {
            dao.likeById(id)
            throw RuntimeException("Server error")
        }

    }

    override fun isEmpty() = dao.isEmpty()

    override fun shareById(id: Long) {
        //сервер не принемает request на изменение счётчика share
        //в теле request нет поля share, нет смысла отправлять запрос на редактирование поста
    }

    override fun getDraft() = draftContent

    override fun setDraft(content: String) {
        draftContent = content
    }

}