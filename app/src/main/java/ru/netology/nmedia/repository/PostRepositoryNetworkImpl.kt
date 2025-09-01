package ru.netology.nmedia.repository


import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dto.Post

class PostRepositoryNetworkImpl : PostRepository {
    private var draftContent = ""

    override fun getAllAsync(callback: PostRepository.PostCallback<List<Post>>) {
        PostApi.service.getAll()
            .enqueue(object : Callback<List<Post>> {
                override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException(response.message()))
                        return
                    }
                    callback.onSuccess(response.body() ?: throw RuntimeException("Body is null"))
                }

                override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                    callback.onError(t)
                }
            })
    }


    override fun likeByIdAsync(id: Long, callback: PostRepository.PostCallback<Post>) {
        PostApi.service.likeById(id)
            .enqueue(object : Callback<Post> {
                override fun onResponse(call: Call<Post>, response: Response<Post>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException("Ощибка добавления Like"))
                        return
                    }
                    callback.onSuccess(response.body() ?: throw RuntimeException("Body is null"))
                }

                override fun onFailure(call: Call<Post>, t: Throwable) {
                    callback.onError(t)
                }
            })
    }


    override fun dislikeByIdAsync(id: Long, callback: PostRepository.PostCallback<Post>) {
        PostApi.service.dislikeById(id)
            .enqueue(object : Callback<Post> {
                override fun onResponse(call: Call<Post>, response: Response<Post>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException("Ощибка удаления Like"))
                        return
                    }
                    callback.onSuccess(response.body() ?: throw RuntimeException("Body is null"))
                }

                override fun onFailure(call: Call<Post>, t: Throwable) {
                    callback.onError(t)
                }
            })
    }

    override fun shareById(id: Long) {
        //сервер не принемает request на изменение счётчика share
        //в теле request нет поля share, нет смысла отправлять запрос на редактирование поста
    }

    override fun removeBiIdAsync(id: Long, callback: PostRepository.PostCallback<Unit>) {
        PostApi.service.removeById(id)
            .enqueue(object : Callback<Unit> {


                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException("Ощибка удаленния"))
                        return
                    }
                    callback.onSuccess(Unit)
                }

                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    callback.onError(t)
                }
            })
    }

    override fun saveAsync(post: Post, callback: PostRepository.PostCallback<Post>) {
        PostApi.service.save(post)
            .enqueue(object : Callback<Post> {
                override fun onResponse(call: Call<Post>, response: Response<Post>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException("Ощибка добавления поста"))
                        return
                    }
                    callback.onSuccess(response.body() ?: throw RuntimeException("Body is null"))
                }

                override fun onFailure(call: Call<Post>, t: Throwable) {
                    callback.onError(t)
                }
            })
    }

    override fun getDraft() = draftContent

    override fun setDraft(content: String) {
        draftContent = content
    }
}