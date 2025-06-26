package ru.netology.nmedia.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PostRepositoryFileImpl(private val context: Context) : PostRepository {


    private var posts = emptyList<Post>()
        set(value) {
            field = value
            data.value = posts
            sync()
        }
    private var indexId = 1L
    private val data = MutableLiveData(posts)

    init {
        val file = context.filesDir.resolve(FILENAME)
        if (file.exists()) {
            context.openFileInput(FILENAME).bufferedReader().use { reader ->
                posts = gson.fromJson(reader, type)
                indexId = (posts.maxOfOrNull { it.id } ?: 0) + 1
            }
        }
    }

    override fun getAll(): LiveData<List<Post>> = data

    override fun likeById(id: Long) {
        posts = posts.map {
            if (it.id != id) it else {
                it.copy(
                    likedByMe = !it.likedByMe,
                    likes = if (it.likedByMe) it.likes - 1 else it.likes + 1
                )
            }
        }
    }

    override fun shareById(id: Long) {
        posts = posts.map {
            if (it.id == id) {
                it.copy(share = it.share + 1)
            } else it
        }
    }

    override fun removeById(id: Long) {
        posts = posts.filter { it.id != id }
    }

    override fun save(post: Post) {
        if (post.id == 0L) {
            posts = listOf(
                post.copy(
                    id = indexId++,
                    author = "Me",
                    published = "Now",
                )
            ) + posts
            return
        }
        posts = posts.map {
            if (it.id != post.id) it else it.copy(content = post.content)
        }
    }

    private fun sync() {
        context.openFileOutput(
            FILENAME, Context.MODE_PRIVATE
        ).bufferedWriter().use { writer ->
            writer.write(gson.toJson(posts, type))
        }
    }

    companion object {
        private const val FILENAME = "post.json"
        private val gson = Gson()
        private val type = TypeToken.getParameterized(
            List::class.java, Post::class.java
        ).type
    }
}