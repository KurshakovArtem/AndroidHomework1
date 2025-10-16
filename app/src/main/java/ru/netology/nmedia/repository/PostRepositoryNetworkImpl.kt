package ru.netology.nmedia.repository


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.AppError

class PostRepositoryNetworkImpl(private val dao: PostDao) : PostRepository {
    private var draftContent = ""

    override val data = dao.getAll()
        .map(List<PostEntity>::toDto)
        .flowOn(Dispatchers.Default)


    override suspend fun getAllAsync() {
        withContext(Dispatchers.IO + SupervisorJob()) {
            try {
                dao.setAllVisible()
                val flowPosts = data.firstOrNull().orEmpty()
                val syncServerTry = flowPosts.map { post ->
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
                        if (flowPosts.find { it.id == postId }?.syncServerState
                                ?: false
                        ) {
                            post.copy(syncServerState = true, isVisible = true)
                        } else {
                            flowPosts.find { it.id == postId } ?: post.copy(
                                syncServerState = true,
                                isVisible = true
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

    override fun getNewerCount(): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            val lastId = getLastId().first()
            val newerPosts = PostApi.service.getNewer(lastId).map {
                it.copy(syncServerState = true, isVisible = false)
            }
            dao.insert(newerPosts.toEntity())
            emit(getInvisibleList().first())
        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)

    override suspend fun updateNewerToOld() {
        dao.setAllVisible()
    }

    override suspend fun saveAsync(post: Post): Post {
        val flowPosts = data.firstOrNull().orEmpty()
        val minimalId = if ((flowPosts.minOfOrNull { it.id } ?: 0) >= 0) -1
        else (flowPosts.minOfOrNull { it.id } ?: 0) - 1
        // Если новый пост, то id отрицательный
        // Если редактируем пост, то id оригинального поста
        val notSyncId = if (post.id != 0L) post.id else minimalId
        //val post1 = post.copy(id = notSyncId, syncServerState = false)
        dao.insert(
            PostEntity.fromDto(
                post.copy(
                    id = notSyncId,
                    syncServerState = false,
                    isVisible = true
                )
            )
        )

        try {
            val postFromServer =
                PostApi.service.save(post).copy(syncServerState = true, isVisible = true)
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
            dao.setAllVisible()
            val postFromServer = if (post.id < 0) {
                PostApi.service.save(post.copy(id = 0))
            } else {
                PostApi.service.save(post)
            }
            dao.removeById(post.id)
            dao.insert(
                PostEntity.fromDto(
                    postFromServer.copy(
                        syncServerState = true,
                        isVisible = true
                    )
                )
            )
            return postFromServer.copy(syncServerState = true, isVisible = true)
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
            if (!isLiked) PostApi.service.likeById(id) else PostApi.service.dislikeById(id)
        } catch (_: Exception) {
            dao.likeById(id)
            throw RuntimeException("Server error")
        }

    }
    private fun getLastId(): Flow<Long> =
        dao.getAllInvisibleAndVisible()
            .map(List<PostEntity>::toDto)
            .map { posts ->
                posts.firstOrNull { it.id > 0 }?.id ?: 0L
            }

    private fun getInvisibleList(): Flow<Int> =
        dao.getAllInvisibleAndVisible()
            .map(List<PostEntity>::toDto)
            .map{ posts ->
                posts.filter { !it.isVisible }.size
            }

    override fun shareById(id: Long) {
        //сервер не принемает request на изменение счётчика share
        //в теле request нет поля share, нет смысла отправлять запрос на редактирование поста
    }

    override fun getDraft() = draftContent

    override fun setDraft(content: String) {
        draftContent = content
    }

}