//package ru.netology.nmedia.repository
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import ru.netology.nmedia.dao.PostDao
//import ru.netology.nmedia.dto.Post
//
//class PostRepositorySQLiteImpl(private val dao: PostDao) : PostRepository {
//
//    private var posts = emptyList<Post>()
//    private val data = MutableLiveData(posts)
//    private var draftContent = ""
//    init {
//        posts = dao.getAll()
//        data.value = posts
//    }
//
//    override fun getAll(): LiveData<List<Post>> = data
//
//    override fun likeById(id: Long) {
//        dao.likeById(id)
//        posts = posts.map {
//            if (it.id != id) it else {
//                it.copy(
//                    likedByMe = !it.likedByMe,
//                    likes = if (it.likedByMe) it.likes - 1 else it.likes + 1
//                )
//            }
//        }
//        data.value = posts
//    }
//
//    override fun shareById(id: Long) {
//        dao.sharedById(id)
//        posts = posts.map {
//            if (it.id == id) {
//                it.copy(share = it.share + 1)
//            } else it
//        }
//        data.value = posts
//    }
//
//    override fun removeById(id: Long) {
//        dao.removeById(id)
//        posts = posts.filter { it.id != id }
//        data.value = posts
//    }
//
//    override fun save(post: Post) {
//        val id = post.id
//        val saved = dao.save(post)
//        posts = if (id == 0L) {
//            listOf(saved) + posts
//        } else {
//            posts.map {
//                if (id != it.id) it else saved
//            }
//        }
//        data.value = posts
//    }
//
//    override fun getDraft() = draftContent
//
//    override fun setDraft(content: String) {
//        draftContent = content
//    }
//}