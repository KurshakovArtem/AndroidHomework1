package ru.netology.nmedia.repository


import androidx.lifecycle.LiveData
import androidx.lifecycle.map
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
        val posts = PostApi.service.getAll()
        dao.insert(posts.toEntity())
    }

    override suspend fun saveAsync(post: Post): Post {
        val postFromServer = PostApi.service.save(post)
        dao.save(PostEntity.fromDto(postFromServer))
        return postFromServer
    }

    override suspend fun removeBiIdAsync(id: Long) {
        val oldPost = dao.getPostById(id)?.toDto() ?: throw RuntimeException("DB error")
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
            dao.save(PostEntity.fromDto(postFromServer))
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
//    override fun getAllAsync(callback: PostRepository.PostCallback<List<Post>>) {
//        PostApi.service.getAll()
//            .enqueue(object : Callback<List<Post>> {
//                override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
//                    if (!response.isSuccessful) {
//                        callback.onError(RuntimeException(response.message()))
//                        return
//                    }
//                    callback.onSuccess(response.body() ?: throw RuntimeException("Body is null"))
//                }
//
//                override fun onFailure(call: Call<List<Post>>, t: Throwable) {
//                    callback.onError(t)
//                }
//            })
//    }
//
//
//    override fun likeByIdAsync(id: Long, callback: PostRepository.PostCallback<Post>) {
//        PostApi.service.likeById(id)
//            .enqueue(object : Callback<Post> {
//                override fun onResponse(call: Call<Post>, response: Response<Post>) {
//                    if (!response.isSuccessful) {
//                        callback.onError(RuntimeException("Ощибка добавления Like"))
//                        return
//                    }
//                    callback.onSuccess(response.body() ?: throw RuntimeException("Body is null"))
//                }
//
//                override fun onFailure(call: Call<Post>, t: Throwable) {
//                    callback.onError(t)
//                }
//            })
//    }
//
//
//    override fun dislikeByIdAsync(id: Long, callback: PostRepository.PostCallback<Post>) {
//        PostApi.service.dislikeById(id)
//            .enqueue(object : Callback<Post> {
//                override fun onResponse(call: Call<Post>, response: Response<Post>) {
//                    if (!response.isSuccessful) {
//                        callback.onError(RuntimeException("Ощибка удаления Like"))
//                        return
//                    }
//                    callback.onSuccess(response.body() ?: throw RuntimeException("Body is null"))
//                }
//
//                override fun onFailure(call: Call<Post>, t: Throwable) {
//                    callback.onError(t)
//                }
//            })
//    }


//    override fun removeBiIdAsync(id: Long, callback: PostRepository.PostCallback<Unit>) {
//        PostApi.service.removeById(id)
//            .enqueue(object : Callback<Unit> {
//
//
//                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
//                    if (!response.isSuccessful) {
//                        callback.onError(RuntimeException("Ощибка удаленния"))
//                        return
//                    }
//                    callback.onSuccess(Unit)
//                }
//
//                override fun onFailure(call: Call<Unit>, t: Throwable) {
//                    callback.onError(t)
//                }
//            })
//    }
//
//    override fun saveAsync(post: Post, callback: PostRepository.PostCallback<Post>) {
//        PostApi.service.save(post)
//            .enqueue(object : Callback<Post> {
//                override fun onResponse(call: Call<Post>, response: Response<Post>) {
//                    if (!response.isSuccessful) {
//                        callback.onError(RuntimeException("Ощибка добавления поста"))
//                        return
//                    }
//                    callback.onSuccess(response.body() ?: throw RuntimeException("Body is null"))
//                }
//
//                override fun onFailure(call: Call<Post>, t: Throwable) {
//                    callback.onError(t)
//                }
//            })
//    }

