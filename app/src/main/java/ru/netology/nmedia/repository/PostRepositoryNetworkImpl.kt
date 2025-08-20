package ru.netology.nmedia.repository


import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import ru.netology.nmedia.dto.Post
import java.util.concurrent.TimeUnit

class PostRepositoryNetworkImpl : PostRepository {
    private var draftContent = ""
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val typeToken = object : TypeToken<List<Post>>() {}

    private companion object {
        private const val BASE_URL = "http://10.0.2.2:9999"
        private val jsonType = "application/json".toMediaType()
    }

    override fun getAll(): List<Post> {
        val request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .build()

        return okHttpClient.newCall(request)
            .execute()
            .let {
                it.body?.string() ?: throw RuntimeException("body is null")
                //println(it)
            }
            .let { gson.fromJson(it, typeToken.type) }
    }

    override fun getAllAsync(callback: PostRepository.GetAllCallback) {
        val request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .build()

        okHttpClient.newCall(request)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: throw RuntimeException("body is null")
                    try {
                        callback.onSuccess(gson.fromJson(body, typeToken.type))
                    } catch (e: Exception) {
                        callback.onError(e)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
                }
            })
    }

    override fun likeById(id: Long): Post {
        val request = Request.Builder()
            .post(RequestBody.EMPTY)
            .url("${BASE_URL}/api/slow/posts/${id}/likes")
            .build()

        return okHttpClient.newCall(request)
            .execute()
            .let { it.body?.string() ?: throw RuntimeException("body is null") }
            .let { gson.fromJson(it, Post::class.java) }
    }

    override fun likeByIdAsync(id: Long, callback: PostRepository.PostBodyCallback) {
        val request = Request.Builder()
            .post(RequestBody.EMPTY)
            .url("${BASE_URL}/api/slow/posts/${id}/likes")
            .build()

        okHttpClient.newCall(request)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: throw RuntimeException("body is null")
                    if (response.isSuccessful) {
                        callback.onSuccess(gson.fromJson(body, Post::class.java))
                    } else {
                        callback.onError(RuntimeException("Ощибка добавления Like"))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
                }
            })
    }

    override fun dislikeById(id: Long): Post {
        val request = Request.Builder()
            .delete()
            .url("${BASE_URL}/api/slow/posts/$id/likes")
            .build()

        return okHttpClient.newCall(request)
            .execute()
            .let { it.body?.string() ?: throw RuntimeException("body is null") }
            .let { gson.fromJson(it, Post::class.java) }
    }

    override fun dislikeByIdAsync(id: Long, callback: PostRepository.PostBodyCallback) {
        val request = Request.Builder()
            .delete()
            .url("${BASE_URL}/api/slow/posts/$id/likes")
            .build()

        okHttpClient.newCall(request)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: throw RuntimeException("body is null")
                    if (response.isSuccessful) {
                        callback.onSuccess(gson.fromJson(body, Post::class.java))
                    } else {
                        callback.onError(RuntimeException("Ощибка удаления Like"))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
                }
            })
    }

    override fun shareById(id: Long) {
        //сервер не принемает request на изменение счётчика share
        //в теле request нет поля share, нет смысла отправлять запрос на редактирование поста
    }

    override fun removeById(id: Long) {
        val request = Request.Builder()
            .delete()
            .url("${BASE_URL}/api/slow/posts/$id")
            .build()

        okHttpClient.newCall(request)
            .execute()
            .close()
    }

    override fun removeBiIdAsync(id: Long, callback: PostRepository.EmptyBodyCallback) {
        val request = Request.Builder()
            .delete()
            .url("${BASE_URL}/api/slow/posts/$id")
            .build()

        okHttpClient.newCall(request)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        callback.onSuccess()
                    } else {
                        callback.onError(RuntimeException("Ощибка удаленния"))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
                }


            })
    }

    override fun save(post: Post) {
        val request = Request.Builder()
            .post(gson.toJson(post).toRequestBody(jsonType))
            .url("${BASE_URL}/api/slow/posts")
            .build()

        okHttpClient.newCall(request)
            .execute()
            .close()
    }

    override fun saveAsync(post: Post, callback: PostRepository.PostBodyCallback) {
        val request = Request.Builder()
            .post(gson.toJson(post).toRequestBody(jsonType))
            .url("${BASE_URL}/api/slow/posts")
            .build()

        okHttpClient.newCall(request)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: throw RuntimeException("body is null")
                    if (response.isSuccessful) {
                        callback.onSuccess(gson.fromJson(body, Post::class.java))
                    } else {
                        callback.onError(RuntimeException("Ощибка добавления поста"))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
                }
            })
    }

    override fun getDraft() = draftContent

    override fun setDraft(content: String) {
        draftContent = content
    }
}