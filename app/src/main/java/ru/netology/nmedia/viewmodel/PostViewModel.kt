package ru.netology.nmedia.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryNetworkImpl
import ru.netology.nmedia.supportingFunctions.SingleLiveEvent


private val empty = Post(
    id = 0,
    author = "",
    published = "",
    content = "",
    likes = 0,
    likedByMe = false
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = PostRepositoryNetworkImpl()
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel>
        get() = _data
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPosts()
    }

    fun loadPosts() {
        _data.postValue(FeedModel(loading = true))
        repository.getAllAsync(object : PostRepository.PostCallback<List<Post>> {
            override fun onSuccess(result: List<Post>) {
                _data.postValue(FeedModel(posts = result, empty = result.isEmpty()))
            }

            override fun onError(e: Exception) {
                _data.postValue(FeedModel(error = true))
            }
        })
    }

    fun likeById(id: Long) {
        val isLiked = data.value?.posts?.find { it.id == id }?.likedByMe ?: return
        if (!isLiked) {
            repository.likeByIdAsync(id, object : PostRepository.PostCallback<Post> {
                override fun onSuccess(result: Post) {
                    _data.postValue(
                        _data.value?.copy(
                            posts = _data.value?.posts.orEmpty().map {
                                if (it.id == id) {
                                    result
                                } else it
                            }
                        )
                    )
                }

                override fun onError(e: Exception) {
                    showErrorToast("Ощибка добавления Like")
                }
            })
        } else {
            repository.dislikeByIdAsync(id, object : PostRepository.PostCallback<Post> {
                override fun onSuccess(result: Post) {
                    _data.postValue(
                        _data.value?.copy(
                            posts = _data.value?.posts.orEmpty().map {
                                if (it.id == id) {
                                    result
                                } else it
                            }
                        )
                    )
                }

                override fun onError(e: Exception) {
                    showErrorToast("Ощибка удаления Like")
                }
            })
        }
    }

    fun shareById(id: Long) = repository.shareById(id)


    fun removeById(id: Long) {
        val old = _data.value?.posts.orEmpty()
        _data.postValue(
            _data.value?.copy(
                posts = _data.value?.posts.orEmpty()
                    .filter { it.id != id }
            )
        )
        repository.removeBiIdAsync(id, object : PostRepository.PostCallback<Unit> {
            override fun onSuccess(result: Unit) {
                _data.postValue(
                    _data.value?.copy(
                        posts = _data.value?.posts.orEmpty().filter { it.id != id })
                )
            }

            override fun onError(e: Exception) {
                _data.postValue(_data.value?.copy(posts = old))
                showErrorToast("Не удалось удалить пост")
            }
        })
    }

    fun save(content: String) {
        edited.value?.let { editPost ->
            val text = content.trim()       //отсекает все пробелы в начале и конце

            if (text != editPost.content) {
                repository.saveAsync(
                    editPost.copy(content = text),
                    object : PostRepository.PostCallback<Post> {
                        override fun onSuccess(result: Post) {
                            if (editPost.id == 0L) {
                                val newListPosts = listOf(result) + _data.value?.posts.orEmpty()
                                _data.postValue(
                                    _data.value?.copy(
                                        posts = newListPosts
                                    )
                                )
                            } else {
                                val newListPosts = _data.value?.posts.orEmpty().map {
                                    if (it.id == result.id) {
                                        result
                                    } else it
                                }
                                _data.postValue(
                                    _data.value?.copy(
                                        posts = newListPosts
                                    )
                                )
                            }

                            _postCreated.postValue(Unit)
                        }

                        override fun onError(e: Exception) {
                            showErrorToast("Не удалось добавить пост")
                            _postCreated.postValue(Unit)
                        }

                    })
            }
        }
        if (edited.value?.id == 0L) repository.setDraft("")  // очищаем черновик
        edited.value = empty
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun cancelEdit() {
        edited.value = empty
    }

    fun createDraft(content: String) {
        if (edited.value?.id != 0L) {
            cancelEdit()
        } else {
            repository.setDraft(content)
        }
    }

    fun getDraft() = repository.getDraft()

    private fun showErrorToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(application, message, Toast.LENGTH_LONG).show()
        }
    }
}