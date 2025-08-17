package ru.netology.nmedia.repository


import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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
            .let { it.body?.string() ?: throw RuntimeException("body is null")
            //println(it)
            }
            .let { gson.fromJson(it, typeToken.type) }
    }

    override fun likeById(id: Long): Post {
        val request = Request.Builder()
            .post(RequestBody.EMPTY)
            .url("${BASE_URL}/api/posts/${id}/likes")
            .build()

        return okHttpClient.newCall(request)
            .execute()
            .let { it.body?.string() ?: throw RuntimeException("body is null") }
            .let { gson.fromJson(it, Post::class.java) }
    }

    override fun dislikeById(id: Long): Post {
        val request = Request.Builder()
            .delete()
            .url("${BASE_URL}/api/posts/$id/likes")
            .build()

        return okHttpClient.newCall(request)
            .execute()
            .let { it.body?.string() ?: throw RuntimeException("body is null") }
            .let { gson.fromJson(it, Post::class.java) }
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

    override fun save(post: Post) {
        val request = Request.Builder()
            .post(gson.toJson(post).toRequestBody(jsonType))
            .url("${BASE_URL}/api/slow/posts")
            .build()

        okHttpClient.newCall(request)
            .execute()
            .close()
    }

    override fun getDraft() = draftContent

    override fun setDraft(content: String) {
        draftContent = content
    }
}